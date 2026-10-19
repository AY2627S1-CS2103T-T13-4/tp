package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static tutorlink.logic.commands.CommandTestUtil.assertCommandFailure;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.Test;

import tutorlink.commons.core.index.Index;
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
}
