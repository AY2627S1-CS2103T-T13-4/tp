package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorlink.testutil.Assert.assertThrows;
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

    @Test
    public void execute_emptyRoster_reportsEmptyList() {
        Model emptyModel = new ModelManager();
        assertEquals(FollowUpsCommand.MESSAGE_EMPTY, new FollowUpsCommand().execute(emptyModel).getFeedbackToUser());
        assertEquals(List.of(), emptyModel.getFilteredStudentList());
    }

    @Test
    public void execute_multipleMarkedStudents_listsOnceInRosterOrderWithoutChangingRecords() {
        Student markedAlice = ALICE.withFollowUp(true);
        Student markedBenson = BENSON.withFollowUp(true);
        model.setStudent(ALICE, markedAlice);
        model.setStudent(BENSON, markedBenson);
        List<Student> roster = List.copyOf(model.getStudentDirectory().getStudentList());
        assertEquals("2 student(s) requiring follow-up listed!",
                new FollowUpsCommand().execute(model).getFeedbackToUser());
        assertEquals(List.of(markedAlice, markedBenson), model.getFilteredStudentList());
        assertEquals(roster, model.getStudentDirectory().getStudentList());
        new FollowUpsCommand().execute(model);
        assertEquals(List.of(markedAlice, markedBenson), model.getFilteredStudentList());
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new FollowUpsCommand().execute(null));
    }
}
