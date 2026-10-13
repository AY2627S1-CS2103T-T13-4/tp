package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CliSyntax.PREFIX_TUTORIAL_GROUP;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorlink.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import tutorlink.logic.Messages;
import tutorlink.logic.commands.ListCommand;
import tutorlink.model.student.TutorialGroup;

public class ListCommandParserTest {
    private final ListCommandParser parser = new ListCommandParser();

    @Test
    public void parse_noArguments_success() {
        assertParseSuccess(parser, "", new ListCommand());
        assertParseSuccess(parser, "   ", new ListCommand());
    }

    @Test
    public void parse_validTutorialGroup_success() {
        assertParseSuccess(parser, " g/T01", new ListCommand(new TutorialGroup("T01")));
        assertParseSuccess(parser, " g/t01", new ListCommand(new TutorialGroup("T01")));
    }

    @Test
    public void parse_invalidTutorialGroup_failure() {
        assertParseFailure(parser, " g/T1", TutorialGroup.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " g/T001", TutorialGroup.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_missingTutorialGroup_failure() {
        assertParseFailure(parser, " g/", TutorialGroup.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_nonEmptyPreamble_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ListCommand.MESSAGE_USAGE);
        assertParseFailure(parser, " T01", expectedMessage);
    }

    @Test
    public void parse_repeatedTutorialGroup_failure() {
        assertParseFailure(parser, " g/T01 g/T02",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TUTORIAL_GROUP));
    }
}
