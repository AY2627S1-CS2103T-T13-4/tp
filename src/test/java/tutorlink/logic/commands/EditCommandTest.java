package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.logic.commands.CommandTestUtil.DESC_AMY;
import static tutorlink.logic.commands.CommandTestUtil.DESC_BOB;
import static tutorlink.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static tutorlink.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static tutorlink.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static tutorlink.logic.commands.CommandTestUtil.assertCommandFailure;
import static tutorlink.logic.commands.CommandTestUtil.assertCommandSuccess;
import static tutorlink.logic.commands.CommandTestUtil.showStudentAtIndex;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorlink.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorlink.commons.core.index.Index;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.EditCommand.EditStudentDescriptor;
import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.StudentDirectory;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.NameContainsKeywordsPredicate;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.model.student.TutorialGroupMatchesPredicate;
import tutorlink.testutil.EditStudentDescriptorBuilder;
import tutorlink.testutil.StudentBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for EditCommand.
 */
public class EditCommandTest {

    private Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());

    @Test
    public void execute_allFieldsSpecifiedUnfilteredList_success() {
        Student studentToEdit = model.getFilteredStudentList().get(0);
        Student studentWithNewDetails = new StudentBuilder().build();
        Student editedStudent = new StudentBuilder(studentWithNewDetails)
                .withMatricNumber(studentToEdit.getMatricNumber().toString())
                .withTutorialGroup(studentToEdit.getTutorialGroup().toString()).build();
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder(studentWithNewDetails).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT, descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentDirectory(model.getStudentDirectory()), new UserPrefs());
        expectedModel.setStudent(model.getFilteredStudentList().get(0), editedStudent);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_someFieldsSpecifiedUnfilteredList_success() {
        Index indexLastStudent = Index.fromOneBased(model.getFilteredStudentList().size());
        Student lastStudent = model.getFilteredStudentList().get(indexLastStudent.getZeroBased());

        StudentBuilder studentInList = new StudentBuilder(lastStudent);
        Student editedStudent = studentInList.withName(VALID_NAME_BOB).withPhone(VALID_PHONE_BOB)
                .withTags(VALID_TAG_HUSBAND).build();

        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).withTags(VALID_TAG_HUSBAND).build();
        EditCommand editCommand = new EditCommand(indexLastStudent, descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentDirectory(model.getStudentDirectory()), new UserPrefs());
        expectedModel.setStudent(lastStudent, editedStudent);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_existingRemark_preservesRemark() throws Exception {
        Student original = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student withRemark = new StudentBuilder(original).withRemark("Needs follow-up").build();
        model.setStudent(original, withRemark);
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withMatricNumber("A1234567Z")
                        .withTutorialGroup("W12").build());

        editCommand.execute(model);

        Student editedStudent = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        assertEquals("Needs follow-up", editedStudent.getRemark());
    }

    @Test
    public void execute_matricNumberOnly_preservesOtherDetails() {
        Student original = model.getFilteredStudentList().get(0);
        Student editedStudent = new StudentBuilder(original).withMatricNumber("a1234567z").build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withMatricNumber("a1234567z").build());
        Model expectedModel = new ModelManager(new StudentDirectory(model.getStudentDirectory()), new UserPrefs());
        expectedModel.setStudent(original, editedStudent);

        assertCommandSuccess(editCommand, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(editedStudent)), expectedModel);
    }

    @Test
    public void execute_duplicateMatricNumberFilteredList_failure() {
        Student firstStudent = model.getFilteredStudentList().get(0);
        Student secondStudent = model.getFilteredStudentList().get(1);
        model.updateFilteredStudentList(student -> student.equals(firstStudent));
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder()
                        .withMatricNumber(secondStudent.getMatricNumber().toString().toLowerCase()).build());

        assertCommandFailure(editCommand, model, EditCommand.MESSAGE_DUPLICATE_STUDENT);
        assertEquals(firstStudent, model.getFilteredStudentList().get(0));
    }

    @Test
    public void execute_tutorialGroupChangedWhileFiltered_studentDisappears() throws Exception {
        Student original = model.getFilteredStudentList().get(0);
        TutorialGroup originalGroup = original.getTutorialGroup();
        model.updateFilteredStudentList(new TutorialGroupMatchesPredicate(originalGroup));
        int originalFilteredSize = model.getFilteredStudentList().size();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withTutorialGroup("w12").build());

        editCommand.execute(model);

        Student editedStudent = new StudentBuilder(original).withTutorialGroup("W12").build();
        assertEquals(originalFilteredSize - 1, model.getFilteredStudentList().size());
        assertTrue(model.getStudentDirectory().getStudentList().contains(editedStudent));
        assertFalse(model.getFilteredStudentList().contains(editedStudent));
    }

    @Test
    public void execute_noFieldSpecifiedUnfilteredList_success() {
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT, new EditStudentDescriptor());
        Student editedStudent = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentDirectory(model.getStudentDirectory()), new UserPrefs());

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_filteredList_success() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);

        Student studentInFilteredList = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student editedStudent = new StudentBuilder(studentInFilteredList).withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB).build());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentDirectory(model.getStudentDirectory()), new UserPrefs());
        expectedModel.setStudent(model.getFilteredStudentList().get(0), editedStudent);
        expectedModel.updateFilteredStudentList(new NameContainsKeywordsPredicate(List.of("Alice")));

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_sameNameDifferentMatricNumberUnfilteredList_success() {
        Student firstStudent = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student secondStudent = model.getFilteredStudentList().get(INDEX_SECOND_STUDENT.getZeroBased());
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder(firstStudent).build();
        EditCommand editCommand = new EditCommand(INDEX_SECOND_STUDENT, descriptor);

        Student editedStudent = new StudentBuilder(firstStudent)
                .withMatricNumber(secondStudent.getMatricNumber().toString())
                .withTutorialGroup(secondStudent.getTutorialGroup().toString()).build();
        Model expectedModel = new ModelManager(new StudentDirectory(model.getStudentDirectory()), new UserPrefs());
        expectedModel.setStudent(secondStudent, editedStudent);
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));
        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_sameNameDifferentMatricNumberFilteredList_success() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);

        // Edit the first student to have the same name and editable details as the second student.
        Student studentInList = model.getStudentDirectory().getStudentList().get(INDEX_SECOND_STUDENT.getZeroBased());
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder(studentInList).build());

        Student firstStudent = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student editedStudent = new StudentBuilder(studentInList)
                .withMatricNumber(firstStudent.getMatricNumber().toString())
                .withTutorialGroup(firstStudent.getTutorialGroup().toString()).build();
        Model expectedModel = new ModelManager(new StudentDirectory(model.getStudentDirectory()), new UserPrefs());
        expectedModel.setStudent(firstStudent, editedStudent);
        expectedModel.updateFilteredStudentList(new NameContainsKeywordsPredicate(List.of("Alice")));
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));
        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidStudentIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredStudentList().size() + 1);
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(outOfBoundIndex, descriptor);

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    /**
     * Edit filtered list where index is larger than size of filtered list,
     * but smaller than size of student directory
     */
    @Test
    public void execute_invalidStudentIndexFilteredList_failure() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);
        Index outOfBoundIndex = INDEX_SECOND_STUDENT;
        // ensures that outOfBoundIndex is still in bounds of student directory list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getStudentDirectory().getStudentList().size());

        EditCommand editCommand = new EditCommand(outOfBoundIndex,
                new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB).build());

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        final EditCommand standardCommand = new EditCommand(INDEX_FIRST_STUDENT, DESC_AMY);

        // same values -> returns true
        EditStudentDescriptor copyDescriptor = new EditStudentDescriptor(DESC_AMY);
        EditCommand commandWithSameValues = new EditCommand(INDEX_FIRST_STUDENT, copyDescriptor);
        assertTrue(standardCommand.equals(commandWithSameValues));

        // same object -> returns true
        assertTrue(standardCommand.equals(standardCommand));

        // null -> returns false
        assertFalse(standardCommand.equals(null));

        // different types -> returns false
        assertFalse(standardCommand.equals(new ClearCommand()));

        // different index -> returns false
        assertFalse(standardCommand.equals(new EditCommand(INDEX_SECOND_STUDENT, DESC_AMY)));

        // different descriptor -> returns false
        assertFalse(standardCommand.equals(new EditCommand(INDEX_FIRST_STUDENT, DESC_BOB)));
    }

    @Test
    public void toStringMethod() {
        Index index = Index.fromOneBased(1);
        EditStudentDescriptor editStudentDescriptor = new EditStudentDescriptor();
        EditCommand editCommand = new EditCommand(index, editStudentDescriptor);
        String expected = EditCommand.class.getCanonicalName() + "{index=" + index + ", editStudentDescriptor="
                + editStudentDescriptor + "}";
        assertEquals(expected, editCommand.toString());
    }

}
