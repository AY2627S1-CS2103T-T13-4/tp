package tutorlink.testutil;

import java.util.HashSet;
import java.util.Set;

import tutorlink.model.student.Address;
import tutorlink.model.student.Email;
import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Phone;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.model.tag.Tag;
import tutorlink.model.util.SampleDataUtil;

/**
 * A utility class to help with building Student objects.
 */
public class StudentBuilder {

    public static final String DEFAULT_NAME = "Amy Bee";
    public static final String DEFAULT_MATRIC_NUMBER = "A0000001A";
    public static final String DEFAULT_TUTORIAL_GROUP = "T01";
    public static final String DEFAULT_PHONE = "85355255";
    public static final String DEFAULT_EMAIL = "amy@gmail.com";
    public static final String DEFAULT_ADDRESS = "123, Jurong West Ave 6, #08-111";

    private Name name;
    private MatricNumber matricNumber;
    private TutorialGroup tutorialGroup;
    private Phone phone;
    private Email email;
    private Address address;
    private String remark;
    private Set<Tag> tags;

    /**
     * Creates a {@code StudentBuilder} with the default details.
     */
    public StudentBuilder() {
        name = new Name(DEFAULT_NAME);
        matricNumber = new MatricNumber(DEFAULT_MATRIC_NUMBER);
        tutorialGroup = new TutorialGroup(DEFAULT_TUTORIAL_GROUP);
        phone = new Phone(DEFAULT_PHONE);
        email = new Email(DEFAULT_EMAIL);
        address = new Address(DEFAULT_ADDRESS);
        tags = new HashSet<>();
    }

    /**
     * Initializes the StudentBuilder with the data of {@code studentToCopy}.
     */
    public StudentBuilder(Student studentToCopy) {
        name = studentToCopy.getName();
        matricNumber = studentToCopy.getMatricNumber();
        tutorialGroup = studentToCopy.getTutorialGroup();
        phone = studentToCopy.getPhone();
        email = studentToCopy.getEmail();
        address = studentToCopy.getAddress();
        remark = studentToCopy.getRemark();
        tags = new HashSet<>(studentToCopy.getTags());
    }

    /**
     * Sets the {@code Name} of the {@code Student} that we are building.
     */
    public StudentBuilder withName(String name) {
        this.name = new Name(name);
        return this;
    }

    /**
     * Sets the {@code MatricNumber} of the {@code Student} that we are building.
     */
    public StudentBuilder withMatricNumber(String matricNumber) {
        this.matricNumber = new MatricNumber(matricNumber);
        return this;
    }

    /**
     * Sets the {@code TutorialGroup} of the {@code Student} that we are building.
     */
    public StudentBuilder withTutorialGroup(String tutorialGroup) {
        this.tutorialGroup = new TutorialGroup(tutorialGroup);
        return this;
    }

    /**
     * Parses the {@code tags} into a {@code Set<Tag>} and sets it to the {@code Student} that we are building.
     */
    public StudentBuilder withTags(String ... tags) {
        this.tags = SampleDataUtil.getTagSet(tags);
        return this;
    }

    /**
     * Sets the {@code Address} of the {@code Student} that we are building.
     */
    public StudentBuilder withAddress(String address) {
        this.address = new Address(address);
        return this;
    }

    /**
     * Sets the {@code Phone} of the {@code Student} that we are building.
     */
    public StudentBuilder withPhone(String phone) {
        this.phone = new Phone(phone);
        return this;
    }

    /**
     * Sets the {@code Email} of the {@code Student} that we are building.
     */
    public StudentBuilder withEmail(String email) {
        this.email = new Email(email);
        return this;
    }

    /**
     * Sets the remark of the student that we are building.
     */
    public StudentBuilder withRemark(String remark) {
        this.remark = remark;
        return this;
    }

    public Student build() {
        return new Student(name, matricNumber, tutorialGroup, phone, email, address, tags, remark);
    }

}
