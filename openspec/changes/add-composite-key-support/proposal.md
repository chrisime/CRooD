## Source

<!-- Deliberate skip: this change does NOT originate from a Jira ticket.
     The jira-driven schema normally populates this section verbatim via the
     jira-import skill (config rule) when a change's requirements come from a
     Jira ticket. This change was identified from a codebase review; the
     jira-import / jira-grill-me skills do not apply because there is no
     ticket to load or interrogate. -->

- **Ticket:** none (codebase-review origin)
- **Summary:** Add composite primary-key support to `PrimaryKey` and the code generators
- **Description:** Not applicable (no Jira ticket).
- **Acceptance Criteria:**
  1. Not applicable (no Jira ticket).

## Why

Multiple tables legitimately use composite primary keys, yet CRooD cannot handle them: `PrimaryKey.equal` hard-requires `ids.size == 1` and both `DomainGenerator.generateCompositeKeyConstructor` and `KDomainGenerator.generateCompositeKeyConstructor` are `TODO("composite key handling not yet implemented")`. For a generic CRUD library this is the largest feature gap.

## What Changes

- Extend `PrimaryKey` to support multiple `TableField`s: use a typed `CompositeKey` value class (one instance per generated composite-PK table, keeping the `ID` generic tied to a single component or the composite type) instead of a raw vararg of `Any`.
- Update `CRooDService` to derive the PK field list (single or composite) and pass the ID components to `PrimaryKey.equal`.
- Implement `generateCompositeKeyConstructor` in both `DomainGenerator` (Java POJO secondary constructor) and `KDomainGenerator` (Kotlin data-class constructor), honoring the existing optimistic-lock and nullable handling. Generate a `CompositeKey` value class alongside for composite-PK tables.
- Keep single-column PKs fully backward compatible.

## Capabilities

### New Capabilities

- `composite-primary-keys`: Runtime condition-building and generated constructors for tables with composite primary keys.

### Modified Capabilities

_(none — no existing specs exist.)_

## Impact

- Affected code: `src/main/kotlin/xyz/chrisime/crood/id/PrimaryKey.kt`, `service/CRooDService.kt`, `codegen/DomainGenerator.kt`, `codegen/KDomainGenerator.kt`.
- API: additive — existing single-PK call sites unchanged.
- Tests: unit tests for `PrimaryKey.equal` with composite keys + generator snapshot tests.

## Decision Questions

- [x] [DQ1] · Should composite PK values be a single value class (`composite PK id = list of components`) or passed as a vararg to each call?
      Decision: Typed `CompositeKey` value class generated per composite-PK table; `CRooDService` ID generic resolves to that type.
      Status: resolved
      Reason: fixes the `PrimaryKey` API and the `CRooDService` generic signature for composite keys.

## Open Questions

_(none open.)_