# ADR Review Manifest

- Status: completed
- Review date: 2026-08-18

## Review Summary

ADR review completed for this change. Two durable decisions — the GitHub Packages publish target and the current-toolchain policy — were recorded as new repository-level ADRs.

## In-Force ADRs Reviewed

- `adr/0001-use-testcontainers-postgresql-for-service-tests.md` (accepted) — test-database strategy; unaffected by the build modernization.
- `adr/0002-pin-generated-domain-output-with-golden-files.md` (accepted) — generator snapshots; Kotlin 2.x codegen output must stay aligned with the goldens.
- `adr/0003-use-top-level-builder-dsl-for-crudservice-updates.md` (accepted) — update builder; not affected.
- `adr/0004-single-record-reads-return-optional-instead-of-throwing.md` (accepted) — read contract; not affected.
- `adr/0005-model-composite-primary-keys-as-generated-compositekey-value-classes.md` (accepted) — composite-key codegen; Kotlin 2.x compile of generated output is a dependency.
- `adr/0006-code-generators-always-emit-valid-output-never-silently-skip-tables.md` (accepted) — codegen contract; generated output compiles under the new toolchain.
- `adr/0007-load-croodconfig-via-hopelite-through-an-injectable-entry-point.md` (accepted) — injectable config; Hoplite's Kotlin requirement may gate the toolchain jump order.
- `adr/0008-publish-releases-to-github-packages-on-version-tags.md` (accepted, this change) — release contract.
- `adr/0009-build-on-the-current-kotlin-2x-toolchain.md` (accepted, this change) — toolchain contract.

## New Durable ADRs Created

- `adr/0008-publish-releases-to-github-packages-on-version-tags.md` — releases publish to GitHub Packages on version tags (design D1/D4, proposal DQ1).
- `adr/0009-build-on-the-current-kotlin-2x-toolchain.md` — the build uses current Kotlin 2.x and dependencies (design D2/D3, proposal DQ2).

Full Context, Decision, and Consequences live in those files; not duplicated here.