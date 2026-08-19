# add-composite-key-support — Design

## Context

CRooD cannot handle composite primary keys today:

- `PrimaryKey` (`src/main/kotlin/xyz/chrisime/crood/id/PrimaryKey.kt:24-35`) is a `@JvmInline value class PrimaryKey(private val id: Any)` whose `equal(vararg ids: TableField<*, *>)` hard-requires `ids.size == 1` and validates the single value against `ids[0].dataType`.
- Both `DomainGenerator.generateCompositeKeyConstructor` (`DomainGenerator.kt:136`) and `KDomainGenerator.generateCompositeKeyConstructor` (`KDomainGenerator.kt:111`) are `TODO("composite key handling not yet implemented")`.
- `CRooDService` derives `pkFields` from the table keys and passes `PrimaryKey(id).equal(*pkFields)`.

Per DQ1 the composite-PK value is a typed `CompositeKey` value class, generated once per composite-PK table, with the `CRooDService` `ID` generic resolving to that type.

## Goals / Non-Goals

**Goals:**
- `PrimaryKey.equal` builds a positional multi-column `Condition` from component values for composite keys, keeping the single-column path unchanged.
- `CRooDService` accepts a typed `CompositeKey` in `findById`, `existsById`, `findOptionalById`, `deleteById` and builds the correct multi-column condition.
- Both generators emit a composite-key constructor and a typed `CompositeKey` value class for composite-PK tables.
- Fully backward compatible for single-column PKs.

**Non-Goals:**
- No schema/DDL work — composites are read from the jOOQ table metadata.
- No change to how optimistic-lock/timestamp fields are matched for the composite case beyond reusing the existing matchers.
- No update-by-key path changes (update flows through the builder in improve-crud-service).

## Decisions

### D1: A typed `CompositeKey` value class carries the PK components
Generate one `CompositeKey` value class per composite-PK table holding the key components positionally (e.g. `CompositeKey(a: Long, b: String)`), and let `CRooDService`'s `ID` generic resolve to that type for composite tables.

- **Rationale:** DQ1 resolved this shape: a single generated value class gives the `ID` generic a concrete type, keeps components type-safe, and removes the `ids.size == 1` requirement from `PrimaryKey.equal`.
- **Alternatives considered:**
  - Raw vararg of `Any` per call → rejected in DQ1: untyped, no compiler safety, can't express the `ID` generic.
  - A generic `CompositeKey<T1, T2>(...)` tuple library → rejected: unreadable for >2 columns and non-idiomatic next to jOOQ's generated table types.

### D2: `PrimaryKey.equal` matches components positionally
`PrimaryKey` holds the typed key value; `equal(vararg ids: TableField<*, *>)` builds `DSL.row(*ids).eq(*components)` (or the single-column `DSL.row(id).eq(value)` form for the existing path). The arity guard becomes "component count == key-column count" with a clear message, replacing `require(ids.size == 1)`.

- **Rationale:** `DSL.row` already supports multi-value row comparison; positional matching is the spec's rule ("matched positionally against the key columns").
- **Alternatives considered:** `AND`-ing per-column `Condition`s → equivalent result, but `DSL.row(...).eq(...)` is the single jOOQ-idiomatic expression and handles the single-column case uniformly.

### D3: `CRooDService` derives PK fields once and consumes the typed key
`pkFields` already derives from `table.keys` (primary). `CRooDService` passes the `CompositeKey` instance's components into `PrimaryKey.equal` in the same positional order as `pkFields`; the ID generic is the generated composite type for composite tables.

- **Rationale:** No new metadata needed — the existing `pkFields` ordering is the contract both the value class and the condition use.
- **Alternatives considered:** a parallel "key descriptor" API → redundant; `pkFields` already carries the column order.

### D4: Generators emit the composite constructor and the value class
`generateCompositeKeyConstructor` (both generators) emits a constructor taking all key columns; for composite tables a `CompositeKey` value class is emitted alongside the domain, honoring the existing optimistic-lock/timestamp and nullable handling used by the single-column path.

- **Rationale:** Matches the spec's scenario set (Kotlin data-class constructor + value class; Java POJO secondary constructor; single-column output unchanged).
- **Alternatives considered:** reusing the existing `generateConstructor` for composites → it assumes `keyColumns[0]` only (`DomainGenerator.kt:99`) and drops non-key columns for `PK-only` tables; rejected.

## Risks / Trade-offs

- [`pkFields` ordering must match the generated `CompositeKey` component order] -> Both are driven by the same jOOQ key-column order; the generator emits components in key-column order and `CRooDService` consumes them in `pkFields` order — a single source of truth (test-covered).
- [Adding a generated type is a codegen output change] -> Snapshot goldens (add-test-coverage) capture it; the generator capability spec pins single-column output as unchanged.
- [`CompositeKey` value class generation needs strategy/type-resolution support] -> Reuses the existing `getStrategy()` / `getJavaType` machinery; only the emission loop is new.
- [Composite-key `findOptionalById`/`existsById` must not regress to the throwing read] -> These delegate through the same `PrimaryKey.equal` path; the composite-key spec scenarios assert the resulting condition only.

## Migration Plan

1. Extend `PrimaryKey` to hold a component list and build a positional row condition; relax the `ids.size == 1` guard to a component-count guard.
2. Add the typed `CompositeKey` value class generation to `DomainGenerator` and `KDomainGenerator`; implement `generateCompositeKeyConstructor` in both.
3. Wire `CRooDService` to consume the composite key in `findById`, `existsById`, `findOptionalById`, `deleteById` positionally.
4. Extend the generator snapshot goldens and `PrimaryKey` unit tests (coordinate with add-test-coverage's suites).
5. Run `./gradlew test`; confirm single-column output is byte-identical.
6. Rollback: revert `PrimaryKey`/generators/service to the single-column state; composite tables fall back to the current `TODO`-free behaviour only after re-adding the guard.

## Open Questions

- Whether the generated `CompositeKey` for a 2-column key should be a separate `.kt`/`.java` file or a nested type of the domain — resolved in favor of a sibling file during codegen, pending snapshot review.
- Whether `CRooDService` composite-key methods take the generated `CompositeKey` directly or its components as a vararg — per DQ1 the typed value is the API; the components stay internal to `PrimaryKey.equal`.