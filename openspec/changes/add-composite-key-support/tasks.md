## 1. PrimaryKey Composite Support

- [ ] 1.1 Extend `PrimaryKey` to hold a positional component list and build a `DSL.row(*ids).eq(*components)` condition; replace the `ids.size == 1` guard with a component-count guard that names the expected key-column count.
- [ ] 1.2 Keep the single-column path behavior byte-compatible.

## 2. Codegen: CompositeKey Value Class and Constructors

- [ ] 2.1 Implement `DomainGenerator.generateCompositeKeyConstructor` (Java POJO secondary constructor accepting all key columns) and emit a typed `CompositeKey` value class for composite-PK tables.
- [ ] 2.2 Implement `KDomainGenerator.generateCompositeKeyConstructor` (Kotlin data-class constructor) and emit the Kotlin `CompositeKey` value class.
- [ ] 2.3 Reuse the existing optimistic-lock/timestamp matchers and nullable/annotation handling in the composite emission path.

## 3. CRooDService Composite-Key Reads

- [ ] 3.1 Wire `CRooDService` to accept the generated `CompositeKey` in `findById`, `existsById`, `findOptionalById`, and `deleteById`, consuming its components positionally against `pkFields`.

## 4. Tests and Snapshots

- [ ] 4.1 Add unit tests for `PrimaryKey.equal` with composite keys (positional match, single-column unchanged, component-count mismatch failure).
- [ ] 4.2 Extend the generator snapshot goldens with a composite-PK table for both generators; confirm single-column golden output is byte-identical.
- [ ] 4.3 Run `./gradlew test` and confirm the full suite passes.

## 5. Verification Gate

- [ ] 5.1 Run `openspec validate add-composite-key-support --type change --strict` and confirm exit 0.
- [ ] 5.2 Run `run_acceptance.py --lint` and `run_acceptance.py --check-ac` and confirm both pass.