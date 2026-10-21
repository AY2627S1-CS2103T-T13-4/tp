package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorlink.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.SortCommand;

public class SortCommandParserTest {
    private final SortCommandParser parser = new SortCommandParser();

    @Test
    public void parse_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, "Sort arguments must not be null.", () -> parser.parse(null));
    }

    @Test
    public void parse_validArguments_success() {
        assertParseSuccess(parser, " name", new SortCommand(false));
        assertParseSuccess(parser, " name asc", new SortCommand(false));
        assertParseSuccess(parser, " name desc", new SortCommand(true));
        assertParseSuccess(parser, " \tname  \t desc \t", new SortCommand(true));
    }

    @Test
    public void parse_invalidArguments_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortCommand.MESSAGE_USAGE);
        for (String arguments : new String[] {"", "   ", "asc", "group", "reset", "Name", "name ASC",
            "name ascending", "name asc desc", "name desc extra", "name n/Alice"}) {
            assertParseFailure(parser, arguments, expectedMessage);
        }
    }
}
