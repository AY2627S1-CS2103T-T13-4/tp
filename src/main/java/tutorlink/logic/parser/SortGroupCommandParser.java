package tutorlink.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import tutorlink.logic.commands.SortGroupCommand;
import tutorlink.logic.parser.exceptions.ParseException;

/**
 * Parses arguments for sorting the displayed students by tutorial group.
 */
public class SortGroupCommandParser implements Parser<SortGroupCommand> {

    @Override
    public SortGroupCommand parse(String args) throws ParseException {
        requireNonNull(args, "Sort group arguments must not be null.");
        String[] arguments = args.trim().split("\\s+");
        if (arguments.length > 2 || !arguments[0].equals("group")) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortGroupCommand.MESSAGE_USAGE));
        }

        if (arguments.length == 1 || arguments[1].equals("asc")) {
            return new SortGroupCommand(false);
        }
        if (arguments[1].equals("desc")) {
            return new SortGroupCommand(true);
        }
        throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortGroupCommand.MESSAGE_USAGE));
    }
}
