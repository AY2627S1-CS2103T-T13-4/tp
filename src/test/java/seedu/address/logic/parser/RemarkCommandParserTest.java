package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.RemarkCommand;
import seedu.address.model.person.Remark;

public class RemarkCommandParserTest {

    private static final String MESSAGE_INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);

    private final RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_validInput_success() {
        assertParseSuccess(parser, "1 r/Likes baseball",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes baseball")));
    }

    @Test
    public void parse_emptyRemark_success() {
        assertParseSuccess(parser, "2 r/", new RemarkCommand(INDEX_SECOND_PERSON, new Remark("")));
        assertParseSuccess(parser, "2", new RemarkCommand(INDEX_SECOND_PERSON, new Remark("")));
    }

    @Test
    public void parse_invalidInput_failure() {
        assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "0 r/remark", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "1 invalid preamble", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "1 r/one r/two", MESSAGE_INVALID_FORMAT);
    }
}
