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
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(args, PREFIX_REMARK);

        try {
            Index index = ParserUtil.parseIndex(argMultimap.getPreamble());
            argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_REMARK);
            if (argMultimap.getValue(PREFIX_REMARK).isEmpty()) {
                throw new ParseException("A remark must be provided with r/TEXT; use r/ to clear it.");
            }

            String remark = argMultimap.getValue(PREFIX_REMARK).get();
            return new RemarkCommand(index, remark.isBlank() ? null : remark);
        } catch (ParseException pe) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE), pe);
        }
    }
}
