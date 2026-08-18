## Source

<!-- Deliberate skip: this change does NOT originate from a Jira ticket.
     The jira-driven schema normally populates this section verbatim via the
     jira-import skill (config rule) when a change's requirements come from a
     Jira ticket. This change was identified from a codebase review; the
     jira-import / jira-grill-me skills do not apply because there is no
     ticket to load or interrogate. -->

- **Ticket:** none (codebase-review origin)
- **Summary:** Improve the jOOQ code generation output for CRooD domains
- **Description:** Not applicable (no Jira ticket).
- **Acceptance Criteria:**
  1. Not applicable (no Jira ticket).

## Why

The generators produce surprising or suboptimal output: tables with only a PK or a single nullable second column are silently skipped by arbitrary heuristics; the Kotlin generator combines a `data class` constructor with calls to `generatePojoEqualsAndHashCode`/`generatePojoToString` (duplicating what `data class` already provides); and `getOptimisticLockMatcher` recompiles a regex on every call.

## What Changes

- Remove the ad-hoc skip heuristics (`only one attribute which is a primary key` / `second attribute is nullable`) entirely: always generate valid domain output for every table.
- In `KDomainGenerator`, stop emitting `equals`/`hashCode`/`toString` for `data class` output (the language generates them) OR delegate to the Kotlin generator's native behavior; add a golden-file/snapshot test to lock in the generated shape.
- Cache compiled optimistic-lock regexes (`getOptimisticLockMatcher`) instead of compiling per use.
- **BREAKING:** Remove the now-unused `future`/Java generation path only if consumers confirm — otherwise keep additive.

## Capabilities

### New Capabilities

- `codegen-determinism`: Predictable, documented generator decisions with no silent skips.

### Modified Capabilities

_(none — no existing specs exist.)_

## Impact

- Affected code: `src/main/kotlin/xyz/chrisime/crood/codegen/KDomainGenerator.kt`, `codegen/DomainGenerator.kt`, `codegen/CRooDGenerator.kt`, and the generator examples.
- API: generator config/output only; `CRooDService` unaffected.
- Verification: snapshot/golden tests of generated output for representative schemas (single-PK, composite-PK, nullable columns, optimistic-lock columns).

## Decision Questions

- [x] [DQ1] · Do we keep silent-skip behavior for degenerate tables, or make skips explicit/logged-only while always generating valid output?
      Decision: Always generate valid output; remove the skip heuristics entirely (no silent losses).
      Status: resolved
      Reason: determines that existing generated code for degenerate tables may change output.

## Open Questions

_(none open.)_