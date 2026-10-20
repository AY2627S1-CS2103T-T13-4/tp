package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;

import tutorlink.commons.util.ToStringBuilder;
import tutorlink.model.Model;
import tutorlink.model.student.Student;

/**
 * Sorts the currently displayed students by name without changing the active filter.
 */
public class SortCommand extends Command {

    public static final String COMMAND_WORD = "sort";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Sorts the displayed students by name.\n"
            + "Parameters: name [asc|desc]\n"
            + "Example: " + COMMAND_WORD + " name desc";
    public static final String MESSAGE_SUCCESS_ASCENDING = "Sorted displayed students by name (A-Z).";
    public static final String MESSAGE_SUCCESS_DESCENDING = "Sorted displayed students by name (Z-A).";

    private static final Comparator<Student> NAME_COMPARATOR = Comparator.comparing(
            student -> student.getName().fullName, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<Student> ASCENDING_COMPARATOR = NAME_COMPARATOR
            .thenComparing(student -> student.getMatricNumber().value);
    private static final Comparator<Student> DESCENDING_COMPARATOR = NAME_COMPARATOR.reversed()
            .thenComparing(student -> student.getMatricNumber().value);

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
        model.sortStudents(isDescending ? DESCENDING_COMPARATOR : ASCENDING_COMPARATOR);
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
