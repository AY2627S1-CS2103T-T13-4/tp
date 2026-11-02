package tutorlink.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

public class NameTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Name(null));
    }

    @Test
    public void constructor_invalidName_throwsIllegalArgumentException() {
        String invalidName = "";
        assertThrows(IllegalArgumentException.class, () -> new Name(invalidName));
        assertThrows(IllegalArgumentException.class, () -> new Name("A".repeat(71)));
    }

    @Test
    public void constructor_consecutiveSpaces_normalizesName() {
        assertEquals("John Doe", new Name("  John   Doe  ").fullName);
        assertEquals("A".repeat(70), new Name("  " + "A".repeat(70) + "  ").fullName);
    }

    @Test
    public void constructor_unicodeAndPunctuation_preservesNameAndNormalizesSpaces() {
        for (String name : List.of("John O'Connor", "Sean O’Neil", "Mary-Jane Tan", "Ravi s/o Kumar",
                "Anita d/o Devi", "José Tan", "Jose\u0301 Tan", "王伟", "J. Tan", "12345")) {
            assertTrue(Name.isValidName(name), name);
            assertEquals(name, new Name("  " + name.replace(" ", "   ") + "  ").fullName);
        }
    }

    @Test
    public void constructor_invalidCharactersOrMissingLettersAndDigits_rejectsName() {
        for (String name : List.of("", "   ", "'’-/.", "\u0301", "Peter*", "Peter\tJack", "Peter\nJack",
                "Peter\u0000Jack", "Peter\u007fJack", "Peter\u00a0Jack", "A".repeat(71))) {
            assertFalse(Name.isValidName(name), name);
            assertThrows(IllegalArgumentException.class, Name.MESSAGE_CONSTRAINTS, () -> new Name(name));
        }
    }

    @Test
    public void constructor_unicodeLengthBoundary_checksNormalizedLength() {
        assertEquals("é".repeat(70), new Name("  " + "é".repeat(70) + "  ").fullName);
        assertThrows(IllegalArgumentException.class, () -> new Name("é".repeat(71)));
        assertEquals("e\u0301".repeat(35), new Name("e\u0301".repeat(35)).fullName);
        assertThrows(IllegalArgumentException.class, () -> new Name("e\u0301".repeat(35) + "A"));
    }

    @Test
    public void isValidName() {
        // null name
        assertThrows(NullPointerException.class, () -> Name.isValidName(null));

        // invalid name
        assertFalse(Name.isValidName("")); // empty string
        assertFalse(Name.isValidName(" ")); // spaces only
        assertFalse(Name.isValidName("^")); // only non-alphanumeric characters
        assertFalse(Name.isValidName("peter*")); // contains non-alphanumeric characters
        assertFalse(Name.isValidName("Peter\tJack")); // internal non-space whitespace
        assertFalse(Name.isValidName("A".repeat(71))); // too long after normalization

        // valid name
        assertTrue(Name.isValidName("peter jack")); // alphabets only
        assertTrue(Name.isValidName("12345")); // numbers only
        assertTrue(Name.isValidName("peter the 2nd")); // alphanumeric characters
        assertTrue(Name.isValidName("Capital Tan")); // with capital letters
        assertTrue(Name.isValidName("David Roger Jackson Ray Jr 2nd")); // long names
        assertTrue(Name.isValidName("  Peter   Jack  ")); // normalized spaces
        assertTrue(Name.isValidName("A".repeat(70))); // maximum length
    }

    @Test
    public void equals() {
        Name name = new Name("Valid Name");

        // same values -> returns true
        assertTrue(name.equals(new Name("Valid Name")));

        // same object -> returns true
        assertTrue(name.equals(name));

        // null -> returns false
        assertFalse(name.equals(null));

        // different types -> returns false
        assertFalse(name.equals(5.0f));

        // different values -> returns false
        assertFalse(name.equals(new Name("Other Valid Name")));
    }
}
