# ADR Review Manifest

- Status: completed
- Review date: 2026-08-18

## Review Summary

ADR review completed for this change. This is a defect fix: it restores the intended receiver-based generic type resolution behavior of `GenericExt` (the stack-overflow-derived pattern it was built on), replacing `Any::class.java` with `this::class.java`. No change supersedes an existing decision and no new long-term architectural commitment is introduced — the receiver-based reflection approach is the design the helper already declared, made functional again.

## In-Force ADRs Reviewed

- `adr/0001-use-testcontainers-postgresql-for-service-tests.md` (accepted) — test-database strategy; not affected by this runtime fix.
- `adr/0002-pin-generated-domain-output-with-golden-files.md` (accepted) — generator snapshot strategy; not affected by this runtime fix.

## New Durable ADRs Created

- None - no major durable architectural decisions were introduced.