package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;

import tutorlink.commons.core.index.Index;
import tutorlink.commons.util.ToStringBuilder;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.model.Model;
import tutorlink.model.student.Student;

/**
 * Changes the current follow-up status of a student identified by their displayed index.
 */
public class FollowUpCommand extends Command {
    public static final String COMMAND_WORD = "followup";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Marks or completes a student's follow-up using the displayed student index.\n"
            + "Parameters: INDEX (must be a positive integer) on|done\n"
            + "Example: " + COMMAND_WORD + " 1 on";
    public static final String MESSAGE_MARKED = "Marked for follow-up: %1$s";
    public static final String MESSAGE_COMPLETED = "Completed follow-up: %1$s";
    public static final String MESSAGE_ALREADY_MARKED = "Already requires follow-up: %1$s";
    public static final String MESSAGE_ALREADY_DONE = "No outstanding follow-up: %1$s";

    private final Index index;
    private final boolean requiresFollowUp;

    /**
     * Creates a command to set the follow-up status of the student at {@code index}.
     */
    public FollowUpCommand(Index index, boolean requiresFollowUp) {
        this.index = requireNonNull(index);
        this.requiresFollowUp = requiresFollowUp;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Student> displayedStudents = model.getFilteredStudentList();
        if (index.getZeroBased() >= displayedStudents.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        }

        Student student = displayedStudents.get(index.getZeroBased());
        if (student.requiresFollowUp() == requiresFollowUp) {
            return new CommandResult(String.format(requiresFollowUp ? MESSAGE_ALREADY_MARKED : MESSAGE_ALREADY_DONE,
                    Messages.format(student)));
        }
        model.setStudent(student, student.withFollowUp(requiresFollowUp));
        return new CommandResult(String.format(requiresFollowUp ? MESSAGE_MARKED : MESSAGE_COMPLETED,
                Messages.format(student)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof FollowUpCommand otherCommand)) {
            return false;
        }
        return index.equals(otherCommand.index) && requiresFollowUp == otherCommand.requiresFollowUp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, requiresFollowUp);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("requiresFollowUp", requiresFollowUp).toString();
    }
}
