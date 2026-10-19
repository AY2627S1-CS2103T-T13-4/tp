package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static tutorlink.testutil.TypicalStudents.ALICE;

import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import tutorlink.model.student.Student;
import tutorlink.testutil.StudentBuilder;

/**
 * Loads the real student-card FXML and checks its displayed follow-up status on the JavaFX thread.
 */
public class StudentCardTest {
    @BeforeAll
    public static void initializeJavaFx() throws Exception {
        FutureTask<Void> initialized = new FutureTask<>(() -> null);
        Platform.startup(initialized);
        initialized.get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    public static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    public void constructor_markedStudent_showsRequiredStatusAndRetainsProfile() throws Exception {
        Student student = new StudentBuilder(ALICE).withFollowUp(true).withTags("zebra", "alpha").build();
        onJavaFxThread(() -> {
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
        onJavaFxThread(() -> {
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
        assertEquals(text, ((Label) card.getRoot().lookup("#" + id)).getText());
    }

    private void onJavaFxThread(Runnable assertions) throws Exception {
        FutureTask<Void> task = new FutureTask<>(assertions, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
