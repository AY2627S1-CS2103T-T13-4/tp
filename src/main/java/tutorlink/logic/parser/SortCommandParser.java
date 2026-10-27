package tutorlink.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import tutorlink.logic.commands.Command;
import tutorlink.logic.commands.SortCommand;
import tutorlink.logic.commands.SortGroupCommand;
import tutorlink.logic.commands.SortResetCommand;
import tutorlink.logic.parser.exceptions.ParseException;

/**
 * Parses the shared sort syntax and selects a sorting or reset command.
 */
public class SortCommandParser implements Parser<Command> {

    /**
     * Parses the sorting field and optional direction.
     *
     * @throws NullPointerException if {@code args} is null.
     * @throws ParseException if the arguments do not follow the command format.
     */
    @Override
    public Command parse(String args) throws ParseException {
        requireNonNull(args, "Sort arguments must not be null.");
        if (args.trim().equals("reset")) {
            return new SortResetCommand();
        }
        String[] arguments = args.trim().split("\\s+");
        if (arguments.length > 2 || !(arguments[0].equals("name") || arguments[0].equals("group"))) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortCommand.MESSAGE_USAGE));
        }

        boolean isDescending = false;
        if (arguments.length == 2) {
            if (!(arguments[1].equals("asc") || arguments[1].equals("desc"))) {
                throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortCommand.MESSAGE_USAGE));
            }
            isDescending = arguments[1].equals("desc");
        }
        return arguments[0].equals("name") ? new SortCommand(isDescending) : new SortGroupCommand(isDescending);
    }
}
