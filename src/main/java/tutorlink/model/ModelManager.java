package tutorlink.model;

import static java.util.Objects.requireNonNull;
import static tutorlink.commons.util.CollectionUtil.requireAllNonNull;

import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import tutorlink.commons.core.GuiSettings;
import tutorlink.commons.core.LogsCenter;
import tutorlink.model.student.Student;

/**
 * Represents the in-memory model of the student directory data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final StudentDirectory studentDirectory;
    private final UserPrefs userPrefs;
    private final FilteredList<Student> filteredStudents;
    private final SortedList<Student> sortedStudents;
    private StudentSortOrder sortOrder = StudentSortOrder.DIRECTORY;

    /**
     * Initializes a ModelManager with the given studentDirectory and userPrefs.
     */
    public ModelManager(ReadOnlyStudentDirectory studentDirectory, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(studentDirectory, userPrefs);

        logger.fine("Initializing with student directory: " + studentDirectory + " and user prefs " + userPrefs);

        this.studentDirectory = new StudentDirectory(studentDirectory);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredStudents = new FilteredList<>(this.studentDirectory.getStudentList());
        sortedStudents = new SortedList<>(filteredStudents);
    }

    public ModelManager() {
        this(new StudentDirectory(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    //=========== StudentDirectory ================================================================================

    @Override
    public void setStudentDirectory(ReadOnlyStudentDirectory studentDirectory) {
        this.studentDirectory.resetData(studentDirectory);
        sortStudents(StudentSortOrder.DIRECTORY);
    }

    @Override
    public ReadOnlyStudentDirectory getStudentDirectory() {
        return studentDirectory;
    }

    @Override
    public boolean hasStudent(Student student) {
        requireNonNull(student);
        return studentDirectory.hasStudent(student);
    }

    @Override
    public void deleteStudent(Student target) {
        studentDirectory.removeStudent(target);
    }

    @Override
    public void addStudent(Student student) {
        studentDirectory.addStudent(student);
        updateFilteredStudentList(PREDICATE_SHOW_ALL_STUDENTS);
    }

    @Override
    public void setStudent(Student target, Student editedStudent) {
        requireAllNonNull(target, editedStudent);

        studentDirectory.setStudent(target, editedStudent);
    }

    //=========== Filtered Student List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the filtered students in their displayed order.
     */
    @Override
    public ObservableList<Student> getFilteredStudentList() {
        return sortedStudents;
    }

    @Override
    public void sortStudents(StudentSortOrder sortOrder) {
        requireNonNull(sortOrder, "Student sort order must not be null.");
        int displayedStudentCount = sortedStudents.size();
        sortedStudents.setComparator(sortOrder.getComparator());
        this.sortOrder = sortOrder;
        assert sortedStudents.size() == displayedStudentCount
                : "Sorting must not change the number of displayed students.";
    }

    @Override
    public void updateFilteredStudentList(Predicate<Student> predicate) {
        requireNonNull(predicate);
        if (sortOrder != StudentSortOrder.DIRECTORY) {
            logger.fine("Resetting student sorting to directory order for a new filter.");
        }
        sortStudents(StudentSortOrder.DIRECTORY);
        filteredStudents.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager otherModelManager)) {
            return false;
        }

        return studentDirectory.equals(otherModelManager.studentDirectory)
                && userPrefs.equals(otherModelManager.userPrefs)
                && filteredStudents.equals(otherModelManager.filteredStudents)
                && sortOrder == otherModelManager.sortOrder;
    }

}
