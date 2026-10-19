package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorlink.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.FindTagCommand;
import tutorlink.model.student.TagMatchesPredicate;
import tutorlink.model.tag.Tag;

public class FindTagCommandParserTest {

    private final FindTagCommandParser parser = new FindTagCommandParser();

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parse_emptyTag_throwsParseException() {
        for (String args : List.of("", "   ", "\t\n")) {
            assertParseFailure(parser, args,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindTagCommand.MESSAGE_USAGE));
        }
    }

    @Test
    public void parse_validTag_returnsFindTagCommand() {
        for (String tag : List.of("needsHelp", "NEEDSHELP", "group2", "123")) {
            FindTagCommand expectedCommand = new FindTagCommand(new TagMatchesPredicate(new Tag(tag)));
            assertParseSuccess(parser, tag, expectedCommand);
            assertParseSuccess(parser, " \t\n" + tag + " \t\n", expectedCommand);
        }
    }

    @Test
    public void parse_invalidTag_throwsParseException() {
        for (String args : List.of("t/friends", "friends family", "friends\tfamily", "friends\nfamily",
                "needs-help", "needs_help", "friends!", "\"friends\"", "学生")) {
            assertParseFailure(parser, args, Tag.MESSAGE_CONSTRAINTS);
        }
    }
}
