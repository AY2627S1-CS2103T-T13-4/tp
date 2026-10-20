package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.logic.Logic;
import tutorlink.logic.LogicManager;
import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.UserPrefs;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;

/**
 * Exercises the remarks acceptance workflow through parsing, commands, and real JSON storage.
 */
public class RemarkCommandIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonStudentDirectoryStorage storage;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(new tutorlink.model.StudentDirectory(), new UserPrefs());
        storage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("students.json"));
        logic = createLogic();
    }

    @Test
    public void execute_addReplaceRestartClear_persistsRemark() throws Exception {
        logic.execute("add n/Alice Tan m/A1234567Z g/T01");
        logic.execute("remark 1 r/Needs help with recursion");
        assertEquals("Needs help with recursion", model.getFilteredStudentList().getFirst().getRemark());

        logic.execute("remark 1 r/Discuss scholarship options");
        assertEquals("Discuss scholarship options", model.getFilteredStudentList().getFirst().getRemark());

        restart();
        assertEquals("Discuss scholarship options", model.getFilteredStudentList().getFirst().getRemark());

        logic.execute("remark 1 r/");
        restart();
        assertNull(model.getFilteredStudentList().getFirst().getRemark());
    }

    private Logic createLogic() {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
    }

    private void restart() throws Exception {
        model = new ModelManager(storage.readStudentDirectory().orElseThrow(), new UserPrefs());
        logic = createLogic();
    }
}
