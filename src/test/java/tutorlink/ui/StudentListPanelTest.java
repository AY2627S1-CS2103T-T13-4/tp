package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import tutorlink.model.student.Student;
import tutorlink.testutil.StudentBuilder;

public class StudentListPanelTest {

    @BeforeAll
    public static void setUpJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException e) {
            // The toolkit may already be initialized by another UI test.
        }
    }

    @Test
    public void studentListViewCell_recalculatesHeightWhenWidthChanges() throws Exception {
        Student amy = new StudentBuilder()
                .withRemark("Needs practice with recursion. ".repeat(20)
                        + "Remember the deadline on Friday.")
                .build();
        Student bob = new StudentBuilder()
                .withName("Bob Tan")
                .withMatricNumber("A0000002B")
                .build();

        onJavaFxThread(() -> {
            StudentListPanel panel = new StudentListPanel(FXCollections.observableArrayList(amy, bob));
            Scene scene = new Scene(panel.getRoot(), 450, 600);
            layoutScene(scene, 450);

            ListView<Student> listView = getStudentListView(panel);
            ListCell<Student> firstCell = getCell(listView, 0);
            assertNotNull(firstCell);
            double narrowComputedHeight = firstCell.prefHeight(450);
            double narrowCellHeight = firstCell.getHeight();

            layoutScene(scene, 800);
            firstCell = getCell(listView, 0);
            double wideComputedHeight = firstCell.prefHeight(800);
            double wideCellHeight = firstCell.getHeight();

            assertTrue(narrowCellHeight > wideCellHeight || narrowComputedHeight > wideComputedHeight,
                    String.format("The cell should shrink when the wrapped remark gets more width "
                            + "(narrowCellHeight=%.2f, wideCellHeight=%.2f, "
                            + "narrowComputedHeight=%.2f, wideComputedHeight=%.2f)",
                            narrowCellHeight, wideCellHeight, narrowComputedHeight, wideComputedHeight));
            assertTrue(wideCellHeight >= 145 || wideComputedHeight >= 145);
            assertTrue(getCell(listView, 1).getLayoutY() < narrowCellHeight + 145,
                    "The second student should follow the resized first cell");
        });
    }

    @Test
    public void studentListViewCell_reusedItemRecalculatesHeight() throws Exception {
        Student longRemarkStudent = new StudentBuilder()
                .withRemark("Needs practice with recursion. ".repeat(20))
                .build();
        Student shortRemarkStudent = new StudentBuilder().withRemark("Short remark").build();

        onJavaFxThread(() -> {
            var students = FXCollections.observableArrayList(longRemarkStudent);
            StudentListPanel panel = new StudentListPanel(students);
            Scene scene = new Scene(panel.getRoot(), 450, 600);

            layoutScene(scene, 450);

            ListView<Student> listView = getStudentListView(panel);
            double longHeight = getCell(listView, 0).getHeight();

            students.set(0, shortRemarkStudent);
            layoutScene(scene, 450);
            double shortHeight = getCell(listView, 0).getHeight();

            students.set(0, longRemarkStudent);
            layoutScene(scene, 450);
            double longHeightAgain = getCell(listView, 0).getHeight();

            assertTrue(shortHeight < longHeight);
            assertTrue(longHeightAgain > shortHeight);
        });
    }

    @Test
    public void studentListViewCell_emptyItem_clearsGraphic() {
        StudentListPanel panel = new StudentListPanel(FXCollections.observableArrayList());
        StudentListPanel.StudentListViewCell cell = panel.new StudentListViewCell();

        cell.updateItem(new StudentBuilder().build(), false);
        cell.updateItem(null, false);
        cell.updateItem(null, true);
        cell.prefHeight(450);
        cell.layout();

        assertNull(cell.getGraphic());
        assertNull(cell.getText());
        assertEquals(javafx.scene.layout.Region.USE_COMPUTED_SIZE, cell.getPrefHeight());
    }

    @SuppressWarnings("unchecked")
    private ListView<Student> getStudentListView(StudentListPanel panel) {
        return (ListView<Student>) panel.getRoot().lookup("#studentListView");
    }

    @SuppressWarnings("unchecked")
    private ListCell<Student> getCell(ListView<Student> listView, int index) {
        return listView.lookupAll(".list-cell").stream()
                .filter(ListCell.class::isInstance)
                .map(node -> (ListCell<Student>) node)
                .filter(cell -> cell.getIndex() == index)
                .findFirst()
                .orElseThrow();
    }

    private void layoutScene(Scene scene, double width) {
        scene.getRoot().resize(width, 600);
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        ListView<?> listView = (ListView<?>) scene.getRoot().lookup("#studentListView");
        listView.setMinWidth(width);
        listView.setPrefWidth(width);
        listView.setMaxWidth(width);
        listView.resize(width, 600);
        listView.requestLayout();
        listView.layout();
        double cellWidth = Math.max(0, width - 2);
        listView.lookupAll(".list-cell").stream()
                .filter(ListCell.class::isInstance)
                .map(ListCell.class::cast)
                .forEach(cell -> {
                    cell.resize(cellWidth, cell.prefHeight(cellWidth));
                    cell.layout();
                });
        listView.requestLayout();
        listView.layout();
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
}
