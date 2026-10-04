package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class MatricNumberTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new MatricNumber(null));
    }

    @Test
    public void constructor_invalidMatricNumber_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new MatricNumber("A012345X"));
    }

    @Test
    public void isValidMatricNumber() {
        assertThrows(NullPointerException.class, () -> MatricNumber.isValidMatricNumber(null));

        assertFalse(MatricNumber.isValidMatricNumber(""));
        assertFalse(MatricNumber.isValidMatricNumber("A012345X")); // 6 digits
        assertFalse(MatricNumber.isValidMatricNumber("A01234567X")); // 8 digits
        assertFalse(MatricNumber.isValidMatricNumber("a0123456X")); // lowercase prefix
        assertFalse(MatricNumber.isValidMatricNumber("A0123456x")); // lowercase suffix
        assertFalse(MatricNumber.isValidMatricNumber("A01234X6X")); // non-digit in the middle
        assertFalse(MatricNumber.isValidMatricNumber(" A0123456X")); // whitespace

        assertTrue(MatricNumber.isValidMatricNumber("A0123456X"));
        assertTrue(MatricNumber.isValidMatricNumber("A0000000A"));
    }

    @Test
    public void equals() {
        MatricNumber matricNumber = new MatricNumber("A0123456X");

        assertTrue(matricNumber.equals(new MatricNumber("A0123456X")));
        assertTrue(matricNumber.equals(matricNumber));
        assertFalse(matricNumber.equals(null));
        assertFalse(matricNumber.equals(5));
        assertFalse(matricNumber.equals(new MatricNumber("A0123457X")));
    }

    @Test
    public void hashCode_sameValue_sameHashCode() {
        assertEquals(new MatricNumber("A0123456X").hashCode(), new MatricNumber("A0123456X").hashCode());
    }

    @Test
    public void toString_returnsValue() {
        assertEquals("A0123456X", new MatricNumber("A0123456X").toString());
    }
}
