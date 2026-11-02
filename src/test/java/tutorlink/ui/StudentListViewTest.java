package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.JavaFxTestUtil.runOnFxThread;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tutorlink.model.student.Student;
import tutorlink.testutil.JavaFxTestUtil;
import tutorlink.testutil.StudentBuilder;

/**
 * Exercises pending scroll requests with real JavaFX cells and deterministic layout pulses.
 */
public class StudentListViewTest {
    private StudentListView list;
    private Stage stage;

    @BeforeAll
    public static void initializeJavaFx() throws Exception {
        JavaFxTestUtil.initialize();
    }

    @BeforeEach
    public void setUp() throws Exception {
        runOnFxThread(() -> {
            list = new StudentListView();
            for (int i = 0; i < 60; i++) {
                list.getItems().add(new StudentBuilder().withName("Student " + i)
                        .withMatricNumber(String.format("A%07dZ", i)).build());
            }
            list.setFixedCellSize(40);
            list.setCellFactory(view -> new ListCell<>() {
                @Override
                protected void updateItem(Student student, boolean empty) {
                    super.updateItem(student, empty);
                    setText(empty || student == null ? null : student.getName().fullName);
                }
            });
            stage = new Stage();
            stage.setScene(new Scene(new StackPane(list), 300, 200));
            stage.show();
            layout();
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        runOnFxThread(() -> {
            list.cancelPendingScroll();
            stage.hide();
        });
    }

    @Test
    public void scrollToSelection_noSelection_doesNotScheduleScroll() throws Exception {
        runOnFxThread(() -> {
            list.scrollToSelection();
            assertFalse(hasPendingScroll());
            assertEquals(-1, list.getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void scrollToSelection_withoutShowingWindow_doesNotScheduleScroll() throws Exception {
        runOnFxThread(() -> {
            StudentListView detached = new StudentListView();
            detached.getItems().add(list.getItems().getFirst());
            detached.getSelectionModel().select(0);
            detached.scrollToSelection();
            assertFalse(hasPendingScroll(detached));
            new Scene(new StackPane(detached));
            detached.scrollToSelection();
            assertFalse(hasPendingScroll(detached));

            stage.hide();
            selectAndScroll(59);
            assertFalse(hasPendingScroll());
        });
    }

    @Test
    public void pendingScroll_selectedStudentRemoved_cancelsBeforeQueuedPulse() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            Runnable queuedPulse = pendingPulse();
            list.getItems().remove(59);
            assertFalse(hasPendingScroll());
            queuedPulse.run();
            assertFalse(hasPendingScroll());
            assertEquals(59, list.getItems().size());
        });
    }

    @Test
    public void pendingScroll_selectionChanged_cancelsWithoutRestoringOldSelection() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            Runnable queuedPulse = pendingPulse();
            list.getSelectionModel().clearAndSelect(0);
            assertFalse(hasPendingScroll());
            queuedPulse.run();
            assertFalse(hasPendingScroll());
            assertSame(list.getItems().getFirst(), list.getSelectionModel().getSelectedItem());
        });
    }

    @Test
    public void pendingScroll_itemsReplaced_removesListenerFromOldList() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            var oldItems = list.getItems();
            list.setItems(FXCollections.observableArrayList(oldItems));
            assertFalse(hasPendingScroll());
            selectAndScroll(0);
            assertTrue(hasPendingScroll());
            oldItems.clear();
            assertTrue(hasPendingScroll(), "An obsolete list must not cancel the new request");
            list.getItems().remove(59);
            assertFalse(hasPendingScroll(), "Changes to the replacement list must cancel the request");
        });
    }

    @Test
    public void pendingScroll_nullItems_cancelsAndAcceptsReplacementList() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            Runnable queuedPulse = pendingPulse();
            var students = list.getItems();
            list.setItems(null);
            assertFalse(hasPendingScroll());
            queuedPulse.run();
            list.setItems(students);
            selectAndScroll(0);
            assertTrue(hasPendingScroll());
        });
    }

    @Test
    public void pendingScroll_skinReplaced_cancelsSafely() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            Runnable queuedPulse = pendingPulse();
            list.setSkin(new ListViewSkin<>(list));
            queuedPulse.run();
            assertFalse(hasPendingScroll());
        });
    }

    @Test
    public void pendingScroll_windowHidden_cancelsAndCanRestart() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            stage.hide();
            assertFalse(hasPendingScroll());
            stage.show();
            selectAndScroll(0);
            assertTrue(hasPendingScroll());
        });
    }

    @Test
    public void pendingScroll_sceneLosesWindow_cancelsRequest() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            stage.setScene(new Scene(new StackPane(), 300, 200));
            assertFalse(hasPendingScroll());
        });
    }

    @Test
    public void pendingScroll_listDetached_cancelsRequest() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            StackPane root = (StackPane) stage.getScene().getRoot();
            root.getChildren().clear();
            assertFalse(hasPendingScroll());
        });
    }

    @Test
    public void pendingScroll_visibleRow_completesAfterStableLayouts() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(0);
            Runnable pulse = pendingPulse();
            pulse.run();
            pulse.run();
            assertTrue(hasPendingScroll());
            pulse.run();
            assertFalse(hasPendingScroll());
            assertTrue(flow().getVisibleCell(0).isVisible());
        });
    }

    @Test
    public void pendingScroll_zeroViewport_waitsUntilMeasurableLayout() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(0);
            Runnable pulse = pendingPulse();
            VirtualFlow<?> flow = flow();
            double height = flow.getHeight();
            flow.resize(flow.getWidth(), 0);
            for (int i = 0; i < 5; i++) {
                pulse.run();
            }
            assertTrue(hasPendingScroll(), "A zero-height viewport cannot count as a stable layout");
            flow.resize(flow.getWidth(), height);
            for (int i = 0; i < 10 && hasPendingScroll(); i++) {
                list.requestLayout();
                layout();
                pulse.run();
            }
            assertFalse(hasPendingScroll(), "The request should complete once rows can be measured again");
            var cell = flow.getVisibleCell(0);
            assertTrue(cell.isVisible());
            assertTrue(cell.getBoundsInParent().getMinY() >= -1);
            assertTrue(cell.getBoundsInParent().getMaxY() <= flow.getHeight() + 1);
        });
    }

    @Test
    public void pendingScroll_neverMeasurable_stopsAtRetryLimit() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(0);
            Runnable pulse = pendingPulse();
            flow().resize(flow().getWidth(), 0);
            for (int i = 0; i < 119; i++) {
                pulse.run();
            }
            assertTrue(hasPendingScroll(), "Keep trying while the layout budget remains");
            pulse.run();
            assertFalse(hasPendingScroll(), "An unmeasurable row must not keep scheduling pulses forever");
            assertEquals(0, list.getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void pendingScroll_hiddenCell_stopsAtRetryLimit() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(0);
            Runnable pulse = pendingPulse();
            for (int i = 0; i < 120; i++) {
                flow().getVisibleCell(0).setVisible(false);
                pulse.run();
            }
            assertFalse(hasPendingScroll(), "An unsettled cell must not schedule pulses forever");
        });
    }

    @Test
    public void pendingScroll_selectionModelReplaced_cancelsOldTargetOnNextPulse() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(59);
            Runnable pulse = pendingPulse();
            StudentListView replacement = new StudentListView();
            replacement.setItems(list.getItems());
            replacement.getSelectionModel().select(0);
            list.setSelectionModel(replacement.getSelectionModel());
            pulse.run();
            assertFalse(hasPendingScroll());
            assertSame(list.getItems().getFirst(), list.getSelectionModel().getSelectedItem());
        });
    }

    @Test
    public void pendingScroll_intermittentlyVisible_stillStopsAtRetryLimit() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(0);
            Runnable pulse = pendingPulse();
            VirtualFlow<?> flow = flow();
            double height = flow.getHeight();
            for (int i = 0; i < 119; i++) {
                flow.resize(flow.getWidth(), i % 3 == 2 ? 0 : height);
                pulse.run();
                assertTrue(hasPendingScroll());
            }
            flow.resize(flow.getWidth(), 0);
            pulse.run();
            assertFalse(hasPendingScroll(), "Brief visibility must not reset the total retry budget");
        });
    }

    @Test
    public void pendingScroll_supersededRequest_getsFreshRetryBudget() throws Exception {
        runOnFxThread(() -> {
            selectAndScroll(0);
            Runnable pulse = pendingPulse();
            flow().resize(flow().getWidth(), 0);
            for (int i = 0; i < 119; i++) {
                pulse.run();
            }
            assertTrue(hasPendingScroll());
            selectAndScroll(0);
            flow().resize(flow().getWidth(), 0);
            for (int i = 0; i < 119; i++) {
                pulse.run();
            }
            assertTrue(hasPendingScroll(), "A new view command needs its own retry budget");
            pulse.run();
            assertFalse(hasPendingScroll());
        });
    }

    private void selectAndScroll(int index) {
        list.getSelectionModel().clearAndSelect(index);
        list.scrollToSelection();
    }

    private void layout() {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private VirtualFlow<?> flow() {
        return (VirtualFlow<?>) list.lookup(".virtual-flow");
    }

    private boolean hasPendingScroll() throws Exception {
        return hasPendingScroll(list);
    }

    private boolean hasPendingScroll(StudentListView view) throws Exception {
        return readField(view, "scrollScene") != null;
    }

    private Runnable pendingPulse() throws Exception {
        assertTrue(hasPendingScroll());
        // Capture the registered callback to simulate a queued pulse after cancellation, and to
        // drive retry exhaustion without depending on the display's pulse rate or wall-clock sleeps.
        return (Runnable) readField(list, "scrollAfterLayout");
    }

    private Object readField(StudentListView view, String name) throws Exception {
        Field field = StudentListView.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(view);
    }
}
