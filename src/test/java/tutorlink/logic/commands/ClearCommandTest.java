package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.logic.commands.CommandTestUtil.assertCommandSuccess;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.Test;

import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.StudentDirectory;
import tutorlink.model.UserPrefs;

public class ClearCommandTest {

    @Test
    public void execute_emptyStudentDirectory_success() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager();

        assertCommandSuccess(new ClearCommand(), model, ClearCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_nonEmptyStudentDirectory_success() {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        model.updateFilteredStudentList(student -> false, "Tutorial group: T99");
        Model expectedModel = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        expectedModel.setStudentDirectory(new StudentDirectory());

        assertCommandSuccess(new ClearCommand(), model, ClearCommand.MESSAGE_SUCCESS, expectedModel);
        assertEquals("All students", model.getFilterDescription());
    }

}
