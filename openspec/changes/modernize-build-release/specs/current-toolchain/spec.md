# current-toolchain

The build moves to a current supported toolchain: modern Kotlin (2.x), current jOOQ/JUnit versions, and current GitHub Actions majors, so the library builds and its generated output survives the language-level updates.

## ADDED Requirements

### Requirement: build compiles on a current Kotlin and JVM target
The Gradle build SHALL compile with an updated Kotlin version and matching `apiVersion`/`languageVersion`/`jvmTarget` settings for the chosen JVM target (no longer pinned to `1.5`), and the full test suite SHALL pass with the updated toolchain.

Feature: current-toolchain
Rule: Modern Kotlin with updated language level

#### Scenario: Compile main and test sources on the updated Kotlin
- **GIVEN** the build configured with the updated Kotlin and JVM target versions
- **WHEN** `./gradlew build` runs
- **THEN** main and test sources compile
- **AND** the test task passes

### Requirement: dependencies are current supported releases
The jOOQ, JUnit (bom), kotest, and JSON dependency versions SHALL be raised to current supported releases, and the examples SHALL still build against the updated library.

#### Scenario: Resolve updated dependencies and build examples
- **GIVEN** updated jOOQ, JUnit, and other dependency versions in the version catalog
- **WHEN** the dependency update check lists the configured versions as current
- **AND** the example projects build against the library
- **THEN** the updated dependency versions are in effect

### Requirement: GitHub Actions use current major versions
The workflows SHALL use current major versions of the checkout, setup-java, and wrapper-validation actions, and the build SHALL stay green after the update.

#### Scenario: CI runs with current actions
- **GIVEN** the CI workflow using updated action versions
- **WHEN** a push triggers the build workflow
- **THEN** the workflow runs the build successfully on the current environment