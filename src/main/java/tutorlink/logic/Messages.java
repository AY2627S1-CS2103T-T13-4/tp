package tutorlink.logic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tutorlink.logic.parser.Prefix;
import tutorlink.model.student.Student;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String NOT_PROVIDED = "Not provided";
    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command.";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX = "The student index provided is invalid.";
    public static final String MESSAGE_STUDENTS_LISTED_OVERVIEW = "%1$d student(s) listed!";
    public static final String MESSAGE_GROUP_COUNT = "%1$s: %2$d %3$s";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields =
                Stream.of(duplicatePrefixes).map(Prefix::toString).collect(Collectors.toSet());

        return MESSAGE_DUPLICATE_FIELDS + String.join(" ", duplicateFields);
    }

    /**
     * Formats the {@code student} for display to the user.
     */
    public static String format(Student student) {
        final StringBuilder builder = new StringBuilder();
        builder.append(student.getName())
                .append("; Matric: ")
                .append(student.getMatricNumber())
                .append("; Group: ")
                .append(student.getTutorialGroup())
                .append("; Phone: ")
                .append(student.getPhone() == null ? NOT_PROVIDED : student.getPhone())
                .append("; Email: ")
                .append(student.getEmail() == null ? NOT_PROVIDED : student.getEmail())
                .append("; Address: ")
                .append(student.getAddress() == null ? NOT_PROVIDED : student.getAddress())
                .append("; Remark: ")
                .append(student.getRemark() == null ? NOT_PROVIDED : student.getRemark())
                .append("; Tags: ")
                .append(student.getTags().isEmpty()
                        ? NOT_PROVIDED
                        : student.getTags().stream()
                                .map(tag -> tag.tagName)
                                .sorted()
                                .collect(Collectors.joining(", ")));
        return builder.toString();
    }

}
