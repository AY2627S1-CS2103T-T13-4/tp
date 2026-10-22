package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import tutorlink.commons.core.LogsCenter;
import tutorlink.commons.util.ToStringBuilder;
import tutorlink.model.Model;
import tutorlink.model.StudentSortOrder;

/**
 * Sorts the currently displayed students by name without changing the active filter.
 */
public class SortCommand extends Command {

    public static final String COMMAND_WORD = "sort";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Sorts displayed students by name or tutorial group, or restores directory order, keeping the filter.\n"
            + "Parameters: name|group [asc|desc] OR reset\n"
            + "Example: " + COMMAND_WORD + " name desc";
    public static final String MESSAGE_SUCCESS_ASCENDING = "Sorted displayed students by name (A-Z).";
    public static final String MESSAGE_SUCCESS_DESCENDING = "Sorted displayed students by name (Z-A).";

    private static final Logger logger = LogsCenter.getLogger(SortCommand.class);

    private final boolean isDescending;

    /**
     * Creates a command that sorts names in ascending or descending order.
     * Equal names, ignoring case, are always ordered by ascending matriculation number.
     */
    public SortCommand(boolean isDescending) {
        this.isDescending = isDescending;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.sortStudents(isDescending ? StudentSortOrder.NAME_DESC : StudentSortOrder.NAME_ASC);
        logger.fine(() -> "Sorted " + model.getFilteredStudentList().size() + " displayed students by name in "
                + (isDescending ? "descending" : "ascending") + " order.");
        return new CommandResult(isDescending ? MESSAGE_SUCCESS_DESCENDING : MESSAGE_SUCCESS_ASCENDING);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        return other instanceof SortCommand otherSortCommand
                && isDescending == otherSortCommand.isDescending;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("isDescending", isDescending).toString();
    }
}
