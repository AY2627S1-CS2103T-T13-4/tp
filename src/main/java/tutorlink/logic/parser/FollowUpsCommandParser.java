package tutorlink.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import tutorlink.logic.commands.FollowUpsCommand;
import tutorlink.logic.parser.exceptions.ParseException;

/**
 * Parses the argument-free follow-up list command.
 */
public class FollowUpsCommandParser implements Parser<FollowUpsCommand> {
    @Override
    public FollowUpsCommand parse(String args) throws ParseException {
        requireNonNull(args);
        if (!args.trim().isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpsCommand.MESSAGE_USAGE));
        }
        return new FollowUpsCommand();
    }
}
