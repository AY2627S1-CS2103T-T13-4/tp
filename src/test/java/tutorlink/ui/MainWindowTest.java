package tutorlink.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.JavaFxTestUtil.runOnFxThread;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import tutorlink.logic.Logic;
import tutorlink.logic.LogicManager;
import tutorlink.logic.commands.CommandResult;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.ModelManager;
import tutorlink.model.ReadOnlyStudentDirectory;
import tutorlink.model.student.Student;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.JavaFxTestUtil;
import tutorlink.testutil.StudentBuilder;

/**
 * Exercises the three-column workspace with the real command layer and JSON storage.
 */
public class MainWindowTest {
    private static final Student BOB = new StudentBuilder().withName("Bob Tan")
            .withMatricNumber("A0000001A").withTutorialGroup("T01").build();
    private static final Student ALICE = new StudentBuilder().withName("Alice Lee")
            .withMatricNumber("A0000002B").withTutorialGroup("T02").withFollowUp(true)
            .withRemark("Needs help with recursion. Discuss the base case during the next consultation.")
            .withTags("needsHelp", "recursion").build();
    private static final Student CARA = new StudentBuilder().withName("Cara Lim")
            .withMatricNumber("A0000003C").withTutorialGroup("T01").build();

    @TempDir
    public Path temporaryFolder;

    private MainWindow window;
    private Logic logic;
    private Stage stage;
    private boolean isSaveBlocked;
    private int saveCount;
    private int commandCount;
    private ModelManager model;

    @BeforeAll
    public static void initializeJavaFx() throws Exception {
        JavaFxTestUtil.initialize();
    }

    @Test
    public void execute_searchCondition_remainsVisibleThroughUpdatesAndEmptyResults() throws Exception {
        runOnFxThread(() -> {
            Label filter = (Label) stage.getScene().lookup("#activeFilter");
            window.executeCommand("findtag needsHelp");
            window.executeCommand("remark 1 r/Consultation");
            window.executeCommand("sort name desc");
            window.executeCommand("delete 1");
            assertEquals("Tag: needsHelp", filter.getText());
            assertThrows(ParseException.class, () -> window.executeCommand("findtag t/friends"));
            assertEquals("Tag: needsHelp", filter.getText());
            window.executeCommand("edit 1 t/");
            assertTrue(model.getFilteredStudentList().isEmpty());
            assertEquals("Tag: needsHelp", filter.getText());
            window.executeCommand("find Nobody"); // empty-to-empty filter change still updates the label
            assertEquals("Name or matriculation: Nobody", filter.getText());
            window.executeCommand("list g/T01");
            assertEquals("Tutorial group: T01", filter.getText());
            window.executeCommand("followups");
            window.executeCommand("followup 1 done");
            assertEquals("Follow-up required", filter.getText());
            window.executeCommand("add n/Dana Test m/A0000004D g/T03");
            assertEquals("All students", filter.getText());
            assertEquals(filter.getText(), filter.getTooltip().getText());
        });
    }

    @Test
    public void execute_filterWithUnavailableStorage_updatesViewWithoutSaving() throws Exception {
        runOnFxThread(() -> {
            isSaveBlocked = true;
            window.executeCommand("find Nobody");
            assertTrue(model.getFilteredStudentList().isEmpty());
            Label filter = (Label) stage.getScene().lookup("#activeFilter");
            assertEquals("Name or matriculation: Nobody", filter.getText());
            assertEquals(0, saveCount);
        });
    }

    @Test
    public void execute_failedMutation_restoresProfileSelectionAndGroup() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("list g/T01");
            window.executeCommand("sort name desc");
            window.executeCommand("view 1");
            isSaveBlocked = true;

            for (String command : List.of("edit 1 n/Unexpected", "edit 1 g/T99", "clear")) {
                assertThrows(CommandException.class, () -> window.executeCommand(command));
                assertEquals(List.of(CARA, BOB), model.getFilteredStudentList());
                assertEquals(CARA, window.getStudentListPanel().getSelectedStudent());
                assertEquals("Cara Lim", label("name").getText());
                assertEquals("Tutorial group: T01", ((Label) stage.getScene().lookup("#activeFilter")).getText());
                ListView<?> groups = (ListView<?>) stage.getScene().lookup("#groupListView");
                assertEquals(1, groups.getSelectionModel().getSelectedIndex());
            }
        });
    }

    @Test
    public void commandBox_tabSeparatedInput_savesDistinctAddressAndEmail() throws Exception {
        runOnFxThread(() -> {
            TextField input = (TextField) stage.getScene().lookup("#commandTextField");
            input.replaceText(0, input.getLength(),
                    "add n/Tab Student m/A2000003C g/T02 a/10 College Road\te/tab@example.com");
            assertTrue(input.getText().contains("Road e/"));
            input.fireEvent(new ActionEvent());

            Student added = model.getStudentDirectory().getStudentList().getLast();
            assertEquals("10 College Road", added.getAddress().value);
            assertEquals("tab@example.com", added.getEmail().value);
            assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            ReadOnlyStudentDirectory saved = new JsonStudentDirectoryStorage(temporaryFolder.resolve("students.json"))
                    .readStudentDirectory().orElseThrow();
            assertEquals(added, saved.getStudentList().getLast());
        });
    }

    @BeforeEach
    public void setUp() throws Exception {
        runOnFxThread(() -> {
            model = new ModelManager();
            model.addStudent(BOB);
            model.addStudent(ALICE);
            model.addStudent(CARA);
            var storage = new StorageManager(
                    new JsonStudentDirectoryStorage(temporaryFolder.resolve("students.json")),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))) {
                @Override
                public void saveStudentDirectory(ReadOnlyStudentDirectory directory) throws IOException {
                    if (isSaveBlocked) {
                        throw new IOException("Storage is unavailable for this test");
                    }
                    saveCount++;
                    super.saveStudentDirectory(directory);
                }
            };
            logic = new LogicManager(model, storage) {
                @Override
                public CommandResult execute(String commandText) throws CommandException, ParseException {
                    commandCount++;
                    return super.execute(commandText);
                }
            };
            stage = new Stage();
            window = new MainWindow(stage, logic, Path.of("data", "tutorlink.json"));
            window.show();
            window.fillInnerParts();
            layout();
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        runOnFxThread(() -> stage.hide());
    }

    @Test
    public void deletePreview_selectsTargetWithoutRemovingOrChangingRecords() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("sort name");
            window.executeCommand("view 1");
            assertEquals("Alice Lee", label("name").getText());
            window.executeCommand("delete 2");
            assertEquals(BOB, window.getStudentListPanel().getSelectedStudent());
            assertEquals("Bob Tan", label("name").getText());
            assertEquals("2. ", label("id").getText());
            window.executeCommand("list g/T01");
            window.executeCommand("delete 2");
            assertEquals(CARA, window.getStudentListPanel().getSelectedStudent());
            assertEquals("Cara Lim", label("name").getText());
            assertEquals(List.of(BOB, ALICE, CARA), logic.getStudentList());
            assertThrows(CommandException.class, () -> window.executeCommand("delete 99"));
            assertEquals(CARA, window.getStudentListPanel().getSelectedStudent());
        });
    }

    @Test
    public void initialView_showsAllStudentsAndAllGroupsWithoutSelectedDetails() throws Exception {
        runOnFxThread(() -> {
            assertEquals(List.of(BOB, ALICE, CARA), studentList().getItems());
            assertSame(logic.getFilteredStudentList(), studentList().getItems());
            assertEquals(Set.of("All Students  (3)", "T01  (2)", "T02  (1)"), groupLabels());
            assertEquals(0, groupList().getSelectionModel().getSelectedIndex());
            assertFalse(detailsScroll().isVisible());
            assertEquals("3 displayed", ((Label) lookup("studentCount")).getText());
        });
    }

    @Test
    public void groupSelection_andCliSearch_shareResultsAndKeepFullGroupList() throws Exception {
        runOnFxThread(() -> {
            groupList().getSelectionModel().select(1);
            assertEquals(List.of(BOB, CARA), logic.getFilteredStudentList());
            studentList().getSelectionModel().select(0);
            assertEquals("Bob Tan", label("name").getText());
            window.executeCommand("find Alice");
            assertEquals(List.of(ALICE), studentList().getItems());
            assertEquals(-1, groupList().getSelectionModel().getSelectedIndex());
            assertFalse(detailsScroll().isVisible());
            assertEquals(3, groupList().getItems().size());
            groupList().getSelectionModel().select(0);
            assertEquals(3, studentList().getItems().size());
            window.executeCommand("list g/t02");
            assertEquals(2, groupList().getSelectionModel().getSelectedIndex());
            assertEquals(List.of(ALICE), studentList().getItems());
        });
    }

    @Test
    public void sortedSelection_editsCorrectStudentUpdatesDetailsAndClearsOnDeletion() throws Exception {
        runOnFxThread(() -> {
            studentList().getSelectionModel().select(0);
            window.executeCommand("sort name");
            assertEquals(BOB, window.getStudentListPanel().getSelectedStudent());
            assertEquals("2. ", label("id").getText());
            layout();
            studentList().lookupAll(".list-cell").stream().filter(ListCell.class::isInstance)
                    .map(ListCell.class::cast).filter(cell -> !cell.isEmpty()).forEach(cell -> {
                        Label summaryIndex = (Label) cell.getGraphic().lookup("#id");
                        assertEquals((cell.getIndex() + 1) + ".", summaryIndex.getText());
                    });
            window.executeCommand("remark 2 r/Changed after sorting");
            assertEquals("Remark: Changed after sorting", label("remark").getText());
            window.executeCommand("edit 2 n/Zoe Tan g/T03");
            assertEquals("Zoe Tan", label("name").getText());
            assertEquals("3. ", label("id").getText());
            assertEquals("Group: T03", label("tutorialGroup").getText());
            window.executeCommand("delete 3 confirm");
            assertNull(window.getStudentListPanel().getSelectedStudent());
            assertFalse(detailsScroll().isVisible());
            assertNull(detailsScroll().getContent());
            assertEquals(2, logic.getStudentList().size());
        });
    }

    @Test
    public void movingOrCompletingSelectedStudent_clearsDetailsWhenTheyLeaveResults() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("list g/T01");
            studentList().getSelectionModel().select(0);
            window.executeCommand("edit 1 g/T02");
            assertEquals(List.of(CARA), studentList().getItems());
            assertFalse(detailsScroll().isVisible());
            layout();
            assertEquals(Set.of("All Students  (3)", "T01  (1)", "T02  (2)"), groupLabels());
            window.executeCommand("followups");
            studentList().getSelectionModel().select(0);
            window.executeCommand("followup 1 done");
            assertTrue(studentList().getItems().isEmpty());
            assertFalse(detailsScroll().isVisible());
        });
    }

    @Test
    public void invalidCommand_retainsSelectionAndFilter() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("list g/T02");
            studentList().getSelectionModel().select(0);
            try {
                window.executeCommand("sort invalid");
                throw new AssertionError("Expected an invalid sort to fail");
            } catch (ParseException expected) {
                assertEquals(ALICE, window.getStudentListPanel().getSelectedStudent());
                assertEquals(2, groupList().getSelectionModel().getSelectedIndex());
                assertEquals("Alice Lee", label("name").getText());
            }
        });
    }

    @Test
    public void viewCommand_selectsStudentByDisplayedIndexAndPreservesSelectionOnError() throws Exception {
        runOnFxThread(() -> {
            studentList().getSelectionModel().select(0);
            window.executeCommand("view 2");
            assertEquals(ALICE, window.getStudentListPanel().getSelectedStudent());
            assertEquals("Alice Lee", label("name").getText());
            window.executeCommand("sort name desc");
            window.executeCommand("view 1");
            assertEquals("Cara Lim", label("name").getText());
            window.executeCommand("list g/T02");
            window.executeCommand("view 1");
            assertEquals("Alice Lee", label("name").getText());
            assertThrows(CommandException.class, () -> window.executeCommand("view 2"));
            assertThrows(ParseException.class, () -> window.executeCommand("view 0"));
            assertEquals(ALICE, window.getStudentListPanel().getSelectedStudent());
            assertEquals("Alice Lee", label("name").getText());
        });
    }

    @Test
    public void viewCommand_doesNotWriteDataOrRequireWritableStorage() throws Exception {
        runOnFxThread(() -> {
            isSaveBlocked = true;
            window.executeCommand("view 2");
            assertEquals("Alice Lee", label("name").getText());
            assertFalse(Files.exists(temporaryFolder.resolve("students.json")));
        });
    }

    @Test
    public void groupActivation_selectedOrNewGroup_resetsSortingExactlyOnce() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("sort name");
            int previousCommands = commandCount;
            clickGroup(0);
            assertEquals(previousCommands + 1, commandCount);
            assertEquals(List.of(BOB, ALICE, CARA), studentList().getItems());
            previousCommands = commandCount;
            clickGroup(1);
            assertEquals(previousCommands + 1, commandCount);
            assertEquals(List.of(BOB, CARA), studentList().getItems());
            window.executeCommand("sort name desc");
            previousCommands = commandCount;
            groupList().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                    false, false, false, false));
            assertEquals(previousCommands + 1, commandCount);
            assertEquals(List.of(BOB, CARA), studentList().getItems());
            window.executeCommand("sort name desc");
            previousCommands = commandCount;
            clickGroup(1);
            assertEquals(previousCommands + 1, commandCount);
            assertEquals(List.of(BOB, CARA), studentList().getItems());
        });
    }

    @Test
    public void deletingOtherStudent_preservesDetailsAndRenumbersSelection() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("view 3");
            window.executeCommand("delete 1 confirm");
            assertEquals(CARA, window.getStudentListPanel().getSelectedStudent());
            assertEquals("2. ", label("id").getText());
            assertEquals("Cara Lim", label("name").getText());
            layout();
            assertEquals(Set.of("All Students  (2)", "T01  (1)", "T02  (1)"), groupLabels());
        });
    }

    @Test
    public void markingSelectedStudent_updatesSummaryAndDetailsRetainingFilterAndSort() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("list g/T01");
            window.executeCommand("sort name desc");
            window.executeCommand("view 1");
            window.executeCommand("followup 1 on");
            assertEquals("Cara Lim", label("name").getText());
            assertEquals("Follow-up: Required", label("followUp").getText());
            assertEquals(List.of(CARA.withFollowUp(true), BOB), studentList().getItems());
            assertEquals(1, groupList().getSelectionModel().getSelectedIndex());
            layout();
            ListCell<?> cell = studentCell(0);
            assertTrue(cell.getGraphic().lookup("#followUp").isVisible());
        });
    }

    @Test
    public void commandBox_enterView_opensDetailsAndKeepsInvalidInput() throws Exception {
        runOnFxThread(() -> {
            TextField input = (TextField) lookup("commandTextField");
            input.setText("view 2");
            input.fireEvent(new ActionEvent());
            assertEquals("Alice Lee", label("name").getText());
            assertEquals("", input.getText());
            input.setText("view 99");
            input.fireEvent(new ActionEvent());
            assertEquals("view 99", input.getText());
            assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            assertEquals("Alice Lee", label("name").getText());
        });
    }

    @Test
    public void viewOffscreenStudent_scrollsToCorrectSummaryAfterReorderingAndFiltering() throws Exception {
        runOnFxThread(() -> {
            for (int i = 0; i < 40; i++) {
                model.addStudent(new StudentBuilder().withName("Student " + i + " With A Longer Name")
                        .withMatricNumber(String.format("A%07dZ", i + 100))
                        .withTutorialGroup("T01").build());
            }
            window.executeCommand("view 43");
        });
        assertVisibleStudentAfterPulse(42);
        runOnFxThread(() -> {
            window.executeCommand("sort name desc");
            window.executeCommand("view 43");
        });
        assertVisibleStudentAfterPulse(42);
        runOnFxThread(() -> {
            window.executeCommand("list g/T01");
            window.executeCommand("view 42");
        });
        assertVisibleStudentAfterPulse(41);
        runOnFxThread(() -> window.executeCommand("view 21"));
        assertVisibleStudentAfterPulse(20);
        runOnFxThread(() -> {
            window.executeCommand("view 42");
            window.executeCommand("view 1"); // replace the earlier target before the next pulse
        });
        assertVisibleStudentAfterPulse(0);
    }

    @Test
    public void editingSelectedMatricNumber_retainsDetailsUnlessStudentLeavesSearch() throws Exception {
        runOnFxThread(() -> {
            studentList().getSelectionModel().select(1);
            window.executeCommand("edit 2 m/A1111111X");
            assertEquals("Matric: A1111111X", label("matricNumber").getText());
            assertEquals("Alice Lee", label("name").getText());
            window.executeCommand("find A1111111X");
            window.executeCommand("edit 1 m/A2222222Y");
            assertTrue(studentList().getItems().isEmpty());
            assertFalse(detailsScroll().isVisible());
        });
    }

    @Test
    public void unavailableStorage_filterChanges_keepHighlightConsistentWithResults() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("list g/T01");
            isSaveBlocked = true;
            window.executeCommand("find Alice");
            assertEquals(List.of(ALICE), studentList().getItems());
            assertEquals(-1, groupList().getSelectionModel().getSelectedIndex());
            window.executeCommand("list g/T02");
            assertEquals(2, groupList().getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void longDetails_wrapInsideNarrowColumnAndResetScrollForNextStudent() throws Exception {
        runOnFxThread(() -> {
            window.executeCommand("remark 1 r/" + "Discuss recursion before the next tutorial. ".repeat(100));
            window.executeCommand("remark 2 r/" + "Prepare an example for the next meeting. ".repeat(100));
            window.executeCommand("edit 2 a/" + "A long address ".repeat(100) + " t/" + "LongTag".repeat(60));
            studentList().getSelectionModel().select(0);
            stage.setWidth(960);
            SplitPane workspace = (SplitPane) lookup("workspace");
            workspace.setDividerPositions(0.2, 0.65);
            layout();
            assertTrue(detailsScroll().getContent().getBoundsInLocal().getHeight()
                    > detailsScroll().getViewportBounds().getHeight());
            detailsScroll().setVvalue(1);
            studentList().getSelectionModel().select(1);
            layout();
            assertEquals(0, detailsScroll().getVvalue());
            Label remark = label("remark");
            Label address = label("address");
            Label tag = (Label) detailsScroll().getContent().lookup("#tags").lookup(".label");
            assertTrue(remark.getHeight() > 100);
            assertTrue(address.getHeight() > 100);
            assertTrue(tag.getHeight() > 20, "Long tag height=" + tag.getHeight()
                    + ", width=" + tag.getWidth() + ", prefHeight=" + tag.prefHeight(tag.getWidth())
                    + ", maxWidth=" + tag.getMaxWidth());
            double availableWidth = detailsScroll().getViewportBounds().getWidth();
            assertTrue(remark.getWidth() <= availableWidth);
            assertTrue(address.getWidth() <= availableWidth);
            assertTrue(tag.getWidth() <= availableWidth);
            assertEquals("LongTag".repeat(60), tag.getTooltip().getText());
            detailsScroll().setVvalue(1);
            window.executeCommand("view 2");
            assertEquals(0, detailsScroll().getVvalue());
        });
    }

    @Test
    public void clearAndAdd_refreshesGroupsWithoutKeepingOldDetails() throws Exception {
        runOnFxThread(() -> {
            studentList().getSelectionModel().select(0);
            window.executeCommand("clear");
            layout();
            assertTrue(studentList().getItems().isEmpty());
            assertEquals(Set.of("All Students  (0)"), groupLabels());
            assertEquals("All students", ((Label) lookup("activeFilter")).getText());
            assertFalse(detailsScroll().isVisible());
            window.executeCommand("groups");
            assertEquals("All students", ((Label) lookup("activeFilter")).getText());
            window.executeCommand("add n/New Student m/A9999999Z g/L03");
            layout();
            assertEquals(Set.of("All Students  (1)", "L03  (1)"), groupLabels());
            assertEquals(0, groupList().getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void workspace_resizesAndRendersThreeColumnsWithScrollableDetails() throws Exception {
        runOnFxThread(() -> {
            studentList().getSelectionModel().select(1);
            SplitPane workspace = (SplitPane) lookup("workspace");
            assertEquals(3, workspace.getItems().size());
            assertTrue(((TextArea) lookup("resultDisplay")).isWrapText());
            for (double width : new double[] {960, 1200}) {
                stage.setWidth(width);
                layout();
                for (Node column : workspace.getItems()) {
                    assertTrue(column.getBoundsInParent().getWidth() > 0);
                }
                assertTrue(detailsScroll().isFitToWidth());
                assertTrue(((Label) detailsScroll().getContent().lookup("#remark")).isWrapText());
            }
            writePreview();
        });
    }

    private void layout() {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private Node lookup(String id) {
        return stage.getScene().lookup("#" + id);
    }

    private Label label(String id) {
        return (Label) detailsScroll().getContent().lookup("#" + id);
    }

    @SuppressWarnings("unchecked")
    private ListView<Student> studentList() {
        return (ListView<Student>) lookup("studentListView");
    }

    private ListView<?> groupList() {
        return (ListView<?>) lookup("groupListView");
    }

    private ScrollPane detailsScroll() {
        return (ScrollPane) lookup("detailsScroll");
    }

    private void clickGroup(int index) {
        layout();
        ListCell<?> cell = groupList().lookupAll(".list-cell").stream().filter(ListCell.class::isInstance)
                .map(ListCell.class::cast).filter(candidate -> candidate.getIndex() == index).findFirst().orElseThrow();
        fireMouseEvent(cell, MouseEvent.MOUSE_PRESSED, true);
        fireMouseEvent(cell, MouseEvent.MOUSE_RELEASED, false);
        fireMouseEvent(cell, MouseEvent.MOUSE_CLICKED, false);
    }

    private void fireMouseEvent(Node target, EventType<MouseEvent> type, boolean isPressed) {
        target.fireEvent(new MouseEvent(target, target, type, 5, 5, 5, 5, MouseButton.PRIMARY, 1,
                false, false, false, false, isPressed, false, false, false, false, true,
                new PickResult(target, new Point3D(5, 5, 0), 1)));
    }

    private ListCell<?> studentCell(int index) {
        String visibleIndices = studentList().lookupAll(".list-cell").stream().filter(ListCell.class::isInstance)
                .map(ListCell.class::cast).filter(cell -> !cell.isEmpty())
                .map(cell -> Integer.toString(cell.getIndex())).collect(Collectors.joining(", "));
        return studentList().lookupAll(".list-cell").stream().filter(ListCell.class::isInstance)
                .map(ListCell.class::cast).filter(cell -> cell.getIndex() == index && !cell.isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("Missing rendered row " + index
                        + "; rendered rows=" + visibleIndices + "; list height=" + studentList().getHeight()
                        + "; selected=" + studentList().getSelectionModel().getSelectedIndex()
                        + "; filter=" + logic.getFilterDescription()));
    }

    private void assertVisibleStudent(int index) {
        ListCell<?> cell = studentCell(index);
        var bounds = studentList().sceneToLocal(cell.localToScene(cell.getBoundsInLocal()));
        assertTrue(bounds.getMaxY() > 0 && bounds.getMinY() < studentList().getHeight());
        assertEquals((index + 1) + ".", ((Label) cell.getGraphic().lookup("#id")).getText());
        Student expected = logic.getFilteredStudentList().get(index);
        assertEquals(expected.getName().fullName, ((Label) cell.getGraphic().lookup("#name")).getText());
        assertEquals(expected.getName().fullName, label("name").getText());
    }

    private void assertVisibleStudentAfterPulse(int index) throws Exception {
        // VirtualFlow can need multiple pulses to measure wrapped rows after a filter change.
        // Wait for the observable viewport state, with a deadline, rather than a fixed delay.
        CountDownLatch laidOut = new CountDownLatch(1);
        Runnable listener = () -> {
            boolean visible = studentList().lookupAll(".list-cell").stream().filter(ListCell.class::isInstance)
                    .map(ListCell.class::cast).filter(cell -> cell.getIndex() == index && !cell.isEmpty())
                    .anyMatch(cell -> {
                        var bounds = studentList().sceneToLocal(cell.localToScene(cell.getBoundsInLocal()));
                        return bounds.getMaxY() > 0 && bounds.getMinY() < studentList().getHeight();
                    });
            if (visible) {
                laidOut.countDown();
            } else {
                Platform.requestNextPulse();
            }
        };
        runOnFxThread(() -> {
            stage.getScene().addPostLayoutPulseListener(listener);
            Platform.requestNextPulse();
        });
        try {
            laidOut.await(5, TimeUnit.SECONDS);
            runOnFxThread(() -> assertVisibleStudent(index));
        } finally {
            runOnFxThread(() -> stage.getScene().removePostLayoutPulseListener(listener));
        }
    }

    private Set<String> groupLabels() {
        return groupList().lookupAll(".list-cell").stream().filter(ListCell.class::isInstance)
                .map(ListCell.class::cast).filter(cell -> !cell.isEmpty()).map(ListCell::getText)
                .collect(Collectors.toSet());
    }

    private void writePreview() throws Exception {
        WritableImage image = stage.getScene().snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        preview.setRGB(0, 0, width, height, pixels, 0, width);
        Path output = Path.of("build", "reports", "ui", "three-column-ui.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(preview, "png", output.toFile());
    }
}
