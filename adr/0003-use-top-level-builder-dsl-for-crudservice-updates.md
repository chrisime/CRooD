---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Use a top-level builder DSL for CRooDService updates

## Context and Problem Statement

`CRooDService` exposed five near-identical `update(field1, value1, ... field5, value5, tableOps)` overloads. Each overload fetched matching rows into memory (`dsl.fetch`), mutated them, then called `store`/`batchUpdate` — a fetch-then-store round trip for what is logically a single `UPDATE ... SET ... WHERE`. Adding a sixth overload would be needed for six fields, and the shape could not express `set`-from-map or jOOQ-typed field safety. The update API needed a single jOOQ-idiomatic shape.

## Decision Drivers

- One SQL statement per update instead of a read + write round trip
- Ability to express arbitrary `set` pairs without an overload per arity
- jOOQ type safety for fields and values
- Consistency for future service features

## Considered Options

- Top-level builder DSL (`update { set(...); where(...) }`)
- Keep varargs overloads (`update(f1, v1, f2, v2) { where(...) }`)
- `update(Map<Field<*>, Any?>, condition)` entry

## Decision Outcome

Chosen option: "Top-level builder DSL (`update { set(...); where(...) }`)", because it composes a single `dsl.update(table).set(...).where(...)` statement, matches jOOQ's own style, and eliminates the overload-per-arity treadmill.

### Consequences

- Good, because updates execute as one statement with no in-memory fetch.
- Good, because the builder accepts any number of `set` pairs with field-safe typing.
- Good, because examples and README read the same way jOOQ users already write.
- Bad, because the public API is a **breaking change**: the five `update` overloads are removed and existing callers (examples, downstream projects) must migrate.
- Bad, because `update` no longer reports per-row `store` semantics; it returns the affected-row count.

### Confirmation

`CRooDService` exposes only the builder-based `update { }` entry point, emits a single `UPDATE ... SET ... WHERE`, and the examples compile against it. The `crud-service-batch-operations` spec scenarios cover the behavior.

## Pros and Cons of the Options

### Top-level builder DSL

`update { set(field, value); where(condition) }`

- Good, because one statement, no N-row fetch.
- Good, because arbitrary sets, typed fields, jOOQ-idiomatic.
- Bad, because new syntax callers must learn and migrate to.

### Keep varargs overloads

`update(f1, v1, f2, v2) { where(...) }` with fetch-then-store

- Good, because existing callers keep working.
- Bad, because keeps the read + write round trip.
- Bad, because overloads multiply with field arity.

### Map with untyped entries

`update(Map<Field<*>, Any?>, condition)`

- Good, because a single signature.
- Bad, because untyped values lose jOOQ field safety.

## More Information

Resolved as Decision Question 1 in the `improve-crud-service` proposal (design D1).