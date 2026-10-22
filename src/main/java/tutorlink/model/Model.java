package tutorlink.model;

import java.util.function.Predicate;

import javafx.collections.ObservableList;
import tutorlink.commons.core.GuiSettings;
import tutorlink.model.student.Student;

/**
 * The API of the Model component.
 */
public interface Model {
    /** {@code Predicate} that always evaluates to true */
    Predicate<Student> PREDICATE_SHOW_ALL_STUDENTS = unused -> true;

    /**
     * Returns the user prefs.
     */
    ReadOnlyUserPrefs getUserPrefs();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    /**
     * Replaces student directory data with the data in {@code studentDirectory}.
     */
    void setStudentDirectory(ReadOnlyStudentDirectory studentDirectory);

    /** Returns the StudentDirectory */
    ReadOnlyStudentDirectory getStudentDirectory();

    /**
     * Returns true if a student with the same identity as {@code student} exists in the student directory.
     */
    boolean hasStudent(Student student);

    /**
     * Deletes the given student.
     * The student must exist in the student directory.
     */
    void deleteStudent(Student target);

    /**
     * Adds the given student.
     * {@code student} must not already exist in the student directory.
     */
    void addStudent(Student student);

    /**
     * Replaces the given student {@code target} with {@code editedStudent}.
     * {@code target} must exist in the student directory.
     * The student identity of {@code editedStudent} must not be the same as another existing student in the
     * student directory.
     */
    void setStudent(Student target, Student editedStudent);

    /** Returns an unmodifiable view of the filtered student list in displayed order. */
    ObservableList<Student> getFilteredStudentList();

    /**
     * Sorts the currently filtered students using {@code sortOrder}, without changing the filter or directory order.
     * The ordering remains active as students are edited or deleted.
     * {@link StudentSortOrder#DIRECTORY} restores the current underlying directory order for the current results.
     * @throws NullPointerException if {@code sortOrder} is null.
     */
    void sortStudents(StudentSortOrder sortOrder);

    /**
     * Updates the filter using {@code predicate} and restores the original directory order.
     * @throws NullPointerException if {@code predicate} is null.
     */
    void updateFilteredStudentList(Predicate<Student> predicate);
}
