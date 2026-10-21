package tutorlink.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import tutorlink.logic.Messages;
import tutorlink.model.Model;

/**
 * Lists every tutorial group and the number of students in it.
 */
public class GroupsCommand extends Command {

    public static final String COMMAND_WORD = "groups";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Lists all tutorial groups and their student counts.\n"
            + "Example: " + COMMAND_WORD;
    public static final String MESSAGE_EMPTY = "No tutorial groups found.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);

        Map<String, Long> groupCounts = model.getStudentDirectory().getStudentList().stream()
                .collect(Collectors.groupingBy(student -> student.getTutorialGroup().value,
                        TreeMap::new, Collectors.counting()));

        if (groupCounts.isEmpty()) {
            return new CommandResult(MESSAGE_EMPTY);
        }

        String result = groupCounts.entrySet().stream()
                .map(entry -> formatGroupCount(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
        return new CommandResult(result);
    }

    private static String formatGroupCount(String group, long count) {
        String studentLabel = count == 1 ? "student" : "students";
        return String.format(Messages.MESSAGE_GROUP_COUNT, group, count, studentLabel);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof GroupsCommand;
    }

    @Override
    public int hashCode() {
        return GroupsCommand.class.hashCode();
    }
}
