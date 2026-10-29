package tutorlink.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tutorlink.commons.exceptions.IllegalValueException;
import tutorlink.model.student.Address;
import tutorlink.model.student.Email;
import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Phone;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.model.tag.Tag;

/**
 * Jackson-friendly version of {@link Student}.
 */
class JsonAdaptedStudent {

    public static final String MISSING_FIELD_MESSAGE_FORMAT = "Student's %s field is missing!";
    public static final String MESSAGE_NULL_TAG = "Student tags must not contain null entries.";
    private static final String LEGACY_TUTORIAL_GROUP_REGEX = "[TLtl][0-9]";

    private final String name;
    private final String matricNumber;
    private final String tutorialGroup;
    private final String phone;
    private final String email;
    private final String address;
    private final String remark;
    private final boolean requiresFollowUp;
    private final List<JsonAdaptedTag> tags = new ArrayList<>();

    /**
     * Constructs a {@code JsonAdaptedStudent} with the given student details.
     */
    @JsonCreator
    public JsonAdaptedStudent(@JsonProperty("name") String name,
            @JsonProperty("matricNumber") String matricNumber, @JsonProperty("tutorialGroup") String tutorialGroup,
            @JsonProperty("phone") String phone, @JsonProperty("email") String email,
            @JsonProperty("address") String address,
            @JsonProperty("remark") String remark,
            @JsonProperty("tags") List<JsonAdaptedTag> tags,
            @JsonProperty("requiresFollowUp") Boolean requiresFollowUp) {
        this.name = name;
        this.matricNumber = matricNumber;
        this.tutorialGroup = tutorialGroup;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.remark = remark;
        this.requiresFollowUp = Boolean.TRUE.equals(requiresFollowUp);
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    /**
     * Creates a student with no follow-up required, for callers using the older data contract.
     */
    public JsonAdaptedStudent(String name, String matricNumber, String tutorialGroup,
            String phone, String email, String address, String remark, List<JsonAdaptedTag> tags) {
        this(name, matricNumber, tutorialGroup, phone, email, address, remark, tags, false);
    }

    /**
     * Creates a student without a remark, for callers that have no remark field.
     */
    public JsonAdaptedStudent(String name, String matricNumber, String tutorialGroup,
            String phone, String email, String address, List<JsonAdaptedTag> tags) {
        this(name, matricNumber, tutorialGroup, phone, email, address, null, tags);
    }

    /**
     * Converts a given {@code Student} into this class for Jackson use.
     */
    public JsonAdaptedStudent(Student source) {
        name = source.getName().fullName;
        matricNumber = source.getMatricNumber().value;
        tutorialGroup = source.getTutorialGroup().value;
        phone = source.getPhone() == null ? null : source.getPhone().value;
        email = source.getEmail() == null ? null : source.getEmail().value;
        address = source.getAddress() == null ? null : source.getAddress().value;
        remark = source.getRemark();
        requiresFollowUp = source.requiresFollowUp();
        tags.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
    }

    /**
     * Converts this Jackson-friendly adapted student object into the model's {@code Student} object.
     *
     * @throws IllegalValueException if there were any data constraints violated in the adapted student.
     */
    public Student toModelType() throws IllegalValueException {
        Set<Tag> modelTags = parseTags();
        Name modelName = parseName();
        MatricNumber modelMatricNumber = parseMatricNumber();
        TutorialGroup modelTutorialGroup = parseTutorialGroup();
        Phone modelPhone = parsePhone();
        Email modelEmail = parseEmail();
        Address modelAddress = parseAddress();

        return new Student(modelName, modelMatricNumber, modelTutorialGroup,
                modelPhone, modelEmail, modelAddress, modelTags, remark, requiresFollowUp);
    }

    private Set<Tag> parseTags() throws IllegalValueException {
        final List<Tag> studentTags = new ArrayList<>();
        for (JsonAdaptedTag tag : tags) {
            if (tag == null) {
                throw new IllegalValueException(MESSAGE_NULL_TAG);
            }
            studentTags.add(tag.toModelType());
        }
        return new HashSet<>(studentTags);
    }

    private Name parseName() throws IllegalValueException {
        if (name == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName()));
        }
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(Name.MESSAGE_CONSTRAINTS);
        }
        return new Name(name);
    }

    private MatricNumber parseMatricNumber() throws IllegalValueException {
        if (matricNumber == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT,
                    MatricNumber.class.getSimpleName()));
        }
        if (!MatricNumber.isValidMatricNumber(matricNumber)) {
            throw new IllegalValueException(MatricNumber.MESSAGE_CONSTRAINTS);
        }
        return new MatricNumber(matricNumber);
    }

    private TutorialGroup parseTutorialGroup() throws IllegalValueException {
        if (tutorialGroup == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT,
                    TutorialGroup.class.getSimpleName()));
        }
        // Earlier versions saved T04 as T4. Accept that stored form and restore its leading zero.
        String normalizedTutorialGroup = tutorialGroup.matches(LEGACY_TUTORIAL_GROUP_REGEX)
                ? tutorialGroup.charAt(0) + "0" + tutorialGroup.charAt(1) : tutorialGroup;
        if (!TutorialGroup.isValidTutorialGroup(normalizedTutorialGroup)) {
            throw new IllegalValueException(TutorialGroup.MESSAGE_CONSTRAINTS);
        }
        return new TutorialGroup(normalizedTutorialGroup);
    }

    private Phone parsePhone() throws IllegalValueException {
        if (phone != null && !Phone.isValidPhone(phone)) {
            throw new IllegalValueException(Phone.MESSAGE_CONSTRAINTS);
        }
        return phone == null ? null : new Phone(phone);
    }

    private Email parseEmail() throws IllegalValueException {
        if (email != null && !Email.isValidEmail(email)) {
            throw new IllegalValueException(Email.MESSAGE_CONSTRAINTS);
        }
        return email == null ? null : new Email(email);
    }

    private Address parseAddress() throws IllegalValueException {
        if (address != null && !Address.isValidAddress(address)) {
            throw new IllegalValueException(Address.MESSAGE_CONSTRAINTS);
        }
        return address == null ? null : new Address(address);
    }

}
