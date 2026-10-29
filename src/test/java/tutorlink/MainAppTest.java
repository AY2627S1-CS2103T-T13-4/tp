package tutorlink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static tutorlink.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.model.Model;
import tutorlink.model.StudentDirectory;
import tutorlink.model.UserPrefs;
import tutorlink.model.util.SampleDataUtil;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.StudentBuilder;

public class MainAppTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    public void stop_withoutInitializedModel_doesNotThrow() {
        assertDoesNotThrow(() -> new MainApp().stop());
    }

    @Test
    public void initModelManager_missingFile_loadsSampleDataWithoutCreatingFile() {
        Path dataFile = temporaryFolder.resolve("tutorlink.json");
        StorageManager storage = new StorageManager(new JsonStudentDirectoryStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));

        Model model = MainApp.initModelManager(storage, new UserPrefs());

        assertEquals(new StudentDirectory(SampleDataUtil.getSampleStudentDirectory()),
                new StudentDirectory(model.getStudentDirectory()));
        assertFalse(Files.exists(dataFile));
    }

    @Test
    public void initModelManager_validFile_loadsStudentWithoutChangingFile() throws Exception {
        Path dataFile = temporaryFolder.resolve("tutorlink.json");
        StudentDirectory storedData = new StudentDirectory();
        storedData.addStudent(new StudentBuilder().build());
        JsonStudentDirectoryStorage studentDirectoryStorage = new JsonStudentDirectoryStorage(dataFile);
        studentDirectoryStorage.saveStudentDirectory(storedData);
        String originalFile = Files.readString(dataFile);
        StorageManager storage = new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));

        Model model = MainApp.initModelManager(storage, new UserPrefs());

        assertEquals(storedData, new StudentDirectory(model.getStudentDirectory()));
        assertEquals(originalFile, Files.readString(dataFile));
    }

    @Test
    public void initModelManager_jsonNull_stopsStartupAndPreservesOriginal() throws Exception {
        Path dataFile = temporaryFolder.resolve("tutorlink.json");
        String originalData = "null\n";
        Files.writeString(dataFile, originalData);
        StorageManager storage = new StorageManager(new JsonStudentDirectoryStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));

        assertThrows(IllegalStateException.class, () -> MainApp.initModelManager(storage, new UserPrefs()));
        assertEquals(originalData, Files.readString(dataFile));
    }

    @Test
    public void initModelManager_trailingContent_stopsStartupAndPreservesOriginal() throws Exception {
        Path dataFile = temporaryFolder.resolve("tutorlink.json");
        JsonStudentDirectoryStorage studentDirectoryStorage = new JsonStudentDirectoryStorage(dataFile);
        StudentDirectory storedData = new StudentDirectory();
        storedData.addStudent(new StudentBuilder().build());
        studentDirectoryStorage.saveStudentDirectory(storedData);
        String validJson = Files.readString(dataFile);
        StorageManager storage = new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
        String expectedMessage = "Data file at " + dataFile
                + " could not be loaded. The original file was not changed; startup has stopped to protect it.";

        for (String suffix : List.of(validJson, "null", "NOT JSON")) {
            Files.writeString(dataFile, validJson + "\n" + suffix);
            byte[] originalBytes = Files.readAllBytes(dataFile);

            assertThrows(IllegalStateException.class, expectedMessage, () ->
                    MainApp.initModelManager(storage, new UserPrefs()));
            assertArrayEquals(originalBytes, Files.readAllBytes(dataFile));
        }
    }

    @Test
    public void initModelManager_nullTag_stopsStartupAndPreservesOriginal() throws Exception {
        Path dataFile = temporaryFolder.resolve("tutorlink.json");
        String originalData = """
                {"students":[{"name":"Protected Student","matricNumber":"A2000005E",
                  "tutorialGroup":"T01","tags":[null]}]}
                """;
        Files.writeString(dataFile, originalData);
        StorageManager storage = new StorageManager(new JsonStudentDirectoryStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));

        assertThrows(IllegalStateException.class, () -> MainApp.initModelManager(storage, new UserPrefs()));
        assertEquals(originalData, Files.readString(dataFile));
    }

    @Test
    public void initModelManager_incompatibleOldFile_preservesOriginal() throws Exception {
        Path dataFile = temporaryFolder.resolve("tutorlink.json");
        String oldData = "{\"students\":[{\"name\":\"Alice Tan\",\"phone\":\"98765432\","
                + "\"email\":\"alice@example.com\",\"address\":\"NUS\",\"tags\":[]}]}";
        Files.writeString(dataFile, oldData);
        StorageManager storage = new StorageManager(new JsonStudentDirectoryStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));

        assertThrows(IllegalStateException.class, () -> MainApp.initModelManager(storage, new UserPrefs()));
        assertEquals(oldData, Files.readString(dataFile));
    }

    @Test
    public void initPrefs_nullGuiSettings_usesDefaultsAndAllowsModelInitialization() throws Exception {
        Path prefsFile = temporaryFolder.resolve("preferences.json");
        Files.writeString(prefsFile, "{\"guiSettings\": null}");
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(prefsFile);
        MainApp app = new MainApp();

        UserPrefs prefs = app.initPrefs(prefsStorage);
        StorageManager storage = new StorageManager(
                new JsonStudentDirectoryStorage(temporaryFolder.resolve("tutorlink.json")), prefsStorage);

        assertEquals(new UserPrefs(), prefs);
        assertDoesNotThrow(() -> MainApp.initModelManager(storage, prefs));
    }
}
