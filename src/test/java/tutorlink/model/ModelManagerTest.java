package tutorlink.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.model.Model.PREDICATE_SHOW_ALL_STUDENTS;
import static tutorlink.testutil.Assert.assertThrows;
import static tutorlink.testutil.TypicalStudents.ALICE;
import static tutorlink.testutil.TypicalStudents.BENSON;
import static tutorlink.testutil.TypicalStudents.CARL;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorlink.commons.core.GuiSettings;
import tutorlink.model.student.NameContainsKeywordsPredicate;
import tutorlink.model.student.Student;
import tutorlink.model.student.TutorialGroupMatchesPredicate;
import tutorlink.testutil.StudentBuilder;
import tutorlink.testutil.StudentDirectoryBuilder;

public class ModelManagerTest {

    private ModelManager modelManager = new ModelManager();

    @Test
    public void constructor() {
        assertEquals(new UserPrefs(), modelManager.getUserPrefs());
        assertEquals(new GuiSettings(), modelManager.getGuiSettings());
        assertEquals(new StudentDirectory(), new StudentDirectory(modelManager.getStudentDirectory()));
    }

    @Test
    public void constructor_validUserPrefs_copiesUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        modelManager = new ModelManager(new StudentDirectory(), userPrefs);
        assertEquals(userPrefs, modelManager.getUserPrefs());

        // Modifying userPrefs should not modify modelManager's userPrefs
        UserPrefs oldUserPrefs = new UserPrefs(userPrefs);
        userPrefs.setGuiSettings(new GuiSettings(5, 6, 7, 8));
        assertEquals(oldUserPrefs, modelManager.getUserPrefs());
    }

    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.setGuiSettings(null));
    }

    @Test
    public void setGuiSettings_validGuiSettings_setsGuiSettings() {
        GuiSettings guiSettings = new GuiSettings(1, 2, 3, 4);
        modelManager.setGuiSettings(guiSettings);
        assertEquals(guiSettings, modelManager.getGuiSettings());
    }

    @Test
    public void hasStudent_nullStudent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.hasStudent(null));
    }

    @Test
    public void hasStudent_studentNotInStudentDirectory_returnsFalse() {
        assertFalse(modelManager.hasStudent(ALICE));
    }

    @Test
    public void hasStudent_studentInStudentDirectory_returnsTrue() {
        modelManager.addStudent(ALICE);
        assertTrue(modelManager.hasStudent(ALICE));
    }

    @Test
    public void getFilteredStudentList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> modelManager.getFilteredStudentList().remove(0));
    }

    @Test
    public void sortStudents_nullSortOrder_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                "Student sort order must not be null.", () -> modelManager.sortStudents(null));
    }

    @Test
    public void sortStudents_nullSortOrder_preservesActiveFilterAndSorting() {
        StudentDirectory directory = new StudentDirectoryBuilder()
                .withStudent(BENSON).withStudent(CARL).withStudent(ALICE).build();
        modelManager = new ModelManager(directory, new UserPrefs());
        modelManager.updateFilteredStudentList(new TutorialGroupMatchesPredicate(ALICE.getTutorialGroup()));
        modelManager.sortStudents(StudentSortOrder.NAME_ASC);

        assertThrows(NullPointerException.class, () -> modelManager.sortStudents(null));
        assertEquals(List.of(ALICE, BENSON), modelManager.getFilteredStudentList());
        assertEquals(directory, modelManager.getStudentDirectory());

        Student editedAlice = new StudentBuilder(ALICE).withName("Zoe").build();
        modelManager.setStudent(ALICE, editedAlice);
        assertEquals(List.of(BENSON, editedAlice), modelManager.getFilteredStudentList());
    }

    @Test
    public void getFilteredStudentList_sortedList_cannotModify() {
        modelManager.addStudent(ALICE);
        modelManager.sortStudents(StudentSortOrder.NAME_ASC);
        assertThrows(UnsupportedOperationException.class, () -> modelManager.getFilteredStudentList().remove(0));
    }

    @Test
    public void equals_emptyModels_comparesSortOrderValues() {
        for (StudentSortOrder firstOrder : StudentSortOrder.values()) {
            for (StudentSortOrder secondOrder : StudentSortOrder.values()) {
                ModelManager firstModel = new ModelManager();
                ModelManager secondModel = new ModelManager();
                firstModel.sortStudents(firstOrder);
                secondModel.sortStudents(secondOrder);
                assertEquals(firstOrder == secondOrder, firstModel.equals(secondModel));
            }
        }
    }

    @Test
    public void equals_independentlySortedModelsWithStudents_returnsTrue() {
        StudentDirectory directory = new StudentDirectoryBuilder()
                .withStudent(BENSON).withStudent(ALICE).build();
        for (StudentSortOrder sortOrder : StudentSortOrder.values()) {
            ModelManager firstModel = new ModelManager(directory, new UserPrefs());
            ModelManager secondModel = new ModelManager(directory, new UserPrefs());
            firstModel.sortStudents(sortOrder);
            secondModel.sortStudents(sortOrder);
            assertEquals(firstModel, secondModel);
        }
    }

    @Test
    public void sortStudents_directoryOrder_restoresOrderAndStateWithoutChangingFilter() {
        StudentDirectory directory = new StudentDirectoryBuilder()
                .withStudent(BENSON).withStudent(CARL).withStudent(ALICE).build();
        TutorialGroupMatchesPredicate predicate = new TutorialGroupMatchesPredicate(ALICE.getTutorialGroup());
        ModelManager expectedModel = new ModelManager(directory, new UserPrefs());
        expectedModel.updateFilteredStudentList(predicate);
        for (StudentSortOrder sortOrder : List.of(StudentSortOrder.NAME_ASC, StudentSortOrder.NAME_DESC)) {
            modelManager = new ModelManager(directory, new UserPrefs());
            modelManager.updateFilteredStudentList(predicate);
            modelManager.sortStudents(sortOrder);
            modelManager.sortStudents(StudentSortOrder.DIRECTORY);
            assertEquals(List.of(BENSON, ALICE), modelManager.getFilteredStudentList());
            assertEquals(expectedModel, modelManager);
            assertEquals(directory, modelManager.getStudentDirectory());
        }
    }

    @Test
    public void updateFilteredStudentList_afterSorting_restoresOrderAndState() {
        StudentDirectory directory = new StudentDirectoryBuilder()
                .withStudent(BENSON).withStudent(CARL).withStudent(ALICE).build();
        TutorialGroupMatchesPredicate predicate = new TutorialGroupMatchesPredicate(ALICE.getTutorialGroup());
        ModelManager expectedModel = new ModelManager(directory, new UserPrefs());
        expectedModel.updateFilteredStudentList(predicate);
        for (StudentSortOrder sortOrder : List.of(StudentSortOrder.NAME_ASC, StudentSortOrder.NAME_DESC)) {
            modelManager = new ModelManager(directory, new UserPrefs());
            modelManager.sortStudents(sortOrder);
            modelManager.updateFilteredStudentList(predicate);
            assertEquals(List.of(BENSON, ALICE), modelManager.getFilteredStudentList());
            assertEquals(expectedModel, modelManager);
        }
    }

    @Test
    public void setStudentDirectory_afterSorting_restoresOrderAndState() {
        StudentDirectory replacement = new StudentDirectoryBuilder()
                .withStudent(BENSON).withStudent(ALICE).build();
        ModelManager expectedModel = new ModelManager(replacement, new UserPrefs());
        for (StudentSortOrder sortOrder : List.of(StudentSortOrder.NAME_ASC, StudentSortOrder.NAME_DESC)) {
            modelManager.sortStudents(sortOrder);
            modelManager.setStudentDirectory(replacement);
            assertEquals(List.of(BENSON, ALICE), modelManager.getFilteredStudentList());
            assertEquals(expectedModel, modelManager);
        }
    }

    @Test
    public void addStudent_afterFilteredSort_restoresFullRosterOrderAndState() {
        StudentDirectory directory = new StudentDirectoryBuilder()
                .withStudent(BENSON).withStudent(ALICE).build();
        ModelManager expectedModel = new ModelManager(directory, new UserPrefs());
        expectedModel.addStudent(CARL);
        for (StudentSortOrder sortOrder : List.of(StudentSortOrder.NAME_ASC, StudentSortOrder.NAME_DESC)) {
            modelManager = new ModelManager(directory, new UserPrefs());
            modelManager.updateFilteredStudentList(new NameContainsKeywordsPredicate(List.of("Alice")));
            modelManager.sortStudents(sortOrder);
            modelManager.addStudent(CARL);
            assertEquals(List.of(BENSON, ALICE, CARL), modelManager.getFilteredStudentList());
            assertEquals(expectedModel, modelManager);
        }
    }

    @Test
    public void equals() {
        StudentDirectory studentDirectory = new StudentDirectoryBuilder()
                .withStudent(ALICE).withStudent(BENSON).build();
        StudentDirectory differentStudentDirectory = new StudentDirectory();
        UserPrefs userPrefs = new UserPrefs();

        // same values -> returns true
        modelManager = new ModelManager(studentDirectory, userPrefs);
        ModelManager modelManagerCopy = new ModelManager(studentDirectory, userPrefs);
        assertTrue(modelManager.equals(modelManagerCopy));

        // same object -> returns true
        assertTrue(modelManager.equals(modelManager));

        // null -> returns false
        assertFalse(modelManager.equals(null));

        // different types -> returns false
        assertFalse(modelManager.equals(5));

        // different studentDirectory -> returns false
        assertFalse(modelManager.equals(new ModelManager(differentStudentDirectory, userPrefs)));

        // different filteredList -> returns false
        String[] keywords = ALICE.getName().fullName.split("\\s+");
        modelManager.updateFilteredStudentList(new NameContainsKeywordsPredicate(List.of(keywords)));
        assertFalse(modelManager.equals(new ModelManager(studentDirectory, userPrefs)));

        // resets modelManager to initial state for upcoming tests
        modelManager.updateFilteredStudentList(PREDICATE_SHOW_ALL_STUDENTS);

        // different userPrefs -> returns false
        UserPrefs differentUserPrefs = new UserPrefs();
        differentUserPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertFalse(modelManager.equals(new ModelManager(studentDirectory, differentUserPrefs)));
    }
}
