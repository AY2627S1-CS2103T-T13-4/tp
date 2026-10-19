package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.FollowUpCommand;

public class FollowUpCommandParserTest {
    private final FollowUpCommandParser parser = new FollowUpCommandParser();

    @Test
    public void parse_validActions_returnsCommand() {
        assertParseSuccess(parser, " 1 on ", new FollowUpCommand(INDEX_FIRST_STUDENT, true));
        assertParseSuccess(parser, " 1 done ", new FollowUpCommand(INDEX_FIRST_STUDENT, false));
    }

    @Test
    public void parse_invalidArguments_reportsUsage() {
        assertParseFailure(parser, "0 on",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpCommand.MESSAGE_USAGE));
        assertParseFailure(parser, "1 off",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpCommand.MESSAGE_USAGE));
    }
}
