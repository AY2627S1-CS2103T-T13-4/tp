package tutorlink.model.student;

import java.util.List;
import java.util.function.Predicate;

import tutorlink.commons.util.StringUtil;
import tutorlink.commons.util.ToStringBuilder;

/**
 * Tests whether a student's name or matriculation number matches any keyword.
 * Names match whole words; matriculation numbers match in full. Both ignore case.
 */
public class StudentContainsKeywordsPredicate implements Predicate<Student> {
    private final List<String> keywords;

    public StudentContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = List.copyOf(keywords);
    }

    @Override
    public boolean test(Student student) {
        return keywords.stream()
                .anyMatch(keyword -> StringUtil.containsWordIgnoreCase(student.getName().fullName, keyword)
                        || student.getMatricNumber().value.equalsIgnoreCase(keyword));
    }

    /** Describes the search condition independently of transient command feedback. */
    public String getDescription() {
        return "Name or matriculation: " + String.join(" ", keywords);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof StudentContainsKeywordsPredicate otherPredicate)) {
            return false;
        }

        return keywords.equals(otherPredicate.keywords);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywords", keywords).toString();
    }
}
