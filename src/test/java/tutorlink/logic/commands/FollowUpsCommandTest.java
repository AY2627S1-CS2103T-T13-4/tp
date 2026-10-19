package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.BENSON;
import static tutorlink.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorlink.model.Model;
import tutorlink.model.ModelManager;
import tutorlink.model.UserPrefs;
import tutorlink.model.student.Student;
import tutorlink.testutil.StudentBuilder;

public class FollowUpsCommandTest {
    private final Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());

    @Test
    public void execute_previousFilter_searchesFullRoster() {
        Student marked = new StudentBuilder(ALICE).withFollowUp(true).build();
        model.setStudent(ALICE, marked);
        model.updateFilteredStudentList(student -> student.equals(BENSON));

        assertEquals("1 student(s) requiring follow-up listed!",
                new FollowUpsCommand().execute(model).getFeedbackToUser());
        assertEquals(List.of(marked), model.getFilteredStudentList());
        assertEquals(7, model.getStudentDirectory().getStudentList().size());
    }

    @Test
    public void execute_noMarkedStudents_reportsEmptyList() {
        assertEquals(FollowUpsCommand.MESSAGE_EMPTY, new FollowUpsCommand().execute(model).getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredStudentList());
    }
}
