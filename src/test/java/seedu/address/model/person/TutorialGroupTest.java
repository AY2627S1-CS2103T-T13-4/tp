package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class TutorialGroupTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TutorialGroup(null));
    }

    @Test
    public void constructor_invalidTutorialGroup_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TutorialGroup("T123"));
    }

    @Test
    public void isValidTutorialGroup() {
        assertThrows(NullPointerException.class, () -> TutorialGroup.isValidTutorialGroup(null));

        assertFalse(TutorialGroup.isValidTutorialGroup(""));
        assertFalse(TutorialGroup.isValidTutorialGroup("T"));
        assertFalse(TutorialGroup.isValidTutorialGroup("T123"));
        assertFalse(TutorialGroup.isValidTutorialGroup("G1"));
        assertFalse(TutorialGroup.isValidTutorialGroup("T1A"));
        assertFalse(TutorialGroup.isValidTutorialGroup(" T1"));
        assertFalse(TutorialGroup.isValidTutorialGroup("T5"));

        assertTrue(TutorialGroup.isValidTutorialGroup("T05"));
        assertTrue(TutorialGroup.isValidTutorialGroup("L03"));
        assertTrue(TutorialGroup.isValidTutorialGroup("W12"));
        assertTrue(TutorialGroup.isValidTutorialGroup("t10"));
        assertTrue(TutorialGroup.isValidTutorialGroup("l12"));
    }

    @Test
    public void constructor_validTutorialGroup_normalizesValue() {
        assertEquals("T05", new TutorialGroup("t05").toString());
        assertEquals("L03", new TutorialGroup("L03").toString());
        assertEquals("T10", new TutorialGroup("t10").toString());
        assertEquals("L12", new TutorialGroup("l12").toString());
    }

    @Test
    public void equals() {
        TutorialGroup tutorialGroup = new TutorialGroup("L03");

        assertTrue(tutorialGroup.equals(new TutorialGroup("l03")));
        assertTrue(tutorialGroup.equals(tutorialGroup));
        assertFalse(tutorialGroup.equals(null));
        assertFalse(tutorialGroup.equals(5));
        assertFalse(tutorialGroup.equals(new TutorialGroup("T03")));
        assertFalse(tutorialGroup.equals(new TutorialGroup("L04")));
    }

    @Test
    public void hashCode_equivalentValues_sameHashCode() {
        assertEquals(new TutorialGroup("L03").hashCode(), new TutorialGroup("l03").hashCode());
    }
}
