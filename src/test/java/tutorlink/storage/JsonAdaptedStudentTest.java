package tutorlink.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tutorlink.storage.JsonAdaptedStudent.MISSING_FIELD_MESSAGE_FORMAT;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.BENSON;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import tutorlink.commons.exceptions.IllegalValueException;
import tutorlink.model.student.Address;
import tutorlink.model.student.Email;
import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Phone;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;

public class JsonAdaptedStudentTest {
    private static final String INVALID_NAME = "R@chel";
    private static final String INVALID_MATRIC_NUMBER = "A123X";
    private static final String INVALID_TUTORIAL_GROUP = "T123";
    private static final String INVALID_PHONE = "+651234";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_TAG = "#friend";

    private static final String VALID_NAME = BENSON.getName().toString();
    private static final String VALID_MATRIC_NUMBER = BENSON.getMatricNumber().toString();
    private static final String VALID_TUTORIAL_GROUP = BENSON.getTutorialGroup().toString();
    private static final String VALID_PHONE = BENSON.getPhone().toString();
    private static final String VALID_EMAIL = BENSON.getEmail().toString();
    private static final String VALID_ADDRESS = BENSON.getAddress().toString();
    private static final List<JsonAdaptedTag> VALID_TAGS = BENSON.getTags().stream()
            .map(JsonAdaptedTag::new)
            .collect(Collectors.toList());

    @Test
    public void toModelType_followUpStatus_roundTripsBothStatesWithRemarkAndTags() throws Exception {
        for (boolean requiresFollowUp : new boolean[]{true, false}) {
            Student original = BENSON.withRemark("Retain context").withFollowUp(requiresFollowUp);
            assertEquals(original, new JsonAdaptedStudent(original).toModelType());
        }
    }

    @Test
    public void toModelType_nullFollowUpStatus_defaultsToFalseAndKeepsOtherFields() throws Exception {
        JsonAdaptedStudent adapted = new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, "Retain context", VALID_TAGS, null);
        Student student = adapted.toModelType();
        assertFalse(student.requiresFollowUp());
        assertEquals(BENSON.withRemark("Retain context"), student);
    }

    @Test
    public void toModelType_validStudentDetails_returnsStudent() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(BENSON);
        assertEquals(BENSON, student.toModelType());
    }

    @Test
    public void toModelType_invalidName_throwsIllegalValueException() {
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(INVALID_NAME, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                        VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Name.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_nullName_throwsIllegalValueException() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(null, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_invalidMatricNumber_throwsIllegalValueException() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, INVALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        assertThrows(IllegalValueException.class, MatricNumber.MESSAGE_CONSTRAINTS, student::toModelType);
    }

    @Test
    public void toModelType_nullMatricNumber_throwsIllegalValueException() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, null, VALID_TUTORIAL_GROUP,
                VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, MatricNumber.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_invalidTutorialGroup_throwsIllegalValueException() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, INVALID_TUTORIAL_GROUP,
                VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        assertThrows(IllegalValueException.class, TutorialGroup.MESSAGE_CONSTRAINTS, student::toModelType);
    }

    @Test
    public void toModelType_nullTutorialGroup_throwsIllegalValueException() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, null,
                VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, TutorialGroup.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_invalidPhone_throwsIllegalValueException() {
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                        INVALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Phone.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_nullOptionalDetails_returnsStudent() throws Exception {
        JsonAdaptedStudent adaptedStudent = new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER,
                VALID_TUTORIAL_GROUP,
                null, null, null, null);
        Student student = adaptedStudent.toModelType();
        assertNull(student.getPhone());
        assertNull(student.getEmail());
        assertNull(student.getAddress());
        assertNull(student.getRemark());
        assertEquals(0, student.getTags().size());
    }

    @Test
    public void toModelType_existingRemark_retainsValue() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                null, null, null, "Needs help with recursion", null);

        assertEquals("Needs help with recursion", student.toModelType().getRemark());
    }

    @Test
    public void toModelType_invalidEmail_throwsIllegalValueException() {
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                        VALID_PHONE, INVALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Email.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_invalidAddress_throwsIllegalValueException() {
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                        VALID_PHONE, VALID_EMAIL, INVALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Address.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_legacyOneDigitGroup_restoresLeadingZero() throws Exception {
        JsonAdaptedStudent adaptedStudent = new JsonAdaptedStudent(VALID_NAME, "a0123456x", "t4",
                null, null, null, null);
        Student student = adaptedStudent.toModelType();
        assertEquals(new MatricNumber("A0123456X"), student.getMatricNumber());
        assertEquals(new TutorialGroup("T04"), student.getTutorialGroup());
    }

    @Test
    public void toModelType_invalidTags_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(new JsonAdaptedTag(INVALID_TAG));
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                        VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, invalidTags);
        assertThrows(IllegalValueException.class, student::toModelType);
    }

    @Test
    public void toModelType_nullTagEntry_throwsIllegalValueException() {
        List<JsonAdaptedTag> tagsWithNull = new ArrayList<>(VALID_TAGS);
        tagsWithNull.add(null);
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_MATRIC_NUMBER, VALID_TUTORIAL_GROUP,
                VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, tagsWithNull);

        assertThrows(IllegalValueException.class, JsonAdaptedStudent.MESSAGE_NULL_TAG, student::toModelType);
    }

}
