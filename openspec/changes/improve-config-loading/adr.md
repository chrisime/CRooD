# ADR Review Manifest

- Status: completed
- Review date: 2026-08-18

## Review Summary

ADR review completed for this change. The move to Hoplite with an injectable loader is a durable technology and public-API contract; recorded as a new repository-level ADR.

## In-Force ADRs Reviewed

- `adr/0001-use-testcontainers-postgresql-for-service-tests.md` (accepted) — test-database strategy; the config spec scenarios run in the same suite.
- `adr/0002-pin-generated-domain-output-with-golden-files.md` (accepted) — generator snapshots; generator output depends on the config loader.
- `adr/0003-use-top-level-builder-dsl-for-crudservice-updates.md` (accepted) — update builder; not affected.
- `adr/0004-single-record-reads-return-optional-instead-of-throwing.md` (accepted) — read contract; not affected.
- `adr/0005-model-composite-primary-keys-as-generated-compositekey-value-classes.md` (accepted) — composite-key codegen; not affected.
- `adr/0006-code-generators-always-emit-valid-output-never-silently-skip-tables.md` (accepted) — codegen contract; generator config flows through the new loader.
- `adr/0007-load-croodconfig-via-hopelite-through-an-injectable-entry-point.md` (accepted, this change) — injectable config contract.

## New Durable ADRs Created

- `adr/0007-load-croodconfig-via-hopelite-through-an-injectable-entry-point.md` — Hoplite loads `CRooDConfig` through an injectable entry point (design D1-D4, proposal DQ1).

Full Context, Decision, and Consequences live in that file; not duplicated here.