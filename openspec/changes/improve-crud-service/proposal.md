## Source

<!-- Deliberate skip: this change does NOT originate from a Jira ticket.
     The jira-driven schema normally populates this section verbatim via the
     jira-import skill (config rule) when a change's requirements come from a
     Jira ticket. This change was identified from a codebase review; the
     jira-import / jira-grill-me skills do not apply because there is no
     ticket to load or interrogate. -->

- **Ticket:** none (codebase-review origin)
- **Summary:** Rework `CRooDService` CRUD operations for correctness and efficiency
- **Description:** Not applicable (no Jira ticket).
- **Acceptance Criteria:**
  1. Not applicable (no Jira ticket).

## Why

The core `CRooDService` has several correctness and performance problems: multi-row `create` does N+1 sequential inserts and sums `dsl.lastID()` (dialect-fragile), `update` loads matching rows into memory before writing, the five near-identical `update` overloads are boilerplate, `findSingle`-based reads throw instead of signalling absence consistently, and Java `Stream` returns leak resource-management concerns into callers.

## What Changes

- **BREAKING:** Replace the 1–5 field/value `update(...)` overloads with a top-level builder DSL (`update { set(field, value); where(...) }`) that emits one `UPDATE ... SET ... WHERE` statement instead of fetch-then-store.
- **BREAKING:** Change multi-row `create(Collection<D>)` to a single batched insert (returning last ID where supported, aggregate otherwise) instead of N+1 sequential inserts.
- Replace `dsl.lastID()` reliance with a jOOQ `returning`-based strategy where possible; fall back deterministically otherwise.
- Make read APIs consistent: `findById`/`findOne` return `Optional<D>` (or nullable) instead of throwing on zero rows; keep `findOptional` naming aligned.
- Introduce `Sequence<D>`-based streaming APIs as a Kotlin-friendly alternative to `Stream<D>` (keep `Stream` for Java interop).
- Add pagination support (e.g. `findAll(limit, offset)` / jOOQ `limit`/`offset`).
- Add bulk `deleteByIds(Collection<ID>)`.

## Capabilities

### New Capabilities

- `crud-service-batch-operations`: Batched inserts and bulk deletes for `CRooDService`.
- `crud-service-pagination`: Offset/limit pagination on `findAll` and friends.
- `crud-service-kotlin-streams`: Kotlin-idiomatic `Sequence` returns alongside the existing `Stream` API.

### Modified Capabilities

_(none — no existing specs exist.)_

## Impact

- Affected code: `src/main/kotlin/xyz/chrisime/crood/service/CRooDService.kt` and its tests.
- API: several signatures change (**BREAKING**); consumers of the 1–5 field `update` overloads and `create`/`find*` semantics must migrate.
- Examples under `examples/` use the current `update(field, value) { where(...) }` shape and must be updated.

## Decision Questions

- [x] [DQ1] · Do we keep the varargs `update`-with-lambda style (`update(f1,v1){ where(...) }`) or move to a builder/`Map` API?
      Decision: Top-level builder DSL (`update { set(...); where(...) }`), jOOQ-idiomatic.
      Status: resolved
      Reason: resolves the breaking `update` API shape that gates the migration scope.

## Open Questions

_(none open.)_