package seedu.address.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.MatricNumber;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.TutorialGroup;
import seedu.address.model.tag.Tag;

/**
 * Jackson-friendly version of {@link Person}.
 */
class JsonAdaptedPerson {

    public static final String MISSING_FIELD_MESSAGE_FORMAT = "Person's %s field is missing!";
    private static final String LEGACY_TUTORIAL_GROUP_REGEX = "[TLtl][0-9]";

    private final String name;
    private final String matricNumber;
    private final String tutorialGroup;
    private final String phone;
    private final String email;
    private final String address;
    private final String remark;
    private final List<JsonAdaptedTag> tags = new ArrayList<>();

    /**
     * Constructs a {@code JsonAdaptedPerson} with the given person details.
     */
    @JsonCreator
    public JsonAdaptedPerson(@JsonProperty("name") String name,
            @JsonProperty("matricNumber") String matricNumber, @JsonProperty("tutorialGroup") String tutorialGroup,
            @JsonProperty("phone") String phone, @JsonProperty("email") String email,
            @JsonProperty("address") String address,
            @JsonProperty("remark") String remark,
            @JsonProperty("tags") List<JsonAdaptedTag> tags) {
        this.name = name;
        this.matricNumber = matricNumber;
        this.tutorialGroup = tutorialGroup;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.remark = remark;
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    /**
     * Creates a person without a remark, for callers that have no remark field.
     */
    public JsonAdaptedPerson(String name, String matricNumber, String tutorialGroup,
            String phone, String email, String address, List<JsonAdaptedTag> tags) {
        this(name, matricNumber, tutorialGroup, phone, email, address, null, tags);
    }

    /**
     * Converts a given {@code Person} into this class for Jackson use.
     */
    public JsonAdaptedPerson(Person source) {
        name = source.getName().fullName;
        matricNumber = source.getMatricNumber().value;
        tutorialGroup = source.getTutorialGroup().value;
        phone = source.getPhone() == null ? null : source.getPhone().value;
        email = source.getEmail() == null ? null : source.getEmail().value;
        address = source.getAddress() == null ? null : source.getAddress().value;
        remark = source.getRemark();
        tags.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
    }

    /**
     * Converts this Jackson-friendly adapted person object into the model's {@code Person} object.
     *
     * @throws IllegalValueException if there were any data constraints violated in the adapted person.
     */
    public Person toModelType() throws IllegalValueException {
        final List<Tag> personTags = new ArrayList<>();
        for (JsonAdaptedTag tag : tags) {
            personTags.add(tag.toModelType());
        }

        if (name == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName()));
        }
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(Name.MESSAGE_CONSTRAINTS);
        }
        final Name modelName = new Name(name);

        if (matricNumber == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT,
                    MatricNumber.class.getSimpleName()));
        }
        if (!MatricNumber.isValidMatricNumber(matricNumber)) {
            throw new IllegalValueException(MatricNumber.MESSAGE_CONSTRAINTS);
        }
        final MatricNumber modelMatricNumber = new MatricNumber(matricNumber);

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
        final TutorialGroup modelTutorialGroup = new TutorialGroup(normalizedTutorialGroup);

        if (phone != null && !Phone.isValidPhone(phone)) {
            throw new IllegalValueException(Phone.MESSAGE_CONSTRAINTS);
        }
        final Phone modelPhone = phone == null ? null : new Phone(phone);

        if (email != null && !Email.isValidEmail(email)) {
            throw new IllegalValueException(Email.MESSAGE_CONSTRAINTS);
        }
        final Email modelEmail = email == null ? null : new Email(email);

        if (address != null && !Address.isValidAddress(address)) {
            throw new IllegalValueException(Address.MESSAGE_CONSTRAINTS);
        }
        final Address modelAddress = address == null ? null : new Address(address);

        final Set<Tag> modelTags = new HashSet<>(personTags);
        return new Person(modelName, modelMatricNumber, modelTutorialGroup,
                modelPhone, modelEmail, modelAddress, modelTags, remark);
    }

}
