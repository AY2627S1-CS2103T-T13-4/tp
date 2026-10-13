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
import seedu.address.model.AddressBook;
import seedu.address.testutil.PersonBuilder;

public class DataFileMigratorTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    public void migrateAddressBook_validLegacyFile_migratesWithoutChangingLegacyFile() throws Exception {
        Path legacyFile = temporaryFolder.resolve("addressbook.json");
        Path targetFile = temporaryFolder.resolve("tutorlink.json");
        AddressBook storedData = new AddressBook();
        storedData.addPerson(new PersonBuilder().build());
        new JsonAddressBookStorage(legacyFile).saveAddressBook(storedData);
        String originalLegacyFile = Files.readString(legacyFile);

        DataFileMigrator.migrateAddressBook(legacyFile, targetFile);

        assertTrue(Files.exists(targetFile));
        assertEquals(storedData, new AddressBook(new JsonAddressBookStorage(targetFile).readAddressBook().get()));
        assertEquals(originalLegacyFile, Files.readString(legacyFile));
    }

    @Test
    public void migrateAddressBook_targetFileExists_doesNotReadOrChangeEitherFile() throws Exception {
        Path legacyFile = temporaryFolder.resolve("addressbook.json");
        Path targetFile = temporaryFolder.resolve("tutorlink.json");
        String invalidLegacyData = "not valid json";
        Files.writeString(legacyFile, invalidLegacyData);
        AddressBook targetData = new AddressBook();
        targetData.addPerson(new PersonBuilder().build());
        new JsonAddressBookStorage(targetFile).saveAddressBook(targetData);
        String originalTargetFile = Files.readString(targetFile);

        assertDoesNotThrow(() -> DataFileMigrator.migrateAddressBook(legacyFile, targetFile));

        assertEquals(invalidLegacyData, Files.readString(legacyFile));
        assertEquals(originalTargetFile, Files.readString(targetFile));
    }

    @Test
    public void migrateAddressBook_invalidLegacyFile_throwsAndPreservesLegacyFile() throws Exception {
        Path legacyFile = temporaryFolder.resolve("addressbook.json");
        Path targetFile = temporaryFolder.resolve("tutorlink.json");
        String invalidLegacyData = "not valid json";
        Files.writeString(legacyFile, invalidLegacyData);

        assertThrows(DataLoadingException.class, () ->
                DataFileMigrator.migrateAddressBook(legacyFile, targetFile));

        assertEquals(invalidLegacyData, Files.readString(legacyFile));
        assertFalse(Files.exists(targetFile));
    }

    @Test
    public void migrateAddressBook_missingLegacyFile_doesNotCreateTargetFile() {
        Path legacyFile = temporaryFolder.resolve("addressbook.json");
        Path targetFile = temporaryFolder.resolve("tutorlink.json");

        assertDoesNotThrow(() -> DataFileMigrator.migrateAddressBook(legacyFile, targetFile));

        assertFalse(Files.exists(targetFile));
    }
}
