# add-test-coverage — Design

## Context

CRooD is a Kotlin CRUD-on-jOOQ library. Today only `CRooDConfigSpec` (2 scenarios) exists in `src/test/`; the core runtime (`CRooDService`, `PrimaryKey`, `GenericExt`), both generators (`DomainGenerator`, `KDomainGenerator`), and the codegen orchestration are completely untested — which is how the `Any::class.java` runtime bug and the N+1 insert issues survived.

The test suite runs on kotest `BehaviorSpec` with JUnit Platform (`useJUnitPlatform()`, kotlin 1.7.20, JVM 11). There is no existing `adr/`, so no in-force ADRs constrain this design.

This change coordinates with `fix-genericext-reflection`: the runtime-test-suite spec (committed) makes this change's suite the owner of the `GenericExt` unit tests (`newInstance`, `getClassAtIndex`, `asType`) and `PrimaryKey.equal`; `fix-genericext-reflection` provides the code fix those tests validate. The `GenericExt` helpers currently throw on any invocation (see the landing-order note in Risks), so the integration suite depends on that fix landing first.

## Goals / Non-Goals

**Goals:**
- A repeatable unit + integration harness covering `CRooDService` CRUD (`create`, `findById`, `update`, `delete`, count, `truncate`) against a real PostgreSQL dialect via Testcontainers.
- Unit tests for `PrimaryKey.equal` (single-key, wrong-arity, type-mismatch) and `GenericExt` helpers without a database.
- Golden-file snapshot tests pinning the exact generated Java and Kotlin domain output for representative schemas.
- Wired into the Gradle `test` task so a failure gates the CI build.
- Coverage of the core paths as a prerequisite for the other improvement changes.

**Non-Goals:**
- No in-memory database (H2/SQLite) for dialect-sensitive service tests — resolved to Testcontainers PostgreSQL in DQ1.
- No full generator-framework snapshotting (e.g. running jOOQ's generation over a real schema dump); the import surface here is one representative in-test schema.
- No CI coverage-threshold enforcement (JaCoCo/kover) in this change.
- No behavior-runner (behave) execution of the Gherkin specs at implementation time; the harness's step bindings for these Kotlin scenarios are out of scope — the Grade `test` task is the executable spec.

## Decisions

### D1: Integration tests run against Testcontainers PostgreSQL
`org.testcontainers:postgresql` + the PostgreSQL JDBC driver provide a throwaway Postgres matching the examples' dialect. The service suite shares one container instance across scenarios to keep startup cost acceptable.

- **Rationale:** jOOQ's `lastID`/`returning` semantics are dialect-specific; H2/SQLite would silently deviate. Postgres is the documented example dialect (see `examples/`), and GitHub Actions ships Docker out of the box.
- **Alternatives considered:**
  - H2/SQLite in-memory → fast, no Docker, but wrong `lastID`/`returning` semantics; rejected as DQ1.
  - A mounted permanent Postgres → not reproducible in CI; rejected.

### D2: Service tests expose one `CRooDService` against a shared container
A single shared container and a small fixture that builds a `DSLContext` + `CRooDConfig` wiring is used across `create/findById/update/delete/count/truncate` scenarios.

- **Rationale:** A shared container keeps the suite fast and lets follow-up changes (improve-crud-service) reuse the same fixture instead of re-inventing setup.
- **Alternatives considered:** one container per scenario → isolated but slow; rejected.

### D3: Snapshot tests compare against committed golden files
Generated Java/Kotlin output is written to `src/test/resources/codegen-snapshots/{java,kotlin}/<table>.{java,kt}` and asserted byte-for-byte.

- **Rationale:** Byte-for-byte comparison against reviewed golden files makes generator changes auditable in PRs (this is the auditability goal of the capability).
- **Alternatives considered:** inline expected strings in the spec → hard to review; kotest data-driven tables → still inline. Rejected.

### D4: The generator fixture is one representative in-test schema
The snapshot suite builds tables covering single-PK, composite-PK, nullable columns, and optimistic-lock columns (the `getOptimisticLockMatcher` path) directly in the test.

- **Rationale:** Matches the codegen-snapshot-suite scenarios without dragging in a schema dump or Dockerized database generation.
- **Alternatives considered:** generating from `examples/generator`'s schema → couples the suite to the example DB setup; rejected.

```mermaid
flowchart LR
  svc[CRooDService integration suite<br/>(kotest BehaviorSpec)]
  pg[(PostgreSQL container<br/>Testcontainers)]
  pk[PrimaryKey unit suite<br/>(no database)]
  gext[GenericExt unit suite<br/>(no database)]
  jg[DomainGenerator snapshot suite]
  kg[KDomainGenerator snapshot suite]
  gold[{java golden files}]
  goldk[{kt golden files}]
  gradle[Gradle test task]

  svc -->|jOOQ DSLContext| pg
  pk -->|PrimaryKey.equal| svc
  gext -->|uses GenericExt and PrimaryKey| svc
  jg -->|asserts exact output| gold
  kg -->|asserts exact output| goldk
  svc --> gradle
  pk --> gradle
  gext --> gradle
  jg --> gradle
  kg --> gradle
```

*Lightweight C4-inspired diagram: the two suites (runtime + codegen) and their test targets. The codegen snapshot suites assert byte-for-byte against committed golden files; the unit suites cover `PrimaryKey`/`GenericExt` directly (`fix-genericext-reflection` supplies the code fix under test).*

### D5: Container availability is tolerated, not assumed
If Docker is unavailable at test run time, the integration suite fails with a clear "Docker required" message rather than silently skipping. Unit and snapshot suites always run.

- **Rationale:** Silent skips (the very failure mode this change removes elsewhere) would let the service suite pass when it did not run.
- **Alternatives considered:** auto-skip integration tests → hides regressions; rejected.

## Risks / Trade-offs

- [Docker not installed on a dev machine] -> Integration suite fails with an explicit Docker-required message; unit + snapshot suites still cover pure logic.
- [`GenericExt` helpers throw until the fix lands -> `CRooDService` cannot be constructed and the integration suite cannot run] -> This change's implementation depends on the `fix-genericext-reflection` code fix being merged first (see Migration Plan, step 0); the unit suites double as the first regression check for that fix.
- [Golden files drift as generators change] -> Goldens are updated deliberately in the same commit as a generator change; PR diffs surface every output change.
- [Duplicate `GenericExt`/fixture code between this change and fix-genericext-reflection] -> The runtime-test-suite spec pins one owner: this change owns the `GenericExt` and `PrimaryKey` unit tests; `fix-genericext-reflection` owns only the code fix.
- [Toolchain is old (kotlin 1.7.20, JVM 11, apiVersion 1.5) and Testcontainers libs expect newer Kotlin metadata] -> Keep the versions within the current catalog; the modernize-build-release change raises the toolchain and this suite revalidates the dependency matrix afterward.

## Migration Plan

0. **Landing order prerequisite:** merge the `fix-genericext-reflection` code fix first — `CRooDService` (and the `GenericExt`/`PrimaryKey` unit suites) cannot run until the `Any::class.java` → `this::class.java` reflection fix is in place.
1. Add `testcontainers` + `postgresql` driver to the dependency catalog and the `testImplementation` configuration; include the JNA resolution pin (`net.java.dev.jna:5.9.0`) known to be required to run Testcontainers on macOS (as already used in `examples/generator`).
2. Add the shared service fixture (container, `DSLContext`, `CRooDConfig`) under `src/test/kotlin/xyz/chrisime/crood/service/`.
3. Add the `PrimaryKey`/`GenericExt` unit specs and the `CRooDService` integration spec.
4. Build the representative generator schema and the Java/Kotlin golden files under `src/test/resources/codegen-snapshots/`; add a `.gitattributes` rule pinning `eol=lf` for that directory so byte-for-byte snapshots are checkout-independent.
5. Run `./gradlew test`; commit goldens only after confirming the output is deliberate.
6. Rollback: revert the added specs/fixtures; the catalog deps are inert if unused.

## Open Questions

- Whether `modernize-build-release` should land before this change, so the Testcontainers dependency matrix is verified on the modern toolchain — raised in Risks; may need a supersession note later but is a sequencing question, not an ADR-worthy design change.
- `fix-genericext-reflection` must land and be merged before this change's implementation; MVP sequencing of branches is covered in Migration Plan, step 0.