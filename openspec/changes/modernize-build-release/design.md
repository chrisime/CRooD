# modernize-build-release — Design

## Context

The release pipeline and toolchain are from the 2022 era:

- `publish.yml` targets **Bintray** (shut down 2021) and triggers `on: watch` (**repository stars**); `release.yml` uses `actions/create-release@v1` (deprecated) with a placeholder body; `build.yml` uses `checkout@v3`/`setup-java@v3` and full-SHA gradle actions.
- Build pins: Kotlin 1.7.20 with `apiVersion/languageVersion = 1.5` (`build.gradle.kts:33-35,45-47`), java 11 target, jOOQ 3.17.4, JUnit 5.8.2, kotest 5.5.1, `org.json` 0.42.0 (`gradle.properties`).
- No journaled publish destination: no consumers can fetch a published artifact.

Per DQ1 publishing moves to GitHub Packages; per DQ2 the toolchain jumps to current Kotlin (2.x) and current dependencies/Actions.

## Goals / Non-Goals

**Goals:**
- Tag-driven publishing to GitHub Packages via `maven-publish` (`github` repository on `packageRegistry`); star-triggered publish removed.
- Real release bodies via `gh release create` (changelog-driven), replacing the placeholder `create-release@v1`.
- Current GitHub Actions majors (`checkout@v4`, `setup-java@v4`, current wrapper-validation + gradle-build actions).
- Current Kotlin (2.x) with matching `apiVersion`/`languageVersion`/`jvmTarget`; current jOOQ/JUnit/kotest/JSON versions; examples still build.
- README documents the new publish destination and removes the dead Bintray badge.

**Non-Goals:**
- No Maven Central publishing (rejected in DQ1: Sonatype setup + GPG, org-scoped free alternative).
- No migration of `org.json` itself (coordinated with `improve-config-loading`, which replaces it with Hoplite); this change only bumps its version if it stays.
- No codegen-behavior rework (owned by improve-codegen); the Kotlin 2.x jump may surface compiler cleanups which follow that change's goldens.

## Decisions

### D1: Publish to GitHub Packages on tags via `maven-publish`
Add a `github` publication (`maven-publish` repository named `github`, URL `https://maven.pkg.github.com/<owner>/<repo>`, credentials from `GITHUB_TOKEN`), wired to run on version-tag pushes. Delete `publish.yml`'s Bintray + star trigger; use `gh release create` for the GitHub Release with a changelog-driven body.

- **Rationale:** DQ1 resolved GitHub Packages — free, org-scoped, no Sonatype/GPG ceremony, and it gives consumers a live repository. Tag gating matches the spec ("publishing is tag-driven").
- **Alternatives considered:**
  - Maven Central → rejected in DQ1: requires Sonatype + GPG setup and coordination.
  - JitPack → rejected in DQ1: build-on-demand differs from a published artifact registry the maintainer controls.

### D2: Jump the toolchain to current Kotlin (2.x)
Raise Kotlin to a current 2.x release, drop the `apiVersion/languageVersion = 1.5` pin (set to the current language level and a chosen JVM target, e.g. 17), and bump jOOQ/JUnit-bom/kotest/JSON to current supported versions in the version catalog. Verify `./gradlew build` and the examples.

- **Rationale:** DQ2 resolved the full jump; the 2022 stack is unmaintained, and Kotlin 2.x changes default compiler behavior that pins need to match.
- **Alternatives considered:** stay on the 1.7.x line → rejected in DQ2: keeps an unmaintained toolchain and would later force the same migration on worse footing.

### D3: Current GitHub Actions majors
Update `build.yml`/`release.yml` to `actions/checkout@v4`, `actions/setup-java@v4`, current `gradle/wrapper-validation-action` and `gradle/gradle-build-action` major refs (or a maintained `setup-gradle` replacement), keeping `on: [push]` build gating.

- **Rationale:** The spec requires current majors and CI green after the update; deprecated `create-release@v1` is replaced by `gh release create` per the release-body requirement.
- **Alternatives considered:** pinning full SHAs for every action → more reproducible but diverges from the "current major" contract and adds maintenance; the version-catalog's existing full-SHA pins were 2022-era and are replaced.

### D4: Changelog-driven release body
`gh release create <tag> --notes-file <changelog-notes>` where notes are derived from commits/tags between releases; remove the placeholder body text.

- **Rationale:** Satisfies "release body contains the actual changes, not a placeholder".

## Risks / Trade-offs

- [GitHub Packages requires `GITHUB_TOKEN` with `packages: write`] -> The release workflow configuration sets `permissions: packages: write`; publish failure surfaces as a workflow failure, not a silent no-op.
- [Kotlin 2.x compiler upgrades may break generated code] -> Coordinate with `improve-codegen` (Kotlin generators) and `add-composite-key-support` (codegen output); the shared snapshot goldens catch drift.
- [Dependency bump interacts with `improve-config-loading`'s Hoplite addition] -> Hoplite's required Kotlin version may dictate which bump lands first; the config change already records that dependency.
- [Tag pattern open/closed (`v*` vs numeric)] -> A documented `v*` tag regex gates publishing; wildcard tags are refused absent a check.
- [JNA/testcontainers on the new toolchain] -> `add-test-coverage` revalidates the service-run matrix once the toolchain lands (its design records this).

## Migration Plan

1. Update `gradle.properties`/version catalog: Kotlin 2.x, current jOOQ/JUnit-bom/kotest/JSON; set `jvmTarget` and matching `apiVersion`/`languageVersion` in `build.gradle.kts`.
2. Update `build.yml`/`release.yml` to current action majors; replace `create-release@v1` with `gh release create` + changelog notes.
3. Add the `github` maven-publish repository; rewrite `publish.yml` to tag-gated publishing of the artifact to GitHub Packages; delete the Bintray workflow and badge from `README.md`.
4. Run `./gradlew build`; fix compiler/dependency fallout; build the examples.
5. Push a `v*` tag in CI-review mode and verify the release + package.
6. Rollback: revert the workflow/properties/build files; publishing reverts to the pre-change state (no live registry).

## Open Questions

- Whether `org.json` is bumped, removed (via `improve-config-loading`), or left pinned — coordinated with that change; this change bumps it only until Hoplite replaces it.
- Exact Kotlin 2.x minor + JVM target (17 vs 21) chosen at implementation based on the running JDK and example stack.