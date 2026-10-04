package seedu.address;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class MainAppTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    public void stop_withoutInitializedModel_doesNotThrow() {
        assertDoesNotThrow(() -> new MainApp().stop());
    }

    @Test
    public void initModelManager_missingFile_loadsSampleDataWithoutCreatingFile() {
        Path dataFile = temporaryFolder.resolve("addressbook.json");
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));

        Model model = MainApp.initModelManager(storage, new UserPrefs());

        assertEquals(new AddressBook(SampleDataUtil.getSampleAddressBook()),
                new AddressBook(model.getAddressBook()));
        assertFalse(Files.exists(dataFile));
    }

    @Test
    public void initModelManager_validFile_loadsStudentWithoutChangingFile() throws Exception {
        Path dataFile = temporaryFolder.resolve("addressbook.json");
        AddressBook storedData = new AddressBook();
        storedData.addPerson(new PersonBuilder().build());
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataFile);
        addressBookStorage.saveAddressBook(storedData);
        String originalFile = Files.readString(dataFile);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));

        Model model = MainApp.initModelManager(storage, new UserPrefs());

        assertEquals(storedData, new AddressBook(model.getAddressBook()));
        assertEquals(originalFile, Files.readString(dataFile));
    }

    @Test
    public void initModelManager_incompatibleOldFile_preservesOriginal() throws Exception {
        Path dataFile = temporaryFolder.resolve("addressbook.json");
        String oldData = "{\"persons\":[{\"name\":\"Alice Tan\",\"phone\":\"98765432\","
                + "\"email\":\"alice@example.com\",\"address\":\"NUS\",\"tags\":[]}]}";
        Files.writeString(dataFile, oldData);
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));

        assertThrows(IllegalStateException.class, () -> MainApp.initModelManager(storage, new UserPrefs()));
        assertEquals(oldData, Files.readString(dataFile));
    }
}
