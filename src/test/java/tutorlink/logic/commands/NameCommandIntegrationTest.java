package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.logic.Logic;
import tutorlink.logic.LogicManager;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.ModelManager;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.Student;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;

/**
 * Verifies names through command parsing, execution, whole-word searches, and JSON persistence.
 */
public class NameCommandIntegrationTest {
    private static final Map<String, String> NAME_SEARCH_CASES = Map.of(
            "John O'Connor", "O'Connor",
            "Sean O’Neil", "O’Neil",
            "Mary-Jane Tan", "Mary-Jane",
            "Ravi s/o Kumar", "s/o",
            "Anita d/o Devi", "d/o",
            "José Tan", "José",
            "Jose\u0301 Tan", "Jose\u0301",
            "王伟", "王伟",
            "J. Tan", "J.",
            "12345", "12345");

    @TempDir
    public Path temporaryFolder;

    private ModelManager model;
    private Logic logic;
    private Path dataPath;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        dataPath = temporaryFolder.resolve("students.json");
        logic = createLogic(new JsonStudentDirectoryStorage(dataPath));
    }

    @Test
    public void execute_addRepresentativeNames_searchesAndReloads() throws Exception {
        int matricNumber = 1;
        for (Map.Entry<String, String> entry : NAME_SEARCH_CASES.entrySet()) {
            logic.execute("add n/  " + entry.getKey().replace(" ", "   ")
                    + String.format("   m/A%07dB g/T01", matricNumber++));
            Student added = model.getStudentDirectory().getStudentList().getLast();
            assertEquals(entry.getKey(), added.getName().fullName);
            assertSearch(entry.getValue(), added);
            reloadAndAssertDirectory();
            assertSearch(entry.getValue(), added);
        }
        assertEquals(NAME_SEARCH_CASES.size(), model.getStudentDirectory().getStudentList().size());
    }

    @Test
    public void execute_editRepresentativeNames_searchesAndReloads() throws Exception {
        logic.execute("add n/Initial Name m/A0000001B g/T01");
        for (Map.Entry<String, String> entry : NAME_SEARCH_CASES.entrySet()) {
            Student previous = model.getStudentDirectory().getStudentList().getFirst();
            logic.execute("edit 1 n/  " + entry.getKey().replace(" ", "   ") + "  ");
            Student edited = model.getStudentDirectory().getStudentList().getFirst();
            assertEquals(entry.getKey(), edited.getName().fullName);
            assertEquals(previous.getMatricNumber(), edited.getMatricNumber());
            assertSearch(entry.getValue(), edited);
            reloadAndAssertDirectory();
            assertSearch(entry.getValue(), edited);
        }
    }

    @Test
    public void execute_invalidNames_rejectsAddAndEditWithoutChangingData() throws Exception {
        logic.execute("add n/Original Name m/A0000001B g/T01");
        List<Student> original = List.copyOf(model.getStudentDirectory().getStudentList());
        String saved = Files.readString(dataPath);
        for (String name : List.of("", "   ", "'’-/.", "\u0301", "Peter*", "Peter\tJack", "Peter\nJack",
                "Peter\u0000Jack", "Peter\u007fJack", "é".repeat(71))) {
            assertThrows(ParseException.class, () -> logic.execute("add n/" + name + " m/A0000002B g/T01"));
            assertThrows(ParseException.class, () -> logic.execute("edit 1 n/" + name));
            assertEquals(original, model.getStudentDirectory().getStudentList());
            assertEquals(saved, Files.readString(dataPath));
        }
    }

    @Test
    public void execute_nameLengthBoundary_acceptsAddAndEditAfterNormalization() throws Exception {
        String name = "é".repeat(70);
        logic.execute("add n/  " + name + "  m/A0000001B g/T01");
        assertEquals(name, model.getStudentDirectory().getStudentList().getFirst().getName().fullName);
        name = "e\u0301".repeat(35);
        logic.execute("edit 1 n/  " + name + "  ");
        assertEquals(name, model.getStudentDirectory().getStudentList().getFirst().getName().fullName);
        reloadAndAssertDirectory();
    }

    private Logic createLogic(JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
    }

    private void assertSearch(String keyword, Student expected) throws Exception {
        assertEquals("1 student(s) listed!", logic.execute("find " + keyword).getFeedbackToUser());
        assertEquals(List.of(expected), model.getFilteredStudentList());
    }

    private void reloadAndAssertDirectory() throws Exception {
        List<Student> expected = List.copyOf(model.getStudentDirectory().getStudentList());
        JsonStudentDirectoryStorage reloadedStorage = new JsonStudentDirectoryStorage(dataPath);
        model = new ModelManager(reloadedStorage.readStudentDirectory().orElseThrow(), new UserPrefs());
        logic = createLogic(reloadedStorage);
        assertEquals(expected, model.getStudentDirectory().getStudentList());
    }
}
