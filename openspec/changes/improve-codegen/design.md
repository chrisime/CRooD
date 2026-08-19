# improve-codegen — Design

## Context

The generators live in `src/main/kotlin/xyz/chrisime/crood/codegen/`. Three issues (per proposal):

1. **Silent skip heuristics** in both `DomainGenerator.generatePojo` (`DomainGenerator.kt:78-81`) and `KDomainGenerator.generatePojo` (`KDomainGenerator.kt:32-35`): a table with only an identity PK, or with a nullable second column, is skipped with a `log.info` — no domain is generated and the skip is easy to miss.
2. **Duplicated `data class` members** in `KDomainGenerator`: the generated Kotlin output is a `data class` (`KDomainGenerator.kt:124`), yet `generatePojo` still calls `generatePojoEqualsAndHashCode` / `generatePojoToString` (`KDomainGenerator.kt:52-58`) when those options are on — emitting members Kotlin already generates for `data class`, producing duplicates.
3. **Recompiled optimistic-lock regexes**: `getOptimisticLockMatcher` (`CRooDGenerator.kt:93-99`) calls `it.toRegex()` inside `.any { }` per field per call.

Per DQ1 the skip heuristics are removed entirely: the generators always produce valid output.

## Goals / Non-Goals

**Goals:**
- Every table produces valid domain output; degenerate tables (PK-only, nullable second column) are no longer silently skipped.
- Kotlin `data class` output does not duplicate `equals`/`hashCode`/`toString`.
- Optimistic-lock regexes are compiled once and reused.
- Golden-file tests lock the generated shape (single-PK, composite-PK, nullable columns, optimistic-lock columns).

**Non-Goals:**
- No rework of `generateConstructor`/`generateCompositeKeyConstructor` semantics (owned by add-composite-key-support).
- No removal of the Java/`future` generation path unless consumers confirm (proposal keeps it additive).
- No change to `asType`/`newInstance`/`getClassAtIndex` (owned by fix-genericext-reflection).

## Decisions

### D1: Remove the skip heuristics; always generate
Delete the `columns.size == 1 && columns[0].type.isIdentity` and `columns.size == 2 && columns[1].type.isNullable` branches from both generators, so every table falls through to `generateConstructor`/`generateCompositeKeyConstructor`.

- **Rationale:** DQ1 resolved "always generate valid output" — a library generator must not quietly produce nothing for a legal table. The constructor paths already handle PK-only and nullable columns (the Java constructor emits `@Nullable` annotations via `generateNullableAnnotation`, `DomainGenerator.kt:119`).
- **Alternatives considered:** keep skip but log at WARN → rejected in DQ1: a log is still a silent loss for downstream builds.

### D2: Stop emitting duplicated members for Kotlin `data class`
In `KDomainGenerator.generatePojo`, call `generatePojoEqualsAndHashCode`/`generatePojoToString` only when the generated output is NOT a Kotlin `data class`. The generator always emits `data class` today, so these calls become no-ops in the Kotlin path (they remain for the Java path in `DomainGenerator`).

- **Rationale:** Kotlin synthesizes `equals`/`hashCode`/`toString` for `data class`; emitting them again yields duplicate members and can break compilation. The spec requires the `data class` output to contain no manually emitted members while staying compilable.
- **Alternatives considered:**
  - Gate on a config flag (e.g. "kotlin data class") → rejected: the generator's output is always a `data class`, so the flag would be dead configuration.
  - Emit a plain class with members → rejected: changes the generated API shape for Kotlin consumers for no benefit.

### D3: Cache compiled optimistic-lock regexes
Change `getOptimisticLockMatcher` from a per-call `Array<String>` scan that recompiles `toRegex()` for every name to a matcher built once: compile each `optimisticFields` regex once, then evaluate all precompiled patterns against the name.

- **Rationale:** Per spec, repeated calls must not recompile; the matcher is invoked for every column in both generators (e.g. `DomainGenerator.kt:96-97,153-154`), so the win is real. The closure keeps the same `(String) -> Boolean` shape, so call sites don't change.
- **Alternatives considered:** cache a single regex alternation (`"a|b|c"`) → semantically close but order/short-circuit semantics differ subtly; compiling the provided list once is the faithful fix.

## Risks / Trade-offs

- [Removing skips changes existing generated output for degenerate tables] -> Intended per DQ1; snapshot goldens (add-test-coverage) and the examples' generated output are re-reviewed in the PR.
- [Kotlin `data class` member removal changes generated source for users with equals/hashCode options enabled] -> Only removes members Kotlin already synthesizes; output stays compilable and behavior-identical.
- [Regex caching across calls holds matcher state per schema] -> The closure is immutable after construction; each table's matcher is built from its own `recordVersionFields`/`recordTimestampFields`.

## Migration Plan

1. Remove the skip branches in `DomainGenerator.generatePojo` and `KDomainGenerator.generatePojo`.
2. Guard `generatePojoEqualsAndHashCode`/`generatePojoToString` in `KDomainGenerator` so they are skipped for `data class` output.
3. Rewrite `getOptimisticLockMatcher` to precompile the regex list once per call (preserving the `(String) -> Boolean` signature).
4. Add/refresh generator snapshot goldens for representative schemas; run `./gradlew test`.
5. Re-run the generator examples (`examples/generator`) and review the diff of generated sources.
6. Rollback: revert the three source files; goldens regenerate.

## Open Questions

- Whether the "duplicate members" guard should also suppress the `[[before=: ]]` / interfaces emission path — implementation detail resolved during codegen, verified by the snapshot test (spec requires only the members and compilability).