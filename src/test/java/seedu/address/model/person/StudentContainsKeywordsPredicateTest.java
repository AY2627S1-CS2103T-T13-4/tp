package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class StudentContainsKeywordsPredicateTest {

    @Test
    public void constructor_keywordsChangedLater_keepsOriginalSearch() {
        List<String> keywords = new ArrayList<>(List.of("Alice"));
        StudentContainsKeywordsPredicate predicate = new StudentContainsKeywordsPredicate(keywords);
        keywords.clear();

        assertTrue(predicate.test(new PersonBuilder().withName("Alice").build()));
    }

    @Test
    public void test_fullMatricNumberIgnoringCase_returnsTrue() {
        Person student = new PersonBuilder().withName("Alice").withMatricNumber("A0123456B").build();
        assertTrue(new StudentContainsKeywordsPredicate(List.of("A0123456B")).test(student));
        assertTrue(new StudentContainsKeywordsPredicate(List.of("a0123456b")).test(student));
        assertTrue(new StudentContainsKeywordsPredicate(List.of("Carol", "A0123456B")).test(student));
    }

    @Test
    public void test_mixedNameAndMatricKeywords_matchesEitherField() {
        StudentContainsKeywordsPredicate predicate =
                new StudentContainsKeywordsPredicate(List.of("Alice", "a0123456b"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Bob").withMatricNumber("A7654321C").build()));
        assertTrue(predicate.test(new PersonBuilder().withName("Carol").withMatricNumber("A0123456B").build()));
        assertFalse(predicate.test(new PersonBuilder().withName("Daniel").withMatricNumber("A7654321C").build()));
    }

    @Test
    public void test_partialOrNonMatchingKeywords_returnsFalse() {
        Person student = new PersonBuilder().withName("Alice Bob").withMatricNumber("A0123456B").build();
        for (String keyword : List.of("Ali", "A0123", "0123456", "A0123456C", "unknown", "A0123456B!")) {
            assertFalse(new StudentContainsKeywordsPredicate(List.of(keyword)).test(student));
        }
    }

    @Test
    public void test_keywordsMatchOtherFields_returnsFalse() {
        Person student = new PersonBuilder().withName("Alice").withMatricNumber("A0123456B")
                .withTutorialGroup("T01").withTags("friends").withRemark("Scholarship").build();
        StudentContainsKeywordsPredicate predicate =
                new StudentContainsKeywordsPredicate(List.of("T1", "friends", "Scholarship"));
        assertFalse(predicate.test(student));
    }

    @Test
    public void equals() {
        List<String> firstPredicateKeywordList = List.of("first");
        List<String> secondPredicateKeywordList = List.of("first", "second");

        StudentContainsKeywordsPredicate firstPredicate =
                new StudentContainsKeywordsPredicate(firstPredicateKeywordList);
        StudentContainsKeywordsPredicate secondPredicate =
                new StudentContainsKeywordsPredicate(secondPredicateKeywordList);

        // same object -> returns true
        assertTrue(firstPredicate.equals(firstPredicate));

        // same values -> returns true
        StudentContainsKeywordsPredicate firstPredicateCopy =
                new StudentContainsKeywordsPredicate(firstPredicateKeywordList);
        assertTrue(firstPredicate.equals(firstPredicateCopy));

        // different types -> returns false
        assertFalse(firstPredicate.equals(1));

        // null -> returns false
        assertFalse(firstPredicate.equals(null));

        // different person -> returns false
        assertFalse(firstPredicate.equals(secondPredicate));
    }

    @Test
    public void test_nameContainsKeywords_returnsTrue() {
        // One keyword
        StudentContainsKeywordsPredicate predicate = new StudentContainsKeywordsPredicate(List.of("Alice"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Bob").build()));

        // Multiple keywords
        predicate = new StudentContainsKeywordsPredicate(List.of("Alice", "Bob"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Bob").build()));

        // Only one matching keyword
        predicate = new StudentContainsKeywordsPredicate(List.of("Bob", "Carol"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Carol").build()));

        // Mixed-case keywords
        predicate = new StudentContainsKeywordsPredicate(List.of("aLIce", "bOB"));
        assertTrue(predicate.test(new PersonBuilder().withName("Alice Bob").build()));
    }

    @Test
    public void test_nameDoesNotContainKeywords_returnsFalse() {
        // Zero keywords
        StudentContainsKeywordsPredicate predicate = new StudentContainsKeywordsPredicate(List.of());
        assertFalse(predicate.test(new PersonBuilder().withName("Alice").build()));

        // Non-matching keyword
        predicate = new StudentContainsKeywordsPredicate(List.of("Carol"));
        assertFalse(predicate.test(new PersonBuilder().withName("Alice Bob").build()));

        // Keywords match phone, email and address, but do not match name
        predicate = new StudentContainsKeywordsPredicate(List.of("12345", "alice@email.com", "Main", "Street"));
        assertFalse(predicate.test(new PersonBuilder().withName("Alice").withPhone("12345")
                .withEmail("alice@email.com").withAddress("Main Street").build()));
    }

    @Test
    public void toStringMethod() {
        List<String> keywords = List.of("keyword1", "keyword2");
        StudentContainsKeywordsPredicate predicate = new StudentContainsKeywordsPredicate(keywords);

        String expected = StudentContainsKeywordsPredicate.class.getCanonicalName() + "{keywords=" + keywords + "}";
        assertEquals(expected, predicate.toString());
    }
}
