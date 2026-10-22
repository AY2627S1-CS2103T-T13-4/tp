package tutorlink.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static tutorlink.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static tutorlink.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static tutorlink.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static tutorlink.logic.commands.CommandTestUtil.MATRIC_NUMBER_DESC_AMY;
import static tutorlink.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static tutorlink.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static tutorlink.logic.commands.CommandTestUtil.TUTORIAL_GROUP_DESC_AMY;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.AMY;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.logic.commands.AddCommand;
import tutorlink.logic.commands.CommandResult;
import tutorlink.logic.commands.EditCommand;
import tutorlink.logic.commands.ListCommand;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.ReadOnlyStudentDirectory;
import tutorlink.model.StudentDirectory;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.StudentBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonStudentDirectoryStorage studentDirectoryStorage =
                new JsonStudentDirectoryStorage(temporaryFolder.resolve("studentDirectory.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(studentDirectoryStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_editMatricNumberAndGroup_persistsAcrossRestart() throws Exception {
        Path studentDirectoryPath = temporaryFolder.resolve("studentDirectory.json");
        logic.execute("add n/Amy Bee m/A0000011K g/T01 t/friend");
        logic.execute("list g/t01");

        logic.execute("edit 1 m/a1234567z g/l03");

        assertEquals(0, logic.getFilteredStudentList().size());
        Student editedStudent = model.getStudentDirectory().getStudentList().get(0);
        assertEquals("A1234567Z", editedStudent.getMatricNumber().toString());
        assertEquals("L03", editedStudent.getTutorialGroup().toString());
        assertEquals(1, editedStudent.getTags().size());

        ReadOnlyStudentDirectory restored = new JsonStudentDirectoryStorage(studentDirectoryPath)
                .readStudentDirectory().get();
        assertEquals(model.getStudentDirectory(), restored);
        assertEquals(editedStudent, restored.getStudentList().get(0));

        Model restartedModel = new ModelManager(restored, new UserPrefs());
        assertEquals(editedStudent, restartedModel.getFilteredStudentList().get(0));
    }

    @Test
    public void execute_invalidOrDuplicateEdit_filteredListAndRecordsUnchanged() throws Exception {
        logic.execute("add n/Amy Bee m/A0000011K g/T01");
        logic.execute("add n/Bob Choo m/A0000012L g/L03");
        logic.execute("list g/T01");
        ReadOnlyStudentDirectory originalStudentDirectory = new StudentDirectory(model.getStudentDirectory());
        List<Student> originalList = new ArrayList<>(logic.getFilteredStudentList());

        assertThrows(ParseException.class, () -> logic.execute("edit 1 g/T123"));
        assertEquals(originalStudentDirectory, model.getStudentDirectory());
        assertEquals(originalList, logic.getFilteredStudentList());

        assertThrows(CommandException.class, EditCommand.MESSAGE_DUPLICATE_STUDENT, () -> logic.execute(
                "edit 1 m/a0000012l"));
        assertEquals(originalStudentDirectory, model.getStudentDirectory());
        assertEquals(originalList, logic.getFilteredStudentList());
        assertEquals(new TutorialGroup("T01"), logic.getFilteredStudentList().get(0).getTutorialGroup());
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, DUMMY_AD_EXCEPTION.getMessage()));
    }

    @Test
    public void getFilteredStudentList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredStudentList().remove(0));
    }

    @Test
    public void getStudentList_filterChanges_retainsCompleteReadOnlyRoster() throws Exception {
        logic.execute("add n/Amy Bee m/A0000011K g/T01");
        var completeRoster = logic.getStudentList();
        logic.execute("list g/T99");
        assertEquals(0, logic.getFilteredStudentList().size());
        assertEquals(1, completeRoster.size());
        assertThrows(UnsupportedOperationException.class, () -> completeRoster.clear());
        logic.execute("add n/Bob Choo m/A0000012L g/T02");
        assertEquals(2, completeRoster.size());
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getStudentDirectory(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonStudentDirectoryStorage that throws the IOException e when saving
        JsonStudentDirectoryStorage studentDirectoryStorage = new JsonStudentDirectoryStorage(prefPath) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory studentDirectory) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(studentDirectoryStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveStudentDirectory method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + MATRIC_NUMBER_DESC_AMY
                + TUTORIAL_GROUP_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        Student expectedStudent = new StudentBuilder(AMY).withTags().build();
        ModelManager expectedModel = new ModelManager();
        expectedModel.addStudent(expectedStudent);
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }
}
