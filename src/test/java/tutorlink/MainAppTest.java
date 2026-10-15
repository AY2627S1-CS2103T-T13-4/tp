package tutorlink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static tutorlink.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

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
}
