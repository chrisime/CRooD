---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Pin generated domain output with golden files

## Context and Problem Statement

Both domain generators (`DomainGenerator`, `KDomainGenerator`) were completely untested, so a code change could silently alter the Java/Kotlin domain output that consuming projects depend on. The suite needed a way to make generator output changes visible and reviewable.

## Decision Drivers

- Auditability of generator output changes in code review
- Stability of generated domain code for downstream consumers
- Coverage of the generator paths without a heavy generation framework

## Considered Options

- Golden/snapshot files compared byte-for-byte
- Inline expected strings in the test specs
- Running jOOQ's full generation over a schema dump

## Decision Outcome

Chosen option: "Golden/snapshot files compared byte-for-byte", because committed golden files make every generated-output change visible as an ordinary file diff in the PR.

### Consequences

- Good, because generator changes are auditable before merge.
- Good, because downstream stability is asserted.
- Bad, because golden files need deliberate updates when generators change (updated in the same commit, reviewed in the PR diff).
- Neutral, because the goldens cover a representative in-test schema, not jOOQ generation over a live schema dump.

### Confirmation

The codegen snapshot suite compares generated Java and Kotlin output byte-for-byte against `src/test/resources/codegen-snapshots/` and runs as part of the `test` task.

## Pros and Cons of the Options

### Golden/snapshot files

Committed expected outputs compared byte-for-byte.

- Good, because output changes show up as reviewable diff hunks.
- Good, because no extra framework is required.
- Bad, because stale goldens require discipline to update deliberately.

### Inline expected strings in specs

Expected output embedded in the test source.

- Good, because everything lives in one file.
- Bad, because large generated sources clutter the spec and diffs are noisier to review.

### Full generation over a schema dump

Running jOOQ generation against a real schema snapshot.

- Good, because it tests the whole pipeline.
- Bad, because it couples the suite to the example database setup and defeats the lightweight in-test fixture goal.

## More Information

See `openspec/changes/add-test-coverage/design.md` (D3, D4).