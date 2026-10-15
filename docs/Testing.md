---
  layout: default.md
  title: "Testing guide"
  pageNav: 3
---

# Testing guide

<!-- * Table of Contents -->
<page-nav-print />

<!-- -------------------------------------------------------------------------------------------------------------------- -->

## Running tests

You can run tests in two ways.

* **Method 1: Using IntelliJ JUnit test runner**
  * To run all tests, right-click on the `src/test/java` folder and choose `Run 'All Tests'`
  * To run a subset of tests, you can right-click on a test package,
    test class, or a test and choose `Run 'ABC'`
* **Method 2: Using Gradle**
  * Open a console and run the command `gradlew clean test` (Mac/Linux: `./gradlew clean test`)

<box type="info" seamless>

**Link**: Read [this Gradle Tutorial from the se-edu/guides](https://se-education.org/guides/tutorials/gradle.html) to learn more about using Gradle.
</box>

--------------------------------------------------------------------------------------------------------------------

## Types of tests

This project has three types of tests:

1. *Unit tests* target the lowest-level methods and classes.<br>
   For example: `tutorlink.commons.StringUtilTest`
1. *Integration tests* check how multiple code units work together; the individual units are assumed to work.<br>
   For example: `tutorlink.storage.StorageManagerTest`
1. *Hybrid tests* combine unit and integration testing. These tests check both the individual units and how they work together.<br>
   For example: `tutorlink.logic.LogicManagerTest`

--------------------------------------------------------------------------------------------------------------------

## Testing the packaged application

Run release checks against the actual candidate JAR in a fresh folder, using only synthetic students.
Record the source commit, JAR filename and SHA-256, OS, Java distribution/version, commands, and results.
A passing Gradle test suite does not replace these checks.

1. Launch using the Java distribution specified by the project and course installation instructions.
1. Add students with optional contact fields omitted; check tutorial filtering and name/matriculation-number search.
1. Edit a student using the displayed index, including matriculation number and tutorial group.
1. Reject invalid fields, duplicate matriculation numbers, and invalid displayed indices without changing records or results.
1. Preview deletion, abandon the preview, and verify that no student was removed.
1. Preview again and confirm deletion using the current displayed index.
1. Run `help` and `exit`, restart from the same folder, and verify saved edits and deletions.
1. In separate disposable folders, test malformed JSON and incompatible student records, including a JSON `null` root.
   Check that startup stops with an error and preserves each original data file byte for byte.
1. Test another available OS, or record that coverage as pending. Repeat affected checks after fixes.

See the [v1.3 packaged-JAR test record](testing/v1.3-packaged-smoke.html) for the tested artifact,
synthetic commands, preservation evidence, and remaining coverage for
[#59](https://github.com/AY2627S1-CS2103T-T13-4/tp/issues/59).
