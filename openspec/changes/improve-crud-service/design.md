# improve-crud-service — Design

## Context

`CRooDService<R, ID, D>` (line ~120-234 in `CRooDService.kt`) has correctness and efficiency problems that follow directly from its implementation:

- **`create(Collection<D>)`** (line 123-127) maps each record through single `create` → N+1 sequential inserts, summing `dsl.lastID()`. Summing last-insert-ids across a batch is dialect-fragile and wrong spot to be optimistically correct.
- **`update(...)`** (5 near-identical overloads, lines 130-212) *fetch matching records into memory* via `dsl.fetch(...)`, mutate them, then `store`/`batchUpdate`. For a big table this is an N-row read + write round trip instead of one `UPDATE ... SET ... WHERE`.
- **`findById`/`findOne`** (lines 92-104) use `fetchSingleInto`, which **throws** `NoDataFoundException` when zero rows match; only `findOptional*` (lines 107-111) return `Optional`.
- **`findAll`/`findAll(whereStream)`** (lines 95-100) return Java `Stream<D>`, pushing resource-management concerns onto Kotlin callers.
- No pagination (no `limit`/`offset`), no bulk delete.

Per DQ1 the `update` overloads are replaced by a jOOQ-idiomatic top-level builder: `update { set(...); where(...) }`.

## Goals / Non-Goals

**Goals:**
- One `UPDATE ... SET ... WHERE` per update call (no fetch-then-store for bulk updates).
- Batched single-statement multi-row `create`; deterministic last-id handling (`returning` where supported, aggregate otherwise).
- Consistent absence signaling: `findById`/`findOne` return `Optional<D>` (matching the existing `findOptional*` naming) instead of throwing.
- Kotlin `Sequence<D>` returns alongside the Java `Stream<D>` API for interop.
- Deterministic pagination (`limit`/`offset`) on `findAll`.
- Bulk `deleteByIds(Collection<ID>)` in one statement.
- Examples updated to the new API shape.

**Non-Goals:**
- No change to the `pkFields`/`PrimaryKey` machinery (owned by the composite-key change).
- No transaction-boundary decisions inside `CRooDService` (caller-managed as today).
- No async/reactive API.
- No change to `truncate`/`exists*`/`selectCount*` behavior beyond what the specs name.

## Decisions

### D1: Update via a jOOQ-idiomatic builder DSL
Replace the 1–5 field overloads with a receiver-scoped builder that compiles to a single statement:

```kotlin
service.update {
    set(field, value)
    where(condition)
}
```

Implementation composes `dsl.update(table).set(...).where(...)`; the builder captures `set` pairs and a `where` predicate and executes once, returning the affected-row count.

- **Rationale:** Matches what jOOQ callers already write and kills the fetch-then-store round trip (the `storeOrBatchUpdate` path disappears). DQ1 resolved this exact shape.
- **Alternatives considered:**
  - Keep varargs `update(f1, v1, f2, v2){ ... }` → rejected: breeds an overload per arity (the current `update` problem) and can't express `set`-from-map cleanly.
  - `update(Map<Field<*>, Any?>, condition)` → rejected: untyped entries lose jOOQ's field-safety; DQ1 chose the builder.

### D2: Batched create with `returning` when available
`create(Collection<D>)` builds one `dsl.batchInsert(records)` (or `batch` of inserts) so the N inserts collapse into a single statement bundle. For the single-record path (and tables with an auto-increment key where jOOQ can return generated keys), use `returning`/`getGeneratedKeys` to read the last id; when the dialect or shape does not support returning, aggregate the batch execution result deterministically (e.g. sum of executed row counts) instead of summing `lastID()`.

- **Rationale:** N+1 disappears; `lastID()` summing was the fragile piece (Postgres `lastval` dance). Per spec, empty collection → `0`, single record → generated key returned.
- **Alternatives considered:**
  - Keep sequential inserts, just remove the `lastID` sum → faster to write but leaves the N+1 query cost; rejected per capability goal.
  - Real upsert-style `MERGE` → out of scope; not requested by specs.

### D3: Consistent `Optional` reads
`findById`, `findOne`, `findOptional`, and `findOptionalById` all return `Optional<D>`, implemented with `fetchOptionalInto(domain)`; the throwing `fetchSingleInto` variants are removed from the read API.

- **Rationale:** Absence is a normal state for these queries; the specs mandate `Optional` (not an exception) for missing `findById`/`findOne`.
- **Alternatives considered:** nullable `D?` → rejected by spec wording ("`Optional<D>` (or nullable)" with `findOptional` naming aligned); `Optional` keeps Java interop.

### D4: Kotlin `Sequence` streaming alongside `Stream`
`findAll` (all forms) return `Sequence<D>` by default; the existing `Stream<D>` variants stay for Java interop. The `Sequence` wraps a lazily-closed `fetchLazy`-style cursor.

- **Rationale:** Kotlin callers get lazy traversal without owning a `Stream`'s closeable resource (the spec's motivation). Java consumers keep `Stream`.
- **Alternatives considered:** replace `Stream` entirely → breaks the Java-facing API and the library's README examples; rejected.

### D5: Deterministic pagination
`findAll(limit, offset)` (and any paginated `findAll(whereStep, limit, offset)`) pins an explicit `ORDER BY` on the primary-key fields so pages are stable across calls, then applies jOOQ `limit(limit).offset(offset)`.

- **Rationale:** Without a stable order, "later page" and "pages do not overlap" are undefined. PK order is the cheapest deterministic key.
- **Alternatives considered:** no-order pagination → nondeterministic page membership; rejected. Secondary-index ordering configurable by callers → scope creep; not in specs.

### D6: `Sequence`/`Stream` pagination and bulk delete wiring
`deleteByIds(Collection<ID>)` compiles `dsl.deleteFrom(table).where(primaryKey.in(ids)).execute()` and returns affected rows; empty collection → `0` no-op.

- **Rationale:** One statement, matches `deleteById`'s single-row shape; spec requires count return including `0` for missing ids.

## Risks / Trade-offs

- [BREAKING API migration] -> Examples under `examples/` and the README update along with the change; version bump communicates the break. Deprecated-bridge overloads considered but rejected to keep the surface clean.
- [`returning` support varies by dialect] -> Single-record path uses `getGeneratedKeys`/`returning` where supported and falls back deterministically (aggregate row counts) otherwise — never `lastID()` summing.
- [Sequence laziness holds a cursor open] -> Sequences close their underlying cursor on exhaustion; documented explicitly so callers don't assume eager materialization.
- [Pagination with PK order can skip after in-place key updates] -> Re-paging after mutable-PK updates yields shifted pages; acceptable for read-mostly paging, noted as a known limitation.

## Migration Plan

1. Introduce the `UpdateBuilder` DSL type and a `CRooDService.update { }` entry point; delete the 5 overloads and the `storeOrBatchUpdate` helper.
2. Rework `create` (single + collection) to batched insert; wire `returning`/generated-keys with the deterministic fallback.
3. Switch `findById`/`findOne`/`findOptional` to `Optional`; remove throwing read helpers from the public API.
4. Add `Sequence<D>` `findAll` variants and `findAll(limit, offset)` with PK-ordered `limit`/`offset`; keep `Stream` variants.
5. Add `deleteByIds`.
6. Update `examples/` and the README to the new signatures.
7. Land after `fix-genericext-reflection` (so `CRooDService` is constructible) and coordinate with `add-test-coverage` to extend the service suite's specs to the new API.
8. Rollback: revert to the previous commit — the overloads and helpers are self-contained in `CRooDService.kt` plus examples.

## Open Questions

- Whether the default `findAll()` (no paging) returns `Sequence` and paginated reads accept an explicit `limit`/`offset`, or a paged result object should be introduced later — the specs only require `findAll(limit, offset)` returning "at most `limit` records"; a richer page model is deferred.
- Whether `GRADLE` version-catalog updates belong to this change or `modernize-build-release`; dependency bumps follow that change to avoid conflicting diffs.