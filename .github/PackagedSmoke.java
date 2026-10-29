import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
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
    private static final int WINDOW_STARTUP_ATTEMPTS = 100;
    private static final int WINDOW_POLL_INTERVAL_MILLIS = 100;
    private static final int FX_ACTION_TIMEOUT_SECONDS = 30;
    private static final int MAX_EMAIL_LOCAL_PART_LENGTH = 64;
    private static final int MAX_EMAIL_DOMAIN_LABEL_LENGTH = 63;

    private static Scene scene;

    public static void premain(String phase) {
        Thread worker = new Thread(() -> {
            try {
                waitForWindow();
                fx(() -> {
                    runPhase(phase);
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

    private static void runPhase(String phase) throws Exception {
        if (phase.equals("invalid")) {
            verifyInvalidDataStartup(phase);
            return;
        }
        Stage stage = (Stage) Window.getWindows().stream().filter(w -> w instanceof Stage
                && ((Stage) w).getTitle().equals("TutorLink")).findFirst().orElseThrow();
        scene = stage.getScene();
        switch (phase) {
            case "reported-alpha" -> reportedAlphaWorkflow();
            case "peer-alpha" -> peerAlphaWorkflow();
            case "alpha-fixes" -> alphaFixWorkflow();
            case "alpha-restart" -> verifyAlphaFixesAfterRestart();
            case "followup-mark", "followup-complete", "followup-verify" -> followUpWorkflow(phase);
            case "workflow" -> directoryWorkflow();
            case "restart" -> verifyDirectoryRestart();
            default -> throw new IllegalArgumentException("Unknown smoke-test phase: " + phase);
        }
        Files.writeString(Path.of("smoke-passed.txt"), phase);
        command("exit", -1, false);
    }

    private static void verifyInvalidDataStartup(String phase) throws Exception {
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
    }

    private static void directoryWorkflow() throws Exception {
        verifySampleSearchAndHelp();
        prepareSyntheticRoster();
        verifyTagSearchWorkflow();
        verifyIndexedRosterWorkflow();
    }

    private static void verifyDirectoryRestart() {
        command("list", 1, false);
        check(items().get(0).toString().contains("Alice Tan"), "normalized name reloaded");
        command("find a1234567z", 1, false);
        check(items().get(0).toString().contains("L03"), "edited group reloaded");
        command("find Bob", 0, false);
        command("findtag followup", 0, false);
        command("list", 1, false);
    }

    private static void verifySampleSearchAndHelp() throws Exception {
        command("find alex david", 2, false);
        snapshot(scene, "findAlexDavidResult.png");
        command("help", 2, false);
        Window help = Window.getWindows().stream().filter(w -> w instanceof Stage
                && ((Stage) w).getTitle().equals("Help")).findFirst().orElseThrow();
        check(((Label) help.getScene().lookup("#helpMessage")).getText()
                .contains("ay2627s1-cs2103t-t13-4.github.io/tp/UserGuide.html"), "help URL");
        snapshot(help.getScene(), "helpMessage.png");
        help.hide();
    }

    private static void prepareSyntheticRoster() throws Exception {
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
    }

    private static void verifyTagSearchWorkflow() throws Exception {
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
    }

    private static void verifyIndexedRosterWorkflow() throws Exception {
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
    }

    private static void waitForWindow() throws Exception {
        for (int attempt = 0; attempt < WINDOW_STARTUP_ATTEMPTS; attempt++) {
            try {
                if (fx(() -> !Window.getWindows().isEmpty())) {
                    return;
                }
            } catch (IllegalStateException toolkitNotReady) {
                // The actual application, not the agent, initializes JavaFX.
            }
            Thread.sleep(WINDOW_POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("Application did not open a window");
    }

    private static void followUpWorkflow(String phase) throws Exception {
        switch (phase) {
            case "followup-mark" -> markFollowUp();
            case "followup-complete" -> completeFollowUpAfterRestart();
            case "followup-verify" -> verifyFollowUpCompletionAfterRestart();
            default -> throw new IllegalArgumentException("Unknown follow-up phase: " + phase);
        }
    }

    private static void markFollowUp() throws Exception {
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
        verifyFollowUpInputErrors();
        command("followups", 1, false);
        command("view 1", 1, false);
        check(detailLabel("followUp").getText().equals("Follow-up: Required"), "follow-up detail label");
        snapshot(scene, "followUpMarked.png");
    }

    private static void completeFollowUpAfterRestart() throws Exception {
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
    }

    private static void verifyFollowUpInputErrors() throws Exception {
        String saved = data();
        command("followup 2 done", 1, true);
        command("followup 0 on", 1, true);
        command("followup 1 off", 1, true);
        command("followups g/T02", 1, true);
        check(data().equals(saved), "followup errors preserve data and filter");
    }

    private static void verifyFollowUpCompletionAfterRestart() {
        command("followups", 0, false);
        check(feedback().equals("No students require follow-up."), "completion survives process restart");
        command("list", 2, false);
    }

    private static void reportedAlphaWorkflow() throws Exception {
        command("clear", 0, false);
        command("add n/Temporary Student m/A2000000A g/T01", 1, false);
        command("list g/T01", 1, false);
        command("clear", 0, false);
        check(((Label) scene.lookup("#activeFilter")).getText().equals("All students"), "clear resets the filter");
        check(((ListView<?>) scene.lookup("#groupListView")).getSelectionModel().getSelectedIndex() == 0,
                "clear selects All Students consistently");
        command("add n/Alpha Student m/A2000001A g/T01 p/91234567", 1, false);
        command("add n/Beta Student m/A2000002B g/T02", 2, false);
        command("add n/Tab Student m/A2000003C g/T02 a/10 College Road\te/tab@example.com", 3, false);
        command("view 3", 3, false);
        check(detailLabel("address").getText().equals("Address: 10 College Road"), "tab keeps the address separate");
        check(detailLabel("email").getText().equals("Email: tab@example.com"), "tab preserves the email parameter");
        snapshot(scene, "tabSeparatedInput.png");
        command("list g/T01", 1, false);
        command("view 1", 1, false);
        verifyReadOnlyRoster();
    }

    private static void verifyReadOnlyRoster() throws Exception {
        Path file = Path.of("data", "tutorlink.json");
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(file);
        String originalData = data();
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("r--r--r--"));
            command("edit 1 p/98765432", 1, true);
            check(detailLabel("phone").getText().equals("Phone: 91234567"), "failed edit restores selected profile");
            command("edit 1 g/T99", 1, true);
            check(detailLabel("tutorialGroup").getText().equals("Group: T01"), "failed group move restores filter");
            snapshot(scene, "failedEditRestored.png");
            command("help", 1, false);
            Window help = Window.getWindows().stream().filter(w -> w instanceof Stage
                    && ((Stage) w).getTitle().equals("Help") && w.isShowing()).findFirst().orElseThrow();
            help.hide();
            command("list", 3, false);
            check(data().equals(originalData), "list does not save a rejected edit");
            command("exit", 3, false);
            check(!scene.getWindow().isShowing(), "exit works without writable roster storage");
        } finally {
            Files.setPosixFilePermissions(file, originalPermissions);
        }
    }

    private static void peerAlphaWorkflow() throws Exception {
        command("clear", 0, false);
        command("add n/Address Test m/A1000002B g/T01 a/Block 10 \\p/Lane \\e/learning centre t/friends", 1, false);
        check(student().getAddress().value.equals("Block 10 p/Lane e/learning centre"), "literal address prefixes");
        command("add n/Punycode Test m/A1000007G g/T02 e/user@xn--bcher-kva.de", 2, false);
        String saved = data();
        command("add n/Domain Test m/A1000006F g/T01 e/user@"
                + "a".repeat(MAX_EMAIL_DOMAIN_LABEL_LENGTH + 1) + ".com", 2, true);
        check(data().equals(saved), "oversized domain label does not add a student");
        command("findtag friends", 1, false);
        command("remark 1 r/Needs consultation", 1, false);
        command("sort name desc", 1, false);
        check(((Label) scene.lookup("#activeFilter")).getText().equals("Tag: friends"), "persistent tag condition");
        command("view 1", 1, false);
        snapshot(scene, "activeSearchCondition.png");
        command("findtag t/friends", 1, true);
        check(feedback().contains("without t/") && feedback().contains("findtag needsHelp"), "corrective tag guidance");
        check(((Label) scene.lookup("#activeFilter")).getText().equals("Tag: friends"), "invalid search keeps condition");
        command("edit 1 a/NUS \\e/learning centre p/12345678", 1, false);
        check(student().getAddress().value.equals("NUS e/learning centre"), "escaped edit with real phone prefix");
        command("find Nobody", 0, false);
        command("find Absent", 0, false);
        check(((Label) scene.lookup("#activeFilter")).getText().equals("Name or matriculation: Absent"),
                "empty-to-empty query changes condition");
        command("list", 2, false);
        check(((Label) scene.lookup("#activeFilter")).getText().equals("All students"), "list clears condition");
    }

    private static void verifyAlphaFixesAfterRestart() {
        command("followups", 1, false);
        check(student().getName().fullName.equals("Zoe Test"), "correct student restored");
        check(student().getPhone() == null && student().getEmail() == null && student().getAddress() == null,
                "cleared contacts survive restart");
        check(student().getRemark().equals("Discuss recursion"), "remark survives contact clears and restart");
        command("view 1", 1, false);
        check(detailLabel("email").getText().equals("Email: Not provided"), "cleared email displayed");
        command("followup 1 done", 0, false);
        command("list", 2, false);
    }

    private static void alphaFixWorkflow() throws Exception {
        prepareAlphaRoster();
        verifyDeletePreview();
        verifyContactClearing();
        verifyRemarkClearing();
        verifySortedDeletion();
    }

    private static void prepareAlphaRoster() {
        command("clear", 0, false);
        command("add n/Zoe Test m/A1111111A g/T01 p/91234567 e/student+_tutorial@example.com a/10 College Road", 1, false);
        command("remark 1 r/Discuss recursion", 1, false);
        command("followup 1 on", 1, false);
        command("add n/Alpha Test m/A2222222B g/T01", 2, false);
        command("add n/Other Test m/A3333333C g/T02", 3, false);
    }

    private static void verifyDeletePreview() throws Exception {
        command("list g/T01", 2, false);
        command("sort name", 2, false);
        command("view 1", 2, false);
        String before = data();
        command("delete 2", 2, false);
        check(data().equals(before), "deletion preview does not change saved records");
        check(list().getSelectionModel().getSelectedIndex() == 1, "preview highlights displayed target");
        check(detailLabel("name").getText().equals("Zoe Test"), "preview details match target");
        snapshot(scene, "deletePreviewTarget.png");
    }

    private static void verifyContactClearing() throws Exception {
        String before = data();
        command("delete 0", 2, true);
        command("delete 99 confirm", 2, true);
        command("edit 2 p/ e/" + "a".repeat(MAX_EMAIL_LOCAL_PART_LENGTH + 1) + "@example.com", 2, true);
        check(data().equals(before), "invalid combined clear is atomic");
        command("edit 2 e/a..b@example.com", 2, true);
        command("edit 2 e/name@" + "a".repeat(MAX_EMAIL_DOMAIN_LABEL_LENGTH + 1) + ".com", 2, true);
        command("edit 2 e/" + "a".repeat(MAX_EMAIL_LOCAL_PART_LENGTH) + "@example.com", 2, false);
        command("edit 2 p/ e/ a/", 2, false);
        check(detailLabel("phone").getText().equals("Phone: Not provided")
                && detailLabel("email").getText().equals("Email: Not provided")
                && detailLabel("address").getText().equals("Address: Not provided"), "cleared contact details visible");
        snapshot(scene, "clearOptionalContacts.png");
        command("edit 2 p/ e/ a/", 2, false);
        command("find Zoe", 1, false);
        check(student().requiresFollowUp() && student().getRemark().equals("Discuss recursion"),
                "contact clear preserves follow-up and remark");
    }

    private static void verifyRemarkClearing() {
        command("edit 1 t/needsHelp", 1, false);
        command("findtag NEEDSHELP", 1, false);
        command("remark 1 r/", 1, false);
        check(student().getRemark() == null, "empty remark clears");
        command("remark 1 r/Discuss recursion", 1, false);
    }

    private static void verifySortedDeletion() {
        command("list", 3, false);
        command("sort group desc", 3, false);
        command("view 2", 3, false);
        command("delete 1", 3, false);
        check(detailLabel("name").getText().equals("Other Test"), "sorted preview target");
        command("delete 1 confirm", 2, false);
        command("sort reset", 2, false);
        command("groups", 2, false);
        check(feedback().contains("T01: 2 students"), "counts reflect deletion");
        command("followups", 1, false);
        command("view 1", 1, false);
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
        return task.get(FX_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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

    private static void command(String commandText, int expectedStudentCount, boolean expectsError) {
        TextField box = (TextField) scene.lookup("#commandTextField");
        box.setText(commandText);
        box.fireEvent(new ActionEvent());
        if (expectedStudentCount >= 0) {
            check(items().size() == expectedStudentCount,
                    commandText + ": expected " + expectedStudentCount + " students, got " + items().size());
            check(box.getStyleClass().contains("error") == expectsError, commandText + ": error state");
        }
        System.out.println("PASS " + commandText);
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
