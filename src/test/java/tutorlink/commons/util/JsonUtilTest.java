package tutorlink.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import tutorlink.testutil.SerializableTestClass;
import tutorlink.testutil.TestUtil;

/**
 * Tests JSON Read and Write
 */
public class JsonUtilTest {

    private static final Path SERIALIZATION_FILE = TestUtil.getFilePathInSandboxFolder("serialize.json");

    @Test
    public void serializeObjectToJsonFile_noExceptionThrown() throws IOException {
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        serializableTestClass.setTestValues();

        JsonUtil.serializeObjectToJsonFile(SERIALIZATION_FILE, serializableTestClass);

        assertEquals(FileUtil.readFromFile(SERIALIZATION_FILE), SerializableTestClass.JSON_STRING_REPRESENTATION);
    }

    @Test
    public void deserializeObjectFromJsonFile_noExceptionThrown() throws IOException {
        FileUtil.writeToFile(SERIALIZATION_FILE, SerializableTestClass.JSON_STRING_REPRESENTATION);

        SerializableTestClass serializableTestClass = JsonUtil
                .deserializeObjectFromJsonFile(SERIALIZATION_FILE, SerializableTestClass.class);

        assertEquals(serializableTestClass.getName(), SerializableTestClass.getNameTestValue());
        assertEquals(serializableTestClass.getListOfLocalDateTimes(), SerializableTestClass.getListTestValues());
        assertEquals(serializableTestClass.getMapOfIntegerToString(), SerializableTestClass.getHashMapTestValues());
    }

    @Test
    public void fromJsonString_trailingContent_throwsIoException() {
        for (String suffix : List.of("{}", "[]", "null", "true", "42", "\"extra\"", "NOT JSON", "{")) {
            String json = SerializableTestClass.JSON_STRING_REPRESENTATION + "\n" + suffix;
            assertThrows(IOException.class, () -> JsonUtil.fromJsonString(json, SerializableTestClass.class));
        }
    }

    @Test
    public void fromJsonString_trailingWhitespace_readsObject() throws IOException {
        String json = SerializableTestClass.JSON_STRING_REPRESENTATION + " \t\r\n";

        SerializableTestClass actual = JsonUtil.fromJsonString(json, SerializableTestClass.class);

        assertEquals(SerializableTestClass.getNameTestValue(), actual.getName());
        assertEquals(SerializableTestClass.getListTestValues(), actual.getListOfLocalDateTimes());
        assertEquals(SerializableTestClass.getHashMapTestValues(), actual.getMapOfIntegerToString());
    }

    //TODO: @Test jsonUtil_writeThenReadObjectToJson_correctObject()
}
