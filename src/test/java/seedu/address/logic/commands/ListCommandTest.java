package seedu.address.logic.commands;

import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.TutorialGroup;
import seedu.address.model.person.TutorialGroupMatchesPredicate;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListCommand.
 */
public class ListCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
    }

    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_listIsFiltered_showsEverything() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_tutorialGroupSpecified_showsMatchingPersons() {
        TutorialGroup tutorialGroup = new TutorialGroup("T01");
        TutorialGroupMatchesPredicate predicate = new TutorialGroupMatchesPredicate(tutorialGroup);
        expectedModel.updateFilteredPersonList(predicate);

        String expectedMessage = String.format(
                Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, expectedModel.getFilteredPersonList().size());
        assertCommandSuccess(new ListCommand(tutorialGroup), model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_tutorialGroupHasNoMatches_showsEmptyList() {
        TutorialGroup tutorialGroup = new TutorialGroup("W12");
        TutorialGroupMatchesPredicate predicate = new TutorialGroupMatchesPredicate(tutorialGroup);
        expectedModel.updateFilteredPersonList(predicate);

        String expectedMessage = String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, 0);
        assertCommandSuccess(new ListCommand(tutorialGroup), model, expectedMessage, expectedModel);
    }
}
