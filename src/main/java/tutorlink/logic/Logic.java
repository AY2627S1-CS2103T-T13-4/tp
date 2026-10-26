package tutorlink.logic;

import javafx.collections.ObservableList;
import tutorlink.commons.core.GuiSettings;
import tutorlink.logic.commands.CommandResult;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.logic.parser.exceptions.ParseException;
import tutorlink.model.student.Student;

/**
 * API of the Logic component
 */
public interface Logic {
    /**
     * Executes the command and returns the result.
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /** Returns an unmodifiable view of the filtered list of students */
    ObservableList<Student> getFilteredStudentList();

    /** Returns the condition currently used to filter the displayed students. */
    String getFilterDescription();

    /** Returns an unmodifiable view of the complete roster, independent of the current filter. */
    ObservableList<Student> getStudentList();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Set the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);
}
