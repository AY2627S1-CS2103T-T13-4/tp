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
public class SortGroupCommandIntegrationTest {
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
    public void setUp() throws IOException {
        StudentDirectory directory = new StudentDirectory();
        directory.setStudents(ORIGINAL_ORDER);
        model = new ModelManager(directory, new UserPrefs());
        studentDirectoryStorage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("students.json"));
        studentDirectoryStorage.saveStudentDirectory(model.getStudentDirectory());
        logic = new LogicManager(model, new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    @Test
    public void execute_emptyAndSingleStudentResults_success() throws Exception {
        logic.execute("list g/T99");
        logic.execute("sort group");
        assertDisplayed();
        logic.execute("sort group desc");
        assertDisplayed();

        logic.execute("find bob");
        logic.execute("sort group");
        assertDisplayed(BOB);
        logic.execute("sort group desc");
        assertDisplayed(BOB);
    }

    @Test
    public void execute_newListOrSearch_restoresDirectoryOrder() throws Exception {
        logic.execute("sort group");
        logic.execute("list");
        assertDisplayed(ZOE, BOB, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);

        logic.execute("sort group");
        logic.execute("list g/T01");
        assertDisplayed(ZOE, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);

        logic.execute("sort group");
        logic.execute("find alice Zoe");
        assertDisplayed(ZOE, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_addAfterGroupSort_restoresFullRosterInDirectoryOrder() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort group desc");
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

        logic.execute("sort group");
        logic.execute("findtag needsHelp");
        assertDisplayed(markedZoe, markedAlice);
        logic.execute("sort group");
        assertDisplayed(markedAlice, markedZoe);

        logic.execute("followups");
        assertDisplayed(markedZoe, markedAlice);
        logic.execute("sort group");
        assertDisplayed(markedAlice, markedZoe);
    }

    @Test
    public void execute_clearAfterSort_restoresDefaultSorting() throws Exception {
        logic.execute("sort group desc");
        logic.execute("clear");
        assertDisplayed();
        assertEquals(new ModelManager(), model);
    }

    @Test
    public void execute_reloadAfterSort_usesOriginalDirectoryOrder() throws Exception {
        logic.execute("sort group desc");
        Model reloadedModel = new ModelManager(studentDirectoryStorage.readStudentDirectory().orElseThrow(),
                new UserPrefs());
        assertEquals(ORIGINAL_ORDER, reloadedModel.getFilteredStudentList());
    }

    @Test
    public void execute_groupAscending_sortsByGroupThenMatricWithoutChangingStorage() throws Exception {
        ObservableList<Student> displayedStudents = logic.getFilteredStudentList();
        assertEquals(SortGroupCommand.MESSAGE_SUCCESS_ASCENDING, logic.execute("sort group").getFeedbackToUser());
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE, BOB);
        assertSame(displayedStudents, logic.getFilteredStudentList());

        logic.execute("sort group asc");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE, BOB);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_groupDescending_keepsMatricTieBreakAscending() throws Exception {
        assertEquals(SortGroupCommand.MESSAGE_SUCCESS_DESCENDING,
                logic.execute("sort group desc").getFeedbackToUser());
        assertDisplayed(BOB, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_groupSort_comparesNormalizedLettersAndBothDigits() throws Exception {
        Student groupT10 = new StudentBuilder(ZOE).withTutorialGroup("t10").build();
        Student groupT02 = new StudentBuilder(BOB).withTutorialGroup("T02").build();
        Student sameGroupT02 = new StudentBuilder(ALICE_HIGHER_MATRIC)
                .withName("Zelda").withTutorialGroup("t02").build();
        Student groupA99 = new StudentBuilder(ALICE_LOWER_MATRIC).withTutorialGroup("a99").build();
        StudentDirectory directory = new StudentDirectory();
        directory.setStudents(List.of(groupT10, groupT02, sameGroupT02, groupA99));
        model.setStudentDirectory(directory);

        logic.execute("sort group");
        assertDisplayed(groupA99, sameGroupT02, groupT02, groupT10);
        logic.execute("sort group desc");
        assertDisplayed(groupT10, sameGroupT02, groupT02, groupA99);
    }

    @Test
    public void execute_groupSortAfterFilters_keepsOnlyMatchingStudents() throws Exception {
        logic.execute("list g/t01");
        logic.execute("sort group desc");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        logic.execute("sort group");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);

        logic.execute("find bob alice");
        logic.execute("sort group");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, BOB);
        logic.execute("sort group desc");
        assertDisplayed(BOB, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_editGroupAfterAscendingSort_updatesDisplayedStudentAndReorders() throws Exception {
        logic.execute("sort group");
        logic.execute("edit 1 g/t03");
        Student editedAlice = new StudentBuilder(ALICE_LOWER_MATRIC).withTutorialGroup("T03").build();
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE, BOB, editedAlice);
        assertEquals(List.of(ZOE, BOB, ALICE_HIGHER_MATRIC, editedAlice),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_editGroupAfterDescendingSort_reordersInDescendingOrder() throws Exception {
        logic.execute("sort group desc");
        logic.execute("edit 1 g/A01");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE,
                new StudentBuilder(BOB).withTutorialGroup("A01").build());
    }

    @Test
    public void execute_editMatricAfterGroupSort_reordersWithinGroupInBothDirections() throws Exception {
        logic.execute("sort group desc");
        logic.execute("edit 2 m/A9999999Z");
        Student editedAlice = new StudentBuilder(ALICE_LOWER_MATRIC).withMatricNumber("A9999999Z").build();
        assertDisplayed(BOB, ALICE_HIGHER_MATRIC, ZOE, editedAlice);
        logic.execute("sort group");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE, editedAlice, BOB);
    }

    @Test
    public void execute_editGroupOutsideFilter_removesStudentAndKeepsGroupSorting() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort group desc");
        logic.execute("edit 1 g/T02");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE);
        logic.execute("edit 1 m/A9999999Z");
        assertDisplayed(ZOE, new StudentBuilder(ALICE_HIGHER_MATRIC).withMatricNumber("A9999999Z").build());
    }

    @Test
    public void execute_editSearchMatchAfterGroupSort_removesStudentAndKeepsSorting() throws Exception {
        logic.execute("find A0000004D alice Zoe");
        logic.execute("sort group desc");
        logic.execute("edit 1 m/A0000005E");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);

        logic.execute("edit 1 n/Wendy");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE);
        logic.execute("edit 1 m/A9999999Z");
        Student editedAlice = new StudentBuilder(ALICE_HIGHER_MATRIC).withMatricNumber("A9999999Z").build();
        assertDisplayed(ZOE, editedAlice);
        assertEquals(List.of(ZOE, new StudentBuilder(BOB).withMatricNumber("A0000005E").build(),
                editedAlice, new StudentBuilder(ALICE_LOWER_MATRIC).withName("Wendy").build()),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_removeTagAfterGroupSort_removesStudentAndKeepsSorting() throws Exception {
        logic.execute("edit 1 t/needsHelp");
        logic.execute("edit 2 t/needsHelp");
        logic.execute("edit 4 t/needsHelp");
        logic.execute("findtag needsHelp");
        logic.execute("sort group desc");
        logic.execute("edit 1 t/other");
        Student taggedZoe = new StudentBuilder(ZOE).withTags("needsHelp").build();
        Student taggedAlice = new StudentBuilder(ALICE_LOWER_MATRIC).withTags("needsHelp").build();
        assertDisplayed(taggedAlice, taggedZoe);

        logic.execute("edit 1 m/A9999999Z");
        Student editedAlice = new StudentBuilder(taggedAlice).withMatricNumber("A9999999Z").build();
        assertDisplayed(taggedZoe, editedAlice);
        assertEquals(List.of(taggedZoe, new StudentBuilder(BOB).withTags("other").build(),
                ALICE_HIGHER_MATRIC, editedAlice),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_deleteAfterFilteredGroupSort_previewsAndDeletesDisplayedStudent() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort group desc");
        assertEquals(String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE, Messages.format(ALICE_LOWER_MATRIC), 1),
                logic.execute("delete 1").getFeedbackToUser());
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        logic.execute("delete 1 confirm");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE);
        assertEquals(List.of(ZOE, BOB, ALICE_HIGHER_MATRIC),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_deleteAfterDescendingGroupSort_deletesDisplayedStudentAndKeepsSorting() throws Exception {
        logic.execute("sort group desc");
        logic.execute("delete 1 confirm");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        assertEquals(List.of(ZOE, ALICE_HIGHER_MATRIC, ALICE_LOWER_MATRIC),
                model.getStudentDirectory().getStudentList());
    }

    @Test
    public void execute_followUpAfterGroupSort_updatesDisplayedStudentAndKeepsFilterAndSorting() throws Exception {
        for (int i = 1; i <= ORIGINAL_ORDER.size(); i++) {
            logic.execute("followup " + i + " on");
        }
        logic.execute("followups");
        logic.execute("sort group desc");
        logic.execute("followup 1 done");
        assertDisplayed(new StudentBuilder(ALICE_LOWER_MATRIC).withFollowUp(true).build(),
                new StudentBuilder(ALICE_HIGHER_MATRIC).withFollowUp(true).build(),
                new StudentBuilder(ZOE).withFollowUp(true).build());
        assertEquals(List.of(new StudentBuilder(ZOE).withFollowUp(true).build(), BOB,
                new StudentBuilder(ALICE_HIGHER_MATRIC).withFollowUp(true).build(),
                new StudentBuilder(ALICE_LOWER_MATRIC).withFollowUp(true).build()),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_invalidCommandsAfterGroupSort_preservesFilterAndSorting() throws Exception {
        logic.execute("list g/T01");
        logic.execute("sort group desc");
        for (String command : List.of("sort group up", "sort reset extra", "find", "list g/T1")) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        }
        assertThrows(CommandException.class,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("edit 4 g/T03"));
        assertThrows(CommandException.class,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("delete 4 confirm"));
        assertThrows(CommandException.class,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("followup 4 on"));
        assertThrows(CommandException.class,
                EditCommand.MESSAGE_DUPLICATE_STUDENT, () -> logic.execute("edit 1 m/A0000004D"));
        assertThrows(CommandException.class,
                AddCommand.MESSAGE_DUPLICATE_STUDENT, () -> logic.execute("add n/Aaron m/A0000004D g/T01"));
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        assertOriginalDirectoryAndStorage();
        logic.execute("edit 1 m/A9999999Z");
        assertDisplayed(ALICE_HIGHER_MATRIC, ZOE,
                new StudentBuilder(ALICE_LOWER_MATRIC).withMatricNumber("A9999999Z").build());
    }

    @Test
    public void execute_switchSortFieldsInBothDirections_replacesPreviousRule() throws Exception {
        logic.execute("sort name desc");
        assertDisplayed(ZOE, BOB, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC);
        logic.execute("sort group asc");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE, BOB);
        logic.execute("sort name asc");
        assertDisplayed(ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, BOB, ZOE);
        logic.execute("sort group desc");
        assertDisplayed(BOB, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC, ZOE);
        logic.execute("sort name desc");
        assertDisplayed(ZOE, BOB, ALICE_LOWER_MATRIC, ALICE_HIGHER_MATRIC);
        assertOriginalDirectoryAndStorage();
    }

    @Test
    public void execute_editAfterSwitchingSortFields_preservesSearchAndUpdatesDisplayedStudent() throws Exception {
        logic.execute("find alice bob");
        logic.execute("sort name desc");
        logic.execute("sort group");
        logic.execute("edit 1 g/T03");
        Student editedAlice = new StudentBuilder(ALICE_LOWER_MATRIC).withTutorialGroup("T03").build();
        assertDisplayed(ALICE_HIGHER_MATRIC, BOB, editedAlice);

        logic.execute("sort name desc");
        assertDisplayed(BOB, editedAlice, ALICE_HIGHER_MATRIC);
        logic.execute("edit 1 n/Wendy");
        Student editedBob = new StudentBuilder(BOB).withName("Wendy").build();
        assertDisplayed(editedAlice, ALICE_HIGHER_MATRIC);
        assertEquals(List.of(ZOE, editedBob, ALICE_HIGHER_MATRIC, editedAlice),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_deleteAfterSwitchingSortFields_usesUpdatedIndicesAndKeepsSearch() throws Exception {
        logic.execute("find alice bob");
        logic.execute("sort name desc");
        logic.execute("sort group");
        assertEquals(String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE, Messages.format(ALICE_LOWER_MATRIC), 1),
                logic.execute("delete 1").getFeedbackToUser());
        logic.execute("delete 1 confirm");
        assertDisplayed(ALICE_HIGHER_MATRIC, BOB);

        logic.execute("sort name desc");
        assertDisplayed(BOB, ALICE_HIGHER_MATRIC);
        logic.execute("delete 1 confirm");
        assertDisplayed(ALICE_HIGHER_MATRIC);
        assertEquals(List.of(ZOE, ALICE_HIGHER_MATRIC),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    public void execute_followUpAfterSwitchingSortFields_usesUpdatedIndicesAndKeepsFilter() throws Exception {
        for (int i = 1; i <= ORIGINAL_ORDER.size(); i++) {
            logic.execute("followup " + i + " on");
        }
        Student markedAliceLow = new StudentBuilder(ALICE_LOWER_MATRIC).withFollowUp(true).build();
        Student markedAliceHigh = new StudentBuilder(ALICE_HIGHER_MATRIC).withFollowUp(true).build();
        Student markedZoe = new StudentBuilder(ZOE).withFollowUp(true).build();
        logic.execute("followups");
        logic.execute("sort name");
        logic.execute("sort group desc");
        logic.execute("followup 1 done");
        assertDisplayed(markedAliceLow, markedAliceHigh, markedZoe);

        logic.execute("sort name desc");
        assertDisplayed(markedZoe, markedAliceLow, markedAliceHigh);
        logic.execute("followup 1 done");
        assertDisplayed(markedAliceLow, markedAliceHigh);
        assertEquals(List.of(ZOE, BOB, markedAliceHigh, markedAliceLow),
                studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    private void assertDisplayed(Student... students) {
        assertEquals(List.of(students), logic.getFilteredStudentList());
    }

    private void assertOriginalDirectoryAndStorage() throws Exception {
        assertEquals(ORIGINAL_ORDER, model.getStudentDirectory().getStudentList());
        assertEquals(ORIGINAL_ORDER, studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }
}
