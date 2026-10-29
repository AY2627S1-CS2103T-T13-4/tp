package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static tutorlink.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static tutorlink.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ObservableList;
import tutorlink.commons.exceptions.DataLoadingException;
import tutorlink.logic.Logic;
import tutorlink.logic.LogicManager;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.StudentDirectory;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.Student;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.StudentBuilder;

/**
 * Exercises resetting sorting through parsing, live filters, indexed operations, and storage.
 */
public class SortResetCommandIntegrationTest {
    private static final Student ZOE = new StudentBuilder().withName("Zoe")
            .withMatricNumber("A0000003C").withTutorialGroup("T02")
            .withTags("needsHelp").withFollowUp(true).build();
    private static final Student BOB = new StudentBuilder().withName("bob")
            .withMatricNumber("A0000004D").withTutorialGroup("T01").withTags().build();
    private static final Student ALICE_HIGHER_MATRIC = new StudentBuilder().withName("Alice")
            .withMatricNumber("A0000002B").withTutorialGroup("T02")
            .withTags().withFollowUp(true).build();
    private static final Student ALICE_LOWER_MATRIC = new StudentBuilder().withName("alice")
            .withMatricNumber("A0000001A").withTutorialGroup("T01").withTags("needsHelp").build();
    private static final List<Student> DIRECTORY_ORDER = List.of(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);
    private static final List<String> SORT_COMMANDS = List.of(
            "sort name", "sort name desc", "sort group", "sort group desc");

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonStudentDirectoryStorage studentDirectoryStorage;

    @BeforeEach
    public void setUp() throws IOException, DataLoadingException {
        StudentDirectory directory = new StudentDirectory();
        directory.setStudents(DIRECTORY_ORDER);
        model = new ModelManager(directory, new UserPrefs());
        studentDirectoryStorage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("students.json"));
        studentDirectoryStorage.readStudentDirectory();
        studentDirectoryStorage.saveStudentDirectory(model.getStudentDirectory());
        logic = new LogicManager(model, new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    @Test
    public void execute_afterEitherSort_restoresDirectoryOrderAndRetainsDisplayedList() throws Exception {
        ObservableList<Student> displayedStudents = logic.getFilteredStudentList();
        for (String sortCommand : SORT_COMMANDS) {
            logic.execute(sortCommand);
            assertEquals(SortResetCommand.MESSAGE_SUCCESS, logic.execute("sort reset").getFeedbackToUser());
            assertDisplayed(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);
            assertSame(displayedStudents, logic.getFilteredStudentList());
            assertDirectoryAndStorage(DIRECTORY_ORDER);
            Model reloaded = new ModelManager(studentDirectoryStorage.readStudentDirectory().orElseThrow(),
                    new UserPrefs());
            assertEquals(DIRECTORY_ORDER, reloaded.getFilteredStudentList());
        }
    }

    @Test
    public void execute_allFilters_preservesResultRangeForBothSortFields() throws Exception {
        List<String> filters = List.of("find alice Zoe", "list g/T02", "findtag needsHelp", "followups");
        List<List<Student>> expectedResults = List.of(List.of(ZOE, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC),
                List.of(ZOE, ALICE_HIGHER_MATRIC), List.of(ZOE, ALICE_LOWER_MATRIC),
                List.of(ZOE, ALICE_HIGHER_MATRIC));
        for (int i = 0; i < filters.size(); i++) {
            for (String sortCommand : SORT_COMMANDS) {
                logic.execute(filters.get(i));
                logic.execute(sortCommand);
                logic.execute("sort reset");
                assertEquals(expectedResults.get(i), logic.getFilteredStudentList());
                logic.execute("sort reset");
                assertEquals(expectedResults.get(i), logic.getFilteredStudentList());
                assertDirectoryAndStorage(DIRECTORY_ORDER);
            }
        }
    }

    @Test
    public void execute_resetThenEdit_filtersRemainActive() throws Exception {
        List<String> filters = List.of("find alice Zoe", "list g/T02", "findtag needsHelp", "followups");
        List<String> edits = List.of("edit 1 n/Dana", "edit 1 g/T03", "edit 1 t/", "followup 1 done");
        for (int i = 0; i < filters.size(); i++) {
            for (String sortCommand : SORT_COMMANDS) {
                setUp();
                logic.execute(filters.get(i));
                logic.execute(sortCommand);
                logic.execute("sort reset");
                logic.execute(edits.get(i));
                if (i == 0) {
                    assertDisplayed(ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);
                } else if (i == 2) {
                    assertDisplayed(ALICE_LOWER_MATRIC);
                } else {
                    assertDisplayed(ALICE_HIGHER_MATRIC);
                }
            }
        }
    }

    @Test
    public void execute_unsortedEmptyAndSingleResults_repeatedResetSucceeds() throws Exception {
        logic.execute("sort reset");
        logic.execute("sort reset");
        assertDisplayed(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);
        for (String filter : List.of("list g/T99", "find bob")) {
            logic.execute(filter);
            List<Student> expectedResults = List.copyOf(logic.getFilteredStudentList());
            for (String sortCommand : SORT_COMMANDS) {
                logic.execute(sortCommand);
                assertEquals(SortResetCommand.MESSAGE_SUCCESS, logic.execute("sort reset").getFeedbackToUser());
                logic.execute("sort reset");
                assertEquals(expectedResults, logic.getFilteredStudentList());
            }
        }
        logic.execute("clear");
        logic.execute("sort reset");
        assertDisplayed();
    }

    @Test
    public void execute_resetThenIndexedCommands_useDisplayedIndicesWithoutReorderingEdits() throws Exception {
        for (String sortCommand : SORT_COMMANDS) {
            setUp();
            logic.execute("list g/T02");
            logic.execute(sortCommand);
            logic.execute("sort reset");
            logic.execute("edit 1 n/Aaron g/T02");
            Student editedZoe = new StudentBuilder(ZOE).withName("Aaron").build();
            assertDisplayed(editedZoe, ALICE_HIGHER_MATRIC);
            logic.execute("edit 1 n/Zulu g/T02");
            editedZoe = new StudentBuilder(ZOE).withName("Zulu").build();
            assertDisplayed(editedZoe, ALICE_HIGHER_MATRIC);
            logic.execute("followup 1 done");
            editedZoe = new StudentBuilder(editedZoe).withFollowUp(false).build();
            assertDisplayed(editedZoe, ALICE_HIGHER_MATRIC);
            assertEquals(String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE, Messages.format(editedZoe), 1),
                    logic.execute("delete 1").getFeedbackToUser());
            assertThrows(CommandException.class,
                    MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("edit 3 n/Dana"));
            assertThrows(CommandException.class,
                    MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("delete 3 confirm"));
            assertThrows(CommandException.class,
                    MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("followup 3 on"));
            logic.execute("delete 1 confirm");
            assertDisplayed(ALICE_HIGHER_MATRIC);
            assertDirectoryAndStorage(List.of(BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC));
        }
    }

    @Test
    public void execute_resetAfterEditAndDelete_usesCurrentDirectoryOrder() throws Exception {
        for (String sortCommand : SORT_COMMANDS) {
            setUp();
            logic.execute(sortCommand);
            Student firstDisplayed = logic.getFilteredStudentList().get(0);
            Student editedStudent = new StudentBuilder(firstDisplayed).withName("Wendy").withTutorialGroup("T03")
                    .build();
            logic.execute("edit 1 n/Wendy g/T03");
            Student deletedStudent = logic.getFilteredStudentList().get(0);
            logic.execute("delete 1 confirm");
            List<Student> expectedDirectory = DIRECTORY_ORDER.stream()
                    .map(student -> student.equals(firstDisplayed) ? editedStudent : student)
                    .filter(student -> !student.equals(deletedStudent)).toList();
            logic.execute("sort reset");
            assertEquals(expectedDirectory, logic.getFilteredStudentList());
            assertDirectoryAndStorage(expectedDirectory);
        }
    }

    @Test
    public void execute_addThenSortAndReset_newStudentRemainsAtEnd() throws Exception {
        logic.execute("find alice");
        logic.execute("sort group");
        logic.execute("add n/Aaron m/A7777777Z g/T03");
        Student addedStudent = logic.getFilteredStudentList().get(4);
        for (String sortCommand : SORT_COMMANDS) {
            logic.execute(sortCommand);
            logic.execute("sort reset");
            assertDisplayed(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC, addedStudent);
            assertDirectoryAndStorage(List.of(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC, addedStudent));
        }
    }

    @Test
    public void execute_invalidReset_preservesFilterAndActiveComparator() throws Exception {
        for (String sortCommand : SORT_COMMANDS) {
            setUp();
            logic.execute("find alice Zoe");
            logic.execute(sortCommand);
            List<Student> sortedResults = List.copyOf(logic.getFilteredStudentList());
            for (String invalidCommand : List.of("sort reset asc", "sort reset desc", "sort reset extra")) {
                assertThrows(ParseException.class, () -> logic.execute(invalidCommand));
                assertEquals(sortedResults, logic.getFilteredStudentList());
                assertDirectoryAndStorage(DIRECTORY_ORDER);
            }
            Student target = sortedResults.get(0);
            logic.execute("edit 1 n/Zoe g/T03");
            Student editedStudent = new StudentBuilder(target).withName("Zoe").withTutorialGroup("T03").build();
            Model expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
            Logic expectedLogic = new LogicManager(expectedModel, new StorageManager(studentDirectoryStorage,
                    new JsonUserPrefsStorage(temporaryFolder.resolve("expected-prefs.json"))));
            expectedLogic.execute("find alice Zoe");
            expectedLogic.execute(sortCommand);
            assertEquals(expectedModel.getFilteredStudentList(), logic.getFilteredStudentList());
            assertEquals(editedStudent, model.getStudentDirectory().getStudentList().stream()
                    .filter(student -> student.getMatricNumber().equals(target.getMatricNumber())).findFirst()
                    .orElseThrow());
        }
    }

    @Test
    public void execute_sortAgainAfterReset_switchesRulesAndRetainsFilter() throws Exception {
        logic.execute("find alice Zoe");
        logic.execute("sort name");
        logic.execute("sort reset");
        logic.execute("sort group desc");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE, ALICE_LOWER_MATRIC);
        logic.execute("sort reset");
        logic.execute("sort name desc");
        assertDisplayed(ZOE, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC);
        logic.execute("sort reset");
        assertDisplayed(ZOE, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);
        logic.execute("list");
        assertDisplayed(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);
    }

    private void assertDisplayed(Student... students) {
        assertEquals(List.of(students), logic.getFilteredStudentList());
    }

    private void assertDirectoryAndStorage(List<Student> expectedStudents) throws Exception {
        assertEquals(expectedStudents, model.getStudentDirectory().getStudentList());
        assertEquals(expectedStudents, studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }
}
