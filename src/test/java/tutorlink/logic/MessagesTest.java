package tutorlink.logic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.model.tag.Tag;

public class MessagesTest {

    @Test
    public void format_missingOptionalDetails_showsProfileAndFallbacks() {
        Student student = new Student(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("T04"), null, null, null, Set.of());

        String result = Messages.format(student);
        assertTrue(result.contains("Matric: A0123456X"));
        assertTrue(result.contains("Group: T04"));
        assertTrue(result.contains("Phone: Not provided"));
        assertTrue(result.contains("Email: Not provided"));
        assertTrue(result.contains("Address: Not provided"));
        assertTrue(result.contains("Tags: Not provided"));
    }

    @Test
    public void format_multipleTags_separatesAndSortsTags() {
        Student student = new Student(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("T04"), null, null, null, Set.of(new Tag("urgent"), new Tag("needsHelp")));

        String result = Messages.format(student);

        assertTrue(result.contains("Tags: needsHelp, urgent"));
    }
}
