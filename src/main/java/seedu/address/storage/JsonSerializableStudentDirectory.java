package seedu.address.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.ReadOnlyStudentDirectory;
import seedu.address.model.StudentDirectory;
import seedu.address.model.student.Student;

/**
 * An Immutable StudentDirectory that is serializable to JSON format.
 */
@JsonRootName(value = "studentDirectory")
class JsonSerializableStudentDirectory {

    public static final String MESSAGE_DUPLICATE_STUDENTS = "Student list contains duplicate students.";
    public static final String MESSAGE_MISSING_STUDENTS = "Student list is missing.";

    private final List<JsonAdaptedStudent> students = new ArrayList<>();
    private final boolean hasStudentList;

    /**
     * Constructs a {@code JsonSerializableStudentDirectory} with the given students.
     */
    @JsonCreator
    public JsonSerializableStudentDirectory(@JsonProperty("students") List<JsonAdaptedStudent> students,
            @JsonProperty("persons") List<JsonAdaptedStudent> legacyStudents) {
        List<JsonAdaptedStudent> storedStudents = students == null ? legacyStudents : students;
        hasStudentList = storedStudents != null;
        if (storedStudents != null) {
            this.students.addAll(storedStudents);
        }
    }

    /**
     * Converts a given {@code ReadOnlyStudentDirectory} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableStudentDirectory}.
     */
    public JsonSerializableStudentDirectory(ReadOnlyStudentDirectory source) {
        hasStudentList = true;
        students.addAll(source.getStudentList().stream().map(JsonAdaptedStudent::new).collect(Collectors.toList()));
    }

    /**
     * Converts this student directory into the model's {@code StudentDirectory} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public StudentDirectory toModelType() throws IllegalValueException {
        if (!hasStudentList) {
            throw new IllegalValueException(MESSAGE_MISSING_STUDENTS);
        }
        StudentDirectory studentDirectory = new StudentDirectory();
        for (JsonAdaptedStudent jsonAdaptedStudent : students) {
            Student student = jsonAdaptedStudent.toModelType();
            if (studentDirectory.hasStudent(student)) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_STUDENTS);
            }
            studentDirectory.addStudent(student);
        }
        return studentDirectory;
    }

}
