package tutorlink.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.stage.Window;
import tutorlink.model.student.Student;

/**
 * Keeps an indexed student visible after variable-height rows finish layout.
 */
public class StudentListView extends ListView<Student> {
    private static final int STABLE_LAYOUT_PULSES = 3;
    private final Runnable scrollAfterLayout = this::finishScroll;
    private final ListChangeListener<Student> itemsListener = change -> cancelPendingScroll();
    private final ChangeListener<Window> sceneWindowListener = (observable, oldWindow, newWindow) ->
            cancelPendingScroll();
    private final ChangeListener<Boolean> windowShowingListener = (observable, oldValue, isShowing) -> {
        if (!isShowing) {
            cancelPendingScroll();
        }
    };
    private Scene scrollScene;
    private Window scrollWindow;
    private Student scrollTarget;
    private int visiblePulses;

    /**
     * Creates a list whose skin is installed by CSS once rows can be measured in the scene.
     */
    public StudentListView() {
        getSelectionModel().selectedItemProperty().addListener(observable -> cancelPendingScroll());
        sceneProperty().addListener(observable -> cancelPendingScroll());
        itemsProperty().addListener((observable, oldItems, newItems) -> {
            cancelPendingScroll();
            if (oldItems != null) {
                oldItems.removeListener(itemsListener);
            }
            if (newItems != null) {
                newItems.addListener(itemsListener);
            }
        });
        getItems().addListener(itemsListener);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new StudentListViewSkin(this);
    }

    /**
     * Scrolls to the selected student, correcting the position after row heights settle.
     */
    public void scrollToSelection() {
        cancelPendingScroll();
        applyCss();
        layout();
        int index = getSelectionModel().getSelectedIndex();
        if (index < 0) {
            return;
        }
        scrollTo(index);
        Scene scene = getScene();
        if (scene == null || scene.getWindow() == null || !scene.getWindow().isShowing()) {
            return;
        }
        scrollTarget = getSelectionModel().getSelectedItem();
        scrollScene = scene;
        scrollWindow = scene.getWindow();
        scrollScene.windowProperty().addListener(sceneWindowListener);
        scrollWindow.showingProperty().addListener(windowShowingListener);
        scrollScene.addPostLayoutPulseListener(scrollAfterLayout);
        Platform.requestNextPulse();
    }

    /**
     * Cancels an earlier view request when another command changes the selection or displayed list.
     */
    public void cancelPendingScroll() {
        if (scrollScene != null) {
            scrollScene.removePostLayoutPulseListener(scrollAfterLayout);
            scrollScene.windowProperty().removeListener(sceneWindowListener);
            scrollWindow.showingProperty().removeListener(windowShowingListener);
        }
        scrollTarget = null;
        scrollScene = null;
        scrollWindow = null;
        visiblePulses = 0;
    }

    private void finishScroll() {
        int index = getItems() == null ? -1 : getItems().indexOf(scrollTarget);
        if (index < 0 || getSelectionModel().getSelectedItem() != scrollTarget
                || !(getSkin() instanceof StudentListViewSkin skin)) {
            cancelPendingScroll();
            return;
        }
        boolean isVisible = skin.ensureVisible(index);
        if (isVisible) {
            visiblePulses++;
            if (visiblePulses == STABLE_LAYOUT_PULSES) {
                cancelPendingScroll();
            } else {
                Platform.requestNextPulse();
            }
        } else {
            visiblePulses = 0;
        }
    }

    /**
     * Exposes measured-cell scrolling while retaining the normal skin initialization lifecycle.
     */
    private static class StudentListViewSkin extends ListViewSkin<Student> {
        StudentListViewSkin(StudentListView list) {
            super(list);
        }

        @Override
        protected VirtualFlow<ListCell<Student>> createVirtualFlow() {
            return new StudentVirtualFlow();
        }

        boolean ensureVisible(int index) {
            return ((StudentVirtualFlow) getVirtualFlow()).ensureVisible(index);
        }
    }

    /**
     * Aligns a rendered cell using its measured bounds instead of an estimated index offset.
     */
    private static class StudentVirtualFlow extends VirtualFlow<ListCell<Student>> {
        boolean ensureVisible(int index) {
            ListCell<Student> cell = getVisibleCell(index);
            double viewportHeight = getHeight() - (getHbar().isVisible() ? getHbar().getHeight() : 0);
            if (viewportHeight <= 0) {
                return true;
            }
            if (cell != null && cell.isVisible()) {
                Bounds bounds = cell.getBoundsInParent();
                double top = bounds.getMinY();
                double visibleHeight = Math.min(bounds.getHeight(), viewportHeight);
                if (top >= -1 && top + visibleHeight <= viewportHeight + 1) {
                    return true;
                }
                scrollPixels(top < 0 || bounds.getHeight() > viewportHeight
                        ? top : bounds.getMaxY() - viewportHeight);
            } else {
                scrollTo(index);
            }
            Platform.requestNextPulse();
            return false;
        }
    }
}
