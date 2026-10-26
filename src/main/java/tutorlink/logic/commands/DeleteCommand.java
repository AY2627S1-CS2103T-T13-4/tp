package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import tutorlink.commons.core.index.Index;
import tutorlink.commons.util.ToStringBuilder;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.model.Model;
import tutorlink.model.student.Student;

/**
 * Previews or deletes a student identified using its displayed index from the student directory.
 */
public class DeleteCommand extends Command {

    public static final String COMMAND_WORD = "delete";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Shows the selected student, or deletes them with explicit confirmation.\n"
            + "Parameters: INDEX [confirm] (INDEX must be a positive integer)\n"
            + "Example: " + COMMAND_WORD + " 1 confirm";

    public static final String MESSAGE_DELETE_STUDENT_SUCCESS = "Deleted student: %1$s";

    public static final String MESSAGE_CONFIRM_DELETE = "Selected student: %1$s\n"
            + "To delete this student, enter: delete %2$d confirm\n"
            + "If the displayed list changes, check the student's index again.";

    private final Index targetIndex;

    private final boolean isConfirmed;

    public DeleteCommand(Index targetIndex) {
        this(targetIndex, false);
    }

    /**
     * Creates a command that previews or deletes the selected student depending on explicit confirmation.
     */
    public DeleteCommand(Index targetIndex, boolean isConfirmed) {
        this.targetIndex = requireNonNull(targetIndex);
        this.isConfirmed = isConfirmed;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Student> lastShownList = model.getFilteredStudentList();

        if (targetIndex.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        }

        Student studentToDelete = lastShownList.get(targetIndex.getZeroBased());
        if (!isConfirmed) {
            return new CommandResult(String.format(MESSAGE_CONFIRM_DELETE,
                    Messages.format(studentToDelete), targetIndex.getOneBased()), studentToDelete);
        }

        model.deleteStudent(studentToDelete);
        return new CommandResult(String.format(MESSAGE_DELETE_STUDENT_SUCCESS, Messages.format(studentToDelete)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof DeleteCommand otherDeleteCommand)) {
            return false;
        }

        return targetIndex.equals(otherDeleteCommand.targetIndex)
                && isConfirmed == otherDeleteCommand.isConfirmed;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .add("isConfirmed", isConfirmed)
                .toString();
    }
}
