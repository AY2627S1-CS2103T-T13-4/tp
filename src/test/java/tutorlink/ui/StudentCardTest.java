package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import tutorlink.model.student.Student;
import tutorlink.testutil.StudentBuilder;

public class StudentCardTest {

    @BeforeAll
    public static void setUpJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException e) {
            // The toolkit may already be initialized by another UI test.
        }
    }

    @Test
    public void constructor_withRemark_displaysRemark() {
        Student student = new StudentBuilder().withRemark("Needs help with recursion").build();
        StudentCard studentCard = new StudentCard(student, 1);

        assertTrue(containsLabelWithText(studentCard.getRoot(), "Remark: Needs help with recursion"));
    }

    @Test
    public void constructor_withoutRemark_displaysNotProvided() {
        Student student = new StudentBuilder().build();
        StudentCard studentCard = new StudentCard(student, 1);

        assertTrue(containsLabelWithText(studentCard.getRoot(), "Remark: Not provided"));
    }

    @Test
    public void constructor_withLongRemark_wrapsAndProvidesTooltip() {
        String longRemark = "Discuss recursion and file access modes with the student";
        Student student = new StudentBuilder().withRemark(longRemark).build();
        StudentCard studentCard = new StudentCard(student, 1);

        Label remark = (Label) studentCard.getRoot().lookup("#remark");
        assertTrue(remark.isWrapText());
        assertEquals(longRemark, remark.getTooltip().getText());
    }

    private boolean containsLabelWithText(Node node, String expectedText) {
        if (node instanceof Label label && expectedText.equals(label.getText())) {
            return true;
        }
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable().stream()
                    .anyMatch(child -> containsLabelWithText(child, expectedText));
        }
        return false;
    }
}
