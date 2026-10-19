package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.BENSON;
import static tutorlink.testutil.TypicalStudents.DANIEL;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.Student;
import tutorlink.model.student.TagMatchesPredicate;
import tutorlink.model.tag.Tag;
import tutorlink.testutil.StudentBuilder;

public class FindTagCommandTest {

    private final Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());

    @Test
    public void constructor_nullPredicate_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FindTagCommand(null));
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> commandFor("friends").execute(null));
    }

    @Test
    public void execute_emptyDirectory_returnsZeroResults() {
        Model emptyModel = new ModelManager();
        assertEquals("0 student(s) listed!", commandFor("friends").execute(emptyModel).getFeedbackToUser());
        assertEquals(List.of(), emptyModel.getFilteredStudentList());
    }

    @Test
    public void execute_noMatches_clearsPreviousResultsWithoutChangingDirectory() {
        CommandResult result = commandFor("unknown").execute(model);
        assertEquals("0 student(s) listed!", result.getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredStudentList());
        assertEquals(getTypicalStudentDirectory(), model.getStudentDirectory());
    }

    @Test
    public void execute_tagIgnoringCase_searchesFullDirectoryInRosterOrder() {
        model.updateFilteredStudentList(student -> student.equals(BENSON));
        CommandResult result = commandFor("FRIENDS").execute(model);
        assertEquals("3 student(s) listed!", result.getFeedbackToUser());
        assertEquals(List.of(ALICE, BENSON, DANIEL), model.getFilteredStudentList());
        assertEquals(getTypicalStudentDirectory(), model.getStudentDirectory());
    }

    @Test
    public void execute_multipleMatchingTags_listsStudentOnce() {
        Student aliceWithTags = new StudentBuilder(ALICE).withTags("friends", "FRIENDS", "other").build();
        model.setStudent(ALICE, aliceWithTags);
        CommandResult result = commandFor("friends").execute(model);
        assertEquals("3 student(s) listed!", result.getFeedbackToUser());
        assertEquals(List.of(aliceWithTags, BENSON, DANIEL), model.getFilteredStudentList());
        assertEquals(aliceWithTags, model.getStudentDirectory().getStudentList().getFirst());
    }

    @Test
    public void equals() {
        FindTagCommand command = commandFor("friends");
        FindTagCommand copy = commandFor("friends");
        assertTrue(command.equals(command));
        assertTrue(command.equals(copy));
        assertEquals(command.hashCode(), copy.hashCode());
        assertFalse(command.equals(commandFor("family")));
        assertFalse(command.equals(null));
        assertFalse(command.equals(1));
    }

    @Test
    public void toStringMethod() {
        TagMatchesPredicate predicate = new TagMatchesPredicate(new Tag("friends"));
        assertEquals(FindTagCommand.class.getCanonicalName() + "{predicate=" + predicate + "}",
                new FindTagCommand(predicate).toString());
    }

    private FindTagCommand commandFor(String tag) {
        return new FindTagCommand(new TagMatchesPredicate(new Tag(tag)));
    }
}
