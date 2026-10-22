package tutorlink.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import tutorlink.model.student.Student;

/**
 * Shows the selected student's full profile or an empty-selection prompt.
 */
public class StudentDetailsPanel extends UiPart<Region> {
    private static final String FXML = "StudentDetailsPanel.fxml";
    private Student displayedStudent;

    @FXML
    private Label emptySelection;
    @FXML
    private ScrollPane detailsScroll;

    /**
     * Creates an initially empty details panel.
     */
    public StudentDetailsPanel() {
        super(FXML);
    }

    /**
     * Displays a student, or clears the panel when no student is selected.
     */
    public void showStudent(Student student, int displayedIndex) {
        boolean hasSelection = student != null;
        boolean isNewSelection = !hasSelection || displayedStudent == null || !displayedStudent.isSameStudent(student);
        emptySelection.setVisible(!hasSelection);
        emptySelection.setManaged(!hasSelection);
        detailsScroll.setVisible(hasSelection);
        detailsScroll.setManaged(hasSelection);
        detailsScroll.setContent(hasSelection ? new StudentCard(student, displayedIndex).getRoot() : null);
        if (isNewSelection) {
            detailsScroll.setVvalue(0);
            detailsScroll.setHvalue(0);
        }
        displayedStudent = student;
    }
}
