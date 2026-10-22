package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorlink.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.SortCommand;
import tutorlink.logic.commands.SortGroupCommand;
import tutorlink.logic.commands.SortResetCommand;

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
        assertParseSuccess(parser, " group", new SortGroupCommand(false));
        assertParseSuccess(parser, " group asc", new SortGroupCommand(false));
        assertParseSuccess(parser, " group desc", new SortGroupCommand(true));
        assertParseSuccess(parser, " \tgroup  \t desc \t", new SortGroupCommand(true));
        assertParseSuccess(parser, " reset", new SortResetCommand());
        assertParseSuccess(parser, " \treset \t", new SortResetCommand());
    }

    @Test
    public void parse_invalidArguments_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SortCommand.MESSAGE_USAGE);
        for (String arguments : new String[] {"", "   ", "asc", "groups", "Reset", "reset asc", "reset desc",
            "reset extra", "reset name", "reset asc extra", "Name", "name ASC",
            "name ascending", "name asc desc", "name desc extra", "name n/Alice", "Group", "group DESC",
            "group ascending", "group asc desc", "group desc extra", "group g/T01", "group name"}) {
            assertParseFailure(parser, arguments, expectedMessage);
        }
    }
}
