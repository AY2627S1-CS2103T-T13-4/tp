package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorlink.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.RemarkCommand;

public class RemarkCommandParserTest {
    private static final String MESSAGE_INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);

    private final RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_validRemark_success() {
        assertParseSuccess(parser, "1 r/Needs help with recursion",
                new RemarkCommand(INDEX_FIRST_STUDENT, "Needs help with recursion"));
    }

    @Test
    public void parse_emptyRemark_success() {
        assertParseSuccess(parser, "2 r/", new RemarkCommand(INDEX_SECOND_STUDENT, null));
        assertParseSuccess(parser, "2 r/   ", new RemarkCommand(INDEX_SECOND_STUDENT, null));
    }

    @Test
    public void parse_remarkContainingPrefix_success() {
        assertParseSuccess(parser, "1 r/Discuss r/w file access modes",
                new RemarkCommand(INDEX_FIRST_STUDENT, "Discuss r/w file access modes"));
    }

    @Test
    public void parse_invalidArguments_failure() {
        assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "0 r/remark", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "1", MESSAGE_INVALID_FORMAT);
    }
}
