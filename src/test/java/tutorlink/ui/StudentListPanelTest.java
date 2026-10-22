package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.JavaFxTestUtil.runOnFxThread;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import tutorlink.model.student.Student;
import tutorlink.testutil.JavaFxTestUtil;
import tutorlink.testutil.StudentBuilder;

public class StudentListPanelTest {
    @BeforeAll
    public static void initializeJavaFx() throws Exception {
        JavaFxTestUtil.initialize();
    }

    @Test
    public void list_showsSummaryWithoutFullRemark() throws Exception {
        runOnFxThread(() -> {
            Student student = new StudentBuilder().withRemark("A long private observation".repeat(20))
                    .withFollowUp(true).build();
            StudentListPanel panel = new StudentListPanel(FXCollections.observableArrayList(student));
            layout(panel);
            ListCell<?> cell = firstCell(panel);
            assertEquals("1.", ((Label) cell.getGraphic().lookup("#id")).getText());
            assertEquals(student.getName().fullName, ((Label) cell.getGraphic().lookup("#name")).getText());
            assertTrue(cell.getGraphic().lookup("#followUp").isVisible());
            assertNull(cell.getGraphic().lookup("#remark"));
            assertNull(cell.getGraphic().lookup("#phone"));
        });
    }

    @Test
    public void cell_reusedAtDifferentIndex_updatesNumberAndClearsGraphic() throws Exception {
        runOnFxThread(() -> {
            StudentListPanel panel = new StudentListPanel(FXCollections.observableArrayList());
            StudentListPanel.StudentListViewCell cell = panel.new StudentListViewCell();
            cell.updateIndex(0);
            cell.updateItem(new StudentBuilder().build(), false);
            cell.updateIndex(4);
            cell.updateItem(new StudentBuilder().build(), false);
            assertEquals("5.", ((Label) cell.getGraphic().lookup("#id")).getText());
            assertFalse(cell.getGraphic().lookup("#followUp").isVisible());
            cell.updateItem(null, true);
            assertNull(cell.getGraphic());
            assertNull(cell.getText());
        });
    }

    @Test
    public void restoreSelection_replacedOrRemovedStudent_usesCurrentRecordOrClears() throws Exception {
        runOnFxThread(() -> {
            Student original = new StudentBuilder().build();
            var students = FXCollections.observableArrayList(original);
            StudentListPanel panel = new StudentListPanel(students);
            Student replacement = original.withRemark("Updated");
            students.set(0, replacement);
            panel.restoreSelection(original);
            assertSame(replacement, panel.getSelectedStudent());
            students.clear();
            panel.restoreSelection(replacement);
            assertNull(panel.getSelectedStudent());
        });
    }

    private void layout(StudentListPanel panel) {
        Scene scene = new Scene(panel.getRoot(), 330, 500);
        scene.getRoot().resize(330, 500);
        scene.getRoot().applyCss();
        scene.getRoot().layout();
    }

    private ListCell<?> firstCell(StudentListPanel panel) {
        ListView<?> list = (ListView<?>) panel.getRoot().lookup("#studentListView");
        return list.lookupAll(".list-cell").stream()
                .filter(ListCell.class::isInstance).map(ListCell.class::cast)
                .filter(cell -> cell.getIndex() == 0).findFirst().orElseThrow();
    }
}
