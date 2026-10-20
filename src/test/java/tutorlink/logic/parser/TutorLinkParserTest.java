package tutorlink.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import tutorlink.logic.commands.AddCommand;
import tutorlink.logic.commands.ClearCommand;
import tutorlink.logic.commands.DeleteCommand;
import tutorlink.logic.commands.EditCommand;
import tutorlink.logic.commands.EditCommand.EditStudentDescriptor;
import tutorlink.logic.commands.ExitCommand;
import tutorlink.logic.commands.FindCommand;
import tutorlink.logic.commands.FindTagCommand;
import tutorlink.logic.commands.HelpCommand;
import tutorlink.logic.commands.ListCommand;
import tutorlink.logic.commands.RemarkCommand;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.student.Student;
import tutorlink.model.student.StudentContainsKeywordsPredicate;
import tutorlink.model.student.TagMatchesPredicate;
import tutorlink.model.student.TutorialGroup;
import tutorlink.model.tag.Tag;
import tutorlink.testutil.EditStudentDescriptorBuilder;
import tutorlink.testutil.StudentBuilder;
import tutorlink.testutil.StudentUtil;

public class TutorLinkParserTest {

    private final TutorLinkParser parser = new TutorLinkParser();

    @Test
    public void parseCommand_add() throws Exception {
        Student student = new StudentBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(StudentUtil.getAddCommand(student));
        assertEquals(new AddCommand(student), command);
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD) instanceof ClearCommand);
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD + " 3") instanceof ClearCommand);
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_STUDENT.getOneBased());
        assertEquals(new DeleteCommand(INDEX_FIRST_STUDENT), command);
    }

    @Test
    public void parseCommand_deleteConfirmed() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_STUDENT.getOneBased() + " confirm");
        assertEquals(new DeleteCommand(INDEX_FIRST_STUDENT, true), command);
    }

    @Test
    public void parseCommand_edit() throws Exception {
        Student student = new StudentBuilder().build();
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder(student).build();
        EditCommand command = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_STUDENT.getOneBased() + " " + StudentUtil.getEditStudentDescriptorDetails(descriptor));
        assertEquals(new EditCommand(INDEX_FIRST_STUDENT, descriptor), command);
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + " 3") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_find() throws Exception {
        List<String> keywords = List.of("foo", "bar", "baz");
        FindCommand command = (FindCommand) parser.parseCommand(
                FindCommand.COMMAND_WORD + " " + keywords.stream().collect(Collectors.joining(" ")));
        assertEquals(new FindCommand(new StudentContainsKeywordsPredicate(keywords)), command);
    }

    @Test
    public void parseCommand_findTag() throws Exception {
        assertEquals(new FindTagCommand(new TagMatchesPredicate(new Tag("NeedsHelp"))),
                parser.parseCommand("findtag \tNeedsHelp  "));
    }

    @Test
    public void parseCommand_findTagWithInvalidArguments_throwsParseException() {
        assertThrows(ParseException.class,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindTagCommand.MESSAGE_USAGE), () ->
                parser.parseCommand("findtag"));
        assertThrows(ParseException.class, Tag.MESSAGE_CONSTRAINTS, () -> parser.parseCommand("findtag t/friends"));
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + " 3") instanceof HelpCommand);
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertEquals(new ListCommand(), parser.parseCommand(ListCommand.COMMAND_WORD));
        assertEquals(new ListCommand(new TutorialGroup("T01")),
                parser.parseCommand(ListCommand.COMMAND_WORD + " g/t01"));
    }

    @Test
    public void parseCommand_remark() throws Exception {
        assertEquals(new RemarkCommand(INDEX_FIRST_STUDENT, "Needs help"),
                parser.parseCommand("remark 1 r/Needs help"));
    }

    @Test
    public void parseCommand_listWithInvalidArguments_throwsParseException() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, ListCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expectedMessage, () -> parser.parseCommand(ListCommand.COMMAND_WORD + " 3"));
    }

    @Test
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
            -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
    }
}
