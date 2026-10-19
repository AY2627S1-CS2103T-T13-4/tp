package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorlink.commons.util.ToStringBuilder;
import tutorlink.logic.Messages;
import tutorlink.model.Model;
import tutorlink.model.student.TagMatchesPredicate;

/**
 * Finds students with an exact tag, ignoring case, across the complete student directory.
 */
public class FindTagCommand extends Command {

    public static final String COMMAND_WORD = "findtag";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Finds students with an exact tag, ignoring case.\n"
            + "Parameters: TAG (one alphanumeric tag, without the t/ prefix)\n"
            + "Example: " + COMMAND_WORD + " needsHelp";

    private final TagMatchesPredicate predicate;

    public FindTagCommand(TagMatchesPredicate predicate) {
        this.predicate = requireNonNull(predicate);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredStudentList(predicate);
        return new CommandResult(
                String.format(Messages.MESSAGE_STUDENTS_LISTED_OVERVIEW, model.getFilteredStudentList().size()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof FindTagCommand otherCommand)) {
            return false;
        }

        return predicate.equals(otherCommand.predicate);
    }

    @Override
    public int hashCode() {
        return predicate.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("predicate", predicate).toString();
    }
}
