package tutorlink.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import tutorlink.logic.commands.SortCommand;
import tutorlink.logic.parser.exceptions.ParseException;

/**
 * Parses arguments for sorting the displayed students by name.
 */
public class SortCommandParser implements Parser<SortCommand> {

    /**
     * Parses the sorting field and optional direction.
     *
     * @throws NullPointerException if {@code args} is null.
     * @throws ParseException if the arguments do not follow the command format.
     */
    @Override
    public SortCommand parse(String args) throws ParseException {
        requireNonNull(args, "Sort arguments must not be null.");
        String[] arguments = args.trim().split("\\s+");
        if (arguments.length > 2 || !arguments[0].equals("name")) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortCommand.MESSAGE_USAGE));
        }

        if (arguments.length == 1 || arguments[1].equals("asc")) {
            return new SortCommand(false);
        }
        if (arguments[1].equals("desc")) {
            return new SortCommand(true);
        }
        throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortCommand.MESSAGE_USAGE));
    }
}
