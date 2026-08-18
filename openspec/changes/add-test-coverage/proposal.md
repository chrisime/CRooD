## Source

<!-- Deliberate skip: this change does NOT originate from a Jira ticket.
     The jira-driven schema normally populates this section verbatim via the
     jira-import skill (config rule) when a change's requirements come from a
     Jira ticket. This change was identified from a codebase review; the
     jira-import / jira-grill-me skills do not apply because there is no
     ticket to load or interrogate. -->

- **Ticket:** none (codebase-review origin)
- **Summary:** Build a baseline test suite for the CRooD runtime and generator
- **Description:** Not applicable (no Jira ticket).
- **Acceptance Criteria:**
  1. Not applicable (no Jira ticket).

## Why

The repository has exactly one test file (`CRooDConfigSpec`, 2 scenarios). The core `CRooDService`, both code generators, `PrimaryKey`, and the reflection helpers in `GenericExt` are completely untested — which is precisely how the `Any::class.java` runtime bug and the N+1 insert issues survived. A baseline suite is a prerequisite for safely making any of the other improvements.

## What Changes

- Add unit tests for `GenericExt` (`newInstance`, `getClassAtIndex`, `asType`).
- Add unit tests for `PrimaryKey.equal` covering single-key, wrong-arity, and type-mismatch cases.
- Add service tests for `CRooDService` CRUD paths against **Testcontainers PostgreSQL** (matching the examples' dialect) exercising `create`, `findById`, `update`, `delete`, select-count, and `truncate`.
- Add generator snapshot/golden-file tests asserting the exact Java and Kotlin domain output for representative schemas.
- Wire the tests into the Gradle `test` task and CI build; aim for meaningful coverage of the core paths.
- Coordinate: the `fix-genericext-reflection` change adds the `GenericExt` tests; this change owns the service + generator suites and any shared fixtures.

## Capabilities

### New Capabilities

- `runtime-test-suite`: A repeatable unit + integration test harness covering service CRUD, `PrimaryKey`, and `GenericExt`.
- `codegen-snapshot-suite`: Golden-file tests locking generated Java/Kotlin domain output.

### Modified Capabilities

_(none — no existing specs exist.)_

## Impact

- Affected code: `src/test/` (new tests + fixtures), `build.gradle.kts`/`gradle.properties` (testcontainers + postgresql + kotest assertions), `.github/workflows/build.yml` if test deps change.
- API: none — tests only.
- CI: test task becomes meaningful; failures gate the build.

## Decision Questions

- [x] [DQ1] · Test database: in-memory H2/SQLite (fast, no Docker) vs Testcontainers PostgreSQL (matches examples)?
      Decision: Testcontainers PostgreSQL for dialect fidelity (lastID/returning semantics).
      Status: resolved
      Reason: service tests need a real jOOQ dialect; Postgres matches the examples and CI gains Docker.

## Open Questions

_(none open.)_