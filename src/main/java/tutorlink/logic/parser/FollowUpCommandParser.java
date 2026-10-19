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
        String[] parts = args.trim().split("\\s+");
        if (parts.length != 2 || !("on".equals(parts[1]) || "done".equals(parts[1]))) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpCommand.MESSAGE_USAGE));
        }
        try {
            Index index = ParserUtil.parseIndex(parts[0]);
            return new FollowUpCommand(index, "on".equals(parts[1]));
        } catch (ParseException pe) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpCommand.MESSAGE_USAGE), pe);
        }
    }
}
