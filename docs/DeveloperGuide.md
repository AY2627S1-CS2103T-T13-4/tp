---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# TutorLink Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* TutorLink adapts the architecture, command framework, storage, testing utilities, and documentation of
  [AddressBook-Level3](https://github.com/se-edu/addressbook-level3) by the [SE-EDU initiative](https://se-education.org).
* Libraries include [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson),
  [JUnit](https://junit.org/), and [Guava](https://github.com/google/guava).

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-T13-4/tp/tree/master/src/main/java/tutorlink/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-T13-4/tp/tree/master/src/main/java/tutorlink/MainApp.java)) is in charge of the app launch and shut down.
* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the user issues the command `delete 1 confirm`.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-T13-4/tp/tree/master/src/main/java/tutorlink/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` containing `CommandBox`, `ResultDisplay`, `GroupListPanel`, `StudentListPanel`,
`StudentDetailsPanel`, and `StatusBarFooter`. These components inherit from `UiPart`.

The group panel observes `Logic#getStudentList()`, the complete read-only roster, so its groups and counts remain
available during searches. Selecting a group executes the existing `list` command through `Logic`. The student panel
observes `Logic#getFilteredStudentList()` directly, keeping its summary indices aligned with indexed commands.
Selecting a summary, or executing `view INDEX`, displays a `StudentCard` in the details panel. `ViewCommand` validates
the displayed index and returns the student in `CommandResult`; `MainWindow` then selects and scrolls to that student.
This view-only result does not write roster data. After each command, `MainWindow` restores the
selection across record replacements if that student remains visible, and otherwise clears the details. Three resizable
columns separate group navigation, student summaries, and full details.

`StudentCard` displays a bounded preview for remarks longer than 160 characters and attaches the complete remark as a
tooltip. This keeps long free-text notes from making the details card unreasonably tall while preserving access to the
stored value.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-T13-4/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-T13-4/tp/tree/master/src/main/java/tutorlink/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Student` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-T13-4/tp/tree/master/src/main/java/tutorlink/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1 confirm")` API call as an example.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1 confirm` Command" />

<box type="info" seamless>

**Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `TutorLinkParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a student).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a user command, the `TutorLinkParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `TutorLinkParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-T13-4/tp/tree/master/src/main/java/tutorlink/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="1000" />


The `Model` component,

* stores the student directory data i.e., all `Student` objects (which are contained in a `UniqueStudentList` object).
* maintains a `FilteredList<Student>` over the directory's unmodifiable student list and a `SortedList<Student>`
  over that filtered view. `getFilteredStudentList()` exposes the sorted view as an `ObservableList<Student>`
  that the UI can observe, so displayed indices follow the current filter and sorting order.
* uses `StudentSortOrder` to choose name or tutorial-group ordering in either direction. Sorting changes only the
  displayed order, preserving the current filter and underlying directory order. `sort reset` selects `DIRECTORY`
  (a null comparator), restoring directory order for the current results. Updating the filter also resets sorting.
* exposes the complete `StudentDirectory` separately from the filtered list, allowing commands such as `groups` to
  aggregate data without being affected by the current display filter.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `StudentDirectory`, and each `Student` references tags from that list. This lets `StudentDirectory` maintain one `Tag` object per unique tag instead of each `Student` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-T13-4/tp/tree/master/src/main/java/tutorlink/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="550" />

The `Storage` component,
* can save both student directory data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonStudentDirectoryStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

### Common classes

Classes used by multiple components are in the `tutorlink.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Inline command completion

`CommandCompletion` is a pure advisory model: it returns a display suffix and an append string.
Its command words reference the command constants and its prefix templates reference `CliSyntax`, following
current parser and usage-message syntax. Exact command names take precedence over longer names; otherwise
only a unique name prefix is suggested. Name acceptance only appends the command suffix. Subsequent
acceptance appends one missing parameter prefix, skipping supplied parameters while preserving every value
and its order. Empty required values block advancement. Positional arguments are hints only, and no
placeholder values or optional brackets are inserted. Within addresses, only recognized parameter prefixes
form boundaries; escaped prefixes and other slash words remain address text. Unknown prefixes outside
addresses, duplicate non-tag prefixes, and invalid preambles suppress suggestions.
Completion does not validate or execute commands.

`CommandBox` filters only an unmodified Tab at the end of an unselected input when an append is available.
Other keys and focus traversal continue through JavaFX. Its text formatter still normalizes pasted tabs.
Enter continues to read only `TextField.getText()` and retains the existing execution and error handling.

`CommandCompletionSkin` extends the native `TextFieldSkin`. It draws a separate unmanaged, mouse-transparent
`Text` node, using the native character bounds after layout for horizontal scroll positioning and the
field's font and baseline. Only the hint is clipped to the input viewport; it never reserves input width
or changes the field's text. Focus, selection, caret, text, font and size changes update its layout.

`CommandCompletionTest` covers command syntax and incremental acceptance. `CommandBoxTest` uses a real
JavaFX stage and skin to cover event dispatch, focus traversal, actual-input execution, paste normalization,
selection, native undo/redo, skin replacement, font changes and horizontal scrolling.
Tests use synthetic commands without accessing user data.

### Tag search

The `findtag TAG` command searches the full roster for students with an exact, case-insensitive tag match.
It accepts one tag using the existing `Tag` validation rules, so add, edit, and search accept the same tag format.
Stored tag spelling and `Tag` equality are unchanged; only the search comparison ignores case.

The parser gives corrective usage guidance for an accidental `t/` prefix. Other invalid tags still use the shared
tag constraints. Neither error changes the current search.

<puml src="diagrams/FindTagSequenceDiagram.puml" alt="Parsing and executing a tag search over the full roster" />

`TutorLinkParser` delegates to `FindTagCommandParser`, which rejects missing arguments with a usage error and
validates the tag through `ParserUtil#parseTag(String)`. It creates a `TagMatchesPredicate` and a `FindTagCommand`.
The predicate tests whether any of a student's tags equals the requested tag ignoring case. It does not inspect
names or other fields, and a student with multiple matching tags still appears only once.

`FindTagCommand` replaces the predicate on the model's `FilteredList<Student>` using
`Model#updateFilteredStudentList(Predicate)` and reports the result count. Because that list is backed by the full
`StudentDirectory`, the command also finds students outside the previous search or tutorial-group results.
The model first resets its `SortedList<Student>` to directory order, then applies the new tag predicate.
`Model#getFilteredStudentList()` returns this sorted view, which is shared by the UI and index-based commands.
No matches produces an empty view. A parse failure leaves the current predicate, sorting, and stored data unchanged.

Edits and deletions continue to use the current displayed indices. Replacing a student's tags with `edit`, including
clearing them with `t/`, causes the filtered view to re-evaluate the updated student. A student who no longer
matches disappears, and the remaining indices change accordingly. The filter remains active after edits and
deletions. Sorting the results also keeps the tag filter; edits that change the sort key update the displayed order.
A subsequent `findtag` search cancels that sorting and searches the full roster again; `sort reset` instead
restores directory order while keeping the current tag predicate. Storage saves the complete
roster in directory order, including students hidden by the search. Tag and ordinary profile edits retain the
student's current remark and follow-up status.

`FindTagCommandIntegrationTest` covers adding two tagged students, clearing one student's tags, searching again,
and reloading the saved roster, as well as filter replacement, invalid input, tag changes, and displayed-index
editing and deletion.

### Student remarks

The `remark INDEX r/TEXT` command updates the current remark of the student at `INDEX` in the displayed list.
The command accepts free text after the `r/` prefix; `r/` with no text clears the remark. It uses the same
`Model#getFilteredStudentList()` index source as `edit`, `delete`, and follow-up commands, so searching or filtering
does not change which displayed student an index refers to.

`RemarkCommand` creates a copy using `Student#withRemark(String)`, which retains the student's name, matriculation
number, tutorial group, contact details, tags, and follow-up status. The existing `JsonAdaptedStudent` mapping stores
the remark in JSON, so it survives application restarts. Only one editable current remark is stored; remark history
is outside the feature scope.

The command flow is represented in the following diagrams:

<puml src="diagrams/add-remark/RemarkClass.puml" alt="Remark command class diagram" />
<puml src="diagrams/add-remark/ParserClass.puml" alt="Remark command parser class diagram" />
### Current follow-up status

`followup INDEX on` and `followup INDEX done` set the immutable `Student.requiresFollowUp` boolean.
`FollowUpCommandParser` validates the two positional arguments and uses `ParserUtil.parseIndex`.
`FollowUpCommand` resolves the index from `Model#getFilteredStudentList()`, rejects an index outside that displayed
list, and replaces just that student through `Model#setStudent`. It uses `Student#withFollowUp(boolean)` to retain
all unrelated fields, including the existing remark. Repeating either action leaves the status unchanged.

<puml src="diagrams/FollowUpSequenceDiagram.puml" alt="Completing a follow-up using the current displayed index" />

`followups` accepts no arguments. `FollowUpsCommand` replaces the active predicate using
`Model#updateFilteredStudentList(Student::requiresFollowUp, "Follow-up required")`.
The filtered list is backed by the entire roster,
so results are independent of the previous name search, tag search, or group filter. The command neither copies nor
mutates the source roster. Results follow roster order and an empty result has an explicit message.
After `setStudent`, JavaFX re-evaluates the active predicate; completing a follow-up therefore immediately removes
the student from that view and renumbers the remaining results. The filter stays active after edits and updates.
`list`, `find`, and `findtag` replace it, and adding a student restores the full roster.

`JsonAdaptedStudent` saves `requiresFollowUp` and restores it through the full `Student` constructor.
Missing or null fields default to false for compatibility with older records. `LogicManager` saves the complete
roster after successful commands that change student records, including hidden students.
Commands that leave the records unchanged do not save again. No history or completion timestamps are stored.

Preservation and ordering contract:
* `Student` equality, hashing, and string output include current status; identity still uses only matriculation number.
* `StudentBuilder(Student)` and `withFollowUp(boolean)` retain status in expected test records.
* `EditCommand` passes the original status and remark when replacing details, groups, or tags.
* Remark commands should call `Student#withRemark(String)` to retain status when setting, replacing, or clearing a
  remark. Older constructor overloads describe **new** records with no follow-up; they must not be used to copy an
  existing student during updates.
* Sorting should order the same list exposed by `getFilteredStudentList()`, because follow-up updates use that
  displayed order. It retains whole `Student` objects and status. A new `followups` query resets sorting to
  directory order through `Model#updateFilteredStudentList`; `sort reset` retains the follow-up predicate.

`FollowUpCommandIntegrationTest` covers mark → list → reload → complete → reload through parsing and JSON storage,
and loads a genuine older record without the field. Command tests cover unrelated-field preservation and
full-roster filtering. Further regressions exercise combined edits and error cases.
### Tutorial group filtering

Tutorial group filtering is implemented as an optional mode of the `list` command. The supported forms are `list`,
which shows every student, and `list g/T01`, which shows only students in the specified tutorial group.

When `TutorLinkParser` receives a `list` command, it delegates the arguments to `ListCommandParser`. With no
arguments, the parser creates a `ListCommand` that uses `Model#PREDICATE_SHOW_ALL_STUDENTS`. When the `g/` prefix is
present, the parser uses `ParserUtil#parseTutorialGroup(String)` to validate and normalize the value before creating
the command. Invalid, missing, or repeated tutorial-group arguments cause a `ParseException`, so the current list is
not changed.

For a group-filtered command, `ListCommand` creates a `TutorialGroupMatchesPredicate`. This predicate performs an
exact comparison between the requested `TutorialGroup` and each student's tutorial group. `ListCommand` passes the
predicate to `Model#updateFilteredStudentList(Predicate)`. `ModelManager` applies it to its JavaFX
`FilteredList<Student>`, and the UI updates automatically because it observes that list. The command then reports the
number of matching students, including zero when no students match.

The original `UniqueStudentList` is not modified or copied during filtering. It remains the complete source of student
records, while `FilteredList<Student>` provides the current view. Consequently, filtering does not change stored data.
Commands that operate on the displayed list use its current one-based indexes. Editing or deleting a displayed
student retains the active filter, while running `list` without `g/` restores the complete list.

### Tutorial-group overview

The `groups` command reports the number of students in every tutorial group represented in the complete roster. It
accepts no arguments; `GroupsCommandParser` rejects non-empty arguments before creating a `GroupsCommand`.

`GroupsCommand` reads `Model#getStudentDirectory().getStudentList()` rather than
`Model#getFilteredStudentList()`. It groups students by their normalized `TutorialGroup` value and stores the counts in
a sorted map, producing deterministic output. Since the counts are derived at execution time, additions, deletions,
and tutorial-group changes made through `edit INDEX g/GROUP` are reflected without a separate cache or update hook.

The command only creates a result message. It does not update the model's active predicate, so the currently displayed
student list and its displayed-index contract remain unchanged. An empty directory produces the explicit message
`No tutorial groups found.`.

`GroupsCommandTest` covers complete-roster counting while a group filter is active, empty rosters, stable ordering,
and changes caused by adding, deleting, and moving students. `GroupsCommandParserTest` covers argument validation.
`GroupsCommandIntegrationTest` covers the complete parsing, filtering, command, and storage path.

### Editing optional contact details

`EditCommandParser` distinguishes three states for each contact prefix: absent (keep the old value), present with
non-empty text (validate and replace), and present with empty text (clear to `null`). The descriptor stores a
`phoneEdited`, `emailEdited`, or `addressEdited` flag alongside each nullable value. A plain `Optional` cannot
distinguish an omitted field from an explicitly cleared field. Descriptor copies and equality retain these flags.

<puml src="diagrams/ClearContactsSequenceDiagram.puml" alt="Clearing a contact while preserving omitted fields" />

All supplied values are parsed before execution. Invalid input therefore cannot partially clear other fields.
`EditCommand` resolves the index from the displayed sorted/filtered list and constructs a new immutable `Student`.
Required fields and tags use the existing descriptor rules; contact flags choose between the new nullable value
and the original value. The old remark and follow-up status are passed through unchanged. Replacing the student
re-evaluates the active filter and sort order. Storage serializes the full directory, including hidden students.

`Email#isValidEmail` is shared by command parsing and JSON loading. A bounded domain-label expression and explicit
length checks enforce 64 characters for the local part, 63 for each domain label, and 254 for the complete address.
Adjacent supported special characters are accepted except consecutive periods. This is the application's documented
email subset; it is not an email-delivery or full RFC-compliance check.

`ContactDetailsIntegrationTest` checks displayed-index clearing, all-or-nothing invalid edits, omitted-field
preservation, remark/status preservation, and saving/reloading. Value-object tests cover each email boundary.

For free-text addresses, a backslash immediately before a recognized prefix at the start of the value or after
whitespace escapes that prefix (for example, `a/Block 10 \p/Lane`). The tokenizer does not recognize `\p/` as `p/`;
`ParserUtil#parseAddress` removes that escape after tokenization. Other backslashes are retained. Both add and edit
use this path, while JSON loading preserves stored address text without interpreting escapes.

### Persistent filter condition

The model stores a human-readable description with the active predicate. Search, group-filter, and follow-up
commands supply both through `updateFilteredStudentList(predicate, description)`. Name/matriculation and tag
predicates expose their description without exposing mutable keyword collections. The all-students update resets
the description; sorting, ordinary edits, remarks, and deletion leave it unchanged. It is transient view state and
is not saved in JSON.

`Logic` exposes the current description to `MainWindow`, which refreshes the label above the student list after
every command, including failures. Refreshing cannot depend only on list-change events: one empty search can be
replaced by a different empty search without any student-list change. Filter changes do not require a data-file write.
Long conditions are truncated in the label and shown in a tooltip.
UI regressions cover these cases and verify that invalid searches retain the previous condition.

### Deletion preview and confirmation

`delete INDEX` and `delete INDEX confirm` resolve the current displayed index independently. The preview returns a
`CommandResult` containing the target `Student`. `MainWindow` uses the same selection path as `view INDEX` to highlight
and scroll to that record and populate its details. `LogicManager` returns this view-only result without saving data.
The preview changes no record and holds no pending deletion state. Confirmation validates and removes the student at
the index in the list at that moment; if the list changes, the user should preview again. `MainWindowTest` verifies
that preview selection matches the target under sorting and filtering, and that invalid indices retain selection.

### Saving and protecting invalid data

Before executing a command, `LogicManager` captures a `ModelSnapshot` containing a copy of the roster and the current
predicate, filter description, and sorting rule. It saves only if the command changes student records. Searches,
sorting, help, exit, selection changes, and repeated status updates therefore work without rewriting the roster.
If saving throws an `IOException`, the model restores the snapshot before reporting the error. `MainWindow` restores
the previous group highlight and student selection, so a rejected edit cannot be committed by a later `list` command.
The snapshot reuses immutable students and predicates while copying the roster list; rollback retains the observable
lists already used by the UI. `LogicManagerTest` covers every mutating command, filter/sort preservation, and a failed
edit followed by a read-only command. UI and packaged tests verify the selected profile and contact values.

`JsonStudentDirectoryStorage` retains the exact bytes from each successful read or save, including the absence
of a file on first launch. Before saving, it obtains a nonblocking operating-system lock on a persistent sibling
`.lock` file and checks that the current bytes match that baseline. A stale or busy writer throws `IOException`,
using the same rollback path and actionable feedback. Rejected saves never update the baseline. Existing files
cannot be overwritten without first being read. Symbolic links resolve to the same target and lock path.
The complete JSON is written to a temporary file in the same directory, then moved atomically over the roster.
Existing file attributes supported by the filesystem are copied to the temporary file, and write-protected rosters
are rejected before replacement. Filesystems without atomic replacement fail the save safely. The lock file is
retained because deleting it can allow competing processes to lock different file objects. No lock remains held
between commands or after exit.
Concurrent storage tests cover first writes, stale updates, external edits/deletion, lock contention, and reload;
storage safety tests cover a separate JVM holding the lock, read-only files, permission preservation, dangling
symbolic links, and failed atomic replacement with temporary-file cleanup and successful retries.
Logic tests cover rollback for every mutating command and successful writes after reopening.

The command box normalizes tabs to spaces with a `TextFormatter` before JavaFX removes them. This preserves field
boundaries in pasted commands, including the address/email case covered by `MainWindowTest` and the packaged smoke test.

`JsonStudentDirectoryStorage` reads JSON through `JsonUtil`, then `JsonSerializableStudentDirectory` converts each
adapted record to the model. The root must be an object with a non-null `students` array; an empty array is valid,
but null student or tag entries, missing required fields, invalid values, and duplicate matriculation numbers are rejected.
Missing optional contact details remain `null`. Older records without follow-up status default to false.

Conversion failures become `DataLoadingException`. `MainApp` shows an explanatory startup alert and stops before
opening the roster UI. This prevents later saves from replacing an unreadable file with sample or empty data.
A missing file is different: it is a first launch and loads sample students. The original invalid bytes are preserved;
the user must correct a backed-up copy or restore a valid backup before retrying.

The storage tests cover malformed structures and data preservation. `.github/smoke-test.py` also launches the real
release JAR with corrupt JSON, a null root, missing/null arrays, null student/tag entries, and a record missing required
fields. Each case must display the protective alert, avoid opening the roster, and leave the file byte-for-byte intact.

--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

* is an undergraduate or graduate teaching assistant (tutor) managing one or more university tutorial groups
* needs to maintain student rosters that may change throughout the semester
* regularly needs to find, inspect, add, edit, or remove student records while preparing for or conducting tutorials
* needs reliable student details such as names, matriculation numbers, tutorial groups, and optional email addresses
* is reasonably comfortable using a desktop application and concise CLI commands for repetitive tasks
* values fast, keyboard-driven workflows, clear validation, and protection against duplicate student records

**Value proposition**: TutorLink helps university tutors manage student rosters quickly and accurately from a desktop application. Concise commands make it easy to add, filter, edit, delete, and list students. The workspace shows student summaries and the selected student's full details, while input validation and duplicate matriculation checks reduce roster errors.

### Student data contract

The following decisions define the current behaviour for the model, commands, storage, and UI.

| Field | Requirement | Missing value |
|-------|-------------|---------------|
| Name | Required; letters, digits, and spaces only; at most 70 characters after normalization | Reject the record or command |
| Matriculation number | Required and unique, ignoring letter case | Reject the record or command |
| Tutorial group | Required | Reject the record or command |
| Phone, email, address | Retain the existing fields, but make each optional | Represent an absent value as `null` in the model and JSON; display `Not provided` in the UI |
| Tags | Retain the existing field | Use an empty set in the model and an empty array in JSON, rather than `null` |
| Follow-up | Track one current `requiresFollowUp` boolean | Default to false for new students and older JSON without the field; retain through replacements |
| Remark | Store one current remark that can be set, replaced, or cleared with `remark INDEX r/TEXT`; retain it through other edits | Use `null` when absent or cleared |

An `add` command must accept a name, matriculation number, and tutorial group without requiring phone, email, or address. Existing `p/`, `e/`, `a/`, and `t/` inputs remain supported as optional details. Commands and UI components must handle absent optional values without dereferencing `null`. An `edit` command leaves omitted fields unchanged. An explicitly empty `p/`, `e/`, or `a/` clears that optional field to `null`; omission is never treated as clearing.

The middle list shows student summaries. Selecting one or executing `view INDEX` displays its name, matriculation
number, tutorial group, optional contact fields, tags, current remark, and follow-up status in the details panel.
A tutorial group is
an attribute of each student; TutorLink supports group filtering, group counts, and changing a student's group through
`edit`, rather than independent tutorial-group records or management commands.

Names are trimmed at both ends, and consecutive internal spaces are collapsed to one before the 70-character limit is checked. For example, `John  Doe` is stored as `John Doe`. Letter case is preserved; punctuation and other internal whitespace are invalid.

Tutorial groups use one letter followed by exactly two digits, stored with an uppercase letter (for example, `T04` or `W12`). Lowercase input such as `t04` is accepted and stored as `T04`. `T4`, `T004`, and groups containing spaces are invalid. Parsing, model validation, filtering, JSON loading, and display must apply the same rule; leading zeros must be preserved. Matriculation numbers are likewise stored uppercase before duplicate checks.

Older data files may lack required matriculation numbers or tutorial groups. Those values cannot be inferred or replaced with `null`. If such a file cannot be loaded, TutorLink must preserve it and prevent subsequent commands from overwriting it with an empty roster. A migration may proceed only after the missing required values are supplied. Tests must cover both absent optional fields and an incompatible old file that remains unchanged after a command.

Files written by the earlier student-profile implementation may contain a one-digit `T` or `L` group such as `T4`. On loading such a file, TutorLink restores the leading zero (`T04`) before applying the current group rule. A newly entered one-digit group remains invalid.


### User stories

This backlog records 33 user stories, including ideas from the team's shared project notes. It covers both the MVP and possible later enhancements; a story's inclusion does not mean it is implemented in the current version. The User Guide describes the available commands.

Priorities: High (must have) - `* * *`, Medium (nice to have) - `* *`, Low (unlikely to have) - `*`

| Priority | As a …                                    | I want to …                 | So that I can…                                                        |
|----------|--------------------------------------------|------------------------------|------------------------------------------------------------------------|
| `* * *`  | new tutor                                  | see usage instructions           | learn how to use TutorLink effectively                                |
| `* * *`  | tutor                                      | add a student's details          | create a roster record for a newly assigned student                    |
| `* * *`  | tutor                                      | list all students                | inspect the complete roster and reset an active group filter           |
| `* * *`  | tutor                                      | view a student's full details | inspect academic and contact information for a selected student    |
| `* * *`  | tutor                                      | filter students by tutorial group | focus on students attending a particular tutorial session            |
| `* * *`  | tutor                                      | edit a student's details         | correct errors or update a student's matriculation number or group     |
| `* * *`  | tutor                                      | delete a student's record        | remove students who drop the course or change tutorials                |
| `* * *`  | tutor                                      | receive clear validation feedback | correct invalid input without risking inaccurate roster data         |
| `* * *`  | tutor                                      | prevent duplicate matriculation numbers | keep each student represented by one accurate roster record      |
| `* *`    | tutor                                      | store a student's email address  | contact the student when an email address is available                 |
| `* *`    | tutor                                      | add free-text remarks to a student record | keep useful context about a student for future reference       |
| `* *`    | tutor                                      | import or export roster data as CSV | reuse roster data and share it with other tools                       |
| `* *`    | tutor                                      | record student attendance        | track participation across tutorial sessions                           |
| `* *`    | tutor                                      | search for a student by name     | find their details without scrolling through the complete roster       |
| `* *`    | tutor                                      | search for a student by matriculation number | identify the correct student when names are similar             |
| `* *`    | tutor                                      | move a student to another tutorial group | reflect changes in tutorial allocation                          |
| `* *`    | tutor managing multiple tutorial groups     | view all tutorial groups I manage | switch between my classes more easily                               |
| `* *`    | tutor                                      | sort students by name            | scan the roster more easily                                           |
| `* *`    | tutor                                      | sort students by tutorial group  | see students from the same class together                             |
| `* *`    | tutor                                      | tag a student with simple labels | categorize students using information relevant to my teaching         |
| `* *`    | tutor                                      | search for students with a particular tag | find students who share a relevant characteristic              |
| `* *`    | tutor                                      | edit a student's remarks         | keep my observations accurate and up to date                           |
| `* *`    | tutor                                      | view previous notes about a student | recall earlier interactions before a consultation                  |
| `* *`    | tutor                                      | mark a student as requiring follow-up | avoid forgetting students who need further attention              |
| `* *`    | tutor                                      | view all students requiring follow-up | identify whom I need to contact or check on                        |
| `* *`    | tutor                                      | mark a follow-up as completed    | keep my follow-up list current                                        |
| `* *`    | tutor                                      | view the number of students in each tutorial group | understand the size of each class I manage                  |
| `* *`    | tutor                                      | view a concise summary of each student in the list | identify students without opening every full profile      |
| `*`      | tutor                                      | seed the application with sample data | explore the application before entering a real roster             |
| `*`      | tutor                                      | filter students using multiple criteria | narrow down a large roster quickly                              |
| `*`      | tutor                                      | undo an accidental deletion      | recover a student record that I removed by mistake                     |
| `*`      | tutor                                      | archive students from previous semesters | keep my current roster uncluttered while preserving old records |
| `*`      | tutor                                      | view archived students           | refer back to information about students I previously taught          |


### Use cases

For all use cases below, the **System** is `TutorLink` and the **Actor** is a `university tutor`. These use cases describe the current student-management behaviour.

**Common precondition**: TutorLink is running.

An index refers to the student's one-based position in the current list, including a list filtered by tutorial group. It is not a permanent student identifier. A rejected request leaves the student records and current list unchanged.

#### UC01: Add a student

**Related requirement**: Add a student's details.

**Main success scenario (MSS)**

1. Tutor requests to add a student, supplying a name, matriculation number, tutorial group, and optionally an email address.
2. TutorLink adds the student record, confirms the addition, and shows the complete student list including the new student.

   Use case ends.

**Extensions**

* 1a. Required details are missing or the request contains an invalid value or command format.

  * 1a1. TutorLink explains the input error without adding a record.

    Use case resumes at step 1.

* 1b. Another record has the same matriculation number, ignoring letter case.

  * 1b1. TutorLink reports the duplicate without changing either record.

    Use case resumes at step 1.

#### UC02: View a student in a tutorial group

**Related requirements**: View students in a specified tutorial group; inspect a student's displayed details.

**MSS**

1. Tutor requests the students in a specified tutorial group with `list g/TUTORIAL_GROUP`.
2. TutorLink shows the matching student summaries and the number of matches.
3. Tutor enters `view INDEX` or selects a student and reads their academic and contact details in the details panel.

   Use case ends.

**Extensions**

* 1a. The tutorial-group identifier or request format is invalid.

  * 1a1. TutorLink explains the input error.

    Use case resumes at step 1.

* 2a. No students match the specified tutorial group.

  * 2a1. TutorLink shows an empty list and reports zero matching students.

    Use case ends.

#### UC03: Edit a student's details

**Related requirements**: List students; edit a student's details.

**MSS**

1. Tutor requests a student list, optionally restricted to a tutorial group.
2. TutorLink shows the requested list.
3. Tutor requests to edit a student using their index in the current list and supplies the replacement field values.
4. TutorLink updates the record, confirms the edit, and refreshes the current list while retaining any tutorial-group filter. Fields omitted from the request keep their existing values.

   Use case ends.

**Extensions**

* 1a. The request to list students has an invalid tutorial-group identifier or command format.

  * 1a1. TutorLink explains the input error without changing the current list.

    Use case resumes at step 1.

* 2a. The requested list is empty.

  Use case ends.

* 3a. The index is invalid, no changes are supplied, or a field value or command format is invalid.

  * 3a1. TutorLink explains the input error without changing the record.

    Use case resumes at step 3.

* 3b. The replacement matriculation number belongs to another student, ignoring letter case.

  * 3b1. TutorLink reports the duplicate without changing either record.

    Use case resumes at step 3.

* 4a. The student's new tutorial group no longer matches the active filter.

  * 4a1. TutorLink excludes the student from the filtered list. The updated record remains in the complete student directory.

    Use case ends.

#### UC04: Delete a student's record

**Related requirements**: List students; delete a student's record.

**MSS**

1. Tutor requests a student list, optionally restricted to a tutorial group.
2. TutorLink shows the requested list.
3. Tutor requests to delete a student using their index in the current list.
4. TutorLink removes the student's record from the directory, confirms the deletion, and refreshes the current list while retaining any tutorial-group filter.

   Use case ends.

**Extensions**

* 1a. The request to list students has an invalid tutorial-group identifier or command format.

  * 1a1. TutorLink explains the input error without changing the current list.

    Use case resumes at step 1.

* 2a. The requested list is empty.

  Use case ends.

* 3a. The given index or command format is invalid.

  * 3a1. TutorLink explains the input error without deleting any record.

    Use case resumes at step 3.

### Non-Functional Requirements

1. TutorLink must run on Windows 10 or 11, macOS 13 or later, and Ubuntu 22.04 LTS or later, provided Java `25` or later is installed.
2. TutorLink must support at least 1,000 student records distributed across at least 20 tutorial groups in one local data file.
3. With 1,000 student records, TutorLink must become ready for command input within 5 seconds of launch on a computer with at least a 2 GHz quad-core processor, 8 GB of RAM, and SSD storage, while no other CPU-intensive application is running.
4. Under the dataset and test conditions stated in NFR 3, TutorLink must display the result within 1 second for at least 95 out of 100 consecutive operations that add, view, edit, delete, search for, or filter student records.
5. A successful data-modifying operation must be saved to the local data file before its success message is shown. After a normal shutdown and restart, TutorLink must restore the most recently saved student data without loss or modification.
6. Invalid commands and invalid student details must not modify stored data. TutorLink must identify the invalid input in an error message within the response-time limit stated in NFR 3.
7. After TutorLink and the Java runtime have been installed, all must-have features listed in the user stories must remain usable without an internet connection.
8. TutorLink must not transmit student data over a network. Student data must remain in the application's local data file unless the user explicitly copies or moves it.
9. Every student-management operation—including adding, viewing, editing, deleting, searching for, and filtering student records by tutorial group—must be executable using only the keyboard.

### Glossary

* **Current list**: The student records currently displayed in TutorLink, which may be all records or a subset produced by a search or tutorial-group filter.
* **Data-modifying operation**: An operation that changes stored student data, such as adding, editing, deleting, or importing student records.
* **Index**: The one-based number shown beside a student in the current list. It identifies that displayed record for an operation and is not a permanent student identifier.
* **Matriculation number**: The unique identifier assigned to a student by the university.
* **Student record**: The information TutorLink stores for one student, including the student's name, matriculation number, and tutorial group.
* **Tutorial group**: A named or numbered class group taught by a university tutor and used by TutorLink to organize student records.
* **University tutor**: A TutorLink user who teaches one or more tutorial groups and manages the records of students in those groups.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.
</box>

### Launch and shutdown

Use a separate empty test folder and the architecture-appropriate released JAR with Java 25. All examples below
use synthetic data and may clear the test roster. Never run them against a real roster you need to keep.

1. Start with `java -ea -jar TutorLink.jar` (or `TutorLink-mac-aarch64.jar` for ARM64 Java on macOS).
   Expect the three-column workspace with sample students, group counts, and no selected student.
1. Resize and move the window, run `exit`, and launch again from the same folder. Expect the saved window position
   and size, along with the previous roster. `help` should link to the published TutorLink User Guide.

### Searching across tutorial groups and editing search results

Use a **disposable test folder** with a copy of the JAR and Java 25. These steps clear its student data; do not run
them in a folder containing records you want to keep. Start with `java -jar TutorLink.jar` from that folder.

1. Enter `clear`, then add these synthetic students:
   * `add n/Alice Test m/A1111111A g/T01`
   * `add n/Ben Test m/A2222222B g/T02`
   * Expected: Both students are listed. Selecting either shows omitted contact details as `Not provided` in the
     details panel.
1. Enter `list g/t01`, then `find a2222222b`.
   * Expected: The group filter first shows only Alice. The search then shows only Ben at index `1`, even though
     Ben belongs to another group. Search replaces the group filter and accepts lowercase matriculation numbers.
1. Enter `list g/T01` again, then `find Ben`.
   * Expected: The group filter replaces the search and shows Alice; the next search shows Ben.
1. Enter `edit 2 n/Charlie Test`, `delete 2 confirm`, and `find` separately.
   * Expected: Each command reports an error. Ben remains the only displayed student and no record changes.
1. Enter `edit 1 n/Charlie Test`.
   * Expected: Ben is renamed to Charlie, retaining matriculation number `A2222222B` and group `T02`.
     The active name search now shows no students because Charlie does not match `Ben`.
1. Enter `find a2222222b`, then `delete 1`.
   * Expected: The search finds Charlie at index `1`. Deletion previews Charlie and the confirmation command;
     Charlie remains in the list until `delete 1 confirm` is entered.
1. Enter `delete 1 confirm`, then `list`.
   * Expected: Charlie is removed. `list` restores Alice, who was not part of the search results.
1. Enter `find Nobody`, then `add n/Dana Test m/A3333333C g/T03`.
   * Expected: Adding Dana resets the empty search results and shows Alice and Dana.
1. Enter `find a3333333c`, then `exit`. Restart the JAR from the same folder and enter `list`.
   * Expected: The search finds Dana before exit. After restart, Alice and Dana remain with their required fields,
     and Charlie remains deleted. Both students can still be found by name or matriculation number.

These checks complement the parser/model/storage regression tests in `FindCommandIntegrationTest`. They also check
the visible student cards and packaged application, which those automated tests do not exercise.

### Optional contacts, remarks, follow-ups, and deletion

1. Enter `clear`, then add `add n/Zoe Test m/A1111111A g/T01 p/91234567 e/student+_tutorial@example.com a/College Road`.
   Expect a successful addition, including the adjacent email special characters.
1. Run `remark 1 r/Discuss recursion`, `followup 1 on`, then
   `add n/Alpha Test m/A2222222B g/T01`. Run `list g/T01`, `sort name`, and `view 1`.
   Expect Alpha's details and Alpha/Zoe at displayed indices 1/2.
1. Run `delete 2`. Expect Zoe highlighted and Zoe's details on the right, a preview naming Zoe, and both students
   still present. `delete 0` and `delete 99 confirm` must report errors and keep the roster unchanged.
1. Run `edit 2 p/ e/ a/`, then `view 2`. Expect all three contact fields to read `Not provided`, with Zoe's remark
   and required follow-up intact. Repeat the clear; it should succeed without affecting other values.
1. Run `edit 2 p/123 e/invalid`. Expect an email error and no partial phone update. `edit 2 n/` must also fail.
   A 64-character local part followed by `@example.com` should be accepted; 65 characters should be rejected.
   `a..b@example.com` and a 64-character domain label must be rejected. Check the existing value remains intact.
1. Run `followups`, then `view 1`. Expect only Zoe at index 1 with the original remark. `followup 2 done` and
   `followup 1 off` must fail without changing status. Run `exit` and restart; `followups` must still find Zoe.
1. Run `followup 1 done`. Expect an empty follow-up list, with Zoe's record retained. Run `list`, `sort name`,
   `delete 2`, then `delete 2 confirm`. Expect Zoe removed, Alpha retained, and group T01's count reduced to one.
1. Restart and run `list`. Expect only Alpha. `groups` should report `T01: 1 student`.

### Saving data and malformed-file protection

1. Close TutorLink. Back up the synthetic test `data/tutorlink.json` outside the test folder.
1. Replace its contents with `{"students":[null]}` and launch the JAR.
   Expect an `Unable to load student data` alert explaining that the original file was not changed; no roster opens.
   Close the alert and confirm the file still contains exactly the input bytes.
1. Repeat independently with `{}`, `{"students":null}`, `null`, malformed JSON, and
   `{"students":[{"name":"Protected Student","tags":[]}]}`. Expect the same protective outcome for each.
1. Restore the valid backup and relaunch. Expect the previous records to load correctly.
1. In a new empty folder, launch without a data file. Expect sample students rather than the invalid-data alert.

The packaged smoke harness automates these workflows using the release JAR's actual entry point and controls,
and retains screenshots and logs in `build/packaged-smoke/`. See [DevOps](DevOps.md#making-a-release) for instructions.
