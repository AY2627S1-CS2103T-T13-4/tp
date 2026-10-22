package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.FollowUpCommand;

public class FollowUpCommandParserTest {
    private final FollowUpCommandParser parser = new FollowUpCommandParser();

    @Test
    public void parse_validActions_returnsCommand() {
        assertParseSuccess(parser, " 1 on ", new FollowUpCommand(INDEX_FIRST_STUDENT, true));
        assertParseSuccess(parser, " 1 done ", new FollowUpCommand(INDEX_FIRST_STUDENT, false));
        assertParseSuccess(parser, " \t1\t on \t", new FollowUpCommand(INDEX_FIRST_STUDENT, true));
    }

    @Test
    public void parse_invalidArguments_reportsUsage() {
        for (String args : List.of("", " ", "1", "on", "1 off", "1 ON", "1 DONE", "1 on extra",
                "1 done on", "0 on", "-1 done", "1.5 on", "abc done", "2147483648 on", "9999999999999999 done")) {
            assertParseFailure(parser, args,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FollowUpCommand.MESSAGE_USAGE));
        }
    }

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }
}
