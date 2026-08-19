## 1. Test Dependencies and Shared Fixture

- [ ] 1.1 Add `testcontainers` + `postgresql` driver (with the JNA resolution pin `net.java.dev.jna:5.9.0`, as used in `examples/generator`) to the Gradle version catalog and wire into the `testImplementation` configuration.
- [ ] 1.2 Add the shared service fixture (shared Testcontainers PostgreSQL container, `DSLContext`, `CRooDConfig` wiring) under `src/test/kotlin/xyz/chrisime/crood/service/`.
- [ ] 1.3 Add a `.gitattributes` rule pinning `eol=lf` for `src/test/resources/codegen-snapshots/` so golden files are checkout-independent.

## 2. Runtime Test Suite

- [ ] 2.1 Add the `PrimaryKey.equal` unit spec (single-key match, wrong-arity rejection, type-mismatch) without a database.
- [ ] 2.2 Add the `GenericExt` unit spec (`newInstance`, `getClassAtIndex`, `asType`) without a database; reconcile with the `fix-genericext-reflection` code fix landing first.
- [ ] 2.3 Add the `CRooDService` integration spec against the shared Testcontainers PostgreSQL container covering `create`, `findById`, `update`, `delete`, select-count, and `truncate`, failing loudly (not silently skipping) when Docker is unavailable.

## 3. Codegen Snapshot Suite

- [ ] 3.1 Build the representative in-test schema (single-PK, composite-PK, nullable columns, optimistic-lock columns) for the generators.
- [ ] 3.2 Add the `DomainGenerator` snapshot spec asserting Java output byte-for-byte against `src/test/resources/codegen-snapshots/java/`.
- [ ] 3.3 Add the `KDomainGenerator` snapshot spec asserting Kotlin output byte-for-byte against `src/test/resources/codegen-snapshots/kotlin/`.
- [ ] 3.4 Run `./gradlew test`, review generated goldens, and commit them deliberately in the same commit as the suite.

## 4. Verification Gate

- [ ] 4.1 Run `openspec validate add-test-coverage --type change --strict` and confirm exit 0.
- [ ] 4.2 Run `run_acceptance.py --lint` and `run_acceptance.py --check-ac` and confirm both pass.