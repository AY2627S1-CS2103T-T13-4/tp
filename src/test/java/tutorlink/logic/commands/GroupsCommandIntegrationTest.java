package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.BENSON;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.nio.file.Path;
import java.util.List;

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
 * Exercises the tutorial-group overview through parsing, commands, filtering, and storage.
 */
public class GroupsCommandIntegrationTest {

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonStudentDirectoryStorage studentDirectoryStorage;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        studentDirectoryStorage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("studentDirectory.json"));
        logic = new LogicManager(model, new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));
    }

    @Test
    public void execute_groupsAfterFilter_usesCompleteRosterAndPreservesView() throws Exception {
        logic.execute("list g/t01");

        CommandResult result = logic.execute("groups");

        assertEquals("L01: 1 student\nT01: 2 students\nT02: 2 students\nT03: 2 students",
                result.getFeedbackToUser());
        assertEquals(List.of(ALICE, BENSON), model.getFilteredStudentList());
        assertEquals(getTypicalStudentDirectory(), studentDirectoryStorage.readStudentDirectory().orElseThrow());
    }

    @Test
    public void execute_groupsAfterMovingStudent_reflectsUpdatedCount() throws Exception {
        logic.execute("list g/t01");
        logic.execute("edit 1 g/t02");

        CommandResult result = logic.execute("groups");

        assertEquals("L01: 1 student\nT01: 1 student\nT02: 3 students\nT03: 2 students",
                result.getFeedbackToUser());
        assertEquals(List.of(BENSON), model.getFilteredStudentList());
    }
}
