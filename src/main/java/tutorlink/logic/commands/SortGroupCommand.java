package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import tutorlink.commons.core.LogsCenter;
import tutorlink.commons.util.ToStringBuilder;
import tutorlink.model.Model;
import tutorlink.model.StudentSortOrder;

/**
 * Sorts the currently displayed students by tutorial group without changing the active filter.
 */
public class SortGroupCommand extends Command {

    public static final String COMMAND_WORD = "sort";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Sorts the displayed students by tutorial group.\n"
            + "Parameters: group [asc|desc]\n"
            + "Example: " + COMMAND_WORD + " group desc";
    public static final String MESSAGE_SUCCESS_ASCENDING =
            "Sorted displayed students by tutorial group (ascending).";
    public static final String MESSAGE_SUCCESS_DESCENDING =
            "Sorted displayed students by tutorial group (descending).";

    private static final Logger logger = LogsCenter.getLogger(SortGroupCommand.class);

    private final boolean isDescending;

    /**
     * Creates a command that sorts tutorial groups in ascending or descending order.
     * Equal normalized tutorial groups are always ordered by ascending matriculation number.
     */
    public SortGroupCommand(boolean isDescending) {
        this.isDescending = isDescending;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        int displayedStudentCount = model.getFilteredStudentList().size();
        model.sortStudents(isDescending ? StudentSortOrder.GROUP_DESC : StudentSortOrder.GROUP_ASC);
        assert model.getFilteredStudentList().size() == displayedStudentCount
                : "Sorting must not change the number of displayed students.";
        logger.fine(() -> "Sorted " + displayedStudentCount + " displayed student(s) by tutorial group in "
                + (isDescending ? "descending" : "ascending") + " order.");
        return new CommandResult(isDescending ? MESSAGE_SUCCESS_DESCENDING : MESSAGE_SUCCESS_ASCENDING);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        return other instanceof SortGroupCommand otherSortGroupCommand
                && isDescending == otherSortGroupCommand.isDescending;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("isDescending", isDescending).toString();
    }
}
