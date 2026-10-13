package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showStudentAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.address.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.student.TutorialGroup;
import seedu.address.model.student.TutorialGroupMatchesPredicate;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListCommand.
 */
public class ListCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
    }

    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_listIsFiltered_showsEverything() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_tutorialGroupSpecified_showsMatchingStudents() {
        TutorialGroup tutorialGroup = new TutorialGroup("T01");
        TutorialGroupMatchesPredicate predicate = new TutorialGroupMatchesPredicate(tutorialGroup);
        expectedModel.updateFilteredStudentList(predicate);

        String expectedMessage = String.format(
                Messages.MESSAGE_STUDENTS_LISTED_OVERVIEW, expectedModel.getFilteredStudentList().size());
        assertCommandSuccess(new ListCommand(tutorialGroup), model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_tutorialGroupHasNoMatches_showsEmptyList() {
        TutorialGroup tutorialGroup = new TutorialGroup("W12");
        TutorialGroupMatchesPredicate predicate = new TutorialGroupMatchesPredicate(tutorialGroup);
        expectedModel.updateFilteredStudentList(predicate);

        String expectedMessage = String.format(Messages.MESSAGE_STUDENTS_LISTED_OVERVIEW, 0);
        assertCommandSuccess(new ListCommand(tutorialGroup), model, expectedMessage, expectedModel);
    }

    @Test
    public void equals() {
        ListCommand listAllCommand = new ListCommand();
        ListCommand listT01Command = new ListCommand(new TutorialGroup("T01"));

        assertTrue(listAllCommand.equals(listAllCommand));
        assertTrue(listAllCommand.equals(new ListCommand()));
        assertTrue(listT01Command.equals(new ListCommand(new TutorialGroup("t01"))));
        assertFalse(listAllCommand.equals(listT01Command));
        assertFalse(listT01Command.equals(new ListCommand(new TutorialGroup("T02"))));
        assertFalse(listAllCommand.equals(null));
        assertFalse(listAllCommand.equals(1));
    }

    @Test
    public void hashCode_equalCommands_sameHashCode() {
        ListCommand firstCommand = new ListCommand(new TutorialGroup("T01"));
        ListCommand secondCommand = new ListCommand(new TutorialGroup("t01"));

        assertEquals(firstCommand.hashCode(), secondCommand.hashCode());
    }

    @Test
    public void toStringMethod() {
        TutorialGroup tutorialGroup = new TutorialGroup("T01");
        ListCommand listCommand = new ListCommand(tutorialGroup);
        String expected = ListCommand.class.getCanonicalName()
                + "{tutorialGroup=Optional[" + tutorialGroup + "]}";

        assertEquals(expected, listCommand.toString());
    }
}
