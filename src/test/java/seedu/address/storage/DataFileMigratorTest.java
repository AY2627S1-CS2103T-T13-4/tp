package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ReadOnlyStudentDirectory;
import seedu.address.model.StudentDirectory;
import seedu.address.testutil.StudentBuilder;

public class DataFileMigratorTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    public void migrateStudentDirectory_validLegacyFile_migratesWithoutChangingLegacyFile() throws Exception {
        Path legacyFile = temporaryFolder.resolve("addressbook.json");
        Path targetFile = temporaryFolder.resolve("tutorlink.json");
        StudentDirectory storedData = new StudentDirectory();
        storedData.addStudent(new StudentBuilder().build());
        new JsonStudentDirectoryStorage(legacyFile).saveStudentDirectory(storedData);
        Files.writeString(legacyFile, Files.readString(legacyFile).replace("\"students\"", "\"persons\""));
        String originalLegacyFile = Files.readString(legacyFile);

        DataFileMigrator.migrateStudentDirectory(legacyFile, targetFile);

        assertTrue(Files.exists(targetFile));
        ReadOnlyStudentDirectory migratedData = new JsonStudentDirectoryStorage(targetFile)
                .readStudentDirectory().get();
        assertEquals(storedData, new StudentDirectory(migratedData));
        assertEquals(originalLegacyFile, Files.readString(legacyFile));
    }

    @Test
    public void migrateStudentDirectory_targetFileExists_doesNotReadOrChangeEitherFile() throws Exception {
        Path legacyFile = temporaryFolder.resolve("addressbook.json");
        Path targetFile = temporaryFolder.resolve("tutorlink.json");
        String invalidLegacyData = "not valid json";
        Files.writeString(legacyFile, invalidLegacyData);
        StudentDirectory targetData = new StudentDirectory();
        targetData.addStudent(new StudentBuilder().build());
        new JsonStudentDirectoryStorage(targetFile).saveStudentDirectory(targetData);
        String originalTargetFile = Files.readString(targetFile);

        assertDoesNotThrow(() -> DataFileMigrator.migrateStudentDirectory(legacyFile, targetFile));

        assertEquals(invalidLegacyData, Files.readString(legacyFile));
        assertEquals(originalTargetFile, Files.readString(targetFile));
    }

    @Test
    public void migrateStudentDirectory_invalidLegacyFile_throwsAndPreservesLegacyFile() throws Exception {
        Path legacyFile = temporaryFolder.resolve("addressbook.json");
        Path targetFile = temporaryFolder.resolve("tutorlink.json");
        String invalidLegacyData = "not valid json";
        Files.writeString(legacyFile, invalidLegacyData);

        assertThrows(DataLoadingException.class, () ->
                DataFileMigrator.migrateStudentDirectory(legacyFile, targetFile));

        assertEquals(invalidLegacyData, Files.readString(legacyFile));
        assertFalse(Files.exists(targetFile));
    }

    @Test
    public void migrateStudentDirectory_missingLegacyFile_doesNotCreateTargetFile() {
        Path legacyFile = temporaryFolder.resolve("addressbook.json");
        Path targetFile = temporaryFolder.resolve("tutorlink.json");

        assertDoesNotThrow(() -> DataFileMigrator.migrateStudentDirectory(legacyFile, targetFile));

        assertFalse(Files.exists(targetFile));
    }
}
