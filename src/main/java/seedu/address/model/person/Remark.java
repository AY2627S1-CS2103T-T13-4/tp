package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

/**
 * Represents an immutable remark about a person. An empty value means no remark.
 */
public class Remark {
    public final String value;

    /**
     * Constructs a remark with the given non-null value. Remarks have no content constraints.
     */
    public Remark(String value) {
        this.value = requireNonNull(value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof Remark otherRemark && value.equals(otherRemark.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
