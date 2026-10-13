package seedu.address;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyStudentDirectory;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.DataFileMigrator;
import seedu.address.storage.JsonStudentDirectoryStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.ui.Ui;
import seedu.address.ui.UiManager;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final String VERSION = "V0.5.1";

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);
    private static final Path USER_PREFS_FILE_PATH = Paths.get("preferences.json");
    private static final Path DATA_FILE_PATH = Paths.get("data", "tutorlink.json");
    private static final Path LEGACY_DATA_FILE_PATH = Paths.get("data", "addressbook.json");

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    private String dataLoadingError;

    @Override
    public void init() throws Exception {
        logger.info("==============================[ Initializing TutorLink ]============================");
        super.init();

        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(USER_PREFS_FILE_PATH);
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        JsonStudentDirectoryStorage studentDirectoryStorage = new JsonStudentDirectoryStorage(DATA_FILE_PATH);
        storage = new StorageManager(studentDirectoryStorage, userPrefsStorage);

        try {
            DataFileMigrator.migrateStudentDirectory(LEGACY_DATA_FILE_PATH, DATA_FILE_PATH);
            model = initModelManager(storage, userPrefs);
        } catch (DataLoadingException | IOException e) {
            dataLoadingError = "Legacy data file at " + LEGACY_DATA_FILE_PATH
                    + " could not be migrated to " + DATA_FILE_PATH
                    + ". The original file was not changed; startup has stopped to protect it.";
            logger.severe(dataLoadingError);
            return;
        } catch (IllegalStateException e) {
            dataLoadingError = e.getMessage();
            return;
        }

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic, storage.getStudentDirectoryFilePath());
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s student directory and {@code userPrefs}. <br>
     * The data from the sample student directory will be used if {@code storage}'s student directory is not found.
     * An invalid existing data file prevents startup so it cannot be overwritten by an empty student directory.
     */
    static Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        logger.info("Using data file : " + storage.getStudentDirectoryFilePath());

        Optional<ReadOnlyStudentDirectory> studentDirectoryOptional;
        ReadOnlyStudentDirectory initialData;
        try {
            studentDirectoryOptional = storage.readStudentDirectory();
            if (studentDirectoryOptional.isEmpty()) {
                logger.info("Creating a new data file " + storage.getStudentDirectoryFilePath()
                        + " populated with sample TutorLink data.");
            }
            initialData = studentDirectoryOptional.orElseGet(SampleDataUtil::getSampleStudentDirectory);
        } catch (DataLoadingException e) {
            String message = "Data file at " + storage.getStudentDirectoryFilePath()
                    + " could not be loaded. The original file was not changed; startup has stopped to protect it.";
            logger.severe(message);
            throw new IllegalStateException(message, e);
        }

        return new ModelManager(initialData, userPrefs);
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(JsonUserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using preference file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            if (prefsOptional.isEmpty()) {
                logger.info("Creating new preference file " + prefsFilePath);
            }
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataLoadingException e) {
            logger.warning("Preference file at " + prefsFilePath + " could not be loaded."
                    + " Using default preferences.");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save preference file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        if (dataLoadingError != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to load student data");
            alert.setHeaderText("TutorLink did not change your data file");
            alert.setContentText(dataLoadingError);
            alert.showAndWait();
            Platform.exit();
            return;
        }

        logger.info("Starting TutorLink " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================== [ Stopping TutorLink ] ==============================");
        if (model == null) {
            return;
        }
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
