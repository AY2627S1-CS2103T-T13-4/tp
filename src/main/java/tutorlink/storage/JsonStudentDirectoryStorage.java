package tutorlink.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import tutorlink.commons.core.LogsCenter;
import tutorlink.commons.exceptions.DataLoadingException;
import tutorlink.commons.exceptions.IllegalValueException;
import tutorlink.commons.util.JsonUtil;
import tutorlink.model.ReadOnlyStudentDirectory;

/**
 * A class to access StudentDirectory data stored as a JSON file on the hard disk.
 */
public class JsonStudentDirectoryStorage {

    public static final String DATA_CHANGED_MESSAGE = "The student data file has changed since it was loaded. "
            + "Your change was not saved. Close and reopen TutorLink to load the latest data, then retry your command.";
    public static final String SAVE_IN_PROGRESS_MESSAGE = "Another TutorLink instance is saving student data. "
            + "Your change was not saved. Retry your command; "
            + "if the data has changed, close and reopen TutorLink first.";

    private static final Logger logger = LogsCenter.getLogger(JsonStudentDirectoryStorage.class);

    private Path filePath;
    // A null value represents a file that did not exist when it was read.
    private final Map<Path, byte[]> loadedContents = new HashMap<>();

    public JsonStudentDirectoryStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getStudentDirectoryFilePath() {
        return filePath;
    }

    /**
     * Returns StudentDirectory data as a {@link ReadOnlyStudentDirectory}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyStudentDirectory> readStudentDirectory() throws DataLoadingException {
        return readStudentDirectory(filePath);
    }

    /**
     * Similar to {@link #readStudentDirectory()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public synchronized Optional<ReadOnlyStudentDirectory> readStudentDirectory(Path filePath)
            throws DataLoadingException {
        requireNonNull(filePath);

        try {
            Path resolvedPath = resolveFilePath(filePath);
            byte[] contents = readContents(resolvedPath);
            if (contents == null) {
                loadedContents.put(resolvedPath, null);
                return Optional.empty();
            }

            // Parse exactly the bytes used for conflict detection, even if another instance saves during loading.
            JsonSerializableStudentDirectory jsonStudentDirectory = JsonUtil.fromJsonString(
                    new String(contents, StandardCharsets.UTF_8), JsonSerializableStudentDirectory.class);
            if (jsonStudentDirectory == null) {
                throw new IOException("JSON file contains null instead of an object: " + filePath);
            }
            ReadOnlyStudentDirectory studentDirectory = jsonStudentDirectory.toModelType();
            loadedContents.put(resolvedPath, contents);
            return Optional.of(studentDirectory);
        } catch (IOException ioe) {
            throw new DataLoadingException(ioe);
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyStudentDirectory} to the storage.
     * Rejects changes if the file differs from the last successful read or save by this storage instance.
     * An existing file must be read before it can be saved.
     * @param studentDirectory cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveStudentDirectory(ReadOnlyStudentDirectory studentDirectory) throws IOException {
        saveStudentDirectory(studentDirectory, filePath);
    }

    /**
     * Similar to {@link #saveStudentDirectory(ReadOnlyStudentDirectory)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public synchronized void saveStudentDirectory(ReadOnlyStudentDirectory studentDirectory, Path filePath)
            throws IOException {
        requireNonNull(studentDirectory);
        requireNonNull(filePath);

        Path resolvedPath = resolveFilePath(filePath);
        Files.createDirectories(resolvedPath.getParent());
        Path lockPath = resolvedPath.resolveSibling(resolvedPath.getFileName() + ".lock");
        // Keep this sidecar file: deleting it could let two processes lock different file objects at the same path.
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = channel.tryLock()) {
            if (lock == null) {
                throw new IOException(SAVE_IN_PROGRESS_MESSAGE);
            }
            if (!Arrays.equals(loadedContents.get(resolvedPath), readContents(resolvedPath))) {
                throw new IOException(DATA_CHANGED_MESSAGE);
            }
            // Replacing a file requires directory permissions, so check the roster's own write protection as well.
            if (Files.exists(resolvedPath) && !Files.isWritable(resolvedPath)) {
                throw new AccessDeniedException(resolvedPath.toString());
            }

            byte[] contents = JsonUtil.toJsonString(new JsonSerializableStudentDirectory(studentDirectory))
                    .getBytes(StandardCharsets.UTF_8);
            writeAtomically(resolvedPath, contents);
            loadedContents.put(resolvedPath, contents);
        } catch (OverlappingFileLockException e) {
            throw new IOException(SAVE_IN_PROGRESS_MESSAGE, e);
        }
    }

    /** Resolves aliases so instances using symbolic links share the same data and lock files. */
    private Path resolveFilePath(Path filePath) throws IOException {
        Path absolutePath = filePath.toAbsolutePath().normalize();
        Path existingAncestor = absolutePath;
        while (!Files.exists(existingAncestor, LinkOption.NOFOLLOW_LINKS)) {
            existingAncestor = existingAncestor.getParent();
        }
        return existingAncestor.toRealPath().resolve(existingAncestor.relativize(absolutePath));
    }

    /** Returns the complete file contents, or null if the file does not exist. */
    private byte[] readContents(Path filePath) throws IOException {
        try {
            return Files.readAllBytes(filePath);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    /** Replaces the roster only after the complete JSON has been written successfully. */
    private void writeAtomically(Path filePath, byte[] contents) throws IOException {
        Path temporaryPath = Files.createTempFile(filePath.getParent(), "tutorlink-", ".tmp");
        try {
            if (Files.exists(filePath)) {
                // Copy the existing attributes supported by the filesystem instead of using temporary-file defaults.
                Files.copy(filePath, temporaryPath, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
            }
            Files.write(temporaryPath, contents);
            replaceFile(temporaryPath, filePath);
        } finally {
            Files.deleteIfExists(temporaryPath);
        }
    }

    /** Atomically replaces the roster; separated so filesystem failures can be exercised without damaging data. */
    void replaceFile(Path temporaryPath, Path filePath) throws IOException {
        Files.move(temporaryPath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

}
