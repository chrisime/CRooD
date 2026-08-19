---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Load CRooDConfig via Hoplite through an injectable entry point

## Context and Problem Statement

`CRooDConfigurationLoader` eagerly read `$HOME/crood.json` at object-initialization time (class-load) and hand-parsed it with `org.json`. Configuration was non-injectable, depended on the machine's home directory, and failed hard on malformed JSON. The generator consumed it via a static `val`, so callers could not control the source or lifecycle.

## Decision Drivers

- No class-load-time reads of the user home directory
- Consumers must control configuration source and lifecycle
- Graceful handling of missing and malformed configuration
- Same well-formed `crood.json` files must keep parsing identically

## Considered Options

- Hoplite (`com.sksamuel.hoplite`) with `CRooDConfig` as a loaded data class and an injectable loader API
- Keep `org.json` parsing, make it lazy only
- Jackson or YAML-only loader

## Decision Outcome

Chosen option: "Hoplite with `CRooDConfig` as a loaded data class and an injectable loader API", because it maps Kotlin data classes directly from JSON/YAML in one dependency, removes the hand-written parser, and gives consumers an explicit load entry point instead of a hidden home-dir read.

### Consequences

- Good, because no class-load I/O; config source and lifecycle are consumer-controlled.
- Good, because malformed config yields Hoplite's detailed decoding errors, wrapped with the source path.
- Good, because the existing well-formed `crood.json` layout and keys keep working.
- Bad, because the `croodConfigOfUserDir` static access is a **breaking change**; consumers migrate to the loader API.
- Bad, because the current old Kotlin toolchain may require the Hoplite version to be pinned or the toolchain bump to land first.

### Confirmation

The `injectable-configuration` spec asserts the generator uses explicitly supplied config with no home-dir read, missing file defaults, malformed content fails with a path-identifying message, unknown keys are ignored/warned, and JSON maps onto `CRooDConfig`.

## Pros and Cons of the Options

### Hoplite with an injectable loader

- Good, because data-class mapping, JSON+YAML in one dependency, explicit API.
- Good, because the hand-written `org.json` mapping is removed.
- Bad, because a new dependency and a breaking API change.

### Keep `org.json`, lazy only

- Good, because no dependency or API break.
- Bad, because the fragile hand-rolled parsing and untestable shape remain.

### Jackson/YAML-only

- Good, because a familiar loader exists.
- Bad, because it does not map Kotlin data classes as directly and does not match the resolved DQ1 choice.

## More Information

Resolved as Decision Question 1 in the `improve-config-loading` proposal (design D1-D4).