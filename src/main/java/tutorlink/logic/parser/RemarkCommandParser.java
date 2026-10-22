package tutorlink.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorlink.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static tutorlink.logic.parser.CliSyntax.PREFIX_REMARK;

import tutorlink.commons.core.index.Index;
import tutorlink.logic.commands.RemarkCommand;
import tutorlink.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a {@link RemarkCommand} object.
 */
public class RemarkCommandParser implements Parser<RemarkCommand> {

    @Override
    public RemarkCommand parse(String args) throws ParseException {
        requireNonNull(args);

        try {
            String prefix = PREFIX_REMARK.getPrefix();
            int prefixPosition = args.indexOf(" " + prefix);
            if (prefixPosition < 0) {
                throw new ParseException("A remark must be provided with r/TEXT; use r/ to clear it.");
            }

            Index index = ParserUtil.parseIndex(args.substring(0, prefixPosition).trim());
            String remark = args.substring(prefixPosition + 1 + prefix.length()).trim();
            return new RemarkCommand(index, remark.isBlank() ? null : remark);
        } catch (ParseException pe) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE), pe);
        }
    }
}
