package tutorlink.model;

import java.util.Comparator;

import tutorlink.model.student.Student;

/**
 * Represents a student sorting rule independently of the comparator object that implements it.
 */
public enum StudentSortOrder {
    DIRECTORY(null),
    NAME_ASC(createNameComparator(false)),
    NAME_DESC(createNameComparator(true)),
    GROUP_ASC(createGroupComparator(false)),
    GROUP_DESC(createGroupComparator(true));

    private final Comparator<Student> comparator;

    StudentSortOrder(Comparator<Student> comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns the comparator for this rule, or null for the original directory order.
     */
    Comparator<Student> getComparator() {
        return comparator;
    }

    private static Comparator<Student> createNameComparator(boolean isDescending) {
        Comparator<Student> nameComparator = Comparator.comparing(
                student -> student.getName().fullName, String.CASE_INSENSITIVE_ORDER);
        if (isDescending) {
            nameComparator = nameComparator.reversed();
        }
        return nameComparator.thenComparing(student -> student.getMatricNumber().value);
    }

    private static Comparator<Student> createGroupComparator(boolean isDescending) {
        Comparator<Student> groupComparator = Comparator.comparing(student -> student.getTutorialGroup().value);
        if (isDescending) {
            groupComparator = groupComparator.reversed();
        }
        return groupComparator.thenComparing(student -> student.getMatricNumber().value);
    }
}
