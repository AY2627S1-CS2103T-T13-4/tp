package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.logic.Logic;
import tutorlink.logic.LogicManager;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.ModelManager;
import tutorlink.model.student.Student;
import tutorlink.storage.JsonStudentDirectoryStorage;
import tutorlink.storage.JsonUserPrefsStorage;
import tutorlink.storage.StorageManager;
import tutorlink.testutil.StudentBuilder;

/** Verifies contact edits through parsing, sorted/filtered models, and persisted records. */
public class ContactDetailsIntegrationTest {
    private static final Student ALICE = new StudentBuilder().withName("Alice Tan")
            .withMatricNumber("A0000001A").build();
    private static final Student ZOE = new StudentBuilder().withName("Zoe Tan")
            .withMatricNumber("A0000002B").withRemark("Consultation pending")
            .withFollowUp(true).withTags("needsHelp").build();

    @TempDir
    public Path temporaryFolder;

    private ModelManager model;
    private Logic logic;
    private JsonStudentDirectoryStorage storage;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        model.addStudent(ALICE);
        model.addStudent(ZOE);
        storage = new JsonStudentDirectoryStorage(temporaryFolder.resolve("students.json"));
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        logic.execute("list g/T01");
        logic.execute("sort name desc");
    }

    @Test
    public void execute_clearContacts_usesDisplayedIndexAndPersistsUnrelatedDetails() throws Exception {
        logic.execute("edit 1 p/");
        Student phoneCleared = model.getFilteredStudentList().getFirst();
        assertNull(phoneCleared.getPhone());
        assertEquals(ZOE.getEmail(), phoneCleared.getEmail());
        assertEquals(ZOE.getAddress(), phoneCleared.getAddress());
        logic.execute("edit 1 e/ a/");
        Student cleared = new Student(ZOE.getName(), ZOE.getMatricNumber(), ZOE.getTutorialGroup(),
                null, null, null, ZOE.getTags(), ZOE.getRemark(), ZOE.requiresFollowUp());
        assertEquals(List.of(cleared, ALICE), model.getFilteredStudentList());
        assertEquals(List.of(ALICE, cleared), storage.readStudentDirectory().orElseThrow().getStudentList());
        logic.execute("edit 1 p/ e/ a/"); // repeating a clear is harmless
        assertEquals(cleared, model.getFilteredStudentList().getFirst());
        logic.execute("edit 1 n/Zoe Lim"); // omitted contacts remain absent
        assertNull(model.getFilteredStudentList().getFirst().getPhone());
        logic.execute("edit 1 p/91234567 e/student+_tutorial@example.com a/10 College Road");
        assertEquals("student+_tutorial@example.com", model.getFilteredStudentList().getFirst().getEmail().value);
        assertEquals(ZOE.getRemark(), model.getFilteredStudentList().getFirst().getRemark());
    }

    @Test
    public void execute_invalidEmailWithClear_isAtomicAndKeepsCurrentResults() throws Exception {
        Path path = temporaryFolder.resolve("students.json");
        String before = Files.readString(path);
        assertThrows(ParseException.class, () -> logic.execute("edit 1 p/ e/" + "a".repeat(65) + "@example.com"));
        assertEquals(List.of(ZOE, ALICE), model.getFilteredStudentList());
        assertEquals(before, Files.readString(path));
    }

    @Test
    public void execute_addEmailBoundary_rejectsOversizedInputWithoutAddingStudent() throws Exception {
        logic.execute("add n/Valid Boundary m/A0000003C g/T02 e/" + "a".repeat(64) + "@example.com");
        List<Student> before = List.copyOf(model.getStudentDirectory().getStudentList());
        assertThrows(ParseException.class, () -> logic.execute(
                "add n/Invalid Boundary m/A0000004D g/T02 e/" + "a".repeat(65) + "@example.com"));
        assertEquals(before, model.getStudentDirectory().getStudentList());
        assertEquals(before, storage.readStudentDirectory().orElseThrow().getStudentList());
    }
}
