# Research notes

This directory is where Cofinance keeps what is known about the project: how each area works today, why it was built that way, and what is still open. It replaces the OpenSpec `specs/` and `changes/` trees, which were removed on 2026-11-16.

Each note answers one question against primary sources — this repository's code and configuration first, then official documentation for the libraries and platforms involved — and cites the source of every claim.

## Notes

| Note | Question it answers |
|------|---------------------|
| [build-and-modules.md](build-and-modules.md) | Which modules, targets, and dependencies make up the build, and what does a build need? |
| [ci-and-distribution.md](ci-and-distribution.md) | What do the GitHub workflows do, and how do builds reach testers? |
| [architecture.md](architecture.md) | How is the shared code laid out, and which conventions does it actually follow? |
| [authentication.md](authentication.md) | How does sign-in work, and what can a signed-out user do? |
| [local-data-and-sync.md](local-data-and-sync.md) | Where does finance data live, and how does it move between the device and Firebase? |
| [client-side-encryption.md](client-side-encryption.md) | How is synchronized data encrypted, and how is the key recovered? |
| [app-lock.md](app-lock.md) | How do the PIN, biometric shortcut, and auto-lock work? |
| [receipt-scanning.md](receipt-scanning.md) | How does a receipt photo become a draft transaction? |
| [history.md](history.md) | Which changes were delivered under OpenSpec, and what did each one decide? |

## Conventions

- **One question per file.** Name the file after the topic in kebab-case.
- **Cite the owner of the claim.** Code claims cite `path:line`. Library and platform claims cite the official documentation, not a blog post about it.
- **Say how a claim was established.** "Read in code" and "observed on a device" are different strengths of evidence; say which. Nothing in these notes was established by an automated test, because the repository currently has none.
- **Record the date and commit** the note was last checked against, in its header. Line numbers drift; the commit says what they referred to.
- **Keep decisions with their reasons.** A decision without its rationale gets re-litigated; a rationale lets a later reader tell whether the reason still holds.
- **List what is open** at the bottom of the note, including defects found while reading. An open item is a finding, not a task list.

To add or refresh a note, run `/mattpocock-skills:research` with the question. Update the relevant note in the same change that alters the behaviour it describes.

## Path shorthand

To keep citations readable, the notes shorten the two long source roots:

| Shorthand | Expands to |
|-----------|------------|
| `app/` | `composeApp/src/commonMain/kotlin/id/andriawan/cofinance/` |
| `app-android/` | `composeApp/src/androidMain/kotlin/id/andriawan/cofinance/` |
| `app-ios/` | `composeApp/src/iosMain/kotlin/id/andriawan/cofinance/` |
| `core/` | `core/src/commonMain/kotlin/id/andriawan/cofinance/data/` |
| `core-android/` | `core/src/androidMain/kotlin/id/andriawan/cofinance/data/` |
| `core-ios/` | `core/src/iosMain/kotlin/id/andriawan/cofinance/data/` |

All notes were last verified on 2026-11-16 against commit `ac66286`.
