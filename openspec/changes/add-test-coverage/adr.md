# ADR Review Manifest

- Status: completed
- Review date: 2026-08-18

## Review Summary

ADR review completed for this change. Two design decisions in `design.md` establish durable, long-lived architectural commitments that will shape future changes (service work, composite keys, generator work), so two repository-level ADRs were created.

## In-Force ADRs Reviewed

- None - `<repo>/adr/` had no prior ADRs; supersession graph starts empty with this change.

## New Durable ADRs Created

- `adr/0001-use-testcontainers-postgresql-for-service-tests.md` — service integration tests run against Testcontainers PostgreSQL (design D1/D2/D5, proposal DQ1).
- `adr/0002-pin-generated-domain-output-with-golden-files.md` — generator output is locked by committed golden files (design D3/D4).

Full Context, Decision, and Consequences live in those files; not duplicated here.