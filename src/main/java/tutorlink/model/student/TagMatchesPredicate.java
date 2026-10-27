package tutorlink.model.student;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import tutorlink.commons.util.ToStringBuilder;
import tutorlink.model.tag.Tag;

/**
 * Tests whether a student has a tag that exactly matches the requested tag, ignoring case.
 */
public class TagMatchesPredicate implements Predicate<Student> {
    private final Tag tag;

    public TagMatchesPredicate(Tag tag) {
        this.tag = requireNonNull(tag);
    }

    @Override
    public boolean test(Student student) {
        return student.getTags().stream()
                .anyMatch(studentTag -> studentTag.tagName.equalsIgnoreCase(tag.tagName));
    }

    /** Describes the search condition independently of transient command feedback. */
    public String getDescription() {
        return "Tag: " + tag.tagName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof TagMatchesPredicate otherPredicate)) {
            return false;
        }

        return tag.equals(otherPredicate.tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("tag", tag).toString();
    }
}
