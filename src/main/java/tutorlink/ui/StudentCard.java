package tutorlink.ui;

import java.util.Comparator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import tutorlink.model.student.Student;

/**
 * A UI component that displays information of a {@code Student}.
 */
public class StudentCard extends UiPart<Region> {

    private static final String FXML = "StudentListCard.fxml";
    private static final String NOT_PROVIDED = "Not provided";

    /**
     * Note: Certain keywords such as "location" and "resources" are reserved keywords in JavaFX.
     * As a consequence, UI elements' variable names cannot be set to such keywords
     * or an exception will be thrown by JavaFX during runtime.
     *
     * @see <a href="https://github.com/se-edu/addressbook-level4/issues/336">The issue on AddressBook level 4</a>
     */

    public final Student student;

    @FXML
    private HBox cardPane;
    @FXML
    private Label name;
    @FXML
    private Label id;
    @FXML
    private Label matricNumber;
    @FXML
    private Label tutorialGroup;
    @FXML
    private Label followUp;
    @FXML
    private Label remark;
    @FXML
    private Label phone;
    @FXML
    private Label address;
    @FXML
    private Label email;
    @FXML
    private FlowPane tags;

    /**
     * Creates a {@code StudentCard} with the given {@code Student} and index to display.
     */
    public StudentCard(Student student, int displayedIndex) {
        super(FXML);
        this.student = student;
        id.setText(displayedIndex + ". ");
        name.setText(student.getName().fullName);
        matricNumber.setText("Matric: " + student.getMatricNumber().value);
        tutorialGroup.setText("Group: " + student.getTutorialGroup().value);
        followUp.setText("Follow-up: " + (student.requiresFollowUp() ? "Required" : "Not required"));
        String remarkText = student.getRemark() == null ? NOT_PROVIDED : student.getRemark();
        remark.setText("Remark: " + remarkText);
        remark.setWrapText(true);
        remark.setMaxWidth(Double.MAX_VALUE);
        if (student.getRemark() != null) {
            remark.setTooltip(new Tooltip(student.getRemark()));
        }
        phone.setText("Phone: " + (student.getPhone() == null ? NOT_PROVIDED : student.getPhone().value));
        address.setText("Address: " + (student.getAddress() == null ? NOT_PROVIDED : student.getAddress().value));
        email.setText("Email: " + (student.getEmail() == null ? NOT_PROVIDED : student.getEmail().value));
        student.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(new Label(tag.tagName)));
    }
}
