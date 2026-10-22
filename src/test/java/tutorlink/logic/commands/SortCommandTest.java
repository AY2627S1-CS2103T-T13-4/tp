package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class SortCommandTest {

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new SortCommand(false).execute(null));
    }

    @Test
    public void equals() {
        SortCommand ascending = new SortCommand(false);
        assertTrue(ascending.equals(ascending));
        assertTrue(ascending.equals(new SortCommand(false)));
        assertFalse(ascending.equals(new SortCommand(true)));
        assertFalse(ascending.equals(null));
        assertFalse(ascending.equals(1));
    }

    @Test
    public void toStringMethod() {
        assertEquals(SortCommand.class.getCanonicalName() + "{isDescending=true}",
                new SortCommand(true).toString());
    }
}
