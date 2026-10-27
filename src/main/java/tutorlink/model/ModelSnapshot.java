package tutorlink.model;

import static tutorlink.commons.util.CollectionUtil.requireAllNonNull;

import java.util.function.Predicate;

import tutorlink.model.student.Student;

/**
 * Captures the roster and displayed view so a failed save can be rolled back together.
 * Students are immutable, and the directory is copied to keep later edits out of the snapshot.
 */
public record ModelSnapshot(ReadOnlyStudentDirectory studentDirectory, Predicate<? super Student> predicate,
        StudentSortOrder sortOrder, String filterDescription) {

    /** Creates an independent roster snapshot with the current filter and sort rule. */
    public ModelSnapshot {
        requireAllNonNull(studentDirectory, predicate, sortOrder, filterDescription);
        studentDirectory = new StudentDirectory(studentDirectory);
    }
}
