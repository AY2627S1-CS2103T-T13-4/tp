package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.BENSON;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.logic.Logic;
import tutorlink.logic.LogicManager;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.logic.parser.exceptions.ParseException;
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

    @Test
    public void execute_searchMoveEditAndClearTags_retainsStatusRemarkAndOptionalDetails() throws Exception {
        Student original = new StudentBuilder(BENSON).withRemark("Discuss recursion").build();
        model.setStudent(BENSON, original);
        logic.execute("findtag owesMoney");
        logic.execute("followup 1 on");
        logic.execute("edit 1 n/Benny Tan m/A5555555Z p/87654321 e/benny@example.com a/NUS g/L03 t/needsHelp");
        Student edited = new StudentBuilder(original).withFollowUp(true).withName("Benny Tan")
                .withMatricNumber("A5555555Z").withPhone("87654321").withEmail("benny@example.com")
                .withAddress("NUS").withTutorialGroup("L03").withTags("needsHelp").build();
        assertEquals(List.of(), model.getFilteredStudentList());
        logic.execute("list g/T01");
        logic.execute("followups");
        assertEquals(List.of(edited), model.getFilteredStudentList());
        logic.execute("edit 1 t/");
        edited = new StudentBuilder(edited).withTags().build();
        assertEquals(List.of(edited), model.getFilteredStudentList());

        restart();
        logic.execute("find A5555555Z");
        assertEquals(List.of(edited), model.getFilteredStudentList());
        logic.execute("followup 1 done");
        assertEquals(List.of(edited.withFollowUp(false)), model.getFilteredStudentList());
        assertEquals(ALICE, model.getStudentDirectory().getStudentList().getFirst());
        assertEquals(model.getStudentDirectory(), storage.readStudentDirectory().orElseThrow());
    }

    @Test
    public void execute_twoFollowUpsAndCompletion_renumbersRemainingDisplayedStudents() throws Exception {
        logic.execute("followup 1 on");
        logic.execute("find Benson");
        logic.execute("followup 1 on");
        logic.execute("findtag unknown");
        logic.execute("followups");
        assertEquals(List.of(ALICE.withFollowUp(true), BENSON.withFollowUp(true)), model.getFilteredStudentList());
        logic.execute("followup 1 done");
        assertEquals(List.of(BENSON.withFollowUp(true)), model.getFilteredStudentList());
        logic.execute("followup 1 done");
        assertEquals(List.of(), model.getFilteredStudentList());
        restart();
        assertEquals(FollowUpsCommand.MESSAGE_EMPTY, logic.execute("followups").getFeedbackToUser());
    }

    @Test
    public void execute_otherListCommands_replaceFollowUpFilter() throws Exception {
        logic.execute("followup 1 on");
        logic.execute("followups");
        logic.execute("find Benson");
        assertEquals(List.of(BENSON), model.getFilteredStudentList());
        logic.execute("followups");
        logic.execute("findtag owesMoney");
        assertEquals(List.of(BENSON), model.getFilteredStudentList());
        logic.execute("followups");
        logic.execute("list g/T02");
        assertEquals(2, model.getFilteredStudentList().size());
        logic.execute("followups");
        logic.execute("add n/Casey Tan m/A6666666Z g/T01");
        assertEquals(8, model.getFilteredStudentList().size());
        assertFalse(model.getFilteredStudentList().getLast().requiresFollowUp());
        logic.execute("followups");
        assertEquals(List.of(ALICE.withFollowUp(true)), model.getFilteredStudentList());
    }

    @Test
    public void execute_invalidCommands_preserveDataAndActiveView() throws Exception {
        logic.execute("followup 1 on");
        logic.execute("followups");
        String saved = Files.readString(storage.getStudentDirectoryFilePath());
        for (String command : List.of("followup", "followup 1", "followup 1 off", "followup 0 on",
                "followup -1 done", "followup 1.5 on", "followup 2147483648 on", "followup 1 on extra",
                "followups 1", "followups g/T01")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(saved, Files.readString(storage.getStudentDirectoryFilePath()));
            assertEquals(List.of(ALICE.withFollowUp(true)), model.getFilteredStudentList());
        }
        assertThrows(CommandException.class, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () ->
                logic.execute("followup 2 done"));
        assertEquals(saved, Files.readString(storage.getStudentDirectoryFilePath()));
        assertEquals(List.of(ALICE.withFollowUp(true)), model.getFilteredStudentList());
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
