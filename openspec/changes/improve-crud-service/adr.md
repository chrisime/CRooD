# ADR Review Manifest

- Status: completed
- Review date: 2026-08-18

## Review Summary

ADR review completed for this change. This change reworks the `CRooDService` public API in two durable, breaking ways (the update builder contract and the non-throwing read contract); both were recorded as new repository-level ADRs.

## In-Force ADRs Reviewed

- `adr/0001-use-testcontainers-postgresql-for-service-tests.md` (accepted) — test-database strategy; the service suite exercises the new API.
- `adr/0002-pin-generated-domain-output-with-golden-files.md` (accepted) — generator snapshots; not affected.
- `adr/0003-use-top-level-builder-dsl-for-crudservice-updates.md` (accepted, this change) — new update contract.
- `adr/0004-single-record-reads-return-optional-instead-of-throwing.md` (accepted, this change) — new read contract.

## New Durable ADRs Created

- `adr/0003-use-top-level-builder-dsl-for-crudservice-updates.md` — the top-level `update { set(...); where(...) }` builder replaces the five arity overloads (design D1, proposal DQ1).
- `adr/0004-single-record-reads-return-optional-instead-of-throwing.md` — `findById`/`findOne`/`findOptional*` all return `Optional` instead of throwing (design D3).

Full Context, Decision, and Consequences live in those files; not duplicated here.