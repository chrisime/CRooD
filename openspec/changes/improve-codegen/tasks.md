## 1. No Silent Skip Heuristics

- [ ] 1.1 Remove the `columns.size == 1 && columns[0].type.isIdentity` and `columns.size == 2 && columns[1].type.isNullable` skip branches from `DomainGenerator.generatePojo` so every table falls through to constructor generation.
- [ ] 1.2 Remove the same skip branches from `KDomainGenerator.generatePojo`.

## 2. Kotlin data class Members

- [ ] 2.1 In `KDomainGenerator.generatePojo`, skip `generatePojoEqualsAndHashCode` / `generatePojoToString` for `data class` output; confirm the emitted `data class` contains no manual `equals`/`hashCode`/`toString`.

## 3. Optimistic-Lock Regex Cache

- [ ] 3.1 Rewrite `getOptimisticLockMatcher` to compile the `optimisticFields` regexes once and evaluate all precompiled patterns against the name, preserving the `(String) -> Boolean` signature.

## 4. Verification and Snapshots

- [ ] 4.1 Add/refresh generator snapshot goldens covering single-PK, composite-PK, nullable columns, and optimistic-lock columns; assert no skip is logged for degenerate tables.
- [ ] 4.2 Run `./gradlew test` and re-run the generator examples; review the generated-source diff.
- [ ] 4.3 Run `openspec validate improve-codegen --type change --strict` and confirm exit 0.
- [ ] 4.4 Run `run_acceptance.py --lint` and `run_acceptance.py --check-ac` and confirm both pass.