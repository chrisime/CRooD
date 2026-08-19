---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Use Testcontainers PostgreSQL for service integration tests

## Context and Problem Statement

CRooD is a Kotlin CRUD library built on jOOQ. Its core `CRooDService` was completely untested; establishing a test suite required choosing a test database. jOOQ's `lastID`/`returning` semantics are dialect-specific, so the database used by the tests must match the dialect the library is documented against (`examples/` use PostgreSQL) or the tests silently assert wrong behavior. The repository previously had no test-database strategy at all.

## Decision Drivers

- Dialect fidelity for jOOQ `lastID`/`returning` semantics
- Repeatable tests in CI without permanent infrastructure
- No silent test skips
- Follow-up changes (service improvements, composite keys) must reuse the same harness

## Considered Options

- Testcontainers PostgreSQL
- In-memory H2/SQLite
- Permanently mounted PostgreSQL

## Decision Outcome

Chosen option: "Testcontainers PostgreSQL", because it provides the real PostgreSQL dialect jOOQ must be verified against, launches repeatably per test run, and runs on GitHub Actions (which ships Docker).

### Consequences

- Good, because integration tests verify real `lastID`/`returning` semantics.
- Good, because the container is throwaway and CI-repeatable.
- Bad, because an integration-suite run requires Docker; tests fail with a loud error when Docker is unavailable rather than silently skipping.
- Bad, because starting a container adds test-run latency (mitigated with a single shared container per suite).

### Confirmation

The `CRooDService` integration suite runs against a Testcontainers PostgreSQL container and the `test` task gates the CI build.

## Pros and Cons of the Options

### Testcontainers PostgreSQL

Provides dialect-accurate jOOQ behavior with throwaway containers.

- Good, because it matches the dialect of the documented examples.
- Good, because it requires no permanent database.
- Neutral, because it needs Docker on the machine running tests.
- Bad, because container startup adds latency.

### In-memory H2/SQLite

Fast, no Docker, but wrong dialect semantics.

- Good, because tests start instantly and need no Docker.
- Bad, because `lastID`/`returning` semantics diverge and could hide service regressions.

### Permanently mounted PostgreSQL

Reproducible but operationally heavy.

- Good, because it is a real PostgreSQL instance.
- Bad, because it is not reproducible in CI without extra orchestration.

## More Information

Resolved as Decision Question 1 in the `add-test-coverage` proposal. See `openspec/changes/add-test-coverage/design.md` (D1, D2, D5).