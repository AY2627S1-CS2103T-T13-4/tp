package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.logic.commands.CommandTestUtil.assertCommandFailure;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorlink.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.Test;

import tutorlink.commons.core.index.Index;
import tutorlink.logic.Messages;
import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.NameContainsKeywordsPredicate;
import tutorlink.model.student.Student;
import tutorlink.testutil.StudentBuilder;

public class RemarkCommandTest {

    private final Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());

    @Test
    public void execute_setReplaceAndClear_preservesOtherFields() throws Exception {
        Student original = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student withExistingData = new StudentBuilder(original)
                .withRemark("Old remark")
                .withFollowUp(true)
                .build();
        model.setStudent(original, withExistingData);

        new RemarkCommand(INDEX_FIRST_STUDENT, "New remark").execute(model);
        Student replaced = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student expectedReplaced = new StudentBuilder(withExistingData).withRemark("New remark").build();
        assertEquals(expectedReplaced, replaced);

        new RemarkCommand(INDEX_FIRST_STUDENT, null).execute(model);
        Student cleared = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        assertEquals(new StudentBuilder(expectedReplaced).withRemark(null).build(), cleared);
    }

    @Test
    public void execute_usesDisplayedIndex() throws Exception {
        model.updateFilteredStudentList(new NameContainsKeywordsPredicate(java.util.List.of("Benson")));

        new RemarkCommand(INDEX_FIRST_STUDENT, "Filtered remark").execute(model);

        assertEquals("Filtered remark", model.getFilteredStudentList().getFirst().getRemark());
        assertEquals("A0000003C", model.getFilteredStudentList().getFirst().getMatricNumber().value);
    }

    @Test
    public void execute_invalidDisplayedIndex_failure() {
        Index invalidIndex = Index.fromOneBased(model.getFilteredStudentList().size() + 1);

        assertCommandFailure(new RemarkCommand(invalidIndex, "remark"), model,
                Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_STUDENT, "remark");
        RemarkCommand equivalentCommand = new RemarkCommand(INDEX_FIRST_STUDENT, "remark");

        assertTrue(command.equals(command));
        assertTrue(command.equals(equivalentCommand));
        assertEquals(command.hashCode(), equivalentCommand.hashCode());
        assertEquals(command.hashCode(), new RemarkCommand(Index.fromOneBased(1), "remark").hashCode());
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new RemarkCommand(INDEX_SECOND_STUDENT, "remark")));
        assertFalse(command.equals(new RemarkCommand(INDEX_FIRST_STUDENT, "different")));
    }

    @Test
    public void toStringMethod() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_STUDENT, "remark");
        String expected = RemarkCommand.class.getCanonicalName()
                + "{index=" + INDEX_FIRST_STUDENT + ", remark=remark}";

        assertEquals(expected, command.toString());
    }
}
