---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Code generators always emit valid output, never silently skip tables

## Context and Problem Statement

Both domain generators silently skipped legal tables via ad-hoc heuristics: a table with only an identity PK, or with a single nullable second column, produced no domain and only a `log.info`. For a code generator consumed by downstream builds, a silent no-op is a hidden loss — consumers believe a domain exists when it does not, and the failure mode is a confusing downstream compile error rather than a clear generator decision.

## Decision Drivers

- Generators must produce predictable, documented output for every table
- No silent losses for degenerate-but-legal tables
- Deterministic, reviewable generator behavior

## Considered Options

- Always generate valid output; remove the skip heuristics entirely
- Keep the skip but raise the log level to WARN
- Keep the current silent `log.info` skip

## Decision Outcome

Chosen option: "Always generate valid output; remove the skip heuristics entirely", because a generator must not quietly produce nothing for a legal table, and the existing constructor paths already handle PK-only and nullable columns (including nullable annotations).

### Consequences

- Good, because every table yields a domain; no hidden no-ops.
- Good, because the generator's decision surface is smaller and testable.
- Bad, because existing generated output for previously-skipped tables changes and must be re-reviewed.
- Neutral, because the Java and Kotlin generator paths must change in lockstep.

### Confirmation

The `codegen-determinism` spec asserts a domain is generated (with no skip logged) for PK-only and nullable-second-column tables, and snapshot goldens pin the output.

## Pros and Cons of the Options

### Always generate valid output

- Good, because no silent losses; constructor paths already handle the edge cases.
- Bad, because previously-skipped tables now emit code (intended, but changes baselines).

### Keep skip, log at WARN

- Good, because unchanged output.
- Bad, because a log is still a silent loss for downstream builds.

### Keep silent `log.info` skip

- Good, because zero change.
- Bad, because the root problem (hidden no-op) persists.

## More Information

Resolved as Decision Question 1 in the `improve-codegen` proposal (design D1).