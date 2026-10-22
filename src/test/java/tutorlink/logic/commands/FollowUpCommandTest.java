package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static tutorlink.logic.commands.CommandTestUtil.assertCommandFailure;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.BENSON;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.Test;

import tutorlink.commons.core.index.Index;
import tutorlink.logic.Messages;
import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.Student;
import tutorlink.testutil.StudentBuilder;

public class FollowUpCommandTest {
    private final Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());

    @Test
    public void execute_markAndComplete_preservesOtherFields() throws Exception {
        Student original = new StudentBuilder(ALICE).withRemark("Discuss recursion").build();
        model.setStudent(ALICE, original);

        new FollowUpCommand(INDEX_FIRST_STUDENT, true).execute(model);
        assertEquals(new StudentBuilder(original).withFollowUp(true).build(),
                model.getFilteredStudentList().getFirst());

        new FollowUpCommand(INDEX_FIRST_STUDENT, false).execute(model);
        assertEquals(original, model.getFilteredStudentList().getFirst());
    }

    @Test
    public void execute_invalidDisplayedIndex_doesNotChangeModel() {
        Index invalidIndex = Index.fromOneBased(model.getFilteredStudentList().size() + 1);
        assertCommandFailure(new FollowUpCommand(invalidIndex, true), model, MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_filteredList_usesDisplayedIndex() throws Exception {
        model.updateFilteredStudentList(student -> student.isSameStudent(BENSON));
        new FollowUpCommand(INDEX_FIRST_STUDENT, true).execute(model);
        assertEquals(BENSON.withFollowUp(true), model.getFilteredStudentList().getFirst());
        assertEquals(ALICE, model.getStudentDirectory().getStudentList().getFirst());
        assertCommandFailure(new FollowUpCommand(Index.fromOneBased(2), false), model,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_repeatedMarkAndCompletion_keepsOriginalRecordAndExplainsStatus() throws Exception {
        FollowUpCommand mark = new FollowUpCommand(INDEX_FIRST_STUDENT, true);
        FollowUpCommand complete = new FollowUpCommand(INDEX_FIRST_STUDENT, false);
        assertEquals(String.format(FollowUpCommand.MESSAGE_ALREADY_DONE, Messages.format(ALICE)),
                complete.execute(model).getFeedbackToUser());
        assertSame(ALICE, model.getFilteredStudentList().getFirst());

        mark.execute(model);
        Student marked = model.getFilteredStudentList().getFirst();
        assertEquals(String.format(FollowUpCommand.MESSAGE_ALREADY_MARKED, Messages.format(ALICE)),
                mark.execute(model).getFeedbackToUser());
        assertSame(marked, model.getFilteredStudentList().getFirst());
        complete.execute(model);
        Student completed = model.getFilteredStudentList().getFirst();
        complete.execute(model);
        assertSame(completed, model.getFilteredStudentList().getFirst());
    }

    @Test
    public void execute_emptyDisplayedList_rejectsIndexEvenWithHiddenStudents() {
        model.updateFilteredStudentList(student -> false);
        assertCommandFailure(new FollowUpCommand(INDEX_FIRST_STUDENT, true), model,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        assertCommandFailure(new FollowUpCommand(INDEX_FIRST_STUDENT, false), model,
                MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FollowUpCommand(null, true));
        assertThrows(NullPointerException.class, () -> new FollowUpCommand(INDEX_FIRST_STUDENT, true).execute(null));
    }

    @Test
    public void equalsAndHashCode_includeIndexAndStatus() {
        FollowUpCommand command = new FollowUpCommand(INDEX_FIRST_STUDENT, true);
        FollowUpCommand same = new FollowUpCommand(INDEX_FIRST_STUDENT, true);
        assertTrue(command.equals(command));
        assertTrue(command.equals(same));
        assertEquals(command.hashCode(), same.hashCode());
        assertFalse(command.equals(new FollowUpCommand(INDEX_FIRST_STUDENT, false)));
        assertFalse(command.equals(new FollowUpCommand(Index.fromOneBased(2), true)));
        assertFalse(command.equals(null));
        assertFalse(command.equals(1));
    }

    @Test
    public void toString_includesDisplayedIndexAndRequestedStatus() {
        String prefix = FollowUpCommand.class.getCanonicalName() + "{index=" + INDEX_FIRST_STUDENT;
        assertEquals(prefix + ", requiresFollowUp=true}",
                new FollowUpCommand(INDEX_FIRST_STUDENT, true).toString());
        assertEquals(prefix + ", requiresFollowUp=false}",
                new FollowUpCommand(INDEX_FIRST_STUDENT, false).toString());
    }
}
