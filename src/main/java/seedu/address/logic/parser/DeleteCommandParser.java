package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a new DeleteCommand object
 */
public class DeleteCommandParser implements Parser<DeleteCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the DeleteCommand
     * and returns a DeleteCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public DeleteCommand parse(String args) throws ParseException {
        String[] parts = args.trim().split("\\s+");
        if (parts.length > 2
                || (parts.length == 2 && !"confirm".equals(parts[1]))) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        }

        try {
            Index index = ParserUtil.parseIndex(parts[0]);
            boolean isConfirmed = parts.length == 2;
            return new DeleteCommand(index, isConfirmed);
        } catch (ParseException pe) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE), pe);
        }
    }

}
