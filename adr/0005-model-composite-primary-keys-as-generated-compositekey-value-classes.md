---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Model composite primary keys as generated CompositeKey value classes

## Context and Problem Statement

CRooD could not handle composite primary keys: `PrimaryKey.equal` hard-required `ids.size == 1`, both generators' `generateCompositeKeyConstructor` were `TODO`, and `CRooDService`'s `ID` generic had no way to express a multi-column key. For a generic CRUD library this was the largest feature gap, and any design needed a type-safe way to carry multiple key components through the existing `PrimaryKey`/`CRooDService` machinery.

## Decision Drivers

- A type-safe representation of a multi-column key for the `ID` generic
- Backward compatibility for single-column PKs
- Positional matching against jOOQ key-column order
- Minimal new machinery in the generators

## Considered Options

- Generated `CompositeKey` value class per composite-PK table
- Raw vararg of `Any` passed to each call
- Generic tuple type (e.g. `CompositeKey<T1, T2>`)

## Decision Outcome

Chosen option: "Generated `CompositeKey` value class per composite-PK table", because it gives `CRooDService`'s `ID` generic a concrete, compiler-checked type, matches the jOOQ generated-table style, and the codegen already owns per-table type emission.

### Consequences

- Good, because composite keys are type-safe and positional with the key-column order.
- Good, because single-column PKs keep their existing API.
- Good, because `PrimaryKey.equal` drops the artificial `ids.size == 1` guard for a component-count guard.
- Bad, because codegen output grows (a sibling type per composite-PK table), captured by snapshot goldens.
- Bad, because the composite-key path adds a generated type to the public surface.

### Confirmation

The `composite-primary-keys` spec asserts composite condition building, `CRooDService` composite-key reads/deletes, and generated composite constructors + value class, with single-column output preserved.

## Pros and Cons of the Options

### Generated `CompositeKey` value class

- Good, because typed, positional, idiomatic with generated tables.
- Good, because the `ID` generic resolves cleanly.
- Bad, because codegen emits an extra type per composite table.

### Raw vararg of `Any`

- Good, because no codegen.
- Bad, because untyped and unsafe; can't express the `ID` generic.

### Generic tuple `CompositeKey<T1, T2>`

- Good, because reusable.
- Bad, because unwieldy past two columns and duplicates what generated types already provide.

## More Information

Resolved as Decision Question 1 in the `add-composite-key-support` proposal (design D1-D4).