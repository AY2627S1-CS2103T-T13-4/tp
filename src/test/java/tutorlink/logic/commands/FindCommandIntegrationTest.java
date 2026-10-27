package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.BENSON;
import static tutorlink.testutil.TypicalStudents.CARL;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;
import static tutorlink.testutil.TypicalStudents.getTypicalStudents;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.logic.Logic;
import tutorlink.logic.LogicManager;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.StudentBuilder;

/**
 * Exercises student searches through parsing, filtering, and the normal storage path.
 */
public class FindCommandIntegrationTest {

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonStudentDirectoryStorage studentDirectoryStorage;

    @BeforeEach
    public void setUp() throws IOException {
        model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        studentDirectoryStorage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("studentDirectory.json"));
        studentDirectoryStorage.saveStudentDirectory(model.getStudentDirectory());
        StorageManager storage = new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_matricNumberIgnoringCase_findsStudent() throws Exception {
        assertSearch("find \ta0000003c  ", List.of(BENSON));
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_mixedRepeatedKeywords_returnsUnionInDirectoryOrder() throws Exception {
        assertSearch("find A0000004D alice A0000002B ALICE", List.of(ALICE, CARL));
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_findAfterPreviousFilter_searchesCompleteDirectory() throws Exception {
        logic.execute("list g/t01");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredStudentList());
        assertSearch("find A0000004D", List.of(CARL));
        assertSearch("find Benson", List.of(BENSON));
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_groupFilterAfterSearch_replacesSearchResults() throws Exception {
        assertSearch("find Carl", List.of(CARL));
        logic.execute("list g/t01");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredStudentList());
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_editSearchResult_preservesFilterAndSavesFullDirectory() throws Exception {
        assertSearch("find Carl", List.of(CARL));
        logic.execute("edit 1 n/Charlie Tan");

        Student editedCarl = new StudentBuilder(CARL).withName("Charlie Tan").build();
        List<Student> expectedStudents = getTypicalStudents();
        expectedStudents.set(expectedStudents.indexOf(CARL), editedCarl);
        assertEquals(List.of(), model.getFilteredStudentList());
        assertEquals(expectedStudents, model.getStudentDirectory().getStudentList());
        assertEquals(expectedStudents, studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
        assertSearch("find a0000004d", List.of(editedCarl));
    }

    @Test
    public void execute_invalidIndexAfterSearch_preservesResultsAndSavedDirectory() throws Exception {
        assertSearch("find Carl", List.of(CARL));

        assertThrows(CommandException.class,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("edit 2 n/Charlie Tan"));
        assertThrows(CommandException.class,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, () -> logic.execute("delete 2 confirm"));

        assertEquals(List.of(CARL), model.getFilteredStudentList());
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_addAfterSearch_resetsViewAndRemainsSearchableAfterReload() throws Exception {
        assertSearch("find Carl", List.of(CARL));
        logic.execute("add n/Dana Tan m/a7777777z g/t01");

        Student addedStudent = new Student(new Name("Dana Tan"), new MatricNumber("A7777777Z"),
                new TutorialGroup("T01"), null, null, null, Set.of());
        List<Student> expectedStudents = getTypicalStudents();
        expectedStudents.add(addedStudent);
        assertEquals(expectedStudents, model.getFilteredStudentList());
        assertSearch("find a7777777z", List.of(addedStudent));

        model = new ModelManager(studentDirectoryStorage.readStudentDirectory().orElseThrow(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(studentDirectoryStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));
        assertEquals(expectedStudents, model.getStudentDirectory().getStudentList());
        assertEquals(expectedStudents, model.getFilteredStudentList());
        assertSearch("find Dana", List.of(addedStudent));
    }

    @Test
    public void execute_noMatchThenList_restoresCompleteDirectory() throws Exception {
        assertSearch("find A9999999Z", List.of());
        logic.execute("list");
        assertEquals(getTypicalStudents(), model.getFilteredStudentList());
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_emptySearch_preservesPreviousResults() throws Exception {
        assertSearch("find Alice", List.of(ALICE));
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expectedMessage, () -> logic.execute("find \t"));
        assertEquals(List.of(ALICE), model.getFilteredStudentList());
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_searchThenDelete_usesResultIndex() throws Exception {
        assertSearch("find A0000004D", List.of(CARL));
        logic.execute("delete 1");
        assertEquals(List.of(CARL), model.getFilteredStudentList());
        assertDirectoryUnchanged();
        logic.execute("delete 1 confirm");

        List<Student> remaining = getTypicalStudents();
        remaining.remove(CARL);
        assertEquals(remaining, model.getStudentDirectory().getStudentList());
        assertEquals(List.of(), model.getFilteredStudentList());
        assertEquals(remaining, studentDirectoryStorage.readStudentDirectory().orElseThrow().getStudentList());
    }

    private void assertSearch(String input, List<Student> expectedStudents) throws Exception {
        CommandResult result = logic.execute(input);
        assertEquals(expectedStudents.size() + " student(s) listed!", result.getFeedbackToUser());
        assertEquals(expectedStudents, model.getFilteredStudentList());
    }

    private void assertDirectoryUnchanged() throws Exception {
        assertEquals(getTypicalStudentDirectory(), model.getStudentDirectory());
        assertEquals(getTypicalStudentDirectory(), studentDirectoryStorage.readStudentDirectory().orElseThrow());
    }
}
