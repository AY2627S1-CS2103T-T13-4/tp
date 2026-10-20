package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.SortGroupCommand;

public class SortGroupCommandParserTest {
    private final SortGroupCommandParser parser = new SortGroupCommandParser();

    @Test
    public void parse_validArguments_success() {
        assertParseSuccess(parser, " group", new SortGroupCommand(false));
        assertParseSuccess(parser, " group asc", new SortGroupCommand(false));
        assertParseSuccess(parser, " group desc", new SortGroupCommand(true));
        assertParseSuccess(parser, " \tgroup  \t desc \t", new SortGroupCommand(true));
    }

    @Test
    public void parse_invalidArguments_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortGroupCommand.MESSAGE_USAGE);
        for (String arguments : new String[] {"", "   ", "asc", "name", "reset", "Group", "group ASC",
            "group ascending", "group asc desc", "group desc extra", "group n/Alice"}) {
            assertParseFailure(parser, arguments, expectedMessage);
        }
    }
}
