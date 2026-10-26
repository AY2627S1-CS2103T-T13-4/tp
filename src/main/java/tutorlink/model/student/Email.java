package tutorlink.model.student;

import static java.util.Objects.requireNonNull;
import static tutorlink.commons.util.AppUtil.checkArgument;

/**
 * Represents a Student's email in the student directory.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    private static final String SPECIAL_CHARACTERS = "+_.-";
    public static final String MESSAGE_CONSTRAINTS = "Emails should be of the format local-part@domain "
            + "and adhere to the following constraints:\n"
            + "1. The local-part should only contain alphanumeric characters and these special characters, excluding "
            + "the parentheses, (" + SPECIAL_CHARACTERS + "). The local-part may not start or end with any special "
            + "characters, contain consecutive periods, or exceed 64 characters.\n"
            + "2. The local-part is followed by an '@' and then a domain name. The domain name is made up of domain "
            + "labels separated by periods.\n"
            + "The domain name must:\n"
            + "    - end with a domain label at least 2 characters long\n"
            + "    - have each domain label start and end with alphanumeric characters\n"
            + "    - have each domain label consist of at most 63 alphanumeric characters or hyphens.\n"
            + "The complete email must not exceed 254 characters.";
    // alphanumeric and special characters
    private static final String LOCAL_PART_REGEX = "[A-Za-z0-9](?:[A-Za-z0-9+_.-]*[A-Za-z0-9])?";
    private static final String DOMAIN_PART_REGEX = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?";
    private static final String DOMAIN_REGEX = "(?:" + DOMAIN_PART_REGEX + "\\.)*" + DOMAIN_PART_REGEX;
    public static final String VALIDATION_REGEX = LOCAL_PART_REGEX + "@" + DOMAIN_REGEX;

    public final String value;

    /**
     * Constructs an {@code Email}.
     *
     * @param email A valid email address.
     */
    public Email(String email) {
        requireNonNull(email);
        checkArgument(isValidEmail(email), MESSAGE_CONSTRAINTS);
        value = email;
    }

    /**
     * Returns true if a given string is a valid email.
     */
    public static boolean isValidEmail(String test) {
        requireNonNull(test);
        if (test.length() > 254 || !test.matches(VALIDATION_REGEX)) {
            return false;
        }
        String localPart = test.substring(0, test.indexOf('@'));
        String domain = test.substring(test.indexOf('@') + 1);
        return localPart.length() <= 64 && !localPart.contains("..")
                && domain.substring(domain.lastIndexOf('.') + 1).length() >= 2;
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
        if (!(other instanceof Email otherEmail)) {
            return false;
        }

        return value.equals(otherEmail.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
