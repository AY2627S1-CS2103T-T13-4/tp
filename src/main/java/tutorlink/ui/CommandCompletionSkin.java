package tutorlink.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import tutorlink.logic.parser.CommandCompletion;

/**
 * Draws a clipped, mouse-transparent syntax suffix using the text field's actual scrolled character geometry.
 */
class CommandCompletionSkin extends TextFieldSkin {
    private final CommandCompletion completion = new CommandCompletion();
    private final Text ghost = new Text();
    private final Rectangle clip = new Rectangle();

    CommandCompletionSkin(TextField field) {
        super(field);
        ghost.setId("commandGhostText");
        ghost.getStyleClass().add("command-ghost");
        ghost.setManaged(false);
        ghost.setMouseTransparent(true);
        ghost.fontProperty().bind(field.fontProperty());
        ghost.setClip(clip);
        getChildren().add(ghost);
        registerChangeListener(field.textProperty(), observable -> field.requestLayout());
        registerChangeListener(field.caretPositionProperty(), observable -> field.requestLayout());
        registerChangeListener(field.anchorProperty(), observable -> field.requestLayout());
        registerChangeListener(field.focusedProperty(), observable -> field.requestLayout());
    }

    @Override
    protected void layoutChildren(double x, double y, double width, double height) {
        super.layoutChildren(x, y, width, height);
        TextField field = getSkinnable();
        boolean canSuggest = field.isFocused() && field.getCaretPosition() == field.getLength()
                && field.getSelection().getLength() == 0;
        ghost.setText(canSuggest ? completion.suggest(field.getText()).display() : "");
        Rectangle2D end = getCharacterBounds(field.getLength());
        double start = end.getMaxX();
        // Keep a small gap after the caret. The native skin still controls scrolling and input width.
        ghost.setX(start + 1);
        ghost.setY(field.getBaselineOffset());
        clip.setX(Math.max(x, start + 1));
        clip.setY(y);
        clip.setWidth(Math.max(0, x + width - clip.getX()));
        clip.setHeight(height);
    }

    @Override
    public void dispose() {
        ghost.fontProperty().unbind();
        super.dispose();
    }
}
