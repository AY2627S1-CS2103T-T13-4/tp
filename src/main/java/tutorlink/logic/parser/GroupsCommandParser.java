package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import tutorlink.logic.commands.GroupsCommand;
import tutorlink.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a new {@code GroupsCommand} object.
 */
public class GroupsCommandParser implements Parser<GroupsCommand> {

    @Override
    public GroupsCommand parse(String args) throws ParseException {
        if (!args.trim().isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                    GroupsCommand.MESSAGE_USAGE));
        }
        return new GroupsCommand();
    }
}
