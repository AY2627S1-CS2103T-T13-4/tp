package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.JavaFxTestUtil.runOnFxThread;
import static tutorlink.testutil.TypicalStudents.ALICE;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import tutorlink.model.student.Student;
import tutorlink.testutil.JavaFxTestUtil;
import tutorlink.testutil.StudentBuilder;

/**
 * Loads the real student-card FXML and checks its displayed follow-up status on the JavaFX thread.
 */
public class StudentCardTest {
    @BeforeAll
    public static void initializeJavaFx() throws Exception {
        JavaFxTestUtil.initialize();
    }

    @Test
    public void initializeJavaFx_toolkitAlreadyStarted_reusesToolkit() throws Exception {
        initializeJavaFx();
        runOnFxThread(() -> assertEquals("Ready", new Label("Ready").getText()));
    }

    @Test
    public void constructor_withRemark_displaysRemark() throws Exception {
        Student student = new StudentBuilder().withRemark("Needs help with recursion").build();

        runOnFxThread(() -> {
            StudentCard studentCard = new StudentCard(student, 1);
            assertTrue(containsLabelWithText(studentCard.getRoot(), "Remark: Needs help with recursion"));
        });
    }

    @Test
    public void constructor_withoutRemark_displaysNotProvided() throws Exception {
        Student student = new StudentBuilder().build();

        runOnFxThread(() -> {
            StudentCard studentCard = new StudentCard(student, 1);
            assertTrue(containsLabelWithText(studentCard.getRoot(), "Remark: Not provided"));
        });
    }

    @Test
    public void constructor_withLongRemark_wrapsAndProvidesTooltip() throws Exception {
        String longRemark = "Discuss recursion and file access modes with the student";
        Student student = new StudentBuilder().withRemark(longRemark).build();

        runOnFxThread(() -> {
            StudentCard studentCard = new StudentCard(student, 1);
            Label remark = (Label) studentCard.getRoot().lookup("#remark");
            assertNotNull(remark);
            assertTrue(remark.isWrapText());
            assertNotNull(remark.getTooltip());
            assertEquals(longRemark, remark.getTooltip().getText());
        });
    }

    @Test
    public void runOnFxThread_assertionFails_reportsOriginalFailure() {
        AssertionError failure = new AssertionError("Original JavaFX assertion failure");
        assertSame(failure, assertThrows(AssertionError.class, () -> runOnFxThread(() -> {
            throw failure;
        })));
    }

    @Test
    public void constructor_markedStudent_showsRequiredStatusAndRetainsProfile() throws Exception {
        Student student = new StudentBuilder(ALICE).withFollowUp(true).withTags("zebra", "alpha").build();
        runOnFxThread(() -> {
            StudentCard card = new StudentCard(student, 2);
            assertSame(student, card.student);
            assertLabel(card, "id", "2. ");
            assertLabel(card, "name", student.getName().fullName);
            assertLabel(card, "matricNumber", "Matric: " + student.getMatricNumber().value);
            assertLabel(card, "tutorialGroup", "Group: " + student.getTutorialGroup().value);
            assertLabel(card, "followUp", "Follow-up: Required");
            assertLabel(card, "phone", "Phone: " + student.getPhone().value);
            assertLabel(card, "email", "Email: " + student.getEmail().value);
            assertLabel(card, "address", "Address: " + student.getAddress().value);
            FlowPane tags = (FlowPane) card.getRoot().lookup("#tags");
            assertEquals(List.of("alpha", "zebra"), tags.getChildren().stream()
                    .map(node -> ((Label) node).getText()).toList());
        });
    }

    @Test
    public void constructor_completedStudent_showsNotRequiredWithMissingContactDetails() throws Exception {
        Student marked = new Student(ALICE.getName(), ALICE.getMatricNumber(), ALICE.getTutorialGroup(),
                null, null, null, Set.of(), "Retain context", true);
        Student completed = marked.withFollowUp(false);
        runOnFxThread(() -> {
            StudentCard card = new StudentCard(completed, 1);
            assertSame(completed, card.student);
            assertLabel(card, "followUp", "Follow-up: Not required");
            assertLabel(card, "phone", "Phone: Not provided");
            assertLabel(card, "email", "Email: Not provided");
            assertLabel(card, "address", "Address: Not provided");
            assertEquals(0, ((FlowPane) card.getRoot().lookup("#tags")).getChildren().size());
            assertEquals("Retain context", card.student.getRemark());
        });
    }

    private void assertLabel(StudentCard card, String id, String text) {
        Label label = (Label) card.getRoot().lookup("#" + id);
        assertNotNull(label);
        assertEquals(text, label.getText());
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
