package tutorlink.logic.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tutorlink.commons.util.StringUtil;
import tutorlink.logic.commands.AddCommand;
import tutorlink.logic.commands.ClearCommand;
import tutorlink.logic.commands.DeleteCommand;
import tutorlink.logic.commands.EditCommand;
import tutorlink.logic.commands.ExitCommand;
import tutorlink.logic.commands.FindCommand;
import tutorlink.logic.commands.FindTagCommand;
import tutorlink.logic.commands.FollowUpCommand;
import tutorlink.logic.commands.FollowUpsCommand;
import tutorlink.logic.commands.GroupsCommand;
import tutorlink.logic.commands.HelpCommand;
import tutorlink.logic.commands.ListCommand;
import tutorlink.logic.commands.RemarkCommand;
import tutorlink.logic.commands.SortCommand;
import tutorlink.logic.commands.ViewCommand;

/**
 * Computes advisory syntax and a single safe append operation, independently of the command executor.
 */
public class CommandCompletion {
    private static final Suggestion NONE = new Suggestion("", "");
    private static final Pattern COMMAND = Pattern.compile("^(\\s*)(\\S+)(.*)$");
    // Match literal-space boundaries; unrecognized slash words are handled according to their current field.
    private static final Pattern PREFIX = Pattern.compile(" ([^\\s/]+/)");
    private static final List<String> COMMANDS = List.of(AddCommand.COMMAND_WORD, ClearCommand.COMMAND_WORD,
            DeleteCommand.COMMAND_WORD, EditCommand.COMMAND_WORD, ExitCommand.COMMAND_WORD, FindCommand.COMMAND_WORD,
            FindTagCommand.COMMAND_WORD, FollowUpCommand.COMMAND_WORD, FollowUpsCommand.COMMAND_WORD,
            GroupsCommand.COMMAND_WORD, HelpCommand.COMMAND_WORD, ListCommand.COMMAND_WORD, RemarkCommand.COMMAND_WORD,
            SortCommand.COMMAND_WORD, ViewCommand.COMMAND_WORD);
    private static final List<Parameter> STUDENT_PARAMETERS = List.of(
            new Parameter(CliSyntax.PREFIX_NAME, "NAME", false),
            new Parameter(CliSyntax.PREFIX_MATRIC_NUMBER, "MATRIC_NUMBER", false),
            new Parameter(CliSyntax.PREFIX_TUTORIAL_GROUP, "TUTORIAL_GROUP", false),
            new Parameter(CliSyntax.PREFIX_PHONE, "PHONE", true),
            new Parameter(CliSyntax.PREFIX_EMAIL, "EMAIL", true),
            new Parameter(CliSyntax.PREFIX_ADDRESS, "ADDRESS", true),
            new Parameter(CliSyntax.PREFIX_TAG, "TAG", true));

    /**
     * Returns a display suffix and the next literal append, never placeholder values or replacement text.
     */
    public Suggestion suggest(String input) {
        Matcher command = COMMAND.matcher(input);
        if (!command.matches()) {
            return NONE;
        }
        String word = command.group(2);
        String arguments = command.group(3);
        if (COMMANDS.contains(word)) {
            return suggestArguments(word, arguments);
        }
        if (!arguments.isEmpty()) {
            return NONE;
        }
        List<String> matches = COMMANDS.stream().filter(candidate -> candidate.startsWith(word)).toList();
        if (matches.size() != 1) {
            return NONE;
        }
        String completed = matches.getFirst();
        String suffix = completed.substring(word.length());
        return new Suggestion(suffix + suggestArguments(completed, "").display(), suffix);
    }

    private Suggestion suggestArguments(String word, String arguments) {
        return switch (word) {
            case AddCommand.COMMAND_WORD, EditCommand.COMMAND_WORD ->
                suggestPrefixes(word, arguments, STUDENT_PARAMETERS);
            case ListCommand.COMMAND_WORD -> suggestPrefixes(word, arguments,
                    List.of(new Parameter(CliSyntax.PREFIX_TUTORIAL_GROUP, "TUTORIAL_GROUP", true)));
            case RemarkCommand.COMMAND_WORD -> suggestPrefixes(word, arguments,
                    List.of(new Parameter(CliSyntax.PREFIX_REMARK, "REMARK", false)));
            default -> suggestPositional(word, arguments);
        };
    }

    private Suggestion suggestPrefixes(String word, String arguments, List<Parameter> parameters) {
        boolean hasIndex = word.equals(EditCommand.COMMAND_WORD) || word.equals(RemarkCommand.COMMAND_WORD);
        boolean isEdit = word.equals(EditCommand.COMMAND_WORD);
        Matcher matcher = PREFIX.matcher(arguments);
        List<Parameter> supplied = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        Set<Parameter> seen = new HashSet<>();
        while (matcher.find()) {
            String prefix = matcher.group(1);
            Parameter parameter = parameters.stream()
                    .filter(candidate -> candidate.prefix().getPrefix().equals(prefix))
                    .findFirst().orElse(null);
            // Like the parser, keep unrecognized slash words and escapes inside free-text addresses.
            if (parameter == null && !supplied.isEmpty()
                    && supplied.getLast().prefix().equals(CliSyntax.PREFIX_ADDRESS)) {
                continue;
            }
            if (parameter == null || (!seen.add(parameter) && !parameter.prefix().equals(CliSyntax.PREFIX_TAG))) {
                return NONE;
            }
            supplied.add(parameter);
            starts.add(matcher.start());
            ends.add(matcher.end());
        }
        String preamble = arguments.substring(0, starts.isEmpty() ? arguments.length() : starts.getFirst()).trim();
        if (hasIndex ? !StringUtil.isNonZeroUnsignedInteger(preamble) : !preamble.isEmpty()) {
            if (hasIndex && arguments.isBlank()) {
                return new Suggestion(separator(arguments) + "INDEX " + syntax(parameters, isEdit), "");
            }
            return NONE;
        }

        String activePlaceholder = "";
        for (int i = 0; i < supplied.size(); i++) {
            int end = i + 1 < supplied.size() ? starts.get(i + 1) : arguments.length();
            if (arguments.substring(ends.get(i), end).isBlank()) {
                boolean canClear = word.equals(RemarkCommand.COMMAND_WORD)
                        || isEdit && supplied.get(i).optional();
                if (!canClear) {
                    if (i != supplied.size() - 1) {
                        return NONE;
                    }
                    activePlaceholder = supplied.get(i).placeholder();
                }
            }
        }
        List<Parameter> remaining = parameters.stream().filter(parameter -> !seen.contains(parameter)).toList();
        String tail = syntax(remaining, isEdit);
        String display = activePlaceholder.isEmpty() ? tail : activePlaceholder + (tail.isEmpty() ? "" : " " + tail);
        String append = activePlaceholder.isEmpty() && !remaining.isEmpty()
                ? separator(arguments) + remaining.getFirst().prefix() : "";
        return new Suggestion(display.isEmpty() ? "" : (activePlaceholder.isEmpty() ? separator(arguments) : "")
                + display, append);
    }

    private String syntax(List<Parameter> parameters, boolean isEdit) {
        return String.join(" ", parameters.stream().map(parameter -> {
            String syntax = parameter.prefix() + parameter.placeholder();
            boolean isOptional = isEdit || parameter.optional();
            return (isOptional ? "[" + syntax + "]" : syntax)
                    + (parameter.prefix().equals(CliSyntax.PREFIX_TAG) ? "..." : "");
        }).toList());
    }

    private Suggestion suggestPositional(String word, String arguments) {
        String trimmed = arguments.trim();
        String hint = switch (word) {
            case FindCommand.COMMAND_WORD -> trimmed.isEmpty() ? "KEYWORD [MORE_KEYWORDS]..." : "";
            case FindTagCommand.COMMAND_WORD -> trimmed.isEmpty() ? "TAG" : "";
            case ViewCommand.COMMAND_WORD -> trimmed.isEmpty() ? "INDEX" : "";
            case DeleteCommand.COMMAND_WORD -> trimmed.isEmpty() ? "INDEX [confirm]"
                    : StringUtil.isNonZeroUnsignedInteger(trimmed) ? "[confirm]" : "";
            case FollowUpCommand.COMMAND_WORD -> trimmed.isEmpty() ? "INDEX on|done"
                    : StringUtil.isNonZeroUnsignedInteger(trimmed) ? "on|done" : "";
            case SortCommand.COMMAND_WORD -> trimmed.isEmpty() ? "name|group [asc|desc] OR reset"
                    : trimmed.equals("name") || trimmed.equals("group") ? "[asc|desc]" : "";
            default -> "";
        };
        return hint.isEmpty() ? NONE : new Suggestion(separator(arguments) + hint, "");
    }

    private String separator(String text) {
        return text.endsWith(" ") ? "" : " ";
    }

    /**
     * A visible suffix and at most one command-name suffix or parameter prefix to append on Tab.
     */
    public record Suggestion(String display, String append) { }

    /** Describes a prefixed argument in the order used by command usage messages. */
    private record Parameter(Prefix prefix, String placeholder, boolean optional) { }
}
