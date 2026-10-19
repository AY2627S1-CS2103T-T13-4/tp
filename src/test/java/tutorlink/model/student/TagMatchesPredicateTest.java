package tutorlink.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorlink.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import tutorlink.model.tag.Tag;
import tutorlink.testutil.StudentBuilder;

public class TagMatchesPredicateTest {

    @Test
    public void constructor_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TagMatchesPredicate(null));
    }

    @Test
    public void test_exactTagIgnoringCase_returnsTrue() {
        TagMatchesPredicate predicate = new TagMatchesPredicate(new Tag("needsHelp"));
        assertTrue(predicate.test(new StudentBuilder().withTags("needsHelp").build()));
        assertTrue(predicate.test(new StudentBuilder().withTags("other", "NEEDSHELP").build()));
        assertTrue(predicate.test(new StudentBuilder().withTags("Needshelp", "needsHelp").build()));
    }

    @Test
    public void test_partialOrDifferentTag_returnsFalse() {
        TagMatchesPredicate predicate = new TagMatchesPredicate(new Tag("needsHelp"));
        assertFalse(predicate.test(new StudentBuilder().withTags("needs", "Help").build()));
        assertFalse(predicate.test(new StudentBuilder().withTags("needsHelpSoon").build()));
        assertFalse(predicate.test(new StudentBuilder().withTags("other").build()));
    }

    @Test
    public void test_noTagsOrMatchingOtherFields_returnsFalse() {
        Student student = new StudentBuilder().withName("NeedsHelp").withMatricNumber("A0123456B")
                .withTutorialGroup("T01").withPhone("12345").withEmail("needsHelp@example.com")
                .withAddress("NeedsHelp").withRemark("NeedsHelp").withTags().build();
        assertFalse(new TagMatchesPredicate(new Tag("needsHelp")).test(student));
        assertFalse(new TagMatchesPredicate(new Tag("A0123456B")).test(student));
        assertFalse(new TagMatchesPredicate(new Tag("T01")).test(student));
        assertFalse(new TagMatchesPredicate(new Tag("12345")).test(student));
    }

    @Test
    public void equals() {
        TagMatchesPredicate predicate = new TagMatchesPredicate(new Tag("friends"));
        TagMatchesPredicate copy = new TagMatchesPredicate(new Tag("friends"));
        assertTrue(predicate.equals(predicate));
        assertTrue(predicate.equals(copy));
        assertEquals(predicate.hashCode(), copy.hashCode());
        assertFalse(predicate.equals(new TagMatchesPredicate(new Tag("family"))));
        assertFalse(predicate.equals(null));
        assertFalse(predicate.equals(1));
    }

    @Test
    public void toStringMethod() {
        Tag tag = new Tag("friends");
        TagMatchesPredicate predicate = new TagMatchesPredicate(tag);
        assertEquals(TagMatchesPredicate.class.getCanonicalName() + "{tag=" + tag + "}", predicate.toString());
    }
}
