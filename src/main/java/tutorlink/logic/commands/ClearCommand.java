package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import tutorlink.model.Model;
import tutorlink.model.StudentDirectory;

/**
 * Clears the student directory.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Deletes all student records.\n"
            + "Example: " + COMMAND_WORD;
    public static final String MESSAGE_SUCCESS = "All student records have been cleared!";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setStudentDirectory(new StudentDirectory());
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
