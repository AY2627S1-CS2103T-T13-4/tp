package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class RemarkCommandIntegrationTest {
    @TempDir
    public Path testFolder;

    @Test
    public void execute_remarkEditAndClear_persistsChanges() throws Exception {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(testFolder.resolve("addressbook.json"));
        LogicManager logic = new LogicManager(model, new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
        Person original = model.getFilteredPersonList().getFirst();
        Person remarked = new PersonBuilder(original).withRemark("Likes swimming").build();

        logic.execute("remark 1 r/Likes swimming");
        assertEquals(remarked, addressBookStorage.readAddressBook().orElseThrow().getPersonList().getFirst());

        logic.execute("edit 1 p/91234567");
        Person edited = new PersonBuilder(remarked).withPhone("91234567").build();
        assertEquals(edited, addressBookStorage.readAddressBook().orElseThrow().getPersonList().getFirst());

        logic.execute("remark 1 r/");
        assertEquals(new PersonBuilder(edited).withRemark("").build(),
                addressBookStorage.readAddressBook().orElseThrow().getPersonList().getFirst());
    }

    @Test
    public void read_oldAddressBookWithoutRemark_preservesContacts() throws Exception {
        Path file = testFolder.resolve("legacy.json");
        Files.writeString(file, """
                {"persons":[{"name":"Alex Yeoh","phone":"87438807","email":"alex@example.com",
                "address":"Geylang","tags":["friends"]}]}
                """);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        Person expected = new PersonBuilder().withName("Alex Yeoh").withPhone("87438807")
                .withEmail("alex@example.com").withAddress("Geylang").withTags("friends").build();
        assertEquals(expected, storage.readAddressBook().orElseThrow().getPersonList().getFirst());
        assertTrue(storage.readAddressBook().orElseThrow().getPersonList().getFirst().getRemark().value.isEmpty());
    }
}
