package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.MatricNumber;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.TutorialGroup;

public class MessagesTest {

    @Test
    public void format_missingOptionalDetails_showsProfileAndFallbacks() {
        Person student = new Person(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("T04"), null, null, null, Set.of());

        String result = Messages.format(student);
        assertTrue(result.contains("Matric: A0123456X"));
        assertTrue(result.contains("Group: T04"));
        assertTrue(result.contains("Phone: Not provided"));
        assertTrue(result.contains("Email: Not provided"));
        assertTrue(result.contains("Address: Not provided"));
    }
}
