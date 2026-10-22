package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import tutorlink.commons.core.LogsCenter;
import tutorlink.model.Model;
import tutorlink.model.StudentSortOrder;

/**
 * Restores the current directory order without changing the active filter.
 */
public class SortResetCommand extends Command {
    public static final String MESSAGE_SUCCESS = "Restored directory order for displayed students, keeping the filter.";

    private static final Logger logger = LogsCenter.getLogger(SortResetCommand.class);

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.sortStudents(StudentSortOrder.DIRECTORY);
        logger.fine(() -> "Restored directory order for " + model.getFilteredStudentList().size()
                + " displayed student(s), keeping the active filter.");
        return new CommandResult(MESSAGE_SUCCESS);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SortResetCommand;
    }

    @Override
    public int hashCode() {
        return SortResetCommand.class.hashCode();
    }
}
