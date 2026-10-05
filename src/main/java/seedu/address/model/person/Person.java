package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.tag.Tag;

/**
 * Represents a Person in the address book.
 * Guarantees: identity fields and tags are present, field values are validated, immutable.
 * Phone, email, and address may be null when not provided.
 */
public class Person {

    // Identity fields
    private final MatricNumber matricNumber;

    // Data fields
    private final Name name;
    private final TutorialGroup tutorialGroup;
    private final Phone phone;
    private final Email email;
    private final Address address;
    private final String remark;
    private final Set<Tag> tags = new HashSet<>();

    /**
     * Name, matriculation number, tutorial group, and tags must be present and not null.
     */
    public Person(Name name, MatricNumber matricNumber, TutorialGroup tutorialGroup,
            Phone phone, Email email, Address address, Set<Tag> tags) {
        this(name, matricNumber, tutorialGroup, phone, email, address, tags, null);
    }

    /**
     * Creates a person while retaining an optional remark from an earlier data file.
     */
    public Person(Name name, MatricNumber matricNumber, TutorialGroup tutorialGroup,
            Phone phone, Email email, Address address, Set<Tag> tags, String remark) {
        requireAllNonNull(name, matricNumber, tutorialGroup, tags);
        this.name = name;
        this.matricNumber = matricNumber;
        this.tutorialGroup = tutorialGroup;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.remark = remark;
        this.tags.addAll(tags);
    }

    public Name getName() {
        return name;
    }

    public MatricNumber getMatricNumber() {
        return matricNumber;
    }

    public TutorialGroup getTutorialGroup() {
        return tutorialGroup;
    }

    public Phone getPhone() {
        return phone;
    }

    public Email getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    public String getRemark() {
        return remark;
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Returns true if both persons have the same matriculation number.
     * This defines a weaker notion of equality between two persons.
     */
    public boolean isSamePerson(Person otherPerson) {
        if (otherPerson == this) {
            return true;
        }

        return otherPerson != null
                && otherPerson.getMatricNumber().equals(getMatricNumber());
    }

    /**
     * Returns true if both persons have the same identity and data fields.
     * This defines a stronger notion of equality between two persons.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Person otherPerson)) {
            return false;
        }

        return name.equals(otherPerson.name)
                && matricNumber.equals(otherPerson.matricNumber)
                && tutorialGroup.equals(otherPerson.tutorialGroup)
                && Objects.equals(phone, otherPerson.phone)
                && Objects.equals(email, otherPerson.email)
                && Objects.equals(address, otherPerson.address)
                && Objects.equals(remark, otherPerson.remark)
                && tags.equals(otherPerson.tags);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, matricNumber, tutorialGroup, phone, email, address, remark, tags);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("matricNumber", matricNumber)
                .add("tutorialGroup", tutorialGroup)
                .add("phone", phone)
                .add("email", email)
                .add("address", address)
                .add("remark", remark)
                .add("tags", tags)
                .toString();
    }

}
