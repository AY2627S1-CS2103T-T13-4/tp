---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# AB-3 Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* _{List the sources of reused or adapted ideas, code, documentation, and third-party libraries here, with links to the originals.}_

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

**`Main`** (consisting of classes [`Main`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.
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

The **API** of this component is specified in [`Ui.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `StudentListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Student` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/logic/Logic.java)

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
**API** : [`Model.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores the student directory data i.e., all `Student` objects (which are contained in a `UniqueStudentList` object).
* stores the `Student` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Student>` that the UI can observe and bind to, so the UI updates when the list changes.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `StudentDirectory`, and each `Student` references tags from that list. This lets `StudentDirectory` maintain one `Tag` object per unique tag instead of each `Student` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/storage/Storage.java)

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

### Tag search

The `findtag TAG` command searches the full roster for students with an exact, case-insensitive tag match.
It accepts one tag using the existing `Tag` validation rules, so add, edit, and search accept the same tag format.
Stored tag spelling and `Tag` equality are unchanged; only the search comparison ignores case.

<puml src="diagrams/FindTagSequenceDiagram.puml" alt="Parsing and executing a tag search over the full roster" />

`TutorLinkParser` delegates to `FindTagCommandParser`, which rejects missing arguments with a usage error and
validates the tag through `ParserUtil#parseTag(String)`. It creates a `TagMatchesPredicate` and a `FindTagCommand`.
The predicate tests whether any of a student's tags equals the requested tag ignoring case. It does not inspect
names or other fields, and a student with multiple matching tags still appears only once.

`FindTagCommand` replaces the predicate on the model's `FilteredList<Student>` using
`Model#updateFilteredStudentList(Predicate)` and reports the result count. Because that list is backed by the full
`StudentDirectory`, the command also finds students outside the previous search or tutorial-group results.
No matches produces an empty view. A parse failure leaves the current predicate and stored data unchanged.

Edits and deletions continue to use the current displayed indices. Replacing a student's tags with `edit`, including
clearing them with `t/`, causes the filtered view to re-evaluate the updated student. A student who no longer
matches disappears, and the remaining indices change accordingly. The filter remains active after edits and
deletions. Storage saves the complete roster, including students hidden by the search.

`FindTagCommandIntegrationTest` covers adding two tagged students, clearing one student's tags, searching again,
and reloading the saved roster, as well as filter replacement, invalid input, tag changes, and displayed-index
editing and deletion.

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

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedStudentDirectory`. It extends `StudentDirectory` with an undo/redo history, stored internally as a `studentDirectoryStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedStudentDirectory#commit()` -- Saves the current student directory state in its history.
* `VersionedStudentDirectory#undo()` -- Restores the previous student directory state from its history.
* `VersionedStudentDirectory#redo()` -- Restores a previously undone student directory state from its history.

These operations are exposed in the `Model` interface as `Model#commitStudentDirectory()`, `Model#undoStudentDirectory()` and `Model#redoStudentDirectory()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedStudentDirectory` will be initialized with the initial student directory state, and the `currentStatePointer` pointing to that single student directory state.

<puml src="diagrams/UndoRedoState0.puml" alt="UndoRedoState0" />

Step 2. The user executes `delete 5 confirm` command to delete the 5th student in the student directory. The `delete` command calls `Model#commitStudentDirectory()`, causing the modified state of the student directory after the `delete 5 confirm` command executes to be saved in the `studentDirectoryStateList`, and the `currentStatePointer` is shifted to the newly inserted student directory state.

<puml src="diagrams/UndoRedoState1.puml" alt="UndoRedoState1" />

Step 3. The user executes `add n/David …​` to add a new student. The `add` command also calls `Model#commitStudentDirectory()`, causing another modified student directory state to be saved into the `studentDirectoryStateList`.

<puml src="diagrams/UndoRedoState2.puml" alt="UndoRedoState2" />

<box type="info" seamless>

**Note:** If a command fails its execution, it will not call `Model#commitStudentDirectory()`, so the student directory state will not be saved into the `studentDirectoryStateList`.
</box>

Step 4. The user now decides that adding the student was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoStudentDirectory()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous student directory state, and restores the student directory to that state.

<puml src="diagrams/UndoRedoState3.puml" alt="UndoRedoState3" />


<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index 0, pointing to the initial StudentDirectory state, then there are no previous StudentDirectory states to restore. The `undo` command uses `Model#canUndoStudentDirectory()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.
</box>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

<puml src="diagrams/UndoSequenceDiagram-Logic.puml" alt="UndoSequenceDiagram-Logic" />

<box type="info" seamless>

**Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</box>

Similarly, how an undo operation goes through the `Model` component is shown below:

<puml src="diagrams/UndoSequenceDiagram-Model.puml" alt="UndoSequenceDiagram-Model" />

The `redo` command does the opposite — it calls `Model#redoStudentDirectory()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the student directory to that state.

<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index `studentDirectoryStateList.size() - 1`, pointing to the latest student directory state, then there are no undone StudentDirectory states to restore. The `redo` command uses `Model#canRedoStudentDirectory()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.
</box>

Step 5. The user then decides to execute the command `list`. Commands that do not modify the student directory, such as `list`, will usually not call `Model#commitStudentDirectory()`, `Model#undoStudentDirectory()` or `Model#redoStudentDirectory()`. Thus, the `studentDirectoryStateList` remains unchanged.

<puml src="diagrams/UndoRedoState4.puml" alt="UndoRedoState4" />

Step 6. The user executes `clear`, which calls `Model#commitStudentDirectory()`. Since the `currentStatePointer` is not pointing at the end of the `studentDirectoryStateList`, all student directory states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add n/David …` command. This is the behavior that most modern desktop applications follow.

<puml src="diagrams/UndoRedoState5.puml" alt="UndoRedoState5" />

The following activity diagram summarizes what happens when a user executes a new command:

<puml src="diagrams/CommitActivityDiagram.puml" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire student directory.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the student being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct.

_{more aspects and alternatives to be added}_

### \[Proposed\] Data archiving

_{Explain here how the data archiving feature will be implemented}_


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

**Value proposition**: TutorLink helps university tutors manage student rosters quickly and accurately from a desktop application. Concise commands make it easy to add, filter, edit, delete, and list students. The list cards show each student's stored details, while input validation and duplicate matriculation checks reduce roster errors.

### MVP student data contract

The following decisions define the MVP behaviour for the model, commands, storage, and UI.

| Field | Requirement | Missing value |
|-------|-------------|---------------|
| Name | Required; letters, digits, and spaces only; at most 70 characters after normalization | Reject the record or command |
| Matriculation number | Required and unique, ignoring letter case | Reject the record or command |
| Tutorial group | Required | Reject the record or command |
| Phone, email, address | Retain the existing fields, but make each optional | Represent an absent value as `null` in the model and JSON; display `Not provided` in the UI |
| Tags | Retain the existing field | Use an empty set in the model and an empty array in JSON, rather than `null` |
| Remark | Preserve any existing remark when loading, editing, or saving a student | Use `null` when absent; adding or changing remarks is outside the profile data contract |

An `add` command must accept a name, matriculation number, and tutorial group without requiring phone, email, or address. Existing `p/`, `e/`, `a/`, and `t/` inputs remain supported as optional details. Commands and UI components must handle absent optional values without dereferencing `null`. An `edit` command leaves omitted fields unchanged; it must not interpret omission as a request to clear a field.

The current list cards are the student profile view. They show the name, matriculation number, tutorial group, optional
contact fields, and tags without a separate `view INDEX`
command. A tutorial group is an attribute of each student; the MVP supports group filtering and changing a student's
group through `edit`, rather than independent tutorial-group records or management commands.

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
| `* * *`  | tutor                                      | see a student's details on the list card | inspect academic and contact information in the current list    |
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

For all use cases below, the **System** is `TutorLink` and the **Actor** is a `university tutor`. These use cases describe the planned MVP behaviour.

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
2. TutorLink shows the matching students, the number of matches, and each student's profile fields on a list card.
3. Tutor reads a student's academic and contact details from that card.

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

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. Double-click the JAR file.<br>
      Expected: The GUI opens with a set of sample students. The window size may not be optimal.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

1. _{ more test cases … }_

### Searching across tutorial groups and editing search results

Use a **disposable test folder** with a copy of the JAR and Java 25. These steps clear its student data; do not run
them in a folder containing records you want to keep. Start with `java -jar TutorLink.jar` from that folder.

1. Enter `clear`, then add these synthetic students:
   * `add n/Alice Test m/A1111111A g/T01`
   * `add n/Ben Test m/A2222222B g/T02`
   * Expected: Both students are listed, with omitted contact details shown as `Not provided`.
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

### Deleting a student

1. Deleting a student while all students are being shown

   1. Prerequisites: List all students using the `list` command, with multiple students in the list.

   1. Test case: `delete 1`<br>
      Expected: No student is deleted. The status message previews the first student and shows `delete 1 confirm`.

   1. Test case: `delete 1 confirm`<br>
      Expected: The first student is deleted from the list. The status message shows the deleted student's details.

   1. Test case: `delete 0`<br>
      Expected: No student is deleted. The status message shows error details.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `...` (where x is larger than the list size)<br>
      Expected: Similar to previous.

1. _{ more test cases … }_

### Saving data

1. Dealing with missing/corrupted data files

   1. _{Explain how to simulate missing or corrupted data files and state the expected behavior.}_

1. _{ more test cases … }_
