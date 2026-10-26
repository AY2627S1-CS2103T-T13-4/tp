package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;
import static tutorlink.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static tutorlink.logic.parser.CliSyntax.PREFIX_EMAIL;
import static tutorlink.logic.parser.CliSyntax.PREFIX_MATRIC_NUMBER;
import static tutorlink.logic.parser.CliSyntax.PREFIX_NAME;
import static tutorlink.logic.parser.CliSyntax.PREFIX_PHONE;
import static tutorlink.logic.parser.CliSyntax.PREFIX_TAG;
import static tutorlink.logic.parser.CliSyntax.PREFIX_TUTORIAL_GROUP;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import tutorlink.commons.core.index.Index;
import tutorlink.commons.util.CollectionUtil;
import tutorlink.commons.util.ToStringBuilder;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.model.Model;
import tutorlink.model.student.Address;
import tutorlink.model.student.Email;
import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Phone;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.model.tag.Tag;

/**
 * Edits the details of an existing student in the student directory.
 */
public class EditCommand extends Command {

    public static final String COMMAND_WORD = "edit";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits the details of the student identified "
            + "by the index number used in the displayed student list. "
            + "Existing values will be overwritten by the input values.\n"
            + "Parameters: INDEX (must be a positive integer) "
            + "[" + PREFIX_NAME + "NAME] "
            + "[" + PREFIX_MATRIC_NUMBER + "MATRIC_NUMBER] "
            + "[" + PREFIX_TUTORIAL_GROUP + "TUTORIAL_GROUP] "
            + "[" + PREFIX_PHONE + "PHONE] "
            + "[" + PREFIX_EMAIL + "EMAIL] "
            + "[" + PREFIX_ADDRESS + "ADDRESS] "
            + "[" + PREFIX_TAG + "TAG]...\n"
            + "Use p/, e/, or a/ without a value to clear that optional contact detail.\n"
            + "Example: " + COMMAND_WORD + " 1 "
            + PREFIX_PHONE + "91234567 "
            + PREFIX_EMAIL + "johndoe@example.com";

    public static final String MESSAGE_EDIT_STUDENT_SUCCESS = "Edited student: %1$s";
    public static final String MESSAGE_NOT_EDITED = "At least one field to edit must be provided.";
    public static final String MESSAGE_DUPLICATE_STUDENT = "This student already exists in TutorLink.";

    private final Index index;
    private final EditStudentDescriptor editStudentDescriptor;

    /**
     * @param index of the student in the filtered student list to edit
     * @param editStudentDescriptor details to edit the student with
     */
    public EditCommand(Index index, EditStudentDescriptor editStudentDescriptor) {
        requireNonNull(index);
        requireNonNull(editStudentDescriptor);

        this.index = index;
        this.editStudentDescriptor = new EditStudentDescriptor(editStudentDescriptor);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Student> lastShownList = model.getFilteredStudentList();

        if (index.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        }

        Student studentToEdit = lastShownList.get(index.getZeroBased());
        Student editedStudent = createEditedStudent(studentToEdit, editStudentDescriptor);

        if (!studentToEdit.isSameStudent(editedStudent) && model.hasStudent(editedStudent)) {
            throw new CommandException(MESSAGE_DUPLICATE_STUDENT);
        }

        model.setStudent(studentToEdit, editedStudent);
        return new CommandResult(String.format(MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(editedStudent)));
    }

    /**
     * Creates and returns a {@code Student} with the details of {@code studentToEdit}
     * edited with {@code editStudentDescriptor}.
     */
    private static Student createEditedStudent(Student studentToEdit, EditStudentDescriptor editStudentDescriptor) {
        assert studentToEdit != null;

        Name updatedName = editStudentDescriptor.getName().orElse(studentToEdit.getName());
        MatricNumber updatedMatricNumber = editStudentDescriptor.getMatricNumber()
                .orElse(studentToEdit.getMatricNumber());
        TutorialGroup updatedTutorialGroup = editStudentDescriptor.getTutorialGroup()
                .orElse(studentToEdit.getTutorialGroup());
        Phone updatedPhone = editStudentDescriptor.phoneEdited
                ? editStudentDescriptor.phone : studentToEdit.getPhone();
        Email updatedEmail = editStudentDescriptor.emailEdited
                ? editStudentDescriptor.email : studentToEdit.getEmail();
        Address updatedAddress = editStudentDescriptor.addressEdited
                ? editStudentDescriptor.address : studentToEdit.getAddress();
        Set<Tag> updatedTags = editStudentDescriptor.getTags().orElse(studentToEdit.getTags());

        return new Student(updatedName, updatedMatricNumber, updatedTutorialGroup,
                updatedPhone, updatedEmail, updatedAddress, updatedTags, studentToEdit.getRemark(),
                studentToEdit.requiresFollowUp());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof EditCommand otherEditCommand)) {
            return false;
        }

        return index.equals(otherEditCommand.index)
                && editStudentDescriptor.equals(otherEditCommand.editStudentDescriptor);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("index", index)
                .add("editStudentDescriptor", editStudentDescriptor)
                .toString();
    }

    /**
     * Stores requested edits. Contact fields distinguish omission (keep the old value) from
     * an explicit null (clear the value).
     */
    public static class EditStudentDescriptor {
        private Name name;
        private MatricNumber matricNumber;
        private TutorialGroup tutorialGroup;
        private Phone phone;
        private Email email;
        private Address address;
        private boolean phoneEdited;
        private boolean emailEdited;
        private boolean addressEdited;
        private Set<Tag> tags;

        public EditStudentDescriptor() {}

        /**
         * Copy constructor.
         * A defensive copy of {@code tags} is used internally.
         */
        public EditStudentDescriptor(EditStudentDescriptor toCopy) {
            setName(toCopy.name);
            setMatricNumber(toCopy.matricNumber);
            setTutorialGroup(toCopy.tutorialGroup);
            setPhone(toCopy.phone);
            setEmail(toCopy.email);
            setAddress(toCopy.address);
            setTags(toCopy.tags);
            phoneEdited = toCopy.phoneEdited;
            emailEdited = toCopy.emailEdited;
            addressEdited = toCopy.addressEdited;
        }

        /**
         * Returns true if at least one field is edited.
         */
        public boolean isAnyFieldEdited() {
            return CollectionUtil.isAnyNonNull(name, matricNumber, tutorialGroup, tags)
                    || phoneEdited || emailEdited || addressEdited;
        }

        public void setName(Name name) {
            this.name = name;
        }

        public Optional<Name> getName() {
            return Optional.ofNullable(name);
        }

        public void setMatricNumber(MatricNumber matricNumber) {
            this.matricNumber = matricNumber;
        }

        public Optional<MatricNumber> getMatricNumber() {
            return Optional.ofNullable(matricNumber);
        }

        public void setTutorialGroup(TutorialGroup tutorialGroup) {
            this.tutorialGroup = tutorialGroup;
        }

        public Optional<TutorialGroup> getTutorialGroup() {
            return Optional.ofNullable(tutorialGroup);
        }

        /** Requests replacement of the phone, or clearing it when {@code phone} is null. */
        public void setPhone(Phone phone) {
            this.phone = phone;
            phoneEdited = true;
        }

        public Optional<Phone> getPhone() {
            return Optional.ofNullable(phone);
        }

        /** Requests replacement of the email, or clearing it when {@code email} is null. */
        public void setEmail(Email email) {
            this.email = email;
            emailEdited = true;
        }

        public Optional<Email> getEmail() {
            return Optional.ofNullable(email);
        }

        /** Requests replacement of the address, or clearing it when {@code address} is null. */
        public void setAddress(Address address) {
            this.address = address;
            addressEdited = true;
        }

        public Optional<Address> getAddress() {
            return Optional.ofNullable(address);
        }

        /**
         * Sets {@code tags} to this object's {@code tags}.
         * A defensive copy of {@code tags} is used internally.
         */
        public void setTags(Set<Tag> tags) {
            this.tags = (tags != null) ? new HashSet<>(tags) : null;
        }

        /**
         * Returns an unmodifiable tag set, which throws {@code UnsupportedOperationException}
         * if modification is attempted.
         * Returns {@code Optional#empty()} if {@code tags} is null.
         */
        public Optional<Set<Tag>> getTags() {
            return (tags != null) ? Optional.of(Collections.unmodifiableSet(tags)) : Optional.empty();
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            // instanceof handles nulls
            if (!(other instanceof EditStudentDescriptor otherEditStudentDescriptor)) {
                return false;
            }

            return Objects.equals(name, otherEditStudentDescriptor.name)
                    && Objects.equals(matricNumber, otherEditStudentDescriptor.matricNumber)
                    && Objects.equals(tutorialGroup, otherEditStudentDescriptor.tutorialGroup)
                    && Objects.equals(phone, otherEditStudentDescriptor.phone)
                    && Objects.equals(email, otherEditStudentDescriptor.email)
                    && Objects.equals(address, otherEditStudentDescriptor.address)
                    && phoneEdited == otherEditStudentDescriptor.phoneEdited
                    && emailEdited == otherEditStudentDescriptor.emailEdited
                    && addressEdited == otherEditStudentDescriptor.addressEdited
                    && Objects.equals(tags, otherEditStudentDescriptor.tags);
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
                    .add("tags", tags)
                    .toString();
        }
    }
}
