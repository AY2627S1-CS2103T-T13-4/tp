package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import tutorlink.logic.commands.ViewCommand;
import tutorlink.logic.parser.exceptions.ParseException;

/**
 * Parses the single displayed index accepted by a view command.
 */
public class ViewCommandParser implements Parser<ViewCommand> {
    @Override
    public ViewCommand parse(String args) throws ParseException {
        try {
            return new ViewCommand(ParserUtil.parseIndex(args));
        } catch (ParseException e) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ViewCommand.MESSAGE_USAGE), e);
        }
    }
}
