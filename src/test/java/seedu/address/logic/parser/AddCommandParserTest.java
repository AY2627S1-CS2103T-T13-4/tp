package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.INVALID_ADDRESS_DESC;
import static seedu.address.logic.commands.CommandTestUtil.INVALID_EMAIL_DESC;
import static seedu.address.logic.commands.CommandTestUtil.INVALID_MATRIC_NUMBER_DESC;
import static seedu.address.logic.commands.CommandTestUtil.INVALID_NAME_DESC;
import static seedu.address.logic.commands.CommandTestUtil.INVALID_PHONE_DESC;
import static seedu.address.logic.commands.CommandTestUtil.INVALID_TAG_DESC;
import static seedu.address.logic.commands.CommandTestUtil.INVALID_TUTORIAL_GROUP_DESC;
import static seedu.address.logic.commands.CommandTestUtil.MATRIC_NUMBER_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.MATRIC_NUMBER_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.PREAMBLE_NON_EMPTY;
import static seedu.address.logic.commands.CommandTestUtil.PREAMBLE_WHITESPACE;
import static seedu.address.logic.commands.CommandTestUtil.TAG_DESC_FRIEND;
import static seedu.address.logic.commands.CommandTestUtil.TAG_DESC_HUSBAND;
import static seedu.address.logic.commands.CommandTestUtil.TUTORIAL_GROUP_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.TUTORIAL_GROUP_DESC_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TAG_FRIEND;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_MATRIC_NUMBER;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TUTORIAL_GROUP;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalPersons.AMY;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.AddCommand;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.MatricNumber;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.TutorialGroup;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class AddCommandParserTest {
    private static final String VALID_BOB_DETAILS = NAME_DESC_BOB + MATRIC_NUMBER_DESC_BOB
            + TUTORIAL_GROUP_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB + ADDRESS_DESC_BOB;

    private final AddCommandParser parser = new AddCommandParser();

    @Test
    public void parse_allFieldsPresent_success() {
        Person expectedPerson = new PersonBuilder(BOB).withTags(VALID_TAG_FRIEND).build();
        assertParseSuccess(parser, PREAMBLE_WHITESPACE + VALID_BOB_DETAILS + TAG_DESC_FRIEND,
                new AddCommand(expectedPerson));

        Person expectedPersonMultipleTags = new PersonBuilder(BOB).withTags(VALID_TAG_FRIEND, VALID_TAG_HUSBAND)
                .build();
        assertParseSuccess(parser, VALID_BOB_DETAILS + TAG_DESC_HUSBAND + TAG_DESC_FRIEND,
                new AddCommand(expectedPersonMultipleTags));
    }

    @Test
    public void parse_tutorialGroupWithLowercaseAndLeadingZero_success() {
        String input = VALID_BOB_DETAILS.replace(TUTORIAL_GROUP_DESC_BOB, " g/l03");
        assertParseSuccess(parser, input, new AddCommand(new PersonBuilder(BOB).withTags().build()));
    }

    @Test
    public void parse_repeatedNonTagValue_failure() {
        assertParseFailure(parser, NAME_DESC_AMY + VALID_BOB_DETAILS,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME));
        assertParseFailure(parser, MATRIC_NUMBER_DESC_AMY + VALID_BOB_DETAILS,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_MATRIC_NUMBER));
        assertParseFailure(parser, TUTORIAL_GROUP_DESC_AMY + VALID_BOB_DETAILS,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TUTORIAL_GROUP));
        assertParseFailure(parser, PHONE_DESC_BOB + VALID_BOB_DETAILS,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));
        assertParseFailure(parser, EMAIL_DESC_BOB + VALID_BOB_DETAILS,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_EMAIL));
        assertParseFailure(parser, ADDRESS_DESC_BOB + VALID_BOB_DETAILS,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_ADDRESS));
    }

    @Test
    public void parse_optionalFieldsMissing_success() {
        String input = NAME_DESC_AMY + MATRIC_NUMBER_DESC_AMY + TUTORIAL_GROUP_DESC_AMY;
        Person expectedPerson = new Person(AMY.getName(), AMY.getMatricNumber(), AMY.getTutorialGroup(),
                null, null, null, Set.of(), AMY.getRemark());
        assertParseSuccess(parser, input, new AddCommand(expectedPerson));
    }

    @Test
    public void parse_compulsoryFieldMissing_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE);
        String[] inputs = {
            VALID_BOB_DETAILS.replace(NAME_DESC_BOB, ""),
            VALID_BOB_DETAILS.replace(MATRIC_NUMBER_DESC_BOB, ""),
            VALID_BOB_DETAILS.replace(TUTORIAL_GROUP_DESC_BOB, "")
        };
        for (String input : inputs) {
            assertParseFailure(parser, input, expectedMessage);
        }
    }

    @Test
    public void parse_invalidValue_failure() {
        assertParseFailure(parser, VALID_BOB_DETAILS.replace(NAME_DESC_BOB, INVALID_NAME_DESC),
                Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, VALID_BOB_DETAILS.replace(MATRIC_NUMBER_DESC_BOB, INVALID_MATRIC_NUMBER_DESC),
                MatricNumber.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, VALID_BOB_DETAILS.replace(TUTORIAL_GROUP_DESC_BOB, INVALID_TUTORIAL_GROUP_DESC),
                TutorialGroup.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, VALID_BOB_DETAILS.replace(PHONE_DESC_BOB, INVALID_PHONE_DESC),
                Phone.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, VALID_BOB_DETAILS.replace(EMAIL_DESC_BOB, INVALID_EMAIL_DESC),
                Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, VALID_BOB_DETAILS.replace(ADDRESS_DESC_BOB, INVALID_ADDRESS_DESC),
                Address.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, VALID_BOB_DETAILS + INVALID_TAG_DESC, Tag.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_nonEmptyPreamble_failure() {
        assertParseFailure(parser, PREAMBLE_NON_EMPTY + VALID_BOB_DETAILS,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
    }
}
