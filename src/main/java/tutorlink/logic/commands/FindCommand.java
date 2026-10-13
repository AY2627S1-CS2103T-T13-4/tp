package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorlink.commons.util.ToStringBuilder;
import tutorlink.logic.Messages;
import tutorlink.model.Model;
import tutorlink.model.student.StudentContainsKeywordsPredicate;

/**
 * Finds students by whole name words or full matriculation numbers.
 * Matches any keyword, ignoring case, across the complete student directory.
 */
public class FindCommand extends Command {

    public static final String COMMAND_WORD = "find";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Finds students by name or matriculation number.\n"
            + "Matches whole name words or full matriculation numbers, ignoring case.\n"
            + "Parameters: KEYWORD [MORE_KEYWORDS]...\n"
            + "Example: " + COMMAND_WORD + " alice A0123456B";

    private final StudentContainsKeywordsPredicate predicate;

    public FindCommand(StudentContainsKeywordsPredicate predicate) {
        this.predicate = predicate;
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
        if (!(other instanceof FindCommand otherFindCommand)) {
            return false;
        }

        return predicate.equals(otherFindCommand.predicate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("predicate", predicate)
                .toString();
    }
}
