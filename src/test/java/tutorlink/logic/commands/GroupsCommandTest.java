package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import org.junit.jupiter.api.Test;

import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.StudentDirectory;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.testutil.StudentBuilder;

public class GroupsCommandTest {

    @Test
    public void execute_listsCompleteRosterInStableOrder() {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        model.updateFilteredStudentList(student -> student.getTutorialGroup().equals(new TutorialGroup("T01")));

        CommandResult result = new GroupsCommand().execute(model);

        assertEquals("L01: 1 student\nT01: 2 students\nT02: 2 students\nT03: 2 students",
                result.getFeedbackToUser());
        assertEquals(2, model.getFilteredStudentList().size());
    }

    @Test
    public void execute_emptyRoster_showsEmptyMessage() {
        Model model = new ModelManager(new StudentDirectory(), new UserPrefs());

        assertEquals(GroupsCommand.MESSAGE_EMPTY, new GroupsCommand().execute(model).getFeedbackToUser());
    }

    @Test
    public void execute_afterGroupChange_reflectsLatestRoster() {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        Student original = model.getStudentDirectory().getStudentList().get(0);
        Student moved = new StudentBuilder(original).withTutorialGroup("T02").build();

        model.setStudent(original, moved);

        assertEquals("L01: 1 student\nT01: 1 student\nT02: 3 students\nT03: 2 students",
                new GroupsCommand().execute(model).getFeedbackToUser());
    }

    @Test
    public void execute_afterAddingAndDeletingStudent_reflectsLatestRoster() {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        Student added = new StudentBuilder().withName("New Student").withMatricNumber("A9999999Z")
                .withTutorialGroup("T04").build();

        model.addStudent(added);
        assertEquals("L01: 1 student\nT01: 2 students\nT02: 2 students\nT03: 2 students\nT04: 1 student",
                new GroupsCommand().execute(model).getFeedbackToUser());

        model.deleteStudent(added);
        assertEquals("L01: 1 student\nT01: 2 students\nT02: 2 students\nT03: 2 students",
                new GroupsCommand().execute(model).getFeedbackToUser());
    }
}
