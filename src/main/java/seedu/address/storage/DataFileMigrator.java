package seedu.address.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ReadOnlyStudentDirectory;

/** Migrates data files from legacy TutorLink storage locations. */
public final class DataFileMigrator {

    private static final Logger logger = LogsCenter.getLogger(DataFileMigrator.class);

    private DataFileMigrator() {}

    /**
     * Migrates student directory data from {@code legacyFilePath} to {@code targetFilePath} when only the legacy file
     * exists. The legacy file is preserved, and the target file is replaced atomically where supported.
     */
    public static void migrateStudentDirectory(Path legacyFilePath, Path targetFilePath)
            throws DataLoadingException, IOException {
        if (Files.exists(targetFilePath) || Files.notExists(legacyFilePath)) {
            return;
        }

        JsonStudentDirectoryStorage legacyStorage = new JsonStudentDirectoryStorage(legacyFilePath);
        Optional<ReadOnlyStudentDirectory> legacyData = legacyStorage.readStudentDirectory();
        if (legacyData.isEmpty()) {
            return;
        }

        Path temporaryFilePath = targetFilePath.resolveSibling(targetFilePath.getFileName() + ".tmp");
        try {
            new JsonStudentDirectoryStorage(temporaryFilePath).saveStudentDirectory(legacyData.get());
            moveTemporaryFile(temporaryFilePath, targetFilePath);
        } finally {
            deleteTemporaryFile(temporaryFilePath);
        }
    }

    private static void moveTemporaryFile(Path temporaryFilePath, Path targetFilePath) throws IOException {
        try {
            Files.move(temporaryFilePath, targetFilePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaryFilePath, targetFilePath);
        }
    }

    private static void deleteTemporaryFile(Path temporaryFilePath) {
        try {
            Files.deleteIfExists(temporaryFilePath);
        } catch (IOException e) {
            logger.warning("Unable to remove temporary migration file: " + temporaryFilePath);
        }
    }
}
