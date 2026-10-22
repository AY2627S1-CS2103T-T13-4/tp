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
import javafx.scene.control.ScrollPane;
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
                    if (phase.startsWith("followup-")) {
                        followUpWorkflow(phase);
                    } else if (phase.equals("workflow")) {
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
                        check(!details().isVisible() && details().getContent() == null,
                                "details remain empty until a student is selected");
                        String beforeView = data();
                        command("view 1", 1, false);
                        check(data().equals(beforeView), "view preserves saved data");
                        check(detailLabel("name").getText().equals("Alice Tan"), "view selects Alice's details");
                        check(detailLabel("phone").getText().equals("Phone: Not provided")
                                && detailLabel("email").getText().equals("Email: Not provided")
                                && detailLabel("address").getText().equals("Address: Not provided")
                                && detailLabel("remark").getText().equals("Remark: Not provided"),
                                "optional details display in the selected profile");
                        command("add n/Bob Lee m/A7654321C g/T02 e/bob@example.com", 2, false);
                        command("edit 1 t/FollowUp", 2, false);
                        command("edit 2 t/followup", 2, false);
                        command("findtag FOLLOWUP", 2, false);
                        snapshot(scene, "findTagTwoStudents.png");
                        command("edit 1 t/", 1, false);
                        check(items().get(0).toString().contains("Bob Lee"), "cleared tag removes Alice from results");
                        snapshot(scene, "findTagAfterClearing.png");
                        command("findtag followup", 1, false);
                        snapshot(scene, "findTagSearchAgain.png");
                        command("findtag follow", 0, false);
                        command("findtag FOLLOWUP", 1, false);
                        String tagSearchData = data();
                        command("findtag follow-up", 1, true);
                        check(data().equals(tagSearchData), "invalid tag search preserves saved data");
                        command("edit 1 e/updatedbob@example.com", 1, false);
                        check(items().get(0).toString().contains("Bob Lee"), "tag result edit uses displayed index");
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
                        command("findtag followup", 1, false);
                        command("delete 1", 1, false);
                        command("delete 1 confirm", 0, false);
                        command("list", 1, false);
                    } else {
                        command("list", 1, false);
                        check(items().get(0).toString().contains("Alice Tan"), "normalized name reloaded");
                        command("find a1234567z", 1, false);
                        check(items().get(0).toString().contains("L03"), "edited group reloaded");
                        command("find Bob", 0, false);
                        command("findtag followup", 0, false);
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

    private static void followUpWorkflow(String phase) throws Exception {
        if (phase.equals("followup-mark")) {
            command("find A6666666Z", 1, false);
            check(!student().requiresFollowUp(), "older record defaults to no follow-up");
            check(student().getRemark().equals("Discuss recursion"), "older remark retained");
            command("followup 1 on", 1, false);
            check(student().requiresFollowUp(), "mark uses displayed index");
            command("followup 1 on", 1, false);
            check(feedback().startsWith("Already requires follow-up:"), "repeated marking feedback");
            command("edit 1 g/L03 t/needsHelp", 1, false);
            check(student().requiresFollowUp(), "group and tag edits retain status");
            check(student().getRemark().equals("Discuss recursion"), "mark and edits retain remark");
            command("findtag NEEDSHELP", 1, false);
            command("edit 1 t/", 0, false);
            command("list g/T02", 1, false);
            command("followups", 1, false);
            check(student().getMatricNumber().value.equals("A6666666Z"), "followups searches full roster");
            check(student().getTutorialGroup().value.equals("L03"), "followups retains group move");
            check(student().getTags().isEmpty(), "followups retains cleared tags");
            check(student().getPhone() == null, "followups retains absent optional details");
            check(student().getRemark().equals("Discuss recursion"), "followups retains remark");
            String saved = data();
            command("followup 2 done", 1, true);
            command("followup 0 on", 1, true);
            command("followup 1 off", 1, true);
            command("followups g/T02", 1, true);
            check(data().equals(saved), "followup errors preserve data and filter");
            command("followups", 1, false);
            command("view 1", 1, false);
            check(detailLabel("followUp").getText().equals("Follow-up: Required"), "follow-up detail label");
            snapshot(scene, "followUpMarked.png");
        } else if (phase.equals("followup-complete")) {
            command("list g/T02", 1, false);
            command("followups", 1, false);
            check(student().requiresFollowUp(), "follow-up status survives process restart");
            check(student().getRemark().equals("Discuss recursion"), "remark survives process restart");
            command("view 1", 1, false);
            check(detailLabel("followUp").getText().equals("Follow-up: Required"),
                    "follow-up detail label survives process restart");
            snapshot(scene, "followUpAfterRestart.png");
            command("followup 1 done", 0, false);
            command("followups", 0, false);
            check(feedback().equals("No students require follow-up."), "empty follow-up list feedback");
            check(!details().isVisible() && details().getContent() == null,
                    "details clear when the selected student leaves the results");
            snapshot(scene, "followUpCompleted.png");
            command("find A6666666Z", 1, false);
            command("followup 1 done", 1, false);
            check(feedback().startsWith("No outstanding follow-up:"), "repeated completion feedback");
            check(!student().requiresFollowUp(), "completed student record retained");
            check(student().getRemark().equals("Discuss recursion"), "completion retains remark");
            command("view 1", 1, false);
            check(detailLabel("followUp").getText().equals("Follow-up: Not required"),
                    "completed follow-up detail label");
        } else {
            command("followups", 0, false);
            check(feedback().equals("No students require follow-up."), "completion survives process restart");
            command("list", 2, false);
        }
    }

    private static tutorlink.model.student.Student student() {
        return (tutorlink.model.student.Student) items().get(0);
    }

    private static String feedback() {
        return ((TextArea) scene.lookup("#resultDisplay")).getText();
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

    private static ScrollPane details() {
        return (ScrollPane) scene.lookup("#detailsScroll");
    }

    private static Label detailLabel(String id) {
        check(details().isVisible() && details().getContent() != null, "selected profile is visible");
        Label label = (Label) details().getContent().lookup("#" + id);
        check(label != null, "selected profile contains " + id);
        return label;
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
