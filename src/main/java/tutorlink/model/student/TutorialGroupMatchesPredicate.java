package tutorlink.model.student;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import tutorlink.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Student}'s tutorial group matches the specified tutorial group.
 */
public class TutorialGroupMatchesPredicate implements Predicate<Student> {
    private final TutorialGroup tutorialGroup;

    public TutorialGroupMatchesPredicate(TutorialGroup tutorialGroup) {
        this.tutorialGroup = requireNonNull(tutorialGroup);
    }

    @Override
    public boolean test(Student student) {
        return student.getTutorialGroup().equals(tutorialGroup);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof TutorialGroupMatchesPredicate otherPredicate)) {
            return false;
        }

        return tutorialGroup.equals(otherPredicate.tutorialGroup);
    }

    @Override
    public int hashCode() {
        return tutorialGroup.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("tutorialGroup", tutorialGroup)
                .toString();
    }
}
