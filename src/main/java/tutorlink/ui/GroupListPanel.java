package tutorlink.ui;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import tutorlink.model.student.Student;

/**
 * Lists groups from the complete roster and requests the same filtering used by CLI commands.
 */
public class GroupListPanel extends UiPart<Region> {
    private static final String FXML = "GroupListPanel.fxml";
    private static final String ALL_STUDENTS = "";

    private final ObservableList<Student> students;
    private final Consumer<String> selectionHandler;
    private boolean isUpdating;

    @FXML
    private ListView<GroupEntry> groupListView;

    /**
     * Creates a group list. The selection handler receives an empty string for all students.
     */
    public GroupListPanel(ObservableList<Student> students, Consumer<String> selectionHandler) {
        super(FXML);
        this.students = students;
        this.selectionHandler = selectionHandler;
        groupListView.setCellFactory(list -> new GroupListViewCell());
        groupListView.getSelectionModel().selectedItemProperty().addListener((observable, oldGroup, newGroup) -> {
            if (!isUpdating && newGroup != null) {
                selectionHandler.accept(newGroup.group());
            }
        });
        groupListView.setOnKeyPressed(event -> {
            GroupEntry selected = groupListView.getSelectionModel().getSelectedItem();
            if (event.getCode() == KeyCode.ENTER && selected != null) {
                selectionHandler.accept(selected.group());
                event.consume();
            }
        });
        students.addListener((ListChangeListener<Student>) change -> refreshGroups());
        refreshGroups();
        showGroup(ALL_STUDENTS);
    }

    private void refreshGroups() {
        String selectedGroup = getSelectedGroup();
        Map<String, Integer> counts = new TreeMap<>();
        students.forEach(student -> counts.merge(student.getTutorialGroup().value, 1, Integer::sum));
        isUpdating = true;
        groupListView.getItems().setAll(new GroupEntry(ALL_STUDENTS, students.size()));
        counts.forEach((group, count) -> groupListView.getItems().add(new GroupEntry(group, count)));
        isUpdating = false;
        showGroup(selectedGroup);
    }

    /**
     * Updates the highlight without executing a command; null clears the highlight for a search view.
     */
    public void showGroup(String group) {
        isUpdating = true;
        groupListView.getSelectionModel().clearSelection();
        for (int i = 0; i < groupListView.getItems().size(); i++) {
            if (groupListView.getItems().get(i).group().equals(group)) {
                groupListView.getSelectionModel().select(i);
                break;
            }
        }
        isUpdating = false;
    }

    String getSelectedGroup() {
        GroupEntry selected = groupListView.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.group();
    }

    private class GroupListViewCell extends ListCell<GroupEntry> {
        private boolean wasSelectedOnPress;

        GroupListViewCell() {
            addEventFilter(MouseEvent.MOUSE_PRESSED, event -> wasSelectedOnPress = isSelected());
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && wasSelectedOnPress && !isEmpty() && getItem() != null) {
                    selectionHandler.accept(getItem().group());
                }
            });
        }

        @Override
        protected void updateItem(GroupEntry group, boolean empty) {
            super.updateItem(group, empty);
            setText(empty || group == null ? null : group.label() + "  (" + group.count() + ")");
        }
    }

    private record GroupEntry(String group, int count) {
        String label() {
            return group.isEmpty() ? "All Students" : group;
        }
    }
}
