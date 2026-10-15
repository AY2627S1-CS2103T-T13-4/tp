package tutorlink.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import tutorlink.commons.exceptions.DataLoadingException;
import tutorlink.model.ReadOnlyStudentDirectory;
import tutorlink.model.ReadOnlyUserPrefs;
import tutorlink.model.UserPrefs;

/**
 * API of the Storage component
 */
public interface Storage {

    /**
     * Returns the file path of the UserPrefs data file.
     */
    Path getUserPrefsFilePath();

    /**
     * Returns UserPrefs data from storage.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if the loading of data from preference file failed.
     */
    Optional<UserPrefs> readUserPrefs() throws DataLoadingException;

    /**
     * Saves the given {@link tutorlink.model.ReadOnlyUserPrefs} to the storage.
     * @param userPrefs cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException;

    /**
     * Returns the file path of the StudentDirectory data file.
     */
    Path getStudentDirectoryFilePath();

    /**
     * Returns StudentDirectory data as a {@link ReadOnlyStudentDirectory}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    Optional<ReadOnlyStudentDirectory> readStudentDirectory() throws DataLoadingException;

    /**
     * Saves the given {@link ReadOnlyStudentDirectory} to the storage.
     * @param studentDirectory cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveStudentDirectory(ReadOnlyStudentDirectory studentDirectory) throws IOException;

}
