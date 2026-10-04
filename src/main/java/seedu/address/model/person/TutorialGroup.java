package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a student's tutorial group.
 * Guarantees: immutable; is valid as declared in {@link #isValidTutorialGroup(String)}.
 */
public class TutorialGroup {

    public static final String MESSAGE_CONSTRAINTS =
            "Tutorial groups should start with T or L, followed by 1 or 2 digits";
    public static final String VALIDATION_REGEX = "[TLtl][0-9]{1,2}";

    public final String value;

    /**
     * Constructs a {@code TutorialGroup}, uppercases its prefix and removes leading zeros from its number.
     *
     * @param tutorialGroup A valid tutorial group.
     */
    public TutorialGroup(String tutorialGroup) {
        requireNonNull(tutorialGroup);
        checkArgument(isValidTutorialGroup(tutorialGroup), MESSAGE_CONSTRAINTS);
        value = Character.toUpperCase(tutorialGroup.charAt(0))
                + Integer.toString(Integer.parseInt(tutorialGroup.substring(1)));
    }

    /**
     * Returns true if a given string is a valid tutorial group.
     */
    public static boolean isValidTutorialGroup(String test) {
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
        if (!(other instanceof TutorialGroup otherTutorialGroup)) {
            return false;
        }

        return value.equals(otherTutorialGroup.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
