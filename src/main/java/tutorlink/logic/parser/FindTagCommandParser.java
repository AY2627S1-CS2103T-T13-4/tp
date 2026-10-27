package tutorlink.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import tutorlink.logic.commands.FindTagCommand;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.student.TagMatchesPredicate;
import tutorlink.model.tag.Tag;

/**
 * Parses input arguments and creates a new FindTagCommand object.
 */
public class FindTagCommandParser implements Parser<FindTagCommand> {

    /**
     * Parses one tag using the same validation as adding or editing tags.
     *
     * @throws ParseException if the tag is missing or invalid.
     */
    public FindTagCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String trimmedArgs = args.trim();
        if (trimmedArgs.isEmpty()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindTagCommand.MESSAGE_USAGE));
        }

        if (trimmedArgs.startsWith("t/")) {
            throw new ParseException("Use the tag without t/.\n" + FindTagCommand.MESSAGE_USAGE);
        }

        Tag tag = ParserUtil.parseTag(trimmedArgs);
        return new FindTagCommand(new TagMatchesPredicate(tag));
    }
}
