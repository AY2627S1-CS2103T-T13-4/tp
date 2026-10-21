package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.layout.Region;
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
    public void studentListViewCell_recalculatesHeightWhenWidthChanges() {
        StudentListPanel panel = new StudentListPanel(FXCollections.observableArrayList());
        StudentListPanel.StudentListViewCell cell = panel.new StudentListViewCell();
        Student student = new StudentBuilder()
                .withRemark("Needs practice with recursion. ".repeat(20))
                .build();

        cell.updateItem(student, false);
        assertNotNull(cell.getGraphic());
        Region graphic = (Region) cell.getGraphic();
        cell.resize(450, 1000);
        cell.layout();
        double narrowHeight = cell.prefHeight(450);
        assertEquals(450, graphic.getPrefWidth());

        cell.resize(800, 1000);
        cell.layout();
        double wideHeight = cell.prefHeight(800);

        assertEquals(800, graphic.getPrefWidth());
        assertTrue(narrowHeight >= 145);
        assertTrue(wideHeight >= 145);
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
}
