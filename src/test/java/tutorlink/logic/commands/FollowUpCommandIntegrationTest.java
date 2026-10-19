package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.nio.file.Files;
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
import tutorlink.model.student.Student;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.StudentBuilder;

/**
 * Exercises the follow-up acceptance workflow through parsing, commands, and real JSON storage.
 */
public class FollowUpCommandIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonStudentDirectoryStorage storage;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        storage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("students.json"));
        logic = createLogic();
    }

    @Test
    public void execute_markListRestartComplete_removesStudentAndSavesCompletion() throws Exception {
        Student marked = new StudentBuilder(ALICE).withFollowUp(true).build();
        logic.execute("followup 1 on");
        logic.execute("followups");
        assertEquals(List.of(marked), model.getFilteredStudentList());

        restart();
        logic.execute("followups");
        assertEquals(List.of(marked), model.getFilteredStudentList());
        logic.execute("followup 1 done");
        assertEquals(List.of(), model.getFilteredStudentList());
        assertEquals(ALICE, model.getStudentDirectory().getStudentList().getFirst());

        restart();
        assertEquals(FollowUpsCommand.MESSAGE_EMPTY, logic.execute("followups").getFeedbackToUser());
    }

    @Test
    public void execute_olderJsonWithoutFollowUp_defaultsToFalseAndRoundTrips() throws Exception {
        Files.writeString(storage.getStudentDirectoryFilePath(), """
                {"students":[{"name":"Legacy Student","matricNumber":"A1234567Z",
                  "tutorialGroup":"T01","remark":"Retain this remark","tags":["needsHelp"]}]}
                """);
        restart();
        Student original = model.getStudentDirectory().getStudentList().getFirst();
        assertFalse(original.requiresFollowUp());
        assertEquals("Retain this remark", original.getRemark());
        assertEquals(FollowUpsCommand.MESSAGE_EMPTY, logic.execute("followups").getFeedbackToUser());
        assertTrue(Files.readString(storage.getStudentDirectoryFilePath()).contains("\"requiresFollowUp\" : false"));

        logic.execute("list");
        logic.execute("followup 1 on");
        restart();
        assertEquals(original.withFollowUp(true), model.getStudentDirectory().getStudentList().getFirst());
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
