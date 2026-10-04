package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's name in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS =
            "Names should contain only letters, numbers, and spaces, and be at most 70 characters long";
    public static final int MAX_LENGTH = 70;
    public static final String VALIDATION_REGEX = "[A-Za-z0-9]+(?: [A-Za-z0-9]+)*";

    public final String fullName;

    /**
     * Constructs a {@code Name} with surrounding whitespace removed and consecutive spaces collapsed.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        String normalizedName = normalizeName(name);
        checkArgument(isValidNormalizedName(normalizedName), MESSAGE_CONSTRAINTS);
        fullName = normalizedName;
    }

    /**
     * Returns true if the given string is a valid name after normalization.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        return isValidNormalizedName(normalizeName(test));
    }

    private static String normalizeName(String name) {
        return name.trim().replaceAll(" +", " ");
    }

    private static boolean isValidNormalizedName(String name) {
        return name.length() <= MAX_LENGTH && name.matches(VALIDATION_REGEX);
    }


    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
