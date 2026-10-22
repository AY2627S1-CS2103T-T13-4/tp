package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorlink.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.FollowUpsCommand;

public class FollowUpsCommandParserTest {
    private final FollowUpsCommandParser parser = new FollowUpsCommandParser();

    @Test
    public void parse_noArguments_acceptsWhitespace() {
        assertParseSuccess(parser, "", new FollowUpsCommand());
        assertParseSuccess(parser, " \t ", new FollowUpsCommand());
    }

    @Test
    public void parse_extraArguments_reportsUsage() {
        assertParseFailure(parser, "1", String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpsCommand.MESSAGE_USAGE));
        assertParseFailure(parser, "g/T01",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpsCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
