package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorlink.model.Model;
import tutorlink.model.student.Student;

/**
 * Lists students requiring follow-up from the full roster, replacing any earlier filter.
 */
public class FollowUpsCommand extends Command {
    public static final String COMMAND_WORD = "followups";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists all students requiring follow-up.\n"
            + "Parameters: none\nExample: " + COMMAND_WORD;
    public static final String MESSAGE_SUCCESS = "%1$d student(s) requiring follow-up listed!";
    public static final String MESSAGE_EMPTY = "No students require follow-up.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredStudentList(Student::requiresFollowUp, "Follow-up required");
        int count = model.getFilteredStudentList().size();
        return new CommandResult(count == 0 ? MESSAGE_EMPTY : String.format(MESSAGE_SUCCESS, count));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FollowUpsCommand;
    }

    @Override
    public int hashCode() {
        return FollowUpsCommand.class.hashCode();
    }
}
