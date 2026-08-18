## Source

<!-- Deliberate skip: this change does NOT originate from a Jira ticket.
     The jira-driven schema normally populates this section verbatim via the
     jira-import skill (config rule) when a change's requirements come from a
     Jira ticket. This change was identified from a codebase review; the
     jira-import / jira-grill-me skills do not apply because there is no
     ticket to load or interrogate. -->

- **Ticket:** none (codebase-review origin)
- **Summary:** Modernize the build, dependency, and release pipeline
- **Description:** Not applicable (no Jira ticket).
- **Acceptance Criteria:**
  1. Not applicable (no Jira ticket).

## Why

The release pipeline targets Bintray, which has been shut down since 2021; its `publish.yml` triggers on repository stars and the README badge is dead. GitHub Actions pin archived/deprecated versions (`actions/create-release@v1`, `checkout@v2.3.4`, `setup-java@v1.4.3`), the release body is a placeholder, Kotlin is pinned to 1.7.20 with `languageVersion/apiVersion = 1.5`, and jOOQ/JUnit/org.json are years old. There is no journaled publish destination for consumers.

## What Changes

- **REPLACED:** Remove the dead Bintray publish workflow and badge; publish snapshots/releases to **GitHub Packages** (`maven-publish` with a `github` repository on `packageRegistry`, gated on tags) and document it in README.
- Replace `actions/create-release@v1` with `gh release create` (or `softprops/action-gh-release`) and a changelog-driven body.
- Bump GitHub Actions to current major versions (`checkout@v4`, `setup-java@v4`, current gradle action).
- Break the star-trigger; publish on tag/version bumps instead.
- **BREAKING:** Raise Kotlin `apiVersion/languageVersion` and the Kotlin/JUnit/jOOQ/org.json dependency versions to current supported releases; verify examples still build.
- Optionally migrate `org.json` parsing off the unmaintained-in-practice dependency (coordinated with `improve-config-loading`).

## Capabilities

### New Capabilities

- `reliable-release-pipeline`: Repeatable tag-based publishing to a live artifact repository with real release notes.
- `current-toolchain`: Updated Kotlin language level, JVM target, and dependency versions.

### Modified Capabilities

_(none — no existing specs exist.)_

## Impact

- Affected files: `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`, `.github/workflows/*.yml`, `README.md`, examples' build files.
- API: language/toolchain bump may surface compiler warnings/errors in generated code (mitigated by the codegen change).
- CI: build must stay green after action and dependency updates.

## Decision Questions

- [x] [DQ1] · Publish target: Maven Central (formal, needs Sonatype setup + GPG) vs JitPack (zero-config) vs maintain a private repo?
      Decision: GitHub Packages (`maven-publish` to `packageRegistry` on releases), free and org-scoped.
      Status: resolved
      Reason: determines the release workflow and the publication repository in build.gradle.kts.
- [x] [DQ2] · Dependency bump scope: minimum viable bumps (keep Kotlin 1.7.x line) vs jump to current Kotlin (2.x)?
      Decision: Jump to a current toolchain (Kotlin 2.x, current jOOQ/JUnit, current GitHub Actions).
      Status: resolved
      Reason: the 2022-era stack is unmaintained; a larger jump changes codegen output and is coordinated with the codegen/config/tooling changes.

## Open Questions

_(none open.)_