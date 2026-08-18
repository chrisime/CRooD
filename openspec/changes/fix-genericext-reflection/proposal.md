## Source

<!-- Deliberate skip: this change does NOT originate from a Jira ticket.
     The jira-driven schema normally populates this section verbatim via the
     jira-import skill (config rule) when a change's requirements come from a
     Jira ticket. This change was identified from a codebase review; the
     jira-import / jira-grill-me skills do not apply because there is no
     ticket to load or interrogate. -->

- **Ticket:** none (codebase-review origin)
- **Summary:** Fix type-resolution reflection in `GenericExt` that breaks instantiation of `CRooDService`
- **Description:** Not applicable (no Jira ticket).
- **Acceptance Criteria:**
  1. Not applicable (no Jira ticket).

## Why

`CRooDService` calls `newInstance<R>()` and `getClassAtIndex<D>(2)` in its constructor, but `GenericExt.kt` resolves the generic superclass against `Any::class.java` (`java.lang.Object`) instead of the receiver — so `CRooDService` can never be instantiated without throwing `RuntimeException("unexpected type null")`. This is a latent runtime bug in the core of the library.

## What Changes

- Fix `getClassAtIndex` / `newInstance` in `src/main/kotlin/xyz/chrisime/crood/extensions/GenericExt.kt` to reflect on the receiver (`this::class.java` / `this::class.java.genericSuperclass`) instead of `Any::class.java`.
- Keep the resolution walk over `ParameterizedType` / `Class<*>` supertypes intact (still supports the generic-superclass chain).
- Add focused unit tests for `newInstance` and `getClassAtIndex` covering: plain generic-superclass, chained superclass, and error cases.

## Capabilities

### New Capabilities

- `genericext-type-resolution`: Generic type resolution helpers that correctly extract the reified superclass type arguments needed by `CRooDService`.

### Modified Capabilities

_(none — no existing specs exist; this restores intended behaviour of an existing helper.)_

## Impact

- Affected code: `src/main/kotlin/xyz/chrisime/crood/extensions/GenericExt.kt`, consumed by `service/CRooDService.kt`.
- API: internal helper functions only; **no public API break**.
- Enables the CRooDService to actually be used — currently impossible at runtime.

## Decision Questions

_(none — this is a defect fix with a single correct target behaviour. No scope decision gates AC interpretation.)_

## Open Questions

_(none open.)_