# ADR Review Manifest

- Status: completed
- Review date: 2026-08-18

## Review Summary

ADR review completed for this change. The "no silent skips" principle is a durable generator contract; the other two fixes (duplicate `data class` members, regex caching) are tactical implementation corrections. One repository-level ADR was created.

## In-Force ADRs Reviewed

- `adr/0001-use-testcontainers-postgresql-for-service-tests.md` (accepted) — test-database strategy; generator snapshot goldens run there.
- `adr/0002-pin-generated-domain-output-with-golden-files.md` (accepted) — generator snapshots; this change's output is locked by those goldens.
- `adr/0003-use-top-level-builder-dsl-for-crudservice-updates.md` (accepted) — update builder; not affected.
- `adr/0004-single-record-reads-return-optional-instead-of-throwing.md` (accepted) — read contract; not affected.
- `adr/0005-model-composite-primary-keys-as-generated-compositekey-value-classes.md` (accepted) — composite-key codegen; `generateCompositeKeyConstructor` paths are invoked by the now-unconditional generation flow.
- `adr/0006-code-generators-always-emit-valid-output-never-silently-skip-tables.md` (accepted, this change) — codegen contract.

## New Durable ADRs Created

- `adr/0006-code-generators-always-emit-valid-output-never-silently-skip-tables.md` — generators always emit valid output; skip heuristics removed (design D1, proposal DQ1).

Full Context, Decision, and Consequences live in that file; not duplicated here.