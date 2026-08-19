---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Single-record reads return Optional instead of throwing

## Context and Problem Statement

`CRooDService.findById` and `findOne` used `fetchSingleInto`, which throws `NoDataFoundException` when zero rows match. Absence is a normal state for these queries, and the library already had `findOptional` / `findOptionalById` returning `Optional`. The read API mixed throwing and non-throwing absence semantics inconsistently.

## Decision Drivers

- Absence must be signaled consistently for single-record reads
- Java interop (keep `Optional`)
- Consumers must be able to distinguish "absent" from an I/O or query failure

## Considered Options

- All single-record reads return `Optional<D>`
- All single-record reads return nullable `D?`
- Keep throwing reads and add separate optional variants

## Decision Outcome

Chosen option: "All single-record reads return `Optional<D>`", because absence then has one consistent, non-exception signal across `findById`, `findOne`, `findOptional`, and `findOptionalById`, and `Optional` keeps Java interop.

### Consequences

- Good, because missing rows are a normal, non-throwing outcome.
- Good, because `Optional` work for Java and Kotlin callers.
- Bad, because it is a **breaking change**: existing callers that relied on the throwing read must migrate to `.orElseThrow(...)` etc.
- Bad, because callers must unwrap `Optional` even when they expect a row.

### Confirmation

The `crud-service-kotlin-streams` spec scenarios assert missing `findById`/`findOne` returns an empty `Optional` instead of an exception.

## Pros and Cons of the Options

### All reads return `Optional<D>`

- Good, because one consistent absence signal.
- Good, because `Optional` interops cleanly.
- Bad, because breaking change for throwing-read callers.

### Nullable `D?`

- Good, because simple in Kotlin.
- Bad, because Java callers lose the null-safety contract and `Optional` idiom.

### Keep throwing + add optional variants

- Good, because non-breaking.
- Bad, because two APIs for the same query encourage inconsistent usage.

## More Information

Design D3 in `openspec/changes/improve-crud-service/design.md`.