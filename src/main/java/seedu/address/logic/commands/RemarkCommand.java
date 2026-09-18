package seedu.address.logic.commands;

import seedu.address.model.Model;

/**
 * Adds a remark command as an example of extending the command set.
 *
 * <p>The command is intentionally kept as a tutorial-sized placeholder. It demonstrates how a
 * command is represented and executed before the product-specific remark model is designed.</p>
 */
public class RemarkCommand extends Command {

    public static final String COMMAND_WORD = "remark";
    public static final String MESSAGE_SUCCESS = "Hello from remark";

    @Override
    public CommandResult execute(Model model) {
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
