# ADR Review Manifest

- Status: completed
- Review date: 2026-08-18

## Review Summary

ADR review completed for this change. Composite primary keys are modeled as generated, typed `CompositeKey` value classes; the durable API-contract decision was recorded as a new repository-level ADR.

## In-Force ADRs Reviewed

- `adr/0001-use-testcontainers-postgresql-for-service-tests.md` (accepted) — test-database strategy; composite-key service scenarios run there.
- `adr/0002-pin-generated-domain-output-with-golden-files.md` (accepted) — generator snapshots; composite-key codegen output is captured by the goldens.
- `adr/0003-use-top-level-builder-dsl-for-crudservice-updates.md` (accepted) — update builder contract; not affected by composite keys.
- `adr/0004-single-record-reads-return-optional-instead-of-throwing.md` (accepted) — read contract; `findById`/`findOptionalById` composite-key variants follow it.
- `adr/0005-model-composite-primary-keys-as-generated-compositekey-value-classes.md` (accepted, this change) — composite-key type contract.

## New Durable ADRs Created

- `adr/0005-model-composite-primary-keys-as-generated-compositekey-value-classes.md` — composite PKs use a generated typed `CompositeKey` value class as the `ID` contract (design D1-D4, proposal DQ1).

Full Context, Decision, and Consequences live in that file; not duplicated here.