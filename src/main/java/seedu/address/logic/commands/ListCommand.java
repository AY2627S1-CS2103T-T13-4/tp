package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TUTORIAL_GROUP;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.person.TutorialGroup;
import seedu.address.model.person.TutorialGroupMatchesPredicate;

/**
 * Lists all persons in the address book to the user.
 */
public class ListCommand extends Command {

    public static final String COMMAND_WORD = "list";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists all persons or only persons in the specified "
            + "tutorial group.\n"
            + "Parameters: [" + PREFIX_TUTORIAL_GROUP + "TUTORIAL_GROUP]\n"
            + "Example: " + COMMAND_WORD + " " + PREFIX_TUTORIAL_GROUP + "T01";

    public static final String MESSAGE_SUCCESS = "Listed all persons.";

    private final Optional<TutorialGroup> tutorialGroup;

    /**
     * Creates a command that lists all persons.
     */
    public ListCommand() {
        tutorialGroup = Optional.empty();
    }

    /**
     * Creates a command that lists persons in the specified tutorial group.
     */
    public ListCommand(TutorialGroup tutorialGroup) {
        this.tutorialGroup = Optional.of(requireNonNull(tutorialGroup));
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        if (tutorialGroup.isEmpty()) {
            model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
            return new CommandResult(MESSAGE_SUCCESS);
        }

        model.updateFilteredPersonList(new TutorialGroupMatchesPredicate(tutorialGroup.get()));
        return new CommandResult(
                String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, model.getFilteredPersonList().size()));
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
    public String toString() {
        return new ToStringBuilder(this)
                .add("tutorialGroup", tutorialGroup)
                .toString();
    }
}
