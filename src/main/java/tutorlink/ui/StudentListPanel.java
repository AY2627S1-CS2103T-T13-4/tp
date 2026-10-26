package tutorlink.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import tutorlink.model.student.Student;

/**
 * Displays student summaries in the same order used by indexed commands.
 */
public class StudentListPanel extends UiPart<Region> {
    private static final String FXML = "StudentListPanel.fxml";
    private int selectionRevision;

    @FXML
    private ListView<Student> studentListView;

    /**
     * Creates a panel backed by the command layer's displayed student list.
     */
    public StudentListPanel(ObservableList<Student> students) {
        super(FXML);
        studentListView.setItems(students);
        studentListView.setCellFactory(list -> new StudentListViewCell());
    }

    public ReadOnlyObjectProperty<Student> selectedStudentProperty() {
        return studentListView.getSelectionModel().selectedItemProperty();
    }

    public Student getSelectedStudent() {
        return studentListView.getSelectionModel().getSelectedItem();
    }

    /**
     * Selects a student requested by a view command and scrolls their summary into view.
     */
    public void showStudent(Student student) {
        studentListView.getSelectionModel().clearSelection();
        restoreSelection(student);
        studentListView.scrollTo(studentListView.getSelectionModel().getSelectedIndex());
        Scene scene = studentListView.getScene();
        if (scene == null) {
            return;
        }
        int revision = selectionRevision;
        // Wrapped row heights can change during layout after filtering. Apply the scroll
        // once more with those measured heights, unless a newer command/selection supersedes it.
        scene.addPostLayoutPulseListener(new Runnable() {
            @Override
            public void run() {
                scene.removePostLayoutPulseListener(this);
                Student selected = getSelectedStudent();
                if (revision == selectionRevision && selected != null && student.isSameStudent(selected)) {
                    studentListView.scrollTo(studentListView.getSelectionModel().getSelectedIndex());
                    Platform.requestNextPulse();
                }
            }
        });
        Platform.requestNextPulse();
    }

    /**
     * Keeps the previously selected student after a command, or clears details if they left the results.
     */
    public void restoreSelection(Student previousSelection) {
        selectionRevision++;
        if (previousSelection != null) {
            for (int i = 0; i < studentListView.getItems().size(); i++) {
                if (previousSelection.isSameStudent(studentListView.getItems().get(i))) {
                    studentListView.getSelectionModel().clearAndSelect(i);
                    return;
                }
            }
        }
        studentListView.getSelectionModel().clearSelection();
    }

    /**
     * Displays a compact summary and keeps its number synchronized when the list is reordered.
     */
    class StudentListViewCell extends ListCell<Student> {
        private StudentSummaryCard summary;

        StudentListViewCell() {
            indexProperty().addListener((observable, oldIndex, newIndex) -> {
                if (summary != null) {
                    summary.setDisplayedIndex(newIndex.intValue() + 1);
                }
            });
        }

        @Override
        protected void updateItem(Student student, boolean empty) {
            super.updateItem(student, empty);
            setText(null);
            if (empty || student == null) {
                summary = null;
                setGraphic(null);
            } else {
                summary = new StudentSummaryCard(student, getIndex() + 1);
                summary.getRoot().prefWidthProperty().bind(widthProperty().subtract(20));
                setGraphic(summary.getRoot());
            }
        }
    }
}
