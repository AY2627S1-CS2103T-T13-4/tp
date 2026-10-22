package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class SortResetCommandTest {
    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new SortResetCommand().execute(null));
    }

    @Test
    public void equals() {
        SortResetCommand command = new SortResetCommand();
        assertTrue(command.equals(command));
        assertTrue(command.equals(new SortResetCommand()));
        assertFalse(command.equals(null));
        assertFalse(command.equals(new SortCommand(false)));
        assertEquals(command.hashCode(), new SortResetCommand().hashCode());
    }
}
