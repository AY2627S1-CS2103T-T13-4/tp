package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TUTORIAL_GROUP;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_STUDENTS;

import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.student.TutorialGroup;
import seedu.address.model.student.TutorialGroupMatchesPredicate;

/**
 * Lists all students in the student directory to the user.
 */
public class ListCommand extends Command {

    public static final String COMMAND_WORD = "list";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists all students or only students in the specified "
            + "tutorial group.\n"
            + "Parameters: [" + PREFIX_TUTORIAL_GROUP + "TUTORIAL_GROUP]\n"
            + "Example: " + COMMAND_WORD + " " + PREFIX_TUTORIAL_GROUP + "T01";

    public static final String MESSAGE_SUCCESS = "Listed all students.";

    private final Optional<TutorialGroup> tutorialGroup;

    /**
     * Creates a command that lists all students.
     */
    public ListCommand() {
        tutorialGroup = Optional.empty();
    }

    /**
     * Creates a command that lists students in the specified tutorial group.
     */
    public ListCommand(TutorialGroup tutorialGroup) {
        this.tutorialGroup = Optional.of(requireNonNull(tutorialGroup));
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        if (tutorialGroup.isEmpty()) {
            model.updateFilteredStudentList(PREDICATE_SHOW_ALL_STUDENTS);
            return new CommandResult(MESSAGE_SUCCESS);
        }

        model.updateFilteredStudentList(new TutorialGroupMatchesPredicate(tutorialGroup.get()));
        return new CommandResult(
                String.format(Messages.MESSAGE_STUDENTS_LISTED_OVERVIEW, model.getFilteredStudentList().size()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ListCommand otherListCommand)) {
            return false;
        }

        return tutorialGroup.equals(otherListCommand.tutorialGroup);
    }

    @Override
    public int hashCode() {
        return tutorialGroup.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("tutorialGroup", tutorialGroup)
                .toString();
    }
}
