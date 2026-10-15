import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Exercises the real packaged application through its controls, without replacing its entry point. */
public class PackagedSmoke {
    private static Scene scene;

    public static void premain(String phase) {
        Thread worker = new Thread(() -> {
            try {
                waitForWindow();
                fx(() -> {
                    if (phase.equals("invalid")) {
                        check(Window.getWindows().stream().noneMatch(w -> w instanceof Stage
                                && ((Stage) w).getTitle().equals("TutorLink")), "no roster on invalid data");
                        Window alert = Window.getWindows().stream().filter(w -> w instanceof Stage
                                && ((Stage) w).getTitle().equals("Unable to load student data"))
                                .findFirst().orElseThrow();
                        DialogPane pane = (DialogPane) alert.getScene().getRoot();
                        check(pane.getContentText().contains("original file was not changed"), "data protection alert");
                        Files.writeString(Path.of("smoke-passed.txt"), phase);
                        alert.hide();
                        Platform.exit();
                        return null;
                    }
                    Stage stage = (Stage) Window.getWindows().stream().filter(w -> w instanceof Stage
                            && ((Stage) w).getTitle().equals("TutorLink")).findFirst().orElseThrow();
                    scene = stage.getScene();
                    if (phase.equals("workflow")) {
                        command("find alex david", 2, false);
                        snapshot(scene, "findAlexDavidResult.png");
                        command("help", 2, false);
                        Window help = Window.getWindows().stream().filter(w -> w instanceof Stage
                                && ((Stage) w).getTitle().equals("Help")).findFirst().orElseThrow();
                        check(((Label) help.getScene().lookup("#helpMessage")).getText()
                                .contains("ay2627s1-cs2103t-t13-4.github.io/tp/UserGuide.html"), "help URL");
                        snapshot(help.getScene(), "helpMessage.png");
                        help.hide();
                        command("clear", 0, false);
                        command("add n/Alice  Tan m/a1234567b g/t01", 1, false);
                        scene.getRoot().applyCss();
                        scene.getRoot().layout();
                        check(scene.getRoot().lookupAll(".label").stream().anyMatch(node -> node instanceof Label
                                && ((Label) node).getText().contains("Not provided")), "optional details display");
                        command("add n/Bob Lee m/A7654321C g/T02 e/bob@example.com", 2, false);
                        command("list g/t01", 1, false);
                        String before = data();
                        command("add n/Duplicate m/a1234567b g/T02", 1, true);
                        command("list g/T1", 1, true);
                        command("edit 9 n/Invalid", 1, true);
                        command("edit 1 m/a7654321c", 1, true);
                        check(data().equals(before), "invalid commands preserve data and filter");
                        command("find bob", 1, false);
                        check(items().get(0).toString().contains("Bob Lee"), "search replaces group filter");
                        command("find a1234567b", 1, false);
                        snapshot(scene, "findMatricNumberResult.png");
                        command("edit 1 m/a1234567z g/l03", 0, false);
                        command("find a1234567z", 1, false);
                        check(items().get(0).toString().contains("L03"), "edited group normalized");
                        command("delete 1", 1, false);
                        check(((TextArea) scene.lookup("#resultDisplay")).getText()
                                .contains("delete 1 confirm"), "deletion preview");
                        command("list", 2, false);
                        command("find Bob", 1, false);
                        command("delete 1 confirm", 0, false);
                        command("list", 1, false);
                    } else {
                        command("list", 1, false);
                        check(items().get(0).toString().contains("Alice Tan"), "normalized name reloaded");
                        command("find a1234567z", 1, false);
                        check(items().get(0).toString().contains("L03"), "edited group reloaded");
                        command("find Bob", 0, false);
                        command("list", 1, false);
                    }
                    Files.writeString(Path.of("smoke-passed.txt"), phase);
                    command("exit", -1, false);
                    return null;
                });
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.exit(2);
            }
        }, "packaged-smoke");
        worker.setDaemon(true);
        worker.start();
    }

    private static void waitForWindow() throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            try {
                if (fx(() -> !Window.getWindows().isEmpty())) {
                    return;
                }
            } catch (IllegalStateException toolkitNotReady) {
                // The actual application, not the agent, initializes JavaFX.
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Application did not open a window");
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(30, TimeUnit.SECONDS);
    }

    private static ListView<?> list() {
        return (ListView<?>) scene.lookup("#studentListView");
    }

    private static java.util.List<?> items() {
        return list().getItems();
    }

    private static void command(String text, int count, boolean error) {
        TextField box = (TextField) scene.lookup("#commandTextField");
        box.setText(text);
        box.fireEvent(new ActionEvent());
        if (count >= 0) {
            check(items().size() == count, text + ": expected " + count + " students, got " + items().size());
            check(box.getStyleClass().contains("error") == error, text + ": error state");
        }
        System.out.println("PASS " + text);
    }

    private static String data() throws Exception {
        return Files.readString(Path.of("data", "tutorlink.json"));
    }

    private static void snapshot(Scene target, String filename) throws Exception {
        target.getRoot().applyCss();
        target.getRoot().layout();
        WritableImage pixels = target.snapshot(null);
        BufferedImage png = new BufferedImage((int) pixels.getWidth(), (int) pixels.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++) {
            for (int x = 0; x < png.getWidth(); x++) {
                png.setRGB(x, y, pixels.getPixelReader().getArgb(x, y));
            }
        }
        ImageIO.write(png, "png", Path.of(filename).toFile());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
