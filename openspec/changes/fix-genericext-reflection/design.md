# fix-genericext-reflection — Design

## Context

`CRooDService<R, ID, D>` calls `newInstance<R>()` and `getClassAtIndex<D>(2)` in its constructor (`CRooDService.kt:47,57`). Both helpers in `GenericExt.kt` currently resolve the generic superclass against `Any::class.java` (`java.lang.Object`), whose `genericSuperclass` is `null` — so every call throws `RuntimeException("unexpected type null")` and **a `CRooDService` subclass can never be instantiated today**. This is a latent runtime bug in the library's core.

The two helpers are top-level generic functions (`fun <T> newInstance(...)`, `fun <T> getClassAtIndex(...)`), ignoring the receiver entirely, and `asType` is the only true extension. The intent (stack-overflow-derived pattern) is to resolve type arguments from the actual subclass's generic superclass.

## Goals / Non-Goals

**Goals:**
- Restore the intended receiver-based reflection: resolve `R` (index 0) and `D` (index 2) from the concrete `CRooDService` subclass's parameterized generic superclass `CRooDService<R, ID, D>`.
- Preserve the existing `ParameterizedType` / `Class<*>` supertype-chain walk and its error semantics (index out of bounds, non-parameterized root, no no-arg constructor).
- Add focused unit tests for the fixed helpers so the runtime path is protected.
- No public API break.

**Non-Goals:**
- No change to `asType` semantics (already correct; only reused).
- No reworking of how `CRooDService` names its type parameters or resolves `pkFields`.
- No performance work on reflection caching (out of scope for a correctness fix).

## Decisions

### D1: Make the helpers receiver-aware extension functions
Convert `newInstance` and `getClassAtIndex` from top-level functions into extension functions on `Any` (`fun <T> Any.newInstance(...)`, `fun <T> Any.getClassAtIndex(...)`), so `this::class.java` is the actual service subclass. Called as `this.newInstance<R>()` / `this.getClassAtIndex<D>(2)` from `CRooDService`, the receiver's generic superclass is `CRooDService<R, ID, D>`.

- **Rationale:** The whole point of the pattern is reading the subclass's generic superclass; there is no other way to learn `R`/`D` from a Kotlin subclass beside the receiver or a reified type argument. Receiver-based keeps the `CRooDService` constructor unchanged in shape and restores the documented behavior.
- **Alternatives considered:**
  - Reified type parameters (`inline fun <reified R> ...`) → would not work: `CRooDService`'s `R`/`D` are non-reified class type parameters, so the helper cannot receive them as reified.
  - Passing `Class` objects from the subclass → requires every `CRooDService` subclass to redundantly specify its key type; rejected as more invasive.

### D2: Root the supertype walk at `this`, keep chain semantics
The `when` over `this::class.java.genericSuperclass` stays: `ParameterizedType` → use its `actualTypeArguments[index]`; `Class<*>` → ascend one `genericSuperclass` level; otherwise throw `RuntimeException("unexpected type ...")`.

- **Rationale:** The chain walk already supports the generic-superclass chain scenarios in the spec; only the *root* object was wrong.
- **Alternatives considered:** rewriting the walk to resolve by `KType`/Kotlin runtime tokens → unnecessary for class hierarchy, which `Class<*>`/`ParameterizedType` already models.

### D3: `newInstance` reports the resolved type, not `Any`
The catch-branch error message uses `this::class.java.typeName` (the concrete resolved class) instead of the `Any::class.java.typeName` placeholder.

- **Rationale:** A failure to find a no-arg constructor should name the class that lacked it; `Any` is always misleading.

## Risks / Trade-offs

- [Extension-function conversion is a binary-visible signature change] -> Internal helpers consumed only by `CRooDService` (verified via grep: no other callers); the codegen output does not emit these helpers.
- [Receiver on a non-`CRooDService` subclass with an unparameterized superclass] -> Spec requires the `RuntimeException("unexpected type")` path; behavior preserved from current `when`.
- [`CRooDService` integration suites (add-test-coverage) depend on this fix] -> This change must land before `add-test-coverage` implementation (landing-order recorded in that change's design).

## Migration Plan

1. Rewrite `newInstance` and `getClassAtIndex` as `Any` extension functions in `GenericExt.kt`, replacing `Any::class.java` with `this::class.java`; update the error message in `newInstance`.
2. Update `CRooDService` call sites to invoke as receiver extensions (`newInstance<R>()` → `this.newInstance<R>()`, `getClassAtIndex<D>(2)` → `this.getClassAtIndex<D>(2)`).
3. Add unit tests for plain generic superclass, chained superclass, and error cases (per spec).
4. Run `./gradlew test`.
5. Rollback: revert the two functions' signature and call sites to top-level form.

## Open Questions

- None — defect fix with a single correct target behavior (per proposal, no DQs).