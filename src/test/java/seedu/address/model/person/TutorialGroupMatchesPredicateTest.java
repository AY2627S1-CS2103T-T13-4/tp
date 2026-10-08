package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class TutorialGroupMatchesPredicateTest {

    @Test
    public void equals() {
        TutorialGroupMatchesPredicate firstPredicate =
                new TutorialGroupMatchesPredicate(new TutorialGroup("T01"));
        TutorialGroupMatchesPredicate firstPredicateCopy =
                new TutorialGroupMatchesPredicate(new TutorialGroup("t01"));
        TutorialGroupMatchesPredicate secondPredicate =
                new TutorialGroupMatchesPredicate(new TutorialGroup("T02"));

        assertTrue(firstPredicate.equals(firstPredicate));
        assertTrue(firstPredicate.equals(firstPredicateCopy));
        assertEquals(firstPredicate.hashCode(), firstPredicateCopy.hashCode());
        assertFalse(firstPredicate.equals(secondPredicate));
        assertFalse(firstPredicate.equals(null));
        assertFalse(firstPredicate.equals(1));
    }

    @Test
    public void test_sameTutorialGroup_returnsTrue() {
        TutorialGroupMatchesPredicate predicate =
                new TutorialGroupMatchesPredicate(new TutorialGroup("T01"));
        Person person = new PersonBuilder().withTutorialGroup("T01").build();

        assertTrue(predicate.test(person));
    }

    @Test
    public void test_differentTutorialGroup_returnsFalse() {
        TutorialGroupMatchesPredicate predicate =
                new TutorialGroupMatchesPredicate(new TutorialGroup("T01"));
        Person person = new PersonBuilder().withTutorialGroup("T02").build();

        assertFalse(predicate.test(person));
    }

    @Test
    public void toStringMethod() {
        TutorialGroup tutorialGroup = new TutorialGroup("T01");
        TutorialGroupMatchesPredicate predicate = new TutorialGroupMatchesPredicate(tutorialGroup);

        String expected = TutorialGroupMatchesPredicate.class.getCanonicalName()
                + "{tutorialGroup=" + tutorialGroup + "}";
        assertEquals(expected, predicate.toString());
    }
}
