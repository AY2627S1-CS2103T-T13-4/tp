package tutorlink.logic.parser;

import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static tutorlink.logic.parser.CliSyntax.PREFIX_EMAIL;
import static tutorlink.logic.parser.CliSyntax.PREFIX_MATRIC_NUMBER;
import static tutorlink.logic.parser.CliSyntax.PREFIX_NAME;
import static tutorlink.logic.parser.CliSyntax.PREFIX_PHONE;
import static tutorlink.logic.parser.CliSyntax.PREFIX_TAG;
import static tutorlink.logic.parser.CliSyntax.PREFIX_TUTORIAL_GROUP;

import java.util.Set;
import java.util.stream.Stream;

import tutorlink.logic.commands.AddCommand;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.student.Address;
import tutorlink.model.student.Email;
import tutorlink.model.student.MatricNumber;
import tutorlink.model.student.Name;
import tutorlink.model.student.Phone;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.model.tag.Tag;

/**
 * Parses input arguments and creates a new AddCommand object
 */
public class AddCommandParser implements Parser<AddCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the AddCommand
     * and returns an AddCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public AddCommand parse(String args) throws ParseException {
        ArgumentMultimap argMultimap =
                ArgumentTokenizer.tokenize(args, PREFIX_NAME, PREFIX_MATRIC_NUMBER, PREFIX_TUTORIAL_GROUP,
                        PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS, PREFIX_TAG);

        if (!arePrefixesPresent(argMultimap, PREFIX_NAME, PREFIX_MATRIC_NUMBER, PREFIX_TUTORIAL_GROUP)
                || !argMultimap.getPreamble().isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
        }

        argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_NAME, PREFIX_MATRIC_NUMBER, PREFIX_TUTORIAL_GROUP,
                PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS);
        Name name = ParserUtil.parseName(argMultimap.getValue(PREFIX_NAME).get());
        MatricNumber matricNumber = ParserUtil.parseMatricNumber(argMultimap.getValue(PREFIX_MATRIC_NUMBER).get());
        TutorialGroup tutorialGroup = ParserUtil.parseTutorialGroup(argMultimap.getValue(PREFIX_TUTORIAL_GROUP).get());
        Phone phone = argMultimap.getValue(PREFIX_PHONE).isPresent()
                ? ParserUtil.parsePhone(argMultimap.getValue(PREFIX_PHONE).get()) : null;
        Email email = argMultimap.getValue(PREFIX_EMAIL).isPresent()
                ? ParserUtil.parseEmail(argMultimap.getValue(PREFIX_EMAIL).get()) : null;
        Address address = argMultimap.getValue(PREFIX_ADDRESS).isPresent()
                ? ParserUtil.parseAddress(argMultimap.getValue(PREFIX_ADDRESS).get()) : null;
        Set<Tag> tagList = ParserUtil.parseTags(argMultimap.getAllValues(PREFIX_TAG));

        Student student = new Student(name, matricNumber, tutorialGroup, phone, email, address, tagList);

        return new AddCommand(student);
    }

    /**
     * Returns true if none of the prefixes contains empty {@code Optional} values in the given
     * {@code ArgumentMultimap}.
     */
    private static boolean arePrefixesPresent(ArgumentMultimap argumentMultimap, Prefix... prefixes) {
        return Stream.of(prefixes).allMatch(prefix -> argumentMultimap.getValue(prefix).isPresent());
    }

}
