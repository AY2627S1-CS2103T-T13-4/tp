package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a student's tutorial group.
 * Guarantees: immutable; is valid as declared in {@link #isValidTutorialGroup(String)}.
 */
public class TutorialGroup {

    public static final String MESSAGE_CONSTRAINTS =
            "Tutorial groups should contain one letter followed by exactly two digits";
    public static final String VALIDATION_REGEX = "[A-Za-z][0-9]{2}";

    public final String value;

    /**
     * Constructs a {@code TutorialGroup}, uppercasing its letter and preserving both digits.
     *
     * @param tutorialGroup A valid tutorial group.
     */
    public TutorialGroup(String tutorialGroup) {
        requireNonNull(tutorialGroup);
        checkArgument(isValidTutorialGroup(tutorialGroup), MESSAGE_CONSTRAINTS);
        value = Character.toUpperCase(tutorialGroup.charAt(0)) + tutorialGroup.substring(1);
    }

    /**
     * Returns true if a given string is a valid tutorial group.
     */
    public static boolean isValidTutorialGroup(String test) {
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
