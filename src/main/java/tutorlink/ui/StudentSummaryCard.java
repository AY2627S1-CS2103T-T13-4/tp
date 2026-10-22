package tutorlink.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import tutorlink.model.student.Student;

/**
 * Shows a student's identity and follow-up indicator in the middle list.
 */
public class StudentSummaryCard extends UiPart<Region> {
    private static final String FXML = "StudentSummaryCard.fxml";

    @FXML
    private Label id;
    @FXML
    private Label name;
    @FXML
    private Label matricNumber;
    @FXML
    private Label tutorialGroup;
    @FXML
    private Label followUp;

    /**
     * Creates a summary with the displayed command index.
     */
    public StudentSummaryCard(Student student, int displayedIndex) {
        super(FXML);
        setDisplayedIndex(displayedIndex);
        name.setText(student.getName().fullName);
        name.setTooltip(new Tooltip(student.getName().fullName));
        matricNumber.setText(student.getMatricNumber().value);
        tutorialGroup.setText(student.getTutorialGroup().value);
        followUp.setVisible(student.requiresFollowUp());
        followUp.setManaged(student.requiresFollowUp());
    }

    void setDisplayedIndex(int displayedIndex) {
        id.setText(displayedIndex + ".");
    }
}
