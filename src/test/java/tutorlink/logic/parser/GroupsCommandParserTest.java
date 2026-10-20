package tutorlink.logic.parser;

import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorlink.logic.Messages;
import tutorlink.logic.commands.GroupsCommand;

public class GroupsCommandParserTest {

    private final GroupsCommandParser parser = new GroupsCommandParser();

    @Test
    public void parse_noArguments_success() {
        assertParseSuccess(parser, "", new GroupsCommand());
        assertParseSuccess(parser, "   ", new GroupsCommand());
    }

    @Test
    public void parse_arguments_failure() {
        String expectedMessage = String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT,
                GroupsCommand.MESSAGE_USAGE);
        assertParseFailure(parser, " T01", expectedMessage);
    }
}
