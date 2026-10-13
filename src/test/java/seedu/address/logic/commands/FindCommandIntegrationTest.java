package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;
import static seedu.address.testutil.TypicalPersons.getTypicalPersons;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.MatricNumber;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.TutorialGroup;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/**
 * Exercises student searches through parsing, filtering, and the normal storage path.
 */
public class FindCommandIntegrationTest {

    @TempDir
    public Path temporaryFolder;

    private Model model;
    private Logic logic;
    private JsonAddressBookStorage addressBookStorage;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        addressBookStorage = new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json")));
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_matricNumberIgnoringCase_findsStudent() throws Exception {
        assertSearch("find \ta0000003c  ", List.of(BENSON));
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_mixedRepeatedKeywords_returnsUnionInDirectoryOrder() throws Exception {
        assertSearch("find A0000004D alice A0000002B ALICE", List.of(ALICE, CARL));
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_findAfterPreviousFilter_searchesCompleteDirectory() throws Exception {
        logic.execute("list g/t01");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
        assertSearch("find A0000004D", List.of(CARL));
        assertSearch("find Benson", List.of(BENSON));
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_groupFilterAfterSearch_replacesSearchResults() throws Exception {
        assertSearch("find Carl", List.of(CARL));
        logic.execute("list g/t01");
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_editSearchResult_preservesFilterAndSavesFullDirectory() throws Exception {
        assertSearch("find Carl", List.of(CARL));
        logic.execute("edit 1 n/Charlie Tan");

        Person editedCarl = new PersonBuilder(CARL).withName("Charlie Tan").build();
        List<Person> expectedStudents = getTypicalPersons();
        expectedStudents.set(expectedStudents.indexOf(CARL), editedCarl);
        assertEquals(List.of(), model.getFilteredPersonList());
        assertEquals(expectedStudents, model.getAddressBook().getPersonList());
        assertEquals(expectedStudents, addressBookStorage.readAddressBook().orElseThrow().getPersonList());
        assertSearch("find a0000004d", List.of(editedCarl));
    }

    @Test
    public void execute_invalidIndexAfterSearch_preservesResultsAndSavedDirectory() throws Exception {
        assertSearch("find Carl", List.of(CARL));

        assertThrows(CommandException.class,
                MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, () -> logic.execute("edit 2 n/Charlie Tan"));
        assertThrows(CommandException.class,
                MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, () -> logic.execute("delete 2 confirm"));

        assertEquals(List.of(CARL), model.getFilteredPersonList());
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_addAfterSearch_resetsViewAndRemainsSearchableAfterReload() throws Exception {
        assertSearch("find Carl", List.of(CARL));
        logic.execute("add n/Dana Tan m/a7777777z g/t01");

        Person addedStudent = new Person(new Name("Dana Tan"), new MatricNumber("A7777777Z"),
                new TutorialGroup("T01"), null, null, null, Set.of());
        List<Person> expectedStudents = getTypicalPersons();
        expectedStudents.add(addedStudent);
        assertEquals(expectedStudents, model.getFilteredPersonList());
        assertSearch("find a7777777z", List.of(addedStudent));

        model = new ModelManager(addressBookStorage.readAddressBook().orElseThrow(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));
        assertEquals(expectedStudents, model.getAddressBook().getPersonList());
        assertEquals(expectedStudents, model.getFilteredPersonList());
        assertSearch("find Dana", List.of(addedStudent));
    }

    @Test
    public void execute_noMatchThenList_restoresCompleteDirectory() throws Exception {
        assertSearch("find A9999999Z", List.of());
        logic.execute("list");
        assertEquals(getTypicalPersons(), model.getFilteredPersonList());
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_emptySearch_preservesPreviousResults() throws Exception {
        assertSearch("find Alice", List.of(ALICE));
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expectedMessage, () -> logic.execute("find \t"));
        assertEquals(List.of(ALICE), model.getFilteredPersonList());
        assertDirectoryUnchanged();
    }

    @Test
    public void execute_searchThenDelete_usesResultIndex() throws Exception {
        assertSearch("find A0000004D", List.of(CARL));
        logic.execute("delete 1");
        assertEquals(List.of(CARL), model.getFilteredPersonList());
        assertDirectoryUnchanged();
        logic.execute("delete 1 confirm");

        List<Person> remaining = getTypicalPersons();
        remaining.remove(CARL);
        assertEquals(remaining, model.getAddressBook().getPersonList());
        assertEquals(List.of(), model.getFilteredPersonList());
        assertEquals(remaining, addressBookStorage.readAddressBook().orElseThrow().getPersonList());
    }

    private void assertSearch(String input, List<Person> expectedStudents) throws Exception {
        CommandResult result = logic.execute(input);
        assertEquals(expectedStudents.size() + " student(s) listed!", result.getFeedbackToUser());
        assertEquals(expectedStudents, model.getFilteredPersonList());
    }

    private void assertDirectoryUnchanged() throws Exception {
        assertEquals(getTypicalAddressBook(), model.getAddressBook());
        assertEquals(getTypicalAddressBook(), addressBookStorage.readAddressBook().orElseThrow());
    }
}
