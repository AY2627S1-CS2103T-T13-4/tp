package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorlink.commons.core.index.Index;
import tutorlink.commons.util.ToStringBuilder;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.model.Model;
import tutorlink.model.student.Student;

/**
 * Opens the full details of a student identified by their displayed index.
 */
public class ViewCommand extends Command {
    public static final String COMMAND_WORD = "view";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Shows a student's full details in the details panel.\n"
            + "Parameters: INDEX (must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1";
    public static final String MESSAGE_SUCCESS = "Showing details for: %1$s (%2$s)";

    private final Index targetIndex;

    /**
     * Creates a command that views the student at the displayed index.
     */
    public ViewCommand(Index targetIndex) {
        this.targetIndex = requireNonNull(targetIndex);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (targetIndex.getZeroBased() >= model.getFilteredStudentList().size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        }
        Student student = model.getFilteredStudentList().get(targetIndex.getZeroBased());
        return new CommandResult(String.format(MESSAGE_SUCCESS, student.getName().fullName,
                student.getMatricNumber().value), student);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ViewCommand otherCommand
                && targetIndex.equals(otherCommand.targetIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("targetIndex", targetIndex).toString();
    }
}
