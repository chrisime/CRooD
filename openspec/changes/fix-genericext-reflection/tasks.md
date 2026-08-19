## 1. Receiver-Aware Type Resolution

- [x] 1.1 Rewrite `newInstance` and `getClassAtIndex` in `src/main/kotlin/xyz/chrisime/crood/extensions/GenericExt.kt` as `Any` extension functions, replacing `Any::class.java` with `this::class.java` and keeping the `ParameterizedType` / `Class<*>` chain walk and `RuntimeException` error semantics.
- [x] 1.2 Update the `newInstance` catch-branch error message to report `this::class.java.typeName`.
- [x] 1.3 Update `CRooDService` call sites (`CRooDService.kt:47,57`) to invoke the helpers as receiver extensions.

## 2. Regression Tests

- [x] 2.1 Add unit tests for `getClassAtIndex`: directly parameterized superclass (indices 0 and 1), chained superclass, no-generic-superclass error, and index-out-of-bounds error.
- [x] 2.2 Add unit tests for `newInstance`: instantiation from a directly parameterized superclass with a no-arg constructor, and the no-no-arg-constructor error case.
- [x] 2.3 Run `./gradlew test` and confirm the full suite passes.

## 3. Verification Gate

- [x] 3.1 Run `openspec validate fix-genericext-reflection --type change --strict` and confirm exit 0.
- [x] 3.2 Run `run_acceptance.py --lint` and `run_acceptance.py --check-ac` and confirm both pass.