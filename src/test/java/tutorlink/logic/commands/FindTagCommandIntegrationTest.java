package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.BENSON;
import static tutorlink.testutil.TypicalStudents.CARL;
import static tutorlink.testutil.TypicalStudents.DANIEL;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;
import static tutorlink.testutil.TypicalStudents.getTypicalStudents;

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
import tutorlink.model.tag.Tag;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.StudentBuilder;

/**
 * Exercises tag searches through parsing, editing, deletion, and persisted student records.
 */
public class FindTagCommandIntegrationTest {

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonStudentDirectoryStorage studentDirectoryStorage;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        studentDirectoryStorage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("studentDirectory.json"));
        logic = createLogic();
    }

    @Test
    public void execute_addTwoTaggedStudentsThenClearOne_findsOnlyRemainingStudentAfterReload() throws Exception {
        logic.execute("add n/Dana Tan m/A7777777Z g/T01 t/needsHelp");
        logic.execute("add n/Evan Lim m/A8888888Z g/T02 t/NEEDSHELP t/other");
        List<Student> expectedStudents = List.copyOf(model.getStudentDirectory().getStudentList());
        Student dana = expectedStudents.get(expectedStudents.size() - 2);
        Student evan = expectedStudents.getLast();
        assertSearch("findtag needshelp", List.of(dana, evan));

        logic.execute("edit 1 t/");
        Student danaWithoutTags = new StudentBuilder(dana).withTags().build();
        assertEquals(List.of(evan), model.getFilteredStudentList());
        assertSearch("findtag NEEDSHELP", List.of(evan));

        List<Student> savedStudents = getTypicalStudents();
        savedStudents.add(danaWithoutTags);
        savedStudents.add(evan);
        assertStoredStudents(savedStudents);
        model = new ModelManager(studentDirectoryStorage.readStudentDirectory().orElseThrow(), new UserPrefs());
        logic = createLogic();
        assertSearch("findtag needsHelp", List.of(evan));
        assertStoredStudents(savedStudents);
    }

    @Test
    public void execute_searchAndGroupFilters_replacePreviousResults() throws Exception {
        logic.execute("list g/T02");
        assertEquals(List.of(CARL, DANIEL), model.getFilteredStudentList());
        assertSearch("findtag FRIENDS", List.of(ALICE, BENSON, DANIEL));
        assertSearch("findtag owesMoney", List.of(BENSON));
        assertSearch("find Carl", List.of(CARL));
        assertSearch("findtag friends", List.of(ALICE, BENSON, DANIEL));
        logic.execute("list g/T02");
        assertEquals(List.of(CARL, DANIEL), model.getFilteredStudentList());
        assertStoredStudents(getTypicalStudents());
    }

    @Test
    public void execute_noMatchThenSearchAndList_restoresResults() throws Exception {
        assertSearch("findtag friend", List.of());
        assertSearch("findtag owesMoney", List.of(BENSON));
        assertSearch("findtag unknown", List.of());
        logic.execute("list");
        assertEquals(getTypicalStudents(), model.getFilteredStudentList());
        assertStoredStudents(getTypicalStudents());
    }

    @Test
    public void execute_invalidTag_preservesPreviousResultsAndSavedDirectory() throws Exception {
        assertSearch("findtag owesMoney", List.of(BENSON));
        for (String command : List.of("findtag t/friends", "findtag friends family", "findtag needs-help")) {
            assertThrows(ParseException.class, Tag.MESSAGE_CONSTRAINTS, () -> logic.execute(command));
        }
        assertThrows(ParseException.class,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindTagCommand.MESSAGE_USAGE), () ->
                logic.execute("findtag \t"));
        assertEquals(List.of(BENSON), model.getFilteredStudentList());
        assertStoredStudents(getTypicalStudents());
    }

    @Test
    public void execute_editOtherFieldThenReplaceTags_usesResultIndexAndRetainsFilter() throws Exception {
        assertSearch("findtag owesMoney", List.of(BENSON));
        logic.execute("edit 1 n/Benny Tan");
        Student renamedBenson = new StudentBuilder(BENSON).withName("Benny Tan").build();
        assertEquals(List.of(renamedBenson), model.getFilteredStudentList());

        logic.execute("edit 1 t/paid");
        Student paidBenson = new StudentBuilder(renamedBenson).withTags("paid").build();
        assertEquals(List.of(), model.getFilteredStudentList());
        assertSearch("findtag owesMoney", List.of());
        assertSearch("findtag PAID", List.of(paidBenson));
        List<Student> expectedStudents = getTypicalStudents();
        expectedStudents.set(expectedStudents.indexOf(BENSON), paidBenson);
        assertStoredStudents(expectedStudents);
    }

    @Test
    public void execute_moveAndRetagSearchResultThenClearTags_preservesRemarkAfterReload() throws Exception {
        Student remarkedBenson = new StudentBuilder(BENSON).withRemark("Review recursion before Friday.").build();
        model.setStudent(BENSON, remarkedBenson);
        assertSearch("findtag owesMoney", List.of(remarkedBenson));

        logic.execute("edit 1 g/T09 t/needsHelp t/paid");
        Student movedBenson = new StudentBuilder(remarkedBenson).withTutorialGroup("T09")
                .withTags("needsHelp", "paid").build();
        assertEquals(List.of(), model.getFilteredStudentList());
        assertSearch("findtag NEEDSHELP", List.of(movedBenson));
        List<Student> expectedStudents = getTypicalStudents();
        int bensonRosterIndex = expectedStudents.indexOf(BENSON);
        expectedStudents.set(bensonRosterIndex, movedBenson);
        assertStoredStudents(expectedStudents);

        model = new ModelManager(studentDirectoryStorage.readStudentDirectory().orElseThrow(), new UserPrefs());
        logic = createLogic();
        assertSearch("findtag needsHelp", List.of(movedBenson));
        logic.execute("edit 1 t/");
        Student bensonWithoutTags = new StudentBuilder(movedBenson).withTags().build();
        expectedStudents.set(bensonRosterIndex, bensonWithoutTags);
        assertEquals(List.of(), model.getFilteredStudentList());
        assertSearch("findtag paid", List.of());
        assertStoredStudents(expectedStudents);
    }

    @Test
    public void execute_removeOneTag_preservesOtherTagsAndRenumbersDeleteResults() throws Exception {
        assertSearch("findtag friends", List.of(ALICE, BENSON, DANIEL));
        logic.execute("edit 2 t/owesMoney");
        Student bensonWithoutFriends = new StudentBuilder(BENSON).withTags("owesMoney").build();
        assertEquals(List.of(ALICE, DANIEL), model.getFilteredStudentList());

        CommandResult preview = logic.execute("delete 2");
        assertEquals(String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE, Messages.format(DANIEL), 2),
                preview.getFeedbackToUser());
        List<Student> expectedStudents = getTypicalStudents();
        expectedStudents.set(expectedStudents.indexOf(BENSON), bensonWithoutFriends);
        assertStoredStudents(expectedStudents);

        logic.execute("delete 2 confirm");
        expectedStudents.remove(DANIEL);
        assertEquals(List.of(ALICE), model.getFilteredStudentList());
        assertStoredStudents(expectedStudents);
        assertSearch("findtag owesMoney", List.of(bensonWithoutFriends));
    }

    @Test
    public void execute_changeTagCase_retainsMatchingStudent() throws Exception {
        assertSearch("findtag owesMoney", List.of(BENSON));
        logic.execute("edit 1 t/OWESMONEY t/friends");
        Student editedBenson = new StudentBuilder(BENSON).withTags("OWESMONEY", "friends").build();
        assertEquals(List.of(editedBenson), model.getFilteredStudentList());
        assertSearch("findtag owesMoney", List.of(editedBenson));
        List<Student> expectedStudents = getTypicalStudents();
        expectedStudents.set(expectedStudents.indexOf(BENSON), editedBenson);
        assertStoredStudents(expectedStudents);
    }

    @Test
    public void execute_invalidDisplayedIndex_preservesResultsAndSavedDirectory() throws Exception {
        assertSearch("findtag owesMoney", List.of(BENSON));
        for (String command : List.of("edit 2 t/paid", "delete 2", "delete 2 confirm")) {
            assertThrows(CommandException.class, MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute(command));
        }
        assertEquals(List.of(BENSON), model.getFilteredStudentList());
        assertStoredStudents(getTypicalStudents());

        assertSearch("findtag unknown", List.of());
        assertThrows(CommandException.class, MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () ->
                logic.execute("delete 1 confirm"));
        assertThrows(CommandException.class, MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () ->
                logic.execute("edit 1 t/paid"));
        assertStoredStudents(getTypicalStudents());
    }

    private Logic createLogic() {
        return new LogicManager(model, new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));
    }

    private void assertSearch(String input, List<Student> expectedStudents) throws Exception {
        CommandResult result = logic.execute(input);
        assertEquals(expectedStudents.size() + " student(s) listed!", result.getFeedbackToUser());
        assertEquals(expectedStudents, model.getFilteredStudentList());
    }

    private void assertStoredStudents(List<Student> expectedStudents) throws Exception {
        assertEquals(expectedStudents, model.getStudentDirectory().getStudentList());
        assertEquals(expectedStudents, studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }
}
