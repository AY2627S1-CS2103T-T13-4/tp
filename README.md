# TutorLink

[![CI Status](https://github.com/AY2627S1-CS2103T-T13-4/tp/workflows/Java%20CI/badge.svg)](https://github.com/AY2627S1-CS2103T-T13-4/tp/actions)

![TutorLink UI](docs/images/Ui.png)

## Overview

TutorLink is a desktop application for managing student rosters across tutorial groups.

Tutors can add, edit, find, filter, sort, and delete student records through concise commands. The graphical list cards
show each student's tutorial group, optional contact information, remark, tags, and follow-up status.

## Target Users

TutorLink is intended for tutors who manage one or more tutorial groups and need a convenient way to organise student information.

## Key Features

* **Student rosters** — Add, edit, and delete students with unique matriculation numbers and tutorial groups.
* **Group filtering** — Show all students or only those in a specified tutorial group with `list`.
* **Group overview** — List tutorial groups and their student counts with `groups`.
* **Student details** — Read academic and contact details directly on each student list card.
* **Student remarks** — Add, replace, and clear one current free-text remark per student.
* **Search** — Find students by name, matriculation number, or tag.
* **Sorting** — Sort the displayed students by name or tutorial group.
* **Follow-ups** — Mark students for follow-up, view the follow-up list, and mark follow-ups complete.
* **Data persistence** — Save student record changes automatically.

## Current release scope

The v1.4 alpha version manages student rosters across tutorial groups. Tutors can filter students by group, view each
group's student count, and move students between groups by editing their records. TutorLink does not provide standalone
tutorial-group creation, editing, or deletion.

Attendance tracking and participation records are future enhancements and are not available in v1.4.

## Documentation

For more information about TutorLink, visit our [Project Website](https://ay2627s1-cs2103t-t13-4.github.io/tp/).

You can also refer to the:

* [User Guide](https://ay2627s1-cs2103t-t13-4.github.io/tp/UserGuide.html)
* [Developer Guide](https://ay2627s1-cs2103t-t13-4.github.io/tp/DeveloperGuide.html)

## Acknowledgements

This project is based on the [AddressBook-Level3](https://github.com/se-edu/addressbook-level3) project created by the [SE-EDU initiative](https://se-education.org).
