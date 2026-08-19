---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Build on the current Kotlin 2.x toolchain with current dependencies

## Context and Problem Statement

The build pinned Kotlin 1.7.20 with `apiVersion/languageVersion = 1.5`, JVM 11, jOOQ 3.17.4, JUnit 5.8.2, and kotest 5.5.1 — a 2022-era, unmaintained toolchain. Continuing on the old line would accumulate compiler and dependency drift, and the codegen/config/test-suite changes planned against this repo assume a current language level.

## Decision Drivers

- A supported, maintained toolchain
- Matching `apiVersion`/`languageVersion`/`jvmTarget` (no `1.5` pin)
- Current jOOQ/JUnit/kotest/JSON versions with examples still building

## Considered Options

- Jump to current Kotlin (2.x) plus current dependencies
- Stay on the Kotlin 1.7.x line with minimal bumps

## Decision Outcome

Chosen option: "Jump to current Kotlin (2.x) plus current dependencies", because it removes the unmaintained pin, matches the modern compiler semantics, and the coordinated codegen/config changes already assume a modern language level.

### Consequences

- Good, because the toolchain becomes supported and current.
- Good, because `apiVersion`/`languageVersion`/`jvmTarget` are aligned to the chosen target.
- Bad, because compiler upgrades may surface warnings/errors in generated code (mitigated by the codegen change's goldens).
- Bad, because dependency bumps interact with the config (Hoplite) change's version requirements.

### Confirmation

The `current-toolchain` spec asserts `./gradlew build` compiles main + tests and the test suite passes on the updated toolchain, dependencies resolve to current versions, and examples still build.

## Pros and Cons of the Options

### Jump to current Kotlin 2.x

- Good, because supported toolchain; coordinated with codegen/config changes.
- Bad, because a larger migration surface and possible compiler fallout.

### Stay on Kotlin 1.7.x

- Good, because low migration risk.
- Bad, because keeps an unmaintained toolchain and defers the inevitable bump; rejected in DQ2.

## More Information

Resolved as Decision Question 2 in the `modernize-build-release` proposal (design D2-D3).