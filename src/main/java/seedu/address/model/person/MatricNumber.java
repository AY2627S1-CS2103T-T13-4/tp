package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;

/**
 * Represents a student's matriculation number.
 * Guarantees: immutable; is valid as declared in {@link #isValidMatricNumber(String)}.
 */
public class MatricNumber {

    public static final String MESSAGE_CONSTRAINTS =
            "Matriculation numbers should start with A, followed by 7 digits and an uppercase letter";
    public static final String VALIDATION_REGEX = "[Aa][0-9]{7}[A-Za-z]";

    public final String value;

    /**
     * Constructs a {@code MatricNumber}.
     *
     * @param matricNumber A valid matriculation number.
     */
    public MatricNumber(String matricNumber) {
        requireNonNull(matricNumber);
        checkArgument(isValidMatricNumber(matricNumber), MESSAGE_CONSTRAINTS);
        value = matricNumber.toUpperCase(Locale.ROOT);
    }

    /**
     * Returns true if a given string is a valid matriculation number.
     */
    public static boolean isValidMatricNumber(String test) {
        requireNonNull(test);
        return test.matches(VALIDATION_REGEX);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof MatricNumber otherMatricNumber)) {
            return false;
        }

        return value.equals(otherMatricNumber.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
