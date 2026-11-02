package tutorlink.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.HOON;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.commons.util.JsonUtil;
import tutorlink.logic.Logic;
import tutorlink.logic.LogicManager;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.StudentDirectory;
import tutorlink.model.UserPrefs;

/**
 * Exercises real filesystem failures and cross-process locking without using production student data.
 */
public class JsonStudentDirectoryStorageSafetyTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void saveStudentDirectory_readOnlyFile_rollsBackAndAllowsRetryAfterPermissionRestored() throws Exception {
        Path filePath = temporaryFolder.resolve("read-only.json");
        StudentDirectory original = originalRoster();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);
        storage.saveStudentDirectory(original);
        assumeTrue(Files.getFileStore(filePath).supportsFileAttributeView("posix"), "POSIX permissions unavailable");
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(filePath);
        byte[] originalBytes = Files.readAllBytes(filePath);
        Model model = new ModelManager(storage.readStudentDirectory().orElseThrow(), new UserPrefs());
        Logic logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));

        try {
            Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("r--r--r--"));
            assumeTrue(!Files.isWritable(filePath), "This account can bypass the file's read-only permissions");

            assertThrows(CommandException.class, () -> logic.execute("edit 1 p/98765432"));

            assertEquals(original, model.getStudentDirectory());
            assertArrayEquals(originalBytes, Files.readAllBytes(filePath));
            assertEquals(PosixFilePermissions.fromString("r--r--r--"), Files.getPosixFilePermissions(filePath));
            assertNoTemporaryFiles();
        } finally {
            Files.setPosixFilePermissions(filePath, originalPermissions);
        }

        logic.execute("edit 1 p/98765432");
        assertEquals("98765432", model.getStudentDirectory().getStudentList().getFirst().getPhone().value);
        assertEquals(model.getStudentDirectory(), storage.readStudentDirectory().orElseThrow());
    }

    @Test
    public void saveStudentDirectory_writablePosixFile_preservesPermissions() throws Exception {
        Path filePath = temporaryFolder.resolve("permissions.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);
        storage.saveStudentDirectory(originalRoster());
        assumeTrue(Files.getFileStore(filePath).supportsFileAttributeView("posix"), "POSIX permissions unavailable");
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-r-----");
        Files.setPosixFilePermissions(filePath, permissions);
        StudentDirectory changed = originalRoster();
        changed.addStudent(HOON);

        storage.saveStudentDirectory(changed);

        assertEquals(permissions, Files.getPosixFilePermissions(filePath));
        assertEquals(changed, storage.readStudentDirectory().orElseThrow());
        assertNoTemporaryFiles();
    }

    @Test
    public void saveStudentDirectory_deletedSymbolicLinkTarget_rejectsSaveAndPreservesLink() throws Exception {
        Path target = temporaryFolder.resolve("target.json");
        Path alias = temporaryFolder.resolve("alias.json");
        new JsonStudentDirectoryStorage(target).saveStudentDirectory(originalRoster());
        try {
            Files.createSymbolicLink(alias, target);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e.getMessage());
        }
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(alias);
        storage.readStudentDirectory();
        Files.delete(target);

        assertThrows(IOException.class, () -> storage.saveStudentDirectory(new StudentDirectory()));

        assertTrue(Files.isSymbolicLink(alias));
        assertEquals(target, Files.readSymbolicLink(alias));
        assertFalse(Files.exists(target));
        assertNoTemporaryFiles();
    }

    @Test
    public void saveStudentDirectory_otherProcessHoldsLock_rejectsSaveAndAllowsRetryAfterRelease() throws Exception {
        Path filePath = temporaryFolder.resolve("process-lock.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);
        storage.saveStudentDirectory(originalRoster());
        byte[] originalBytes = Files.readAllBytes(filePath);
        StudentDirectory changed = originalRoster();
        changed.addStudent(HOON);
        Path readyPath = temporaryFolder.resolve("lock-ready");
        Path releasePath = temporaryFolder.resolve("release-lock");
        Path logPath = temporaryFolder.resolve("lock-holder.log");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path javaPath = Path.of(System.getProperty("java.home"), "bin", executable);
        Path classesPath = Path.of(LockHolder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(javaPath.toString(), "-cp", classesPath.toString(),
                LockHolder.class.getName(), filePath.resolveSibling("process-lock.json.lock").toString(),
                readyPath.toString(), releasePath.toString()).redirectErrorStream(true)
                .redirectOutput(logPath.toFile()).start();

        try {
            waitForLockHolder(process, readyPath, logPath);

            assertThrows(IOException.class, JsonStudentDirectoryStorage.SAVE_IN_PROGRESS_MESSAGE, () ->
                    storage.saveStudentDirectory(changed));

            assertArrayEquals(originalBytes, Files.readAllBytes(filePath));
            assertNoTemporaryFiles();
            Files.writeString(releasePath, "release");
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Lock holder did not exit after release");
            assertEquals(0, process.exitValue(), Files.readString(logPath));

            storage.saveStudentDirectory(changed);
            assertEquals(changed, storage.readStudentDirectory().orElseThrow());
        } finally {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Lock holder could not be terminated");
            }
        }
    }

    @Test
    public void saveStudentDirectory_atomicMoveFails_preservesOriginalCleansTemporaryFileAndAllowsRetry()
            throws Exception {
        Path filePath = temporaryFolder.resolve("move-failure.json");
        new JsonStudentDirectoryStorage(filePath).saveStudentDirectory(originalRoster());
        byte[] originalBytes = Files.readAllBytes(filePath);
        StudentDirectory changed = originalRoster();
        changed.addStudent(HOON);
        String expectedJson = JsonUtil.toJsonString(new JsonSerializableStudentDirectory(changed));
        AtomicBoolean isReplacementBlocked = new AtomicBoolean(true);
        List<Path> temporaryPaths = new ArrayList<>();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath) {
            @Override
            void replaceFile(Path temporaryPath, Path destinationPath) throws IOException {
                temporaryPaths.add(temporaryPath);
                assertTrue(Files.isRegularFile(temporaryPath));
                assertEquals(expectedJson, Files.readString(temporaryPath));
                if (isReplacementBlocked.get()) {
                    throw new AtomicMoveNotSupportedException(temporaryPath.toString(), destinationPath.toString(),
                            "Simulated unsupported atomic replacement");
                }
                super.replaceFile(temporaryPath, destinationPath);
            }
        };
        storage.readStudentDirectory();

        assertThrows(AtomicMoveNotSupportedException.class, () -> storage.saveStudentDirectory(changed));

        assertArrayEquals(originalBytes, Files.readAllBytes(filePath));
        assertEquals(1, temporaryPaths.size());
        assertFalse(Files.exists(temporaryPaths.getFirst()));
        assertNoTemporaryFiles();

        isReplacementBlocked.set(false);
        storage.saveStudentDirectory(changed);

        assertEquals(changed, storage.readStudentDirectory().orElseThrow());
        assertEquals(2, temporaryPaths.size());
        assertFalse(Files.exists(temporaryPaths.getLast()));
        assertNoTemporaryFiles();
    }

    private StudentDirectory originalRoster() {
        StudentDirectory roster = new StudentDirectory();
        roster.addStudent(ALICE);
        return roster;
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(temporaryFolder)) {
            assertEquals(0, files.filter(path -> path.getFileName().toString().startsWith("tutorlink-")
                    && path.getFileName().toString().endsWith(".tmp")).count());
        }
    }

    private void waitForLockHolder(Process process, Path readyPath, Path logPath) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!Files.exists(readyPath) && process.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(Files.exists(readyPath), "Lock holder did not become ready: " + Files.readString(logPath));
        assertTrue(process.isAlive(), "Lock holder exited before the save attempt: " + Files.readString(logPath));
    }

    /**
     * Holds an operating-system file lock in an independent JVM for the cross-process regression test.
     */
    public static class LockHolder {
        /**
         * Locks the given sidecar file until the release marker appears or a bounded deadline expires.
         */
        public static void main(String[] args) throws Exception {
            Path lockPath = Path.of(args[0]);
            Path readyPath = Path.of(args[1]);
            Path releasePath = Path.of(args[2]);
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE);
                    FileLock lock = channel.lock()) {
                if (!lock.isValid()) {
                    throw new IOException("Child process did not acquire a valid file lock");
                }
                Files.writeString(readyPath, "ready");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                while (!Files.exists(releasePath) && System.nanoTime() < deadline) {
                    Thread.sleep(20);
                }
                if (!Files.exists(releasePath)) {
                    throw new IOException("Timed out waiting for the parent to release the file lock");
                }
            }
        }
    }
}
