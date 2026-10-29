package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.JavaFxTestUtil.runOnFxThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tutorlink.logic.commands.CommandResult;
import tutorlink.logic.parser.TutorLinkParser;
import tutorlink.testutil.JavaFxTestUtil;

/** Exercises the real text field, native skin, event dispatch, layout and focus traversal. */
public class CommandBoxTest {
    private final List<String> executed = new ArrayList<>();
    private Stage stage;
    private VBox root;
    private TextField field;
    private Button next;

    @BeforeAll
    public static void initializeJavaFx() throws Exception {
        JavaFxTestUtil.initialize();
    }

    @BeforeEach
    public void setUp() throws Exception {
        runOnFxThread(() -> {
            CommandBox box = new CommandBox(command -> {
                executed.add(command);
                new TutorLinkParser().parseCommand(command);
                return new CommandResult("Accepted");
            });
            field = (TextField) box.getRoot().lookup("#commandTextField");
            next = new Button("Next focus target");
            root = new VBox(box.getRoot(), next);
            Scene scene = new Scene(root, 900, 130);
            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
            stage = new Stage();
            stage.setScene(scene);
            stage.show();
            stage.requestFocus();
            field.requestFocus();
        });
        CountDownLatch focused = new CountDownLatch(1);
        runOnFxThread(() -> {
            field.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    focused.countDown();
                }
            });
            stage.toFront();
            stage.requestFocus();
            field.requestFocus();
            if (field.isFocused()) {
                focused.countDown();
            }
            layout();
        });
        assertTrue(focused.await(5, TimeUnit.SECONDS), "The native test window must gain focus");
    }

    @AfterEach
    public void tearDown() throws Exception {
        runOnFxThread(() -> stage.hide());
    }

    @Test
    public void tab_stepwiseCompletion_keepsValuesAndFocus() throws Exception {
        runOnFxThread(() -> {
            enter("ad");
            assertTrue(ghost().getText().startsWith("d n/NAME"));
            tab(false);
            assertEquals("add", field.getText());
            tab(false);
            assertEquals("add n/", field.getText());
            field.appendText("Alice Tan");
            tab(false);
            assertEquals("add n/Alice Tan m/", field.getText());
            field.appendText("A0123456X");
            tab(false);
            field.appendText("T01");
            assertEquals("add n/Alice Tan m/A0123456X g/T01", field.getText());
            layout();
            assertTrue(ghost().getText().startsWith(" [p/PHONE]"));
            assertTrue(field.isFocused());
            assertEquals(field.getLength(), field.getCaretPosition());
            field.fireEvent(new ActionEvent());
            assertEquals(List.of("add n/Alice Tan m/A0123456X g/T01"), executed);
            assertEquals("", field.getText());
        });
    }

    @Test
    public void enter_unacceptedGhost_executesOnlyActualInputAndRetainsErrors() throws Exception {
        runOnFxThread(() -> {
            enter("ad");
            field.fireEvent(new ActionEvent());
            assertEquals(List.of("ad"), executed);
            assertEquals("ad", field.getText());
            assertTrue(field.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            field.appendText("d");
            assertFalse(field.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            field.fireEvent(new ActionEvent());
            assertEquals(List.of("ad", "add"), executed);
            assertTrue(field.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
        });
    }

    @Test
    public void tab_noAppendOrModifiedTab_preservesTraversal() throws Exception {
        runOnFxThread(() -> {
            for (String text : List.of("f", "help", "view", "unknown", "add n/")) {
                field.requestFocus();
                enter(text);
                tab(false);
                assertEquals(text, field.getText());
                assertTrue(next.isFocused(), text);
            }
            field.requestFocus();
            enter("ad");
            tab(true);
            assertEquals("ad", field.getText());
            assertTrue(next.isFocused());
        });
    }

    @Test
    public void ghost_middleCaretSelectionAndBlur_hidesWithoutChangingInput() throws Exception {
        runOnFxThread(() -> {
            enter("add n/Alice Tan");
            assertFalse(ghost().getText().isEmpty());
            field.positionCaret(6);
            layout();
            assertEquals("", ghost().getText());
            tab(false);
            assertEquals("add n/Alice Tan", field.getText());
            field.requestFocus();
            field.selectRange(0, field.getLength());
            layout();
            assertEquals("", ghost().getText());
            field.end();
            next.requestFocus();
            layout();
            assertEquals("", ghost().getText());
        });
    }

    @Test
    public void paste_tabsAndSpacedValues_normalizesWithoutMergingParameters() throws Exception {
        runOnFxThread(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent original = new ClipboardContent();
            for (DataFormat format : clipboard.getContentTypes()) {
                original.put(format, clipboard.getContent(format));
            }
            try {
                ClipboardContent content = new ClipboardContent();
                content.putString("add\tn/Alice  Tan\tm/A0123456X\tg/T01");
                clipboard.setContent(content);
                field.paste();
            } finally {
                clipboard.setContent(original);
            }
            assertEquals("add n/Alice  Tan m/A0123456X g/T01", field.getText());
            tab(false);
            assertEquals("add n/Alice  Tan m/A0123456X g/T01 p/", field.getText());
        });
    }

    @Test
    public void ghost_resizingFontsAndHorizontalScroll_followsNativeTextAndClipsOnlyHint() throws Exception {
        runOnFxThread(() -> {
            for (double size : new double[] {13, 25, 38}) {
                field.setFont(Font.font("System", size));
                enter("add n/Alice Tan");
                assertGhostAligned();
                root.setScaleX(1.25);
                root.setScaleY(1.25);
                assertGhostAligned();
                root.setScaleX(1);
                root.setScaleY(1);
                stage.setWidth(440);
                layout();
                assertGhostAligned();
                enter("add n/" + "Long Name ".repeat(30));
                assertGhostAligned();
                assertTrue(field.getText().endsWith("Long Name "));
                assertEquals(field.getLength(), field.getCaretPosition());
                Rectangle clip = (Rectangle) ghost().getClip();
                assertTrue(clip.getWidth() >= 0);
                assertTrue(clip.getX() + clip.getWidth() <= field.getWidth());
                tab(false);
                assertTrue(field.getText().endsWith("Long Name m/"));
                stage.setWidth(1000);
                layout();
                assertGhostAligned();
            }
        });
    }

    private void assertGhostAligned() {
        layout();
        Text ghost = ghost();
        double end = ((TextFieldSkin) field.getSkin()).getCharacterBounds(field.getLength()).getMaxX();
        assertEquals(end + 1, ghost.getX(), 0.01);
        assertEquals(field.getBaselineOffset(), ghost.getY(), 0.01);
        Text actual = field.lookupAll(".text").stream().filter(Text.class::isInstance)
                .map(Text.class::cast).filter(text -> text.getText().equals(field.getText())).findFirst().orElseThrow();
        Bounds bounds = field.sceneToLocal(actual.localToScene(actual.getBoundsInLocal()));
        assertEquals(bounds.getMaxX() + 1, ghost.getX(), 1.0);
    }

    private void enter(String text) {
        field.setText(text);
        field.end();
        layout();
    }

    private void layout() {
        root.applyCss();
        root.layout();
    }

    private Text ghost() {
        return (Text) field.lookup("#commandGhostText");
    }

    private void tab(boolean shift) {
        field.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, shift, false, false, false));
        layout();
    }
}
