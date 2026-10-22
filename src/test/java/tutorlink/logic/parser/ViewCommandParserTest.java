package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.ViewCommand;

public class ViewCommandParserTest {
    private final ViewCommandParser parser = new ViewCommandParser();

    @Test
    public void parse_validIndex_acceptsWhitespace() {
        assertParseSuccess(parser, " 1 ", new ViewCommand(INDEX_FIRST_STUDENT));
    }

    @Test
    public void parse_invalidOrExtraArguments_rejected() {
        String message = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ViewCommand.MESSAGE_USAGE);
        for (String args : new String[] {"", " ", "0", "-1", "abc", "1 2", "1 extra", "999999999999"}) {
            assertParseFailure(parser, args, message);
        }
    }
}
