package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static tutorlink.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static tutorlink.testutil.Assert.assertThrows;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ObservableList;
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
 * Exercises sorting through parsing, the displayed list, indexed commands, and storage.
 */
public class SortCommandIntegrationTest {
    private static final Student ZOE = new StudentBuilder().withName("Zoe")
            .withMatricNumber("A0000003C").withTutorialGroup("T01").build();
    private static final Student BOB = new StudentBuilder().withName("bob")
            .withMatricNumber("A0000004D").withTutorialGroup("T02").build();
    private static final Student ALICE_HIGHER_MATRIC = new StudentBuilder().withName("Alice")
            .withMatricNumber("A0000002B").withTutorialGroup("T01").build();
    private static final Student ALICE_LOWER_MATRIC = new StudentBuilder().withName("alice")
            .withMatricNumber("A0000001A").withTutorialGroup("T01").build();
    private static final List<Student> ORIGINAL_ORDER = List.of(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonStudentDirectoryStorage studentDirectoryStorage;

    @BeforeEach
    public void setUp() {
        StudentDirectory directory = new StudentDirectory();
        directory.setStudents(ORIGINAL_ORDER);
        model = new ModelManager(directory, new UserPrefs());
        studentDirectoryStorage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("students.json"));
        logic = new LogicManager(model, new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    @Test
    public void execute_defaultAndExplicitAscending_sortsIgnoringCaseWithMatricTieBreak() throws Exception {
        ObservableList<Student> displayedStudents = logic.getFilteredStudentList();
        assertEquals(SortCommand.MESSAGE_SUCCESS_ASCENDING, logic.execute("sort name").getFeedbackToUser());
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, BOB, ZOE);
        assertSame(displayedStudents, logic.getFilteredStudentList());

        logic.execute("sort name asc");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, BOB, ZOE);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_descending_keepsMatricTieBreakAscending() throws Exception {
        assertEquals(SortCommand.MESSAGE_SUCCESS_DESCENDING, logic.execute("sort name desc").getFeedbackToUser());
        assertDisplayed(ZOE, BOB, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC);
        logic.execute("sort name");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, BOB, ZOE);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_identicalNames_ordersByMatricNumber() throws Exception {
        Student sameName = new StudentBuilder(ALICE_LOWER_MATRIC).withName("Alice").build();
        model.setStudent(ALICE_LOWER_MATRIC, sameName);
        logic.execute("sort name");
        assertDisplayed(sameName, ALICE_HIGHER_MATRIC, BOB, ZOE);
        logic.execute("sort name desc");
        assertDisplayed(ZOE, BOB, sameName, ALICE_HIGHER_MATRIC);
    }

    @Test
    public void execute_groupFiltered_sortsOnlyMatchingStudents() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort name");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        logic.execute("sort name desc");
        assertDisplayed(ZOE, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_searchFiltered_sortsOnlyMatchingStudents() throws Exception {
        logic.execute("find Zoe alice");
        logic.execute("sort name");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_emptyAndSingleStudentResults_success() throws Exception {
        logic.execute("list g/T99");
        logic.execute("sort name");
        assertDisplayed();
        logic.execute("sort name desc");
        assertDisplayed();

        logic.execute("find bob");
        logic.execute("sort name");
        assertDisplayed(BOB);
        logic.execute("sort name desc");
        assertDisplayed(BOB);
    }

    @Test
    public void execute_newListOrSearch_restoresDirectoryOrder() throws Exception {
        logic.execute("sort name");
        logic.execute("list");
        assertDisplayed(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);

        logic.execute("sort name");
        logic.execute("list g/T01");
        assertDisplayed(ZOE, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);

        logic.execute("sort name");
        logic.execute("find alice Zoe");
        assertDisplayed(ZOE, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_editFirstSortedStudent_updatesCorrectStudentAndReorders() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort name");
        logic.execute("edit 1 n/Wendy");

        Student editedAlice = new StudentBuilder(ALICE_LOWER_MATRIC).withName("Wendy").build();
        assertDisplayed(ALICE_HIGHER_MATRIC, editedAlice, ZOE);
        assertEquals(List.of(ZOE, BOB, ALICE_HIGHER_MATRIC, editedAlice),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_editFirstDescendingStudent_reordersInDescendingOrder() throws Exception {
        logic.execute("sort name desc");
        logic.execute("edit 1 n/Aaron");
        Student editedZoe = new StudentBuilder(ZOE).withName("Aaron").build();
        assertDisplayed(BOB, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, editedZoe);
    }

    @Test
    public void execute_editMatricNumber_reordersEqualNames() throws Exception {
        logic.execute("sort name");
        logic.execute("edit 1 m/A9999999Z");
        Student editedAlice = new StudentBuilder(ALICE_LOWER_MATRIC).withMatricNumber("A9999999Z").build();
        assertDisplayed(ALICE_HIGHER_MATRIC, editedAlice, BOB, ZOE);
    }

    @Test
    public void execute_editStudentOutsideFilter_preservesFilterAndSorting() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort name");
        logic.execute("edit 1 g/T02");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE);
        logic.execute("edit 1 n/Zulu");
        assertDisplayed(ZOE, new StudentBuilder(ALICE_HIGHER_MATRIC).withName("Zulu").build());
    }

    @Test
    public void execute_deleteFirstSortedStudent_previewsAndDeletesDisplayedStudent() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort name");
        assertEquals(String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE, Messages.format(ALICE_LOWER_MATRIC), 1),
                logic.execute("delete 1").getFeedbackToUser());
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);

        logic.execute("delete 1 confirm");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE);
        assertEquals(List.of(ZOE, BOB, ALICE_HIGHER_MATRIC),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_deleteFirstDescendingStudent_deletesDisplayedStudent() throws Exception {
        logic.execute("sort name desc");
        logic.execute("delete 1 confirm");
        assertDisplayed(BOB, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC);
        assertEquals(List.of(BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC),
                model.getStudentDirectory().getStudentList());
    }

    @Test
    public void execute_invalidIndexAfterFilteredSort_preservesResults() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort name");
        assertThrows(CommandException.class,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("edit 4 n/Dana"));
        assertThrows(CommandException.class,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("delete 4 confirm"));
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_invalidSortOrQuery_preservesFilterAndSorting() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort name");
        for (String command : List.of("sort", "sort group", "sort reset", "sort name up", "find", "list g/T1")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        }
        logic.execute("edit 1 n/Wendy");
        assertDisplayed(ALICE_HIGHER_MATRIC,
                new StudentBuilder(ALICE_LOWER_MATRIC).withName("Wendy").build(), ZOE);
    }

    @Test
    public void execute_addAfterSort_restoresFullRosterInDirectoryOrder() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort name");
        logic.execute("add n/Aaron m/A7777777Z g/T02");
        List<Student> displayedStudents = model.getFilteredStudentList();
        assertEquals(ORIGINAL_ORDER, displayedStudents.subList(0, ORIGINAL_ORDER.size()));
        assertEquals("Aaron", displayedStudents.get(4).getName().fullName);
    }

    @Test
    public void execute_tagAndFollowUpQueries_resetSortingAndCanBeSorted() throws Exception {
        logic.execute("edit 1 t/needsHelp");
        logic.execute("edit 4 t/needsHelp");
        logic.execute("followup 1 on");
        logic.execute("followup 4 on");
        Student markedZoe = new StudentBuilder(ZOE).withTags("needsHelp").withFollowUp(true).build();
        Student markedAlice = new StudentBuilder(ALICE_LOWER_MATRIC)
                .withTags("needsHelp").withFollowUp(true).build();

        logic.execute("sort name");
        logic.execute("findtag needsHelp");
        assertDisplayed(markedZoe, markedAlice);
        logic.execute("sort name");
        assertDisplayed(markedAlice, markedZoe);

        logic.execute("followups");
        assertDisplayed(markedZoe, markedAlice);
        logic.execute("sort name");
        assertDisplayed(markedAlice, markedZoe);
    }

    @Test
    public void execute_followUpAfterSortedFilter_updatesDisplayedStudentAndKeepsSorting() throws Exception {
        for (int i = 1; i <= ORIGINAL_ORDER.size(); i++) {
            logic.execute("followup " + i + " on");
        }
        logic.execute("followups");
        logic.execute("sort name");
        logic.execute("followup 1 done");

        assertDisplayed(new StudentBuilder(ALICE_HIGHER_MATRIC).withFollowUp(true).build(),
                new StudentBuilder(BOB).withFollowUp(true).build(),
                new StudentBuilder(ZOE).withFollowUp(true).build());
        assertEquals(List.of(new StudentBuilder(ZOE).withFollowUp(true).build(),
                new StudentBuilder(BOB).withFollowUp(true).build(),
                new StudentBuilder(ALICE_HIGHER_MATRIC).withFollowUp(true).build(), ALICE_LOWER_MATRIC),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_duplicateMatricAfterSortedFilter_preservesFilterAndSorting() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort name");
        assertThrows(CommandException.class,
                EditCommand.MESSAGE_DUPLICATE_STUDENT, () -> logic.execute("edit 1 m/A0000004D"));
        assertThrows(CommandException.class,
                AddCommand.MESSAGE_DUPLICATE_STUDENT, () -> logic.execute("add n/Aaron m/A0000004D g/T01"));
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        assertOriginalDirectoryAndStorage();

        logic.execute("edit 1 n/Wendy");
        assertDisplayed(ALICE_HIGHER_MATRIC,
                new StudentBuilder(ALICE_LOWER_MATRIC).withName("Wendy").build(), ZOE);
    }

    @Test
    public void execute_sortedSearchResultNoLongerMatches_removesStudentAndKeepsSorting() throws Exception {
        logic.execute("find alice Zoe");
        logic.execute("sort name");
        logic.execute("edit 1 n/Wendy");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE);
        logic.execute("edit 1 n/zoe");
        assertDisplayed(new StudentBuilder(ALICE_HIGHER_MATRIC).withName("zoe").build(), ZOE);
    }

    @Test
    public void execute_clearAfterSort_restoresDefaultSorting() throws Exception {
        logic.execute("sort name desc");
        logic.execute("clear");
        assertDisplayed();
        assertEquals(new ModelManager(), model);
    }

    @Test
    public void execute_reloadAfterSort_usesOriginalDirectoryOrder() throws Exception {
        logic.execute("sort name desc");
        Model reloadedModel = new ModelManager(studentDirectoryStorage.readStudentDirectory().orElseThrow(),
                new UserPrefs());
        assertEquals(ORIGINAL_ORDER, reloadedModel.getFilteredStudentList());
    }

    private void assertDisplayed(Student... students) {
        assertEquals(List.of(students), logic.getFilteredStudentList());
    }

    private void assertOriginalDirectoryAndStorage() throws Exception {
        assertEquals(ORIGINAL_ORDER, model.getStudentDirectory().getStudentList());
        assertEquals(ORIGINAL_ORDER, studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }
}
