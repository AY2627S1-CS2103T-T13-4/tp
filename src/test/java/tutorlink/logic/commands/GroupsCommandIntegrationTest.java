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
import tutorlink.model.ReadOnlyStudentDirectory;
import tutorlink.model.StudentDirectory;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.Student;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.StudentBuilder;

/**
 * Exercises the tutorial-group overview through parsing, commands, filtering, and storage.
 */
public class GroupsCommandIntegrationTest {

    private static final Student ALICE_T01 = new StudentBuilder().withName("Alice Test")
            .withMatricNumber("A1111111A").withTutorialGroup("T01").build();
    private static final Student BEN_T01 = new StudentBuilder().withName("Ben Test")
            .withMatricNumber("A2222222B").withTutorialGroup("T01").build();
    private static final Student CARA_T02 = new StudentBuilder().withName("Cara Test")
            .withMatricNumber("A3333333C").withTutorialGroup("T02").build();

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonStudentDirectoryStorage studentDirectoryStorage;

    @BeforeEach
    public void setUp() {
        setUpLogic(getTypicalStudentDirectory());
    }

    private void setUpLogic(ReadOnlyStudentDirectory studentDirectory) {
        model = new ModelManager(studentDirectory, new UserPrefs());
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
    public void execute_groupsAfterMovingStudent_matchesAcceptanceScenarioAndPreservesView() throws Exception {
        StudentDirectory acceptanceRoster = new StudentDirectory();
        acceptanceRoster.setStudents(List.of(ALICE_T01, BEN_T01, CARA_T02));
        setUpLogic(acceptanceRoster);

        logic.execute("list g/t01");

        CommandResult initialResult = logic.execute("groups");

        assertEquals("T01: 2 students\nT02: 1 student", initialResult.getFeedbackToUser());
        assertEquals(List.of(ALICE_T01, BEN_T01), model.getFilteredStudentList());

        logic.execute("edit 1 g/t02");

        CommandResult updatedResult = logic.execute("groups");

        assertEquals("T01: 1 student\nT02: 2 students", updatedResult.getFeedbackToUser());
        assertEquals(List.of(BEN_T01), model.getFilteredStudentList());
    }

    @Test
    public void execute_groupsAfterCliAddAndDelete_reflectsConfirmedMutationsOnly() throws Exception {
        logic.execute("clear");
        logic.execute("add n/Alice Test m/A1111111A g/T01");

        assertEquals("T01: 1 student", logic.execute("groups").getFeedbackToUser());

        logic.execute("add n/Ben Test m/A2222222B g/T02");
        assertEquals("T01: 1 student\nT02: 1 student", logic.execute("groups").getFeedbackToUser());

        logic.execute("delete 2");
        assertEquals("T01: 1 student\nT02: 1 student", logic.execute("groups").getFeedbackToUser());

        logic.execute("delete 2 confirm");

        assertEquals("T01: 1 student", logic.execute("groups").getFeedbackToUser());
        assertEquals(model.getStudentDirectory(), studentDirectoryStorage.readStudentDirectory().orElseThrow());
    }
}
