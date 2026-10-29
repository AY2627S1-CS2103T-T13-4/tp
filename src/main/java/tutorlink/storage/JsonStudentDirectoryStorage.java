package tutorlink.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
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

    private static final Logger logger = LogsCenter.getLogger(JsonStudentDirectoryStorage.class);

    private Path filePath;

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
    public Optional<ReadOnlyStudentDirectory> readStudentDirectory(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        Optional<JsonSerializableStudentDirectory> jsonStudentDirectory = JsonUtil.readJsonFile(
                filePath, JsonSerializableStudentDirectory.class);
        if (!jsonStudentDirectory.isPresent()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonStudentDirectory.get().toModelType());
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyStudentDirectory} to the storage.
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
    public void saveStudentDirectory(ReadOnlyStudentDirectory studentDirectory, Path filePath) throws IOException {
        requireNonNull(studentDirectory);
        requireNonNull(filePath);

        JsonUtil.saveJsonFile(new JsonSerializableStudentDirectory(studentDirectory), filePath);
    }

}
