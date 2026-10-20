package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import tutorlink.commons.core.LogsCenter;
import tutorlink.commons.core.index.Index;
import tutorlink.commons.util.ToStringBuilder;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.model.Model;
import tutorlink.model.student.Student;

/**
 * Sets, replaces, or clears the remark of a student identified by the displayed index.
 */
public class RemarkCommand extends Command {

    public static final String COMMAND_WORD = "remark";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Sets, replaces, or clears a student's remark.\n"
            + "Parameters: INDEX (must be a positive integer) r/REMARK\n"
            + "Use r/ without text to clear the remark.\n"
            + "Example: " + COMMAND_WORD + " 1 r/Needs help with recursion";
    public static final String MESSAGE_SET_REMARK_SUCCESS = "Updated remark for student: %1$s";
    public static final String MESSAGE_CLEAR_REMARK_SUCCESS = "Cleared remark for student: %1$s";

    private static final Logger logger = LogsCenter.getLogger(RemarkCommand.class);

    private final Index index;
    private final String remark;

    /**
     * Creates a command to set the remark of the student at {@code index}.
     * A {@code null} remark means that the existing remark should be cleared.
     */
    public RemarkCommand(Index index, String remark) {
        this.index = requireNonNull(index);
        this.remark = remark;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Student> displayedStudents = model.getFilteredStudentList();
        if (index.getZeroBased() >= displayedStudents.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        }

        Student studentToEdit = displayedStudents.get(index.getZeroBased());
        Student editedStudent = studentToEdit.withRemark(remark);
        assert studentToEdit.isSameStudent(editedStudent) : "Remark updates must preserve student identity";
        model.setStudent(studentToEdit, editedStudent);
        logger.fine(() -> "Remark update completed for displayed index " + index.getOneBased());

        String message = remark == null ? MESSAGE_CLEAR_REMARK_SUCCESS : MESSAGE_SET_REMARK_SUCCESS;
        return new CommandResult(String.format(message, Messages.format(editedStudent)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof RemarkCommand otherRemarkCommand)) {
            return false;
        }
        return index.equals(otherRemarkCommand.index)
                && Objects.equals(remark, otherRemarkCommand.remark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, remark);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("index", index).add("remark", remark).toString();
    }
}
