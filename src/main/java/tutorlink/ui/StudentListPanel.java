package tutorlink.ui;

import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import tutorlink.commons.core.LogsCenter;
import tutorlink.model.student.Student;

/**
 * Panel containing the list of students.
 */
public class StudentListPanel extends UiPart<Region> {
    private static final String FXML = "StudentListPanel.fxml";
    private final Logger logger = LogsCenter.getLogger(StudentListPanel.class);

    @FXML
    private ListView<Student> studentListView;

    /**
     * Creates a {@code StudentListPanel} with the given {@code ObservableList}.
     */
    public StudentListPanel(ObservableList<Student> studentList) {
        super(FXML);
        studentListView.setItems(studentList);
        studentListView.setCellFactory(listView -> new StudentListViewCell());
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Student} using a {@code StudentCard}.
     */
    class StudentListViewCell extends ListCell<Student> {
        private StudentCard studentCard;
        private double lastAvailableWidth = -1;

        StudentListViewCell() {
            widthProperty().addListener((observable, oldWidth, newWidth) -> {
                if (Double.compare(oldWidth.doubleValue(), newWidth.doubleValue()) != 0) {
                    updateCardWidth();
                }
            });
        }

        /**
         * Recomputes the card width when the list cell is resized. This is needed because a wrapped remark's
         * preferred height depends on the width available to the card.
         */
        private void updateCardWidth() {
            if (studentCard == null) {
                return;
            }

            Region card = studentCard.getRoot();
            double availableWidth = Math.max(0,
                    getWidth() - snappedLeftInset() - snappedRightInset());

            boolean widthChanged = Double.compare(lastAvailableWidth, availableWidth) != 0;
            if (widthChanged) {
                card.setPrefWidth(availableWidth);
                card.setMaxWidth(availableWidth);
            }
            if (!widthChanged) {
                return;
            }

            lastAvailableWidth = availableWidth;
            setPrefHeight(USE_COMPUTED_SIZE);
            requestLayout();
            if (getListView() != null) {
                getListView().requestLayout();
            }
        }

        @Override
        protected void updateItem(Student student, boolean empty) {
            super.updateItem(student, empty);

            if (empty || student == null) {
                studentCard = null;
                lastAvailableWidth = -1;
                setGraphic(null);
                setText(null);
                setPrefHeight(USE_COMPUTED_SIZE);
            } else {
                studentCard = new StudentCard(student, getIndex() + 1);
                lastAvailableWidth = -1;
                setGraphic(studentCard.getRoot());
                updateCardWidth();
            }
        }

        @Override
        protected double computePrefHeight(double width) {
            if (studentCard == null) {
                return super.computePrefHeight(width);
            }

            double cellWidth = width > 0 ? width : getWidth();
            double availableWidth = Math.max(0, cellWidth - snappedLeftInset() - snappedRightInset());
            return snappedTopInset() + snappedBottomInset()
                    + studentCard.getRoot().prefHeight(availableWidth);
        }

        @Override
        protected void layoutChildren() {
            updateCardWidth();
            super.layoutChildren();
        }
    }

}
