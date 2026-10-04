package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

public class MainAppTest {

    @TempDir
    public Path temporaryFolder;

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
