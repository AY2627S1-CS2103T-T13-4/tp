package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.StudentDirectory;
import seedu.address.testutil.TypicalStudents;

public class JsonSerializableStudentDirectoryTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data",
            "JsonSerializableStudentDirectoryTest");
    private static final Path TYPICAL_STUDENTS_FILE = TEST_DATA_FOLDER.resolve("typicalStudents.json");
    private static final Path INVALID_STUDENT_FILE = TEST_DATA_FOLDER.resolve("invalidStudent.json");
    private static final Path DUPLICATE_STUDENT_FILE = TEST_DATA_FOLDER.resolve("duplicateStudents.json");

    @Test
    public void toModelType_legacyPersonsProperty_success() throws Exception {
        JsonSerializableStudentDirectory dataFromFile = JsonUtil.readJsonFile(TYPICAL_STUDENTS_FILE,
                JsonSerializableStudentDirectory.class).get();
        StudentDirectory studentDirectoryFromFile = dataFromFile.toModelType();
        StudentDirectory typicalStudentDirectory = TypicalStudents.getTypicalStudentDirectory();
        assertEquals(studentDirectoryFromFile, typicalStudentDirectory);
    }

    @Test
    public void toModelType_invalidStudentFile_throwsIllegalValueException() throws Exception {
        JsonSerializableStudentDirectory dataFromFile = JsonUtil.readJsonFile(INVALID_STUDENT_FILE,
                JsonSerializableStudentDirectory.class).get();
        assertThrows(IllegalValueException.class, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicateStudents_throwsIllegalValueException() throws Exception {
        JsonSerializableStudentDirectory dataFromFile = JsonUtil.readJsonFile(DUPLICATE_STUDENT_FILE,
                JsonSerializableStudentDirectory.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableStudentDirectory.MESSAGE_DUPLICATE_STUDENTS,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_missingStudentList_throwsIllegalValueException() {
        JsonSerializableStudentDirectory studentDirectory = new JsonSerializableStudentDirectory(null, null);

        assertThrows(IllegalValueException.class, JsonSerializableStudentDirectory.MESSAGE_MISSING_STUDENTS,
                studentDirectory::toModelType);
    }

}
