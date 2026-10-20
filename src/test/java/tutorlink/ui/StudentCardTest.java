package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
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
    public void constructor_displaysFollowUpStatus() throws Exception {
        Student student = new StudentBuilder().withFollowUp(true).build();

        onJavaFxThread(() -> {
            StudentCard card = new StudentCard(student, 1);
            Label followUp = (Label) card.getRoot().lookup("#followUp");
            assertEquals("Follow-up: Required", followUp.getText());
        });
    }

    @Test
    public void constructor_displaysNotRequiredStatus() throws Exception {
        Student student = new StudentBuilder().withFollowUp(false).build();

        onJavaFxThread(() -> {
            StudentCard card = new StudentCard(student, 1);
            Label followUp = (Label) card.getRoot().lookup("#followUp");
            assertEquals("Follow-up: Not required", followUp.getText());
        });
    }

    private void onJavaFxThread(Runnable assertions) throws Exception {
        FutureTask<Void> task = new FutureTask<>(assertions, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
