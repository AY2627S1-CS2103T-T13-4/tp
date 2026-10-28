package tutorlink.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.HOON;
import static tutorlink.testutil.TypicalStudents.IDA;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.commons.exceptions.DataLoadingException;
import tutorlink.model.ReadOnlyStudentDirectory;
import tutorlink.model.StudentDirectory;
import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;

public class JsonStudentDirectoryStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonStudentDirectoryStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readStudentDirectory_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readStudentDirectory(null));
    }

    private java.util.Optional<ReadOnlyStudentDirectory> readStudentDirectory(String filePath) throws Exception {
        return new JsonStudentDirectoryStorage(Paths.get(filePath))
                .readStudentDirectory(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readStudentDirectory("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readStudentDirectory("notJsonFormat.json"));
    }

    @Test
    public void readStudentDirectory_invalidStudentFile_throwsDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readStudentDirectory("invalidStudent.json"));
    }

    @Test
    public void readStudentDirectory_invalidAndValidStudentsFile_throwsDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readStudentDirectory("invalidAndValidStudents.json"));
    }

    @Test
    public void readStudentDirectory_nullOrMissingStudents_preservesInvalidFile() throws Exception {
        Path filePath = testFolder.resolve("invalid-structure.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);
        for (String content : List.of("{}", "{\"students\":null}", "{\"students\":[null]}")) {
            Files.writeString(filePath, content);
            assertThrows(DataLoadingException.class, storage::readStudentDirectory);
            assertEquals(content, Files.readString(filePath));
        }
    }

    @Test
    public void readAndSaveStudentDirectory_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempStudentDirectory.json");
        StudentDirectory original = getTypicalStudentDirectory();
        JsonStudentDirectoryStorage jsonStudentDirectoryStorage = new JsonStudentDirectoryStorage(filePath);

        // Save in new file and read back
        jsonStudentDirectoryStorage.saveStudentDirectory(original, filePath);
        String savedJson = Files.readString(filePath);
        assertTrue(savedJson.contains("\"students\""));
        ReadOnlyStudentDirectory readBack = jsonStudentDirectoryStorage.readStudentDirectory(filePath).get();
        assertEquals(original, new StudentDirectory(readBack));

        // Modify data, overwrite existing file, and read back
        original.addStudent(HOON);
        original.removeStudent(ALICE);
        jsonStudentDirectoryStorage.saveStudentDirectory(original, filePath);
        readBack = jsonStudentDirectoryStorage.readStudentDirectory(filePath).get();
        assertEquals(original, new StudentDirectory(readBack));

        // Save and read without specifying file path
        original.addStudent(IDA);
        jsonStudentDirectoryStorage.saveStudentDirectory(original); // file path not specified
        readBack = jsonStudentDirectoryStorage.readStudentDirectory().get(); // file path not specified
        assertEquals(original, new StudentDirectory(readBack));

    }

    @Test
    public void readAndSaveStudentDirectory_missingOptionalDetails_roundTrips() throws Exception {
        Path filePath = testFolder.resolve("OptionalStudent.json");
        StudentDirectory original = new StudentDirectory();
        Student student = new Student(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("W12"), null, null, null, Set.of());
        original.addStudent(student);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);

        storage.saveStudentDirectory(original);
        assertEquals(original, new StudentDirectory(storage.readStudentDirectory().get()));
    }

    @Test
    public void readAndSaveStudentDirectory_existingRemark_roundTrips() throws Exception {
        Path filePath = testFolder.resolve("StudentWithRemark.json");
        StudentDirectory original = new StudentDirectory();
        Student student = new Student(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("W12"), null, null, null, Set.of(), "Needs help with recursion");
        original.addStudent(student);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);

        storage.saveStudentDirectory(original);
        ReadOnlyStudentDirectory readBack = storage.readStudentDirectory().get();
        assertEquals("Needs help with recursion", readBack.getStudentList().get(0).getRemark());
        assertEquals(original, new StudentDirectory(readBack));
    }

    @Test
    public void saveStudentDirectory_nullStudentDirectory_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveStudentDirectory(null, "SomeFile.json"));
    }

    /**
     * Saves {@code studentDirectory} at the specified {@code filePath}.
     */
    private void saveStudentDirectory(ReadOnlyStudentDirectory studentDirectory, String filePath) {
        try {
            new JsonStudentDirectoryStorage(Paths.get(filePath))
                    .saveStudentDirectory(studentDirectory, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveStudentDirectory_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveStudentDirectory(new StudentDirectory(), null));
    }

    @Test
    public void saveStudentDirectory_staleInstance_rejectsRepeatedSavesUntilReload() throws Exception {
        Path filePath = testFolder.resolve("shared.json");
        JsonStudentDirectoryStorage first = new JsonStudentDirectoryStorage(filePath);
        first.saveStudentDirectory(new StudentDirectory());
        JsonStudentDirectoryStorage second = new JsonStudentDirectoryStorage(filePath);
        second.readStudentDirectory();

        first.saveStudentDirectory(getTypicalStudentDirectory());
        String saved = Files.readString(filePath);
        for (int i = 0; i < 2; i++) {
            assertThrows(IOException.class, JsonStudentDirectoryStorage.DATA_CHANGED_MESSAGE, () ->
                    second.saveStudentDirectory(new StudentDirectory()));
            assertEquals(saved, Files.readString(filePath));
        }

        StudentDirectory reloaded = new StudentDirectory(second.readStudentDirectory().orElseThrow());
        reloaded.addStudent(HOON);
        second.saveStudentDirectory(reloaded);
        assertEquals(reloaded, new StudentDirectory(first.readStudentDirectory().orElseThrow()));
    }

    @Test
    public void saveStudentDirectory_fileCreatedDeletedOrEdited_rejectsUnexpectedChanges() throws Exception {
        Path filePath = testFolder.resolve("external.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);
        storage.readStudentDirectory();
        Files.writeString(filePath, "{\"students\":[]}");
        assertThrows(IOException.class, JsonStudentDirectoryStorage.DATA_CHANGED_MESSAGE, () ->
                storage.saveStudentDirectory(getTypicalStudentDirectory()));

        storage.readStudentDirectory();
        Files.delete(filePath);
        assertThrows(IOException.class, JsonStudentDirectoryStorage.DATA_CHANGED_MESSAGE, () ->
                storage.saveStudentDirectory(getTypicalStudentDirectory()));
        assertFalse(Files.exists(filePath));

        storage.readStudentDirectory();
        storage.saveStudentDirectory(getTypicalStudentDirectory());
        String edited = Files.readString(filePath) + "\n";
        Files.writeString(filePath, edited);
        assertThrows(IOException.class, JsonStudentDirectoryStorage.DATA_CHANGED_MESSAGE, () ->
                storage.saveStudentDirectory(new StudentDirectory()));
        assertEquals(edited, Files.readString(filePath));
    }

    @Test
    public void saveStudentDirectory_existingFileWithoutRead_rejectsOverwrite() throws Exception {
        Path filePath = testFolder.resolve("unread.json");
        Files.writeString(filePath, "{\"students\":[]}");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);

        assertThrows(IOException.class, JsonStudentDirectoryStorage.DATA_CHANGED_MESSAGE, () ->
                storage.saveStudentDirectory(getTypicalStudentDirectory()));
        assertEquals("{\"students\":[]}", Files.readString(filePath));
    }

    @Test
    public void saveStudentDirectory_lockHeld_rejectsSaveAndAllowsRetry() throws Exception {
        Path filePath = testFolder.resolve("locked.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);
        storage.saveStudentDirectory(new StudentDirectory());
        String saved = Files.readString(filePath);

        try (FileChannel channel = FileChannel.open(testFolder.resolve("locked.json.lock"), StandardOpenOption.WRITE);
                FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            assertThrows(IOException.class, JsonStudentDirectoryStorage.SAVE_IN_PROGRESS_MESSAGE, () ->
                    storage.saveStudentDirectory(getTypicalStudentDirectory()));
            assertEquals(saved, Files.readString(filePath));
        }

        storage.saveStudentDirectory(getTypicalStudentDirectory());
        assertEquals(getTypicalStudentDirectory(), storage.readStudentDirectory().orElseThrow());
        try (var files = Files.list(testFolder)) {
            assertEquals(2, files.count()); // Only the data and persistent lock files remain.
        }
    }

    @Test
    public void saveStudentDirectory_simultaneousSaves_onlyOneSucceeds() throws Exception {
        Path filePath = testFolder.resolve("simultaneous.json");
        JsonStudentDirectoryStorage first = new JsonStudentDirectoryStorage(filePath);
        JsonStudentDirectoryStorage second = new JsonStudentDirectoryStorage(filePath);
        first.readStudentDirectory();
        second.readStudentDirectory();
        StudentDirectory firstRoster = new StudentDirectory();
        firstRoster.addStudent(ALICE);
        StudentDirectory secondRoster = new StudentDirectory();
        secondRoster.addStudent(HOON);
        CyclicBarrier barrier = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstSave = executor.submit(attemptSave(first, firstRoster, barrier));
            var secondSave = executor.submit(attemptSave(second, secondRoster, barrier));
            assertTrue(firstSave.get(10, TimeUnit.SECONDS) ^ secondSave.get(10, TimeUnit.SECONDS));
        }

        ReadOnlyStudentDirectory saved = new JsonStudentDirectoryStorage(filePath).readStudentDirectory().orElseThrow();
        assertTrue(saved.equals(firstRoster) || saved.equals(secondRoster));
    }

    @Test
    public void saveStudentDirectory_symbolicLink_usesTargetAndDetectsConflict() throws Exception {
        Path target = testFolder.resolve("target.json");
        Path alias = testFolder.resolve("alias.json");
        JsonStudentDirectoryStorage first = new JsonStudentDirectoryStorage(target);
        first.saveStudentDirectory(new StudentDirectory());
        try {
            Files.createSymbolicLink(alias, target);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e.getMessage());
        }
        JsonStudentDirectoryStorage second = new JsonStudentDirectoryStorage(alias);
        second.readStudentDirectory();
        first.saveStudentDirectory(getTypicalStudentDirectory());

        assertThrows(IOException.class, JsonStudentDirectoryStorage.DATA_CHANGED_MESSAGE, () ->
                second.saveStudentDirectory(new StudentDirectory()));
        assertTrue(Files.isSymbolicLink(alias));
        assertEquals(getTypicalStudentDirectory(), second.readStudentDirectory().orElseThrow());
        second.saveStudentDirectory(new StudentDirectory());
        assertTrue(Files.isSymbolicLink(alias));
        assertEquals(new StudentDirectory(), first.readStudentDirectory().orElseThrow());
    }

    private Callable<Boolean> attemptSave(JsonStudentDirectoryStorage storage, StudentDirectory roster,
            CyclicBarrier barrier) {
        return () -> {
            barrier.await(10, TimeUnit.SECONDS);
            try {
                storage.saveStudentDirectory(roster);
                return true;
            } catch (IOException e) {
                assertTrue(e.getMessage().equals(JsonStudentDirectoryStorage.DATA_CHANGED_MESSAGE)
                        || e.getMessage().equals(JsonStudentDirectoryStorage.SAVE_IN_PROGRESS_MESSAGE));
                return false;
            }
        };
    }
}
