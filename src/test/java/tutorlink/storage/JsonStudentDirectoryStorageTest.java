package tutorlink.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.HOON;
import static tutorlink.testutil.TypicalStudents.IDA;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorlink.commons.exceptions.DataLoadingException;
import tutorlink.model.ReadOnlyStudentDirectory;
import tutorlink.model.StudentDirectory;
import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;

public class JsonStudentDirectoryStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonStudentDirectoryStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readStudentDirectory_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readStudentDirectory(null));
    }

    private java.util.Optional<ReadOnlyStudentDirectory> readStudentDirectory(String filePath) throws Exception {
        return new JsonStudentDirectoryStorage(Paths.get(filePath))
                .readStudentDirectory(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readStudentDirectory("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readStudentDirectory("notJsonFormat.json"));
    }

    @Test
    public void readStudentDirectory_invalidStudentFile_throwsDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readStudentDirectory("invalidStudent.json"));
    }

    @Test
    public void readStudentDirectory_invalidAndValidStudentsFile_throwsDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readStudentDirectory("invalidAndValidStudents.json"));
    }

    @Test
    public void readStudentDirectory_nullOrMissingStudents_preservesInvalidFile() throws Exception {
        Path filePath = testFolder.resolve("invalid-structure.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);
        for (String content : List.of("{}", "{\"students\":null}", "{\"students\":[null]}")) {
            Files.writeString(filePath, content);
            assertThrows(DataLoadingException.class, storage::readStudentDirectory);
            assertEquals(content, Files.readString(filePath));
        }
    }

    @Test
    public void readAndSaveStudentDirectory_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempStudentDirectory.json");
        StudentDirectory original = getTypicalStudentDirectory();
        JsonStudentDirectoryStorage jsonStudentDirectoryStorage = new JsonStudentDirectoryStorage(filePath);

        // Save in new file and read back
        jsonStudentDirectoryStorage.saveStudentDirectory(original, filePath);
        String savedJson = Files.readString(filePath);
        assertTrue(savedJson.contains("\"students\""));
        ReadOnlyStudentDirectory readBack = jsonStudentDirectoryStorage.readStudentDirectory(filePath).get();
        assertEquals(original, new StudentDirectory(readBack));

        // Modify data, overwrite existing file, and read back
        original.addStudent(HOON);
        original.removeStudent(ALICE);
        jsonStudentDirectoryStorage.saveStudentDirectory(original, filePath);
        readBack = jsonStudentDirectoryStorage.readStudentDirectory(filePath).get();
        assertEquals(original, new StudentDirectory(readBack));

        // Save and read without specifying file path
        original.addStudent(IDA);
        jsonStudentDirectoryStorage.saveStudentDirectory(original); // file path not specified
        readBack = jsonStudentDirectoryStorage.readStudentDirectory().get(); // file path not specified
        assertEquals(original, new StudentDirectory(readBack));

    }

    @Test
    public void readAndSaveStudentDirectory_missingOptionalDetails_roundTrips() throws Exception {
        Path filePath = testFolder.resolve("OptionalStudent.json");
        StudentDirectory original = new StudentDirectory();
        Student student = new Student(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("W12"), null, null, null, Set.of());
        original.addStudent(student);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);

        storage.saveStudentDirectory(original);
        assertEquals(original, new StudentDirectory(storage.readStudentDirectory().get()));
    }

    @Test
    public void readAndSaveStudentDirectory_existingRemark_roundTrips() throws Exception {
        Path filePath = testFolder.resolve("StudentWithRemark.json");
        StudentDirectory original = new StudentDirectory();
        Student student = new Student(new Name("Alice Tan"), new MatricNumber("A0123456X"),
                new TutorialGroup("W12"), null, null, null, Set.of(), "Needs help with recursion");
        original.addStudent(student);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(filePath);

        storage.saveStudentDirectory(original);
        ReadOnlyStudentDirectory readBack = storage.readStudentDirectory().get();
        assertEquals("Needs help with recursion", readBack.getStudentList().get(0).getRemark());
        assertEquals(original, new StudentDirectory(readBack));
    }

    @Test
    public void saveStudentDirectory_nullStudentDirectory_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveStudentDirectory(null, "SomeFile.json"));
    }

    /**
     * Saves {@code studentDirectory} at the specified {@code filePath}.
     */
    private void saveStudentDirectory(ReadOnlyStudentDirectory studentDirectory, String filePath) {
        try {
            new JsonStudentDirectoryStorage(Paths.get(filePath))
                    .saveStudentDirectory(studentDirectory, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveStudentDirectory_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveStudentDirectory(new StudentDirectory(), null));
    }
}
