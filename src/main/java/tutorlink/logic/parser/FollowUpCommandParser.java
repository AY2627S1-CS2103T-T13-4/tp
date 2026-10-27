package tutorlink.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import tutorlink.commons.core.index.Index;
import tutorlink.logic.commands.FollowUpCommand;
import tutorlink.logic.parser.exceptions.ParseException;

/**
 * Parses a displayed index and the on/done action for a follow-up.
 */
public class FollowUpCommandParser implements Parser<FollowUpCommand> {
    @Override
    public FollowUpCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String[] arguments = args.trim().split("\\s+");
        if (arguments.length != 2) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpCommand.MESSAGE_USAGE));
        }
        try {
            Index index = ParserUtil.parseIndex(arguments[0]);
            boolean requiresFollowUp = parseAction(arguments[1]);
            return new FollowUpCommand(index, requiresFollowUp);
        } catch (ParseException pe) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpCommand.MESSAGE_USAGE), pe);
        }
    }

    private boolean parseAction(String action) throws ParseException {
        return switch (action) {
            case "on" -> true;
            case "done" -> false;
            default -> throw new ParseException("The follow-up action must be on or done.");
        };
    }
}
