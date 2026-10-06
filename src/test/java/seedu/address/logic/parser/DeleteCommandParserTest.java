package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.DeleteCommand;

/**
 * Tests parsing of student indices and the optional confirmation keyword.
 */
public class DeleteCommandParserTest {

    private DeleteCommandParser parser = new DeleteCommandParser();

    @Test
    public void parse_validArgs_returnsDeleteCommand() {
        assertParseSuccess(parser, "1", new DeleteCommand(INDEX_FIRST_PERSON));
    }

    @Test
    public void parse_confirmedArgs_returnsConfirmedDeleteCommand() {
        assertParseSuccess(parser, "1 confirm", new DeleteCommand(INDEX_FIRST_PERSON, true));
        assertParseSuccess(parser, "  1 \t confirm  ", new DeleteCommand(INDEX_FIRST_PERSON, true));
    }

    @Test
    public void parse_invalidArgs_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE);
        String[] invalidArgs = {"", " ", "a", "0", "-1", "2147483648", "a confirm", "0 confirm",
            "-1 confirm", "1 yes", "1 Confirm", "1 confirm extra", "1 2", "confirm"};
        for (String args : invalidArgs) {
            assertParseFailure(parser, args, expectedMessage);
        }
    }
}
