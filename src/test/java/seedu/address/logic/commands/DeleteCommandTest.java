package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showStudentAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static seedu.address.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.student.Student;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code DeleteCommand}.
 */
public class DeleteCommandTest {

    private Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());

    @Test
    public void execute_validIndexUnfilteredList_success() {
        Student studentToDelete = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_STUDENT, true);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(studentToDelete));

        ModelManager expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredStudentList().size() + 1);
        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        assertCommandFailure(new DeleteCommand(outOfBoundIndex, true), model,
                Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validIndexFilteredList_success() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);

        Student studentToDelete = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_STUDENT, true);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(studentToDelete));

        Model expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);
        showNoStudent(expectedModel);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexFilteredList_throwsCommandException() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);

        Index outOfBoundIndex = INDEX_SECOND_STUDENT;
        // ensures that outOfBoundIndex is still in bounds of student directory list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getStudentDirectory().getStudentList().size());

        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        assertCommandFailure(new DeleteCommand(outOfBoundIndex, true), model,
                Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_unconfirmedIndexUnfilteredList_previewsWithoutDeleting() {
        Student student = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Model expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
        String expectedMessage = String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE,
                Messages.format(student), INDEX_FIRST_STUDENT.getOneBased());

        assertCommandSuccess(new DeleteCommand(INDEX_FIRST_STUDENT), model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_unconfirmedIndexFilteredList_previewsWithoutDeleting() {
        showStudentAtIndex(model, INDEX_SECOND_STUDENT);
        Student student = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Model expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
        showStudentAtIndex(expectedModel, INDEX_SECOND_STUDENT);
        String expectedMessage = String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE,
                Messages.format(student), INDEX_FIRST_STUDENT.getOneBased());

        assertCommandSuccess(new DeleteCommand(INDEX_FIRST_STUDENT), model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_emptyDisplayedList_throwsCommandException() {
        showNoStudent(model);
        assertCommandFailure(new DeleteCommand(INDEX_FIRST_STUDENT), model,
                Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        assertCommandFailure(new DeleteCommand(INDEX_FIRST_STUDENT, true), model,
                Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_previewThenConfirmation_deletesSelectedStudent() {
        Student student = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Model expectedPreviewModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
        String previewMessage = String.format(DeleteCommand.MESSAGE_CONFIRM_DELETE,
                Messages.format(student), INDEX_FIRST_STUDENT.getOneBased());
        assertCommandSuccess(new DeleteCommand(INDEX_FIRST_STUDENT), model, previewMessage, expectedPreviewModel);

        Model expectedDeletedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
        expectedDeletedModel.deleteStudent(student);
        String deletionMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS, Messages.format(student));
        assertCommandSuccess(new DeleteCommand(INDEX_FIRST_STUDENT, true), model, deletionMessage,
                expectedDeletedModel);
    }

    @Test
    public void equals() {
        DeleteCommand deleteFirstCommand = new DeleteCommand(INDEX_FIRST_STUDENT);
        DeleteCommand deleteSecondCommand = new DeleteCommand(INDEX_SECOND_STUDENT);

        // same object -> returns true
        assertTrue(deleteFirstCommand.equals(deleteFirstCommand));

        // same values -> returns true
        DeleteCommand deleteFirstCommandCopy = new DeleteCommand(INDEX_FIRST_STUDENT);
        assertTrue(deleteFirstCommand.equals(deleteFirstCommandCopy));

        // different confirmation flags -> returns false
        assertFalse(deleteFirstCommand.equals(new DeleteCommand(INDEX_FIRST_STUDENT, true)));
        assertEquals(new DeleteCommand(INDEX_FIRST_STUDENT, true), new DeleteCommand(INDEX_FIRST_STUDENT, true));

        // different types -> returns false
        assertFalse(deleteFirstCommand.equals(1));

        // null -> returns false
        assertFalse(deleteFirstCommand.equals(null));

        // different student -> returns false
        assertFalse(deleteFirstCommand.equals(deleteSecondCommand));
    }

    @Test
    public void toStringMethod() {
        Index targetIndex = Index.fromOneBased(1);
        DeleteCommand deleteCommand = new DeleteCommand(targetIndex);
        String expected = DeleteCommand.class.getCanonicalName()
                + "{targetIndex=" + targetIndex + ", isConfirmed=false}";
        assertEquals(expected, deleteCommand.toString());
    }

    /**
     * Updates {@code model}'s filtered list to show no one.
     */
    private void showNoStudent(Model model) {
        model.updateFilteredStudentList(p -> false);

        assertTrue(model.getFilteredStudentList().isEmpty());
    }
}
