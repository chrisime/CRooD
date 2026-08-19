## 1. Update Builder DSL

- [ ] 1.1 Introduce the top-level `update { set(...); where(...) }` builder and `CRooDService.update { }` entry point compiling to a single `UPDATE ... SET ... WHERE`, returning the affected-row count.
- [ ] 1.2 Delete the five `update` overloads and the `storeOrBatchUpdate` helper from `CRooDService.kt`.

## 2. Batched Create and Bulk Delete

- [ ] 2.1 Rework `create(Collection<D>)` into a single batched insert; empty collection returns `0`, single record returns the generated key.
- [ ] 2.2 Replace `dsl.lastID()` reliance with `returning`/generated-keys where supported and a deterministic aggregate fallback otherwise.
- [ ] 2.3 Add `deleteByIds(Collection<ID>)` as a single `WHERE pk IN (ids)` statement returning the deleted-row count.

## 3. Read API Consistency and Streaming

- [ ] 3.1 Switch `findById`/`findOne`/`findOptional`/`findOptionalById` to `Optional<D>` via `fetchOptionalInto`; remove throwing read helpers from the public API.
- [ ] 3.2 Add Kotlin `Sequence<D>` `findAll` variants (lazy cursor, closed on exhaustion) alongside the existing `Stream<D>` variants.

## 4. Pagination

- [ ] 4.1 Add `findAll(limit, offset)` (and paginated whereStep variant) with explicit PK-ordered `limit`/`offset` so pages are deterministic and non-overlapping.

## 5. Examples and Docs Migration

- [ ] 5.1 Update `examples/` and the README to the new builder, `Optional`, `Sequence`, pagination, and `deleteByIds` shapes.

## 6. Verification Gate

- [ ] 6.1 Run `openspec validate improve-crud-service --type change --strict` and confirm exit 0.
- [ ] 6.2 Run `run_acceptance.py --lint` and `run_acceptance.py --check-ac` and confirm both pass.