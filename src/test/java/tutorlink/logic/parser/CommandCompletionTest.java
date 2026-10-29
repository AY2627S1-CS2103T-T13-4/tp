package tutorlink.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tutorlink.logic.parser.CommandCompletion.Suggestion;
import tutorlink.model.ModelManager;
import tutorlink.model.student.Student;
import tutorlink.testutil.StudentBuilder;

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
    public void suggest_addEscapedAddressPrefixes_preservesHintsAndAddress() throws Exception {
        for (String prefix : new String[] {"n/", "m/", "g/", "p/", "e/", "a/", "t/"}) {
            for (String address : new String[] {"Block 10 \\" + prefix + "Lane", "\\" + prefix + "Lane"}) {
                String input = "add a/" + address + " n/Alice";
                assertSuggestion(input, " m/MATRIC_NUMBER g/TUTORIAL_GROUP [p/PHONE] [e/EMAIL] [t/TAG]...", " m/");
                input = accept(input, input + " m/") + "A0123456X";
                input = accept(input, input + " g/") + "T01";
                assertSuggestion(input, " [p/PHONE] [e/EMAIL] [t/TAG]...", " p/");

                ModelManager model = new ModelManager();
                new TutorLinkParser().parseCommand(input).execute(model);
                Student added = model.getStudentDirectory().getStudentList().getFirst();
                assertEquals("Alice", added.getName().fullName);
                assertEquals(address.replace("\\", ""), added.getAddress().value);
            }
        }
    }

    @Test
    public void suggest_editEscapedAddressPrefixes_preservesHintsAndAddress() throws Exception {
        for (String prefix : new String[] {"n/", "m/", "g/", "p/", "e/", "a/", "t/"}) {
            for (String address : new String[] {"Block 10 \\" + prefix + "Lane", "\\" + prefix + "Lane"}) {
                String input = "edit 1 a/" + address + " n/Alice";
                assertSuggestion(input, " [m/MATRIC_NUMBER] [g/TUTORIAL_GROUP] [p/PHONE] [e/EMAIL] [t/TAG]...",
                        " m/");
                input = accept(input, input + " m/") + "A0123456X";
                assertSuggestion(input, " [g/TUTORIAL_GROUP] [p/PHONE] [e/EMAIL] [t/TAG]...", " g/");

                ModelManager model = new ModelManager();
                model.addStudent(new StudentBuilder().build());
                new TutorLinkParser().parseCommand(input).execute(model);
                Student edited = model.getStudentDirectory().getStudentList().getFirst();
                assertEquals("Alice", edited.getName().fullName);
                assertEquals(address.replace("\\", ""), edited.getAddress().value);
            }
        }
    }

    @Test
    public void suggest_addressEscapeBoundaries_keepsRealPrefixesAndRejectsUnknownPrefixes() {
        assertSuggestion("add a/Block 10 \\p/Lane", " n/NAME m/MATRIC_NUMBER g/TUTORIAL_GROUP"
                + " [p/PHONE] [e/EMAIL] [t/TAG]...", " n/");
        assertSuggestion("edit 1 a/Block 10 \\p/Lane", " [n/NAME] [m/MATRIC_NUMBER] [g/TUTORIAL_GROUP]"
                + " [p/PHONE] [e/EMAIL] [t/TAG]...", " n/");
        assertSuggestion("add a/Block 10 p/91234567 n/Alice", " m/MATRIC_NUMBER g/TUTORIAL_GROUP"
                + " [e/EMAIL] [t/TAG]...", " m/");
        assertSuggestion("add a/Block 10 \\x/Lane n/Alice", " m/MATRIC_NUMBER g/TUTORIAL_GROUP"
                + " [p/PHONE] [e/EMAIL] [t/TAG]...", " m/");
        assertSuggestion("edit 1 a/Block 10 \\x/Lane", " [n/NAME] [m/MATRIC_NUMBER] [g/TUTORIAL_GROUP]"
                + " [p/PHONE] [e/EMAIL] [t/TAG]...", " n/");
        assertSuggestion("add a/Block 10 \\p/Lane x/value", " n/NAME m/MATRIC_NUMBER g/TUTORIAL_GROUP"
                + " [p/PHONE] [e/EMAIL] [t/TAG]...", " n/");
        for (String input : new String[] {"add n/Alice \\p/Lane", "edit 1 a/Block 10 \\p/Lane n/A n/B"}) {
            assertSuggestion(input, "", "");
        }
    }

    @Test
    public void suggest_freeTextAddressSlashes_preservesHintsAndStoredAddress() throws Exception {
        for (String command : new String[] {"add", "edit 1"}) {
            for (String address : new String[] {"Block 10 North/Lane", "Block 10 \\x/Lane",
                "Block 10 \\\\p/Lane", "C:\\Rooms\\p/Lane", "Block 10 x/value", "Block 10 r/Note"}) {
                String input = command + " n/Alice m/A0123456X g/T01 a/" + address;
                assertSuggestion(input, " [p/PHONE] [e/EMAIL] [t/TAG]...", " p/");
                input = accept(input, input + " p/") + "91234567";
                assertSuggestion(input, " [e/EMAIL] [t/TAG]...", " e/");

                ModelManager model = new ModelManager();
                if (command.equals("edit 1")) {
                    model.addStudent(new StudentBuilder().build());
                }
                new TutorLinkParser().parseCommand(input).execute(model);
                Student student = model.getStudentDirectory().getStudentList().getFirst();
                assertEquals(address, student.getAddress().value);
                assertEquals("91234567", student.getPhone().value);
            }
        }
    }

    @Test
    public void suggest_multipleAddressEscapes_preservesLiteralTextAndRealContactPrefix() throws Exception {
        for (String command : new String[] {"add", "edit 1"}) {
            String input = command + " n/Alice m/A0123456X g/T01"
                    + " a/Block 10 \\p/Lane \\e/Centre p/91234567";
            assertSuggestion(input, " [e/EMAIL] [t/TAG]...", " e/");

            ModelManager model = new ModelManager();
            if (command.equals("edit 1")) {
                model.addStudent(new StudentBuilder().build());
            }
            new TutorLinkParser().parseCommand(input).execute(model);
            Student student = model.getStudentDirectory().getStudentList().getFirst();
            assertEquals("Block 10 p/Lane e/Centre", student.getAddress().value);
            assertEquals("91234567", student.getPhone().value);
        }
    }

    @Test
    public void suggest_emptyEditableValues_preservesClearOperations() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withTags("friend").withRemark("Needs practice").build());

        String edit = "edit 1 p/ e/ a/ t/";
        assertSuggestion(edit, " [n/NAME] [m/MATRIC_NUMBER] [g/TUTORIAL_GROUP]", " n/");
        new TutorLinkParser().parseCommand(edit).execute(model);
        Student edited = model.getStudentDirectory().getStudentList().getFirst();
        assertEquals(StudentBuilder.DEFAULT_NAME, edited.getName().fullName);
        assertNull(edited.getPhone());
        assertNull(edited.getEmail());
        assertNull(edited.getAddress());
        assertTrue(edited.getTags().isEmpty());
        assertEquals("Needs practice", edited.getRemark());

        String remark = "remark 1 r/";
        assertSuggestion(remark, "", "");
        new TutorLinkParser().parseCommand(remark).execute(model);
        assertNull(model.getStudentDirectory().getStudentList().getFirst().getRemark());
    }

    @Test
    public void suggest_emptyRequiredEditField_doesNotAdvanceOrTreatItAsClearing() {
        assertSuggestion("edit 1 n/", "NAME [m/MATRIC_NUMBER] [g/TUTORIAL_GROUP] [p/PHONE]"
                + " [e/EMAIL] [a/ADDRESS] [t/TAG]...", "");
        assertSuggestion("edit 1 n/ p/", "", "");
        assertSuggestion("edit 1 m/ e/", "", "");
        assertSuggestion("edit 1 g/ t/", "", "");
    }

    @Test
    public void suggest_emptyLastValueWithNoRemainingPrefixes_displaysOnlyItsPlaceholder() {
        assertSuggestion("add n/Alice m/A0123456X g/T01 p/91234567 e/alice@example.com a/Block 10 t/", "TAG", "");
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
        assertSuggestion("sort name", " [asc|desc]", "");
        assertSuggestion("sort reset", "", "");
        for (String input : new String[] {"help", "exit", "clear", "groups", "followups"}) {
            assertSuggestion(input, "", "");
            assertSuggestion(input + " unexpected", "", "");
        }
    }

    @Test
    public void suggest_suppliedPositionalArguments_doesNotAppendAdditionalPlaceholders() {
        for (String input : new String[] {"findtag friend", "view 1", "sort name asc", "sort group desc",
            "sort unsupported", "delete 0", "followup invalid", "followup 1 done"}) {
            assertSuggestion(input, "", "");
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
