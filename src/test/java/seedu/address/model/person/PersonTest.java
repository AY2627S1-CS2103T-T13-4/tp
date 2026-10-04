package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_MATRIC_NUMBER_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TUTORIAL_GROUP_BOB;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class PersonTest {

    @Test
    public void constructor_nullMatricNumberOrTutorialGroup_throwsNullPointerException() {
        Person person = new PersonBuilder().build();
        assertThrows(NullPointerException.class, () -> new Person(person.getName(), null,
                person.getTutorialGroup(), person.getPhone(), person.getEmail(), person.getAddress(),
                person.getTags()));
        assertThrows(NullPointerException.class, () -> new Person(person.getName(), person.getMatricNumber(),
                null, person.getPhone(), person.getEmail(), person.getAddress(), person.getTags()));
    }

    @Test
    public void getters_returnStudentProfileFields() {
        Person student = new Person(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("L03"), new Phone("98765432"), new Email("alice@example.com"),
                new Address("NUS"), Set.of());

        assertEquals(new Name("Alice Tan"), student.getName());
        assertEquals(new MatricNumber("A0123456X"), student.getMatricNumber());
        assertEquals(new TutorialGroup("L03"), student.getTutorialGroup());
    }

    @Test
    public void constructor_missingOptionalDetails_retainsNullValues() {
        Person student = new Person(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("L03"), null, null, null, Set.of());

        assertNull(student.getPhone());
        assertNull(student.getEmail());
        assertNull(student.getAddress());
        assertNull(student.getRemark());
        assertEquals(student, new Person(new Name("Alice Tan"), new MatricNumber("a0123456x"),
                new TutorialGroup("l03"), null, null, null, Set.of()));
    }

    @Test
    public void constructor_existingRemark_retainsValueAndAffectsEquality() {
        Person withRemark = new Person(ALICE.getName(), ALICE.getMatricNumber(), ALICE.getTutorialGroup(),
                ALICE.getPhone(), ALICE.getEmail(), ALICE.getAddress(), ALICE.getTags(), "Needs follow-up");

        assertEquals("Needs follow-up", withRemark.getRemark());
        assertFalse(ALICE.equals(withRemark));
    }

    @Test
    public void asObservableList_modifyList_throwsUnsupportedOperationException() {
        Person person = new PersonBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> person.getTags().remove(0));
    }

    @Test
    public void isSamePerson() {
        // same object -> returns true
        assertTrue(ALICE.isSamePerson(ALICE));

        // null -> returns false
        assertFalse(ALICE.isSamePerson(null));

        // same matriculation number, all other attributes different -> returns true
        Person editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB)
                .withTutorialGroup(VALID_TUTORIAL_GROUP_BOB).withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_BOB)
                .withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // different matriculation number, all other attributes same -> returns false
        editedAlice = new PersonBuilder(ALICE).withMatricNumber(VALID_MATRIC_NUMBER_BOB).build();
        assertFalse(ALICE.isSamePerson(editedAlice));

        // different name, same matriculation number -> returns true
        editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertTrue(ALICE.isSamePerson(editedAlice));
    }

    @Test
    public void equals() {
        // same values -> returns true
        Person aliceCopy = new PersonBuilder(ALICE).build();
        assertTrue(ALICE.equals(aliceCopy));

        // same object -> returns true
        assertTrue(ALICE.equals(ALICE));

        // null -> returns false
        assertFalse(ALICE.equals(null));

        // different type -> returns false
        assertFalse(ALICE.equals(5));

        // different person -> returns false
        assertFalse(ALICE.equals(BOB));

        // different name -> returns false
        Person editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different matriculation number -> returns false
        editedAlice = new PersonBuilder(ALICE).withMatricNumber(VALID_MATRIC_NUMBER_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different tutorial group -> returns false
        editedAlice = new PersonBuilder(ALICE).withTutorialGroup(VALID_TUTORIAL_GROUP_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different phone -> returns false
        editedAlice = new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different email -> returns false
        editedAlice = new PersonBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different address -> returns false
        editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different tags -> returns false
        editedAlice = new PersonBuilder(ALICE).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.equals(editedAlice));
    }

    @Test
    public void toStringMethod() {
        String expected = Person.class.getCanonicalName() + "{name=" + ALICE.getName()
                + ", matricNumber=" + ALICE.getMatricNumber() + ", tutorialGroup=" + ALICE.getTutorialGroup()
                + ", phone=" + ALICE.getPhone()
                + ", email=" + ALICE.getEmail() + ", address=" + ALICE.getAddress()
                + ", remark=" + ALICE.getRemark() + ", tags=" + ALICE.getTags() + "}";
        assertEquals(expected, ALICE.toString());
    }
}
