package tutorlink.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorlink.commons.core.index.Index;
import tutorlink.logic.Messages;
import tutorlink.logic.commands.exceptions.CommandException;
import tutorlink.model.ModelManager;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroup;
import tutorlink.testutil.StudentBuilder;

public class ViewCommandTest {
    @Test
    public void execute_filteredAndSorted_usesDisplayedIndexWithoutChangingRecordsOrView() throws Exception {
        Student bob = new StudentBuilder().withName("Bob Tan").withMatricNumber("A0000001A")
                .withTutorialGroup("T01").build();
        Student alice = new StudentBuilder().withName("Alice Lee").withMatricNumber("A0000002B")
                .withTutorialGroup("T02").build();
        Student cara = new StudentBuilder().withName("Cara Lim").withMatricNumber("A0000003C")
                .withTutorialGroup("T01").build();
        ModelManager model = new ModelManager();
        model.addStudent(bob);
        model.addStudent(alice);
        model.addStudent(cara);
        new ListCommand(new TutorialGroup("T01")).execute(model);
        new SortCommand(true).execute(model);

        CommandResult result = new ViewCommand(Index.fromOneBased(1)).execute(model);
        assertEquals(cara, result.getStudentToView().orElseThrow());
        assertEquals(String.format(ViewCommand.MESSAGE_SUCCESS, "Cara Lim", "A0000003C"), result.getFeedbackToUser());
        assertEquals(List.of(cara, bob), model.getFilteredStudentList());
        assertEquals(List.of(bob, alice, cara), model.getStudentDirectory().getStudentList());
    }

    @Test
    public void execute_emptyOrOutOfRange_throwsCommandException() {
        ModelManager model = new ModelManager();
        ViewCommand command = new ViewCommand(Index.fromOneBased(1));
        CommandException error = assertThrows(CommandException.class, () -> command.execute(model));
        assertEquals(Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, error.getMessage());
        model.addStudent(new StudentBuilder().build());
        assertThrows(CommandException.class, () -> new ViewCommand(Index.fromOneBased(2)).execute(model));
    }
}
