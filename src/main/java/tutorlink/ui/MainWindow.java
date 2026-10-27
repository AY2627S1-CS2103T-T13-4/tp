package tutorlink.ui;

import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Logger;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tutorlink.commons.core.GuiSettings;
import tutorlink.commons.core.LogsCenter;
import tutorlink.logic.Logic;
import tutorlink.logic.commands.CommandResult;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.student.Student;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private Path dataFilePath;

    // Independent Ui parts residing in this Ui container
    private StudentListPanel studentListPanel;
    private GroupListPanel groupListPanel;
    private StudentDetailsPanel studentDetailsPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane studentListPanelPlaceholder;

    @FXML
    private StackPane groupListPanelPlaceholder;

    @FXML
    private StackPane studentDetailsPanelPlaceholder;

    @FXML
    private Label studentCount;

    @FXML
    private Label activeFilter;

    @FXML
    private SplitPane workspace;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the help window purposely so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        resultDisplay = new ResultDisplay();
        resultDisplay.setFeedbackToUser("Ready. Showing all students.");
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        studentDetailsPanel = new StudentDetailsPanel();
        studentDetailsPanelPlaceholder.getChildren().add(studentDetailsPanel.getRoot());

        studentListPanel = new StudentListPanel(logic.getFilteredStudentList());
        studentListPanelPlaceholder.getChildren().add(studentListPanel.getRoot());
        studentListPanel.selectedStudentProperty().addListener((observable, oldStudent, newStudent) -> updateDetails());
        logic.getFilteredStudentList().addListener((ListChangeListener<Student>) change -> updateStudentCount());
        updateStudentCount();

        groupListPanel = new GroupListPanel(logic.getStudentList(), this::selectGroup);
        groupListPanelPlaceholder.getChildren().add(groupListPanel.getRoot());
        workspace.setDividerPositions(0.19, 0.52);

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(Math.max(primaryStage.getMinHeight(), guiSettings.getWindowHeight()));
        primaryStage.setWidth(Math.max(primaryStage.getMinWidth(), guiSettings.getWindowWidth()));
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    public StudentListPanel getStudentListPanel() {
        return studentListPanel;
    }

    private void updateStudentCount() {
        studentCount.setText(logic.getFilteredStudentList().size() + " displayed");
        activeFilter.setText(logic.getFilterDescription());
        activeFilter.setTooltip(new javafx.scene.control.Tooltip(logic.getFilterDescription()));
    }

    private void updateDetails() {
        Student selected = studentListPanel.getSelectedStudent();
        int index = selected == null ? -1 : logic.getFilteredStudentList().indexOf(selected);
        studentDetailsPanel.showStudent(index < 0 ? null : selected, index + 1);
    }

    private void selectGroup(String group) {
        try {
            executeCommand(group.isEmpty() ? "list" : "list g/" + group);
        } catch (CommandException | ParseException e) {
            // executeCommand already displays the failure in the shared feedback panel.
            logger.warning("Could not select tutorial group: " + e.getMessage());
        }
    }

    private void updateGroupHighlight(String commandText) {
        String[] parts = commandText.trim().split("\\s+", 2);
        switch (parts[0]) {
            case "list" -> {
                String group = parts.length == 1 ? "" : parts[1].trim().substring(2).trim();
                groupListPanel.showGroup(group.toUpperCase(Locale.ROOT));
            }
            case "add", "clear" -> groupListPanel.showGroup("");
            case "find", "findtag", "followups" -> groupListPanel.showGroup(null);
            default -> { }
        }
    }

    /**
     * Executes the command and returns the result.
     *
     * @see tutorlink.logic.Logic#execute(String)
     */
    CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        Student previousSelection = studentListPanel.getSelectedStudent();
        String previousGroup = groupListPanel.getSelectedGroup();
        int selectedDirectoryIndex = logic.getStudentList().indexOf(previousSelection);
        int previousRosterSize = logic.getStudentList().size();
        Student requestedSelection = null;
        try {
            CommandResult commandResult = logic.execute(commandText);
            requestedSelection = commandResult.getStudentToView().orElse(null);
            updateGroupHighlight(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            groupListPanel.showGroup(previousGroup);
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        } finally {
            updateStudentCount();
            if (requestedSelection != null) {
                studentListPanel.showStudent(requestedSelection);
            } else {
                if (selectedDirectoryIndex >= 0 && previousRosterSize == logic.getStudentList().size()) {
                    // Record replacements retain directory positions, including edits to a matriculation number.
                    previousSelection = logic.getStudentList().get(selectedDirectoryIndex);
                }
                studentListPanel.restoreSelection(previousSelection);
            }
            updateDetails();
        }
    }
}
