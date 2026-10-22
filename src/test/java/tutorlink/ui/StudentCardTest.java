package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import tutorlink.model.student.Student;
import tutorlink.testutil.StudentBuilder;

/**
 * Tests the labels rendered by a student card.
 */
public class StudentCardTest {

    @BeforeAll
    public static void initializeJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException e) {
            // Another UI test may have initialized the toolkit already.
        }
    }

    @Test
    public void initializeJavaFxToolkit_whenCalledTwice_doesNotThrow() {
        initializeJavaFxToolkit();
    }

    @Test
    public void constructor_withRemark_displaysRemark() throws Exception {
        Student student = new StudentBuilder().withRemark("Needs help with recursion").build();

        onJavaFxThread(() -> {
            StudentCard studentCard = new StudentCard(student, 1);
            assertTrue(containsLabelWithText(studentCard.getRoot(), "Remark: Needs help with recursion"));
        });
    }

    @Test
    public void constructor_withoutRemark_displaysNotProvided() throws Exception {
        Student student = new StudentBuilder().build();

        onJavaFxThread(() -> {
            StudentCard studentCard = new StudentCard(student, 1);
            assertTrue(containsLabelWithText(studentCard.getRoot(), "Remark: Not provided"));
        });
    }

    @Test
    public void constructor_withLongRemark_wrapsAndProvidesTooltip() throws Exception {
        String longRemark = "Discuss recursion and file access modes with the student";
        Student student = new StudentBuilder().withRemark(longRemark).build();

        onJavaFxThread(() -> {
            StudentCard studentCard = new StudentCard(student, 1);
            Label remark = (Label) studentCard.getRoot().lookup("#remark");
            assertNotNull(remark);
            assertTrue(remark.isWrapText());
            assertNotNull(remark.getTooltip());
            assertEquals(longRemark, remark.getTooltip().getText());
        });
    }

    @Test
    public void constructor_displaysFollowUpStatus() throws Exception {
        Student student = new StudentBuilder().withFollowUp(true).build();

        onJavaFxThread(() -> {
            StudentCard card = new StudentCard(student, 1);
            Label followUp = (Label) card.getRoot().lookup("#followUp");
            assertNotNull(followUp);
            assertEquals("Follow-up: Required", followUp.getText());
        });
    }

    @Test
    public void constructor_displaysNotRequiredStatus() throws Exception {
        Student student = new StudentBuilder().withFollowUp(false).build();

        onJavaFxThread(() -> {
            StudentCard card = new StudentCard(student, 1);
            Label followUp = (Label) card.getRoot().lookup("#followUp");
            assertNotNull(followUp);
            assertEquals("Follow-up: Not required", followUp.getText());
        });
    }

    private void onJavaFxThread(Runnable assertions) throws Exception {
        FutureTask<Void> task = new FutureTask<>(assertions, null);
        Platform.runLater(task);
        try {
            task.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new AssertionError(cause);
        }
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
