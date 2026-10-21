package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;

import org.junit.jupiter.api.Test;

import tutorlink.model.ModelManager;
import tutorlink.model.StudentSortOrder;

public class SortGroupCommandTest {

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new SortGroupCommand(false).execute(null));
    }

    @Test
    public void execute_modelChangesDisplayedCount_throwsAssertionError() {
        ModelManager model = new ModelManager() {
            @Override
            public void sortStudents(StudentSortOrder sortOrder) {
                super.sortStudents(sortOrder);
                if (sortOrder != StudentSortOrder.DIRECTORY) {
                    updateFilteredStudentList(student -> false);
                }
            }
        };
        model.addStudent(ALICE);

        String expectedMessage = "Sorting must not change the number of displayed students.";
        assertThrows(AssertionError.class, expectedMessage, () -> new SortGroupCommand(false).execute(model));
    }

    @Test
    public void equals() {
        SortGroupCommand ascending = new SortGroupCommand(false);
        assertTrue(ascending.equals(ascending));
        assertTrue(ascending.equals(new SortGroupCommand(false)));
        assertFalse(ascending.equals(new SortGroupCommand(true)));
        assertFalse(ascending.equals(null));
        assertFalse(ascending.equals(1));
    }

    @Test
    public void toStringMethod() {
        assertEquals(SortGroupCommand.class.getCanonicalName() + "{isDescending=true}",
                new SortGroupCommand(true).toString());
    }
}
