---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# TutorLink User Guide

TutorLink is a **desktop application for managing students' contact details, optimized for use through a Command Line Interface (CLI)** while retaining the benefits of a Graphical User Interface (GUI). If you type quickly, TutorLink can help you manage contacts faster than traditional GUI applications.

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download the latest `.jar` file from the [TutorLink releases page](https://github.com/AY2627S1-CS2103T-T13-4/tp/releases).

1. Copy the file to the folder you want to use as the _home folder_ for TutorLink.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar addressbook.jar`.<br>
   A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   Some example commands you can try:

   * `list` : Lists all contacts.

   * `add n/John Doe m/A0123456X g/T01 e/johnd@example.com` : Adds a student named `John Doe` to TutorLink.

   * `delete 3` : Deletes the 3rd contact shown in the current list.

   * `clear` : Deletes all contacts.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<box type="info" seamless>

**Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `...` can appear zero or more times.<br>
  For example, `[t/TAG]... ` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Extraneous parameters for commands that take no parameters, such as `help`, `list`, `exit`, and `clear`, are ignored.<br>
  For example, `help 123` is interpreted as `help`.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</box>

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

Format: `help`


### Adding a person: `add`

Adds a person to the contact list.

Format: `add n/NAME m/MATRIC_NUMBER g/TUTORIAL_GROUP [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]... `

The name, matriculation number, and tutorial group are required. Phone number, email, address, and tags are optional.
If omitted, the corresponding contact details are displayed as `Not provided`.

Names can contain letters, digits, and spaces, up to 70 characters after leading/trailing whitespace is removed and consecutive spaces are collapsed. For example, `John  Doe` is saved as `John Doe`.

The matriculation number must be `A` followed by seven digits and a letter. Letter case is ignored on input, and the
value is saved in uppercase. A tutorial group must be one letter followed by exactly two digits. Its letter is saved
in uppercase, while leading zeros are kept, so `l03` is stored as `L03`.

<box type="tip" seamless>

**Tip:** A person can have any number of tags, including zero.
</box>

Examples:
* `add n/John Doe m/A0123456X g/T01`
* `add n/John Doe m/A0123456X g/T01 p/98765432 e/johnd@example.com a/John street, block 123, #01-01`
* `add n/Betsy Crowe m/A1234567Y g/l03 t/friend e/betsycrowe@example.com a/Newgate Prison p/1234567 t/criminal`

### Listing all persons: `list`

Shows a list of all contacts.

Format: `list`

### Editing a person: `edit`

Edits an existing contact.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]... `

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, ...
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* When editing tags, all of the person's existing tags are removed; adding tags is not cumulative.
* To remove all of a person's tags, enter `t/` without a tag after it.

Examples:
*  `edit 1 p/91234567 e/johndoe@example.com` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@example.com` respectively.
*  `edit 2 n/Betsy Crower t/` Edits the name of the 2nd person to be `Betsy Crower` and clears all existing tags.

### Finding students by name or matriculation number: `find`

Finds students whose names or matriculation numbers match any of the given keywords.

Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.
* Each keyword can match a whole word in a name or a complete matriculation number.
  Partial matches are not supported: `Han` does not match `Hans`, and `A0123` does not match `A0123456B`.
* Matriculation-number matching is also case-insensitive: `a0123456b` matches `A0123456B`.
* Students matching at least one keyword are returned (an `OR` search). You can mix names and matriculation numbers.
* Each matching student appears once, in their original directory order, even if multiple keywords match.
* Each search covers the complete directory and replaces the previous displayed results. Use `list` to show all students again.
* No matches produces an empty list and a zero-result message. Searching does not change student records.
* `find` without keywords shows a usage error and keeps the current list unchanged.
* Phone numbers, email addresses, addresses, tutorial groups, tags, and remarks are not searched.

Examples:
* `find John` returns `john` and `John Doe`
* `find A0123456B` returns the student with that matriculation number, if present.
* `find John a0123456b` returns students with `John` in their name or the matriculation number `A0123456B`.
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

### Deleting a person: `delete`

Deletes the specified contact.

Format: `delete INDEX`

* Deletes the person at the specified `INDEX`.
* The index refers to the index number shown in the displayed person list.
* The index **must be a positive integer** 1, 2, 3, ...

Examples:
* `list` followed by `delete 2` deletes the 2nd contact in the list.
* `find Betsy` followed by `delete 1` deletes the 1st person in the results of the `find` command.

### Clearing all entries: `clear`

Clears all contacts from TutorLink.

Format: `clear`

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

TutorLink automatically saves data after every command. You do not need to save manually.

### Editing the data file

TutorLink data is saved automatically as a JSON file `[JAR file location]/data/addressbook.json`. Advanced users are welcome to update data directly by editing that data file.

<box type="warning" seamless>

**Caution:**
If TutorLink cannot load the data file, it shows an error and stops starting. It does not replace the file with an empty student list or change the original file. Correct the invalid data or restore a valid backup before opening TutorLink again. In particular, older files without a matriculation number or tutorial group for each student need those required values added; TutorLink cannot infer them. Back up the file before editing it.<br>
Furthermore, certain edits can cause TutorLink to behave in unexpected ways (e.g., if a value entered is outside of the acceptable range). Therefore, edit the data file only if you are confident that you can update it correctly.
</box>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install the app on the other computer and overwrite the data file it creates with the data file from your previous TutorLink home folder.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
2. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action     | Format, Examples
-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------
**Add**    | `add n/NAME m/MATRIC_NUMBER g/TUTORIAL_GROUP [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]... ` <br> e.g., `add n/James Ho m/A2345678Z g/T05 e/jamesho@example.com`
**Clear**  | `clear`
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**Edit**   | `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]... `<br> e.g.,`edit 2 n/James Lee e/jameslee@example.com`
**Find**   | `find KEYWORD [MORE_KEYWORDS]`<br> e.g., `find James A0123456B`
**List**   | `list`
**Help**   | `help`
