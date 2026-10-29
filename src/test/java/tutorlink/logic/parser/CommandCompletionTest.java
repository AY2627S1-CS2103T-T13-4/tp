package tutorlink.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tutorlink.logic.parser.CommandCompletion.Suggestion;

public class CommandCompletionTest {
    private final CommandCompletion completion = new CommandCompletion();

    @Test
    public void suggest_uniqueAndAmbiguousCommands_completesOnlyUniqueName() {
        assertSuggestion("ad", "d n/NAME m/MATRIC_NUMBER g/TUTORIAL_GROUP [p/PHONE] [e/EMAIL]"
                + " [a/ADDRESS] [t/TAG]...", "d");
        assertSuggestion("  hel", "p", "p");
        for (String input : new String[] {"", " ", "f", "fi", "follow", "unknown", "ad n/Alice"}) {
            assertSuggestion(input, "", "");
        }
        // An exact command wins over longer command names such as findtag and followups.
        assertSuggestion("find", " KEYWORD [MORE_KEYWORDS]...", "");
        assertSuggestion("followup", " INDEX on|done", "");
    }

    @Test
    public void suggest_add_acceptsOnePrefixAndPreservesSpacedValues() {
        String input = "ad";
        input = accept(input, "add");
        input = accept(input, "add n/");
        assertEquals("", completion.suggest(input).append());
        assertEquals("NAME m/MATRIC_NUMBER g/TUTORIAL_GROUP [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...",
                completion.suggest(input).display());
        input += "Alice Tan";
        input = accept(input, "add n/Alice Tan m/");
        input += "A0123456X";
        input = accept(input, "add n/Alice Tan m/A0123456X g/");
        input += "T01";
        assertEquals(" [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...", completion.suggest(input).display());
        input = accept(input, "add n/Alice Tan m/A0123456X g/T01 p/");
        input += "91234567";
        input = accept(input, "add n/Alice Tan m/A0123456X g/T01 p/91234567 e/");
    }

    @Test
    public void suggest_manualOrderAndRepeatedTags_skipsSuppliedPrefixes() {
        assertSuggestion("add g/T01 n/Alice  Tan t/one t/two",
                " m/MATRIC_NUMBER [p/PHONE] [e/EMAIL] [a/ADDRESS]", " m/");
        assertSuggestion("add n/Alice m/A0123456X g/T01 p/91234567 e/a@b.com a/Some Place t/tag", "", "");
        assertSuggestion("add n/Alice ", "m/MATRIC_NUMBER g/TUTORIAL_GROUP [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...",
                "m/");
        assertSuggestion("edit 2 g/T02 n/Alice",
                " [m/MATRIC_NUMBER] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...", " m/");
        assertSuggestion("edit 2 p/", " [n/NAME] [m/MATRIC_NUMBER] [g/TUTORIAL_GROUP] [e/EMAIL] [a/ADDRESS] [t/TAG]...",
                " n/");
    }

    @Test
    public void suggest_incompleteOrUnknownArguments_doesNotAppend() {
        for (String input : new String[] {"add Alice", "add n/Alice x/value", "add n/ m/A0123456X",
            "add n/Alice n/Bob", "edit nope", "edit 0", "edit 999999999999", "remark 1 x/text", "list random"}) {
            assertSuggestion(input, "", "");
        }
        assertSuggestion("list g/", "TUTORIAL_GROUP", "");
        assertSuggestion("remark 1 r/Needs help with recursion", "", "");
        assertSuggestion("list g/T01", "", "");
    }

    @Test
    public void suggest_allCommandSyntax_neverInsertsPositionalValues() {
        assertSuggestion("edit", " INDEX [n/NAME] [m/MATRIC_NUMBER] [g/TUTORIAL_GROUP] [p/PHONE] [e/EMAIL]"
                + " [a/ADDRESS] [t/TAG]...", "");
        assertSuggestion("remark", " INDEX r/REMARK", "");
        assertSuggestion("remark 1", " r/REMARK", " r/");
        assertSuggestion("list", " [g/TUTORIAL_GROUP]", " g/");
        assertSuggestion("view", " INDEX", "");
        assertSuggestion("delete", " INDEX [confirm]", "");
        assertSuggestion("delete 2", " [confirm]", "");
        assertSuggestion("delete 2 confirm", "", "");
        assertSuggestion("findtag", " TAG", "");
        assertSuggestion("find Alice Tan", "", "");
        assertSuggestion("followup 2 ", "on|done", "");
        assertSuggestion("followup 2 on", "", "");
        assertSuggestion("sort", " name|group [asc|desc] OR reset", "");
        assertSuggestion("sort group", " [asc|desc]", "");
        assertSuggestion("sort reset", "", "");
        for (String input : new String[] {"help", "exit", "clear", "groups", "followups"}) {
            assertSuggestion(input, "", "");
            assertSuggestion(input + " unexpected", "", "");
        }
    }

    private String accept(String input, String expected) {
        String result = input + completion.suggest(input).append();
        assertEquals(expected, result);
        return result;
    }

    private void assertSuggestion(String input, String display, String append) {
        assertEquals(new Suggestion(display, append), completion.suggest(input), input);
    }
}
