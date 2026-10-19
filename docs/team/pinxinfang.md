---
  layout: default.md
  title: "Fang Pin Xin's Project Portfolio Page"
---

# Fang Pin Xin — Project Portfolio

## Project: TutorLink

TutorLink is a desktop student-roster application for university tutors. It combines commands with a JavaFX
interface to manage student details and tutorial groups.

GitHub: [@pinxinfang](https://github.com/pinxinfang)

## Code and testing

* **Name and matriculation-number search:** implemented case-insensitive searches using whole name words or full
  matriculation numbers. Searches cover the full roster and replace the previous displayed results.
  [PR #47](https://github.com/AY2627S1-CS2103T-T13-4/tp/pull/47).
* **Search workflow regression tests:** covered interactions with group filtering, displayed-index edits and
  confirmed deletion, invalid input, optional contact details, and saving/reloading the full roster.
  [PR #61](https://github.com/AY2627S1-CS2103T-T13-4/tp/pull/61).
* **Release packaging and verification:** corrected JavaFX native-library packaging for Intel and Apple Silicon
  Macs, added packaged-application checks to CI, and handled JSON-null input through the protective startup path.
  [PR #68](https://github.com/AY2627S1-CS2103T-T13-4/tp/pull/68).

### v1.4 work submitted for review

* **Tag search:** added `findtag TAG` with exact, case-insensitive matching, full-roster searches, and validation
  consistent with add/edit. Added 28 tests covering parser and search behaviour, clearing/replacing tags,
  result-index edits/deletions, and persisted data. The User Guide records the matching rule and acceptance
  scenario; the Developer Guide includes the search sequence and updated student model UML.
  [Issue #74](https://github.com/AY2627S1-CS2103T-T13-4/tp/issues/74),
  [PR #75](https://github.com/AY2627S1-CS2103T-T13-4/tp/pull/75).
* **Assertions configuration:** explicitly enabled assertions for Gradle application runs and tests on a separate
  branch, allowing this change to be reviewed independently of tag search.
  [PR #76](https://github.com/AY2627S1-CS2103T-T13-4/tp/pull/76).

The PR pages show the current review, CI, and merge status. Submitted work is distinguished here from changes
already included in a released version.

## Documentation

* Wrote TutorLink's student-management use cases.
  [PR #39](https://github.com/AY2627S1-CS2103T-T13-4/tp/pull/39).
* Documented the complete 33-story backlog, distinguishing planned enhancements from implemented features.
  [PR #50](https://github.com/AY2627S1-CS2103T-T13-4/tp/pull/50).
* Clarified the Mac Java setup, architecture-specific JAR selection, and automatic saving in the User Guide.
  [PR #73](https://github.com/AY2627S1-CS2103T-T13-4/tp/pull/73).

## Project management and release checks

* Recorded v1.3 public-artifact verification, including JAR checksums, packaged workflows, saved-data restart,
  invalid-file protection, and public documentation links.
  [Verification record #72](https://github.com/AY2627S1-CS2103T-T13-4/tp/issues/72).
* Set the v1.4 deadline and recorded the assigned tag-search scope and acceptance scenario.
  [v1.4 milestone](https://github.com/AY2627S1-CS2103T-T13-4/tp/milestone/4).
* Used separate branches for concurrent work, including the tag-search and assertions PRs above.

## Contribution records

* [Code dashboard filtered by GitHub ID](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/?search=pinxinfang)
* [Pull requests authored in the team repository](https://github.com/AY2627S1-CS2103T-T13-4/tp/pulls?q=is%3Apr+author%3Apinxinfang)
* [Issues assigned in the team repository](https://github.com/AY2627S1-CS2103T-T13-4/tp/issues?q=is%3Aissue+assignee%3Apinxinfang)

The course's author configuration maps both `Fang Pin Xin` and `pinxin` to `pinxinfang`. The project builds on
AddressBook-Level3; the linked PRs identify my changes within that shared codebase and the team's work.
