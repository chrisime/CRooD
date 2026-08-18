# crud-service-kotlin-streams

Kotlin-idiomatic `Sequence`-based read APIs on `CRooDService`, alongside the existing Java `Stream` API. Sequences avoid the resource-management burden of `Stream` for Kotlin callers.

## ADDED Requirements

### Requirement: findAll provides a Sequence return
`findAll()` SHALL return the matching domain records as a Kotlin `Sequence<D>` so callers can consume results lazily without managing a `Stream`.

Feature: crud-service-kotlin-streams
Rule: Sequence reads are the Kotlin-friendly counterpart to Stream reads

#### Scenario: Consume all records as a sequence
- **GIVEN** a table containing several rows
- **WHEN** `findAll()` is called
- **THEN** the result is a `Sequence` yielding all domain records

#### Scenario: Consume records matching a filter as a sequence
- **GIVEN** a `findAll(whereStep)` variant
- **WHEN** it is called with a filter
- **THEN** the result is a `Sequence` yielding only the matching domain records

### Requirement: single-record reads signal absence consistently
`findById` and `findOne` SHALL return an `Optional<D>` (or nullable) instead of throwing when no row matches, matching `findOptional` semantics.

#### Scenario: Find an existing record by id
- **GIVEN** a table containing a row with a known id
- **WHEN** `findById` is called with that id
- **THEN** an `Optional` present with the domain record is returned

#### Scenario: Find a missing record by id
- **GIVEN** a table with no row for the id
- **WHEN** `findById` is called with that id
- **THEN** an empty `Optional` is returned instead of an exception

#### Scenario: Find one matching record
- **GIVEN** a table with a single row matching a filter
- **WHEN** `findOne` is called with that filter
- **THEN** an `Optional` present with the domain record is returned

#### Scenario: Find one with no matches
- **GIVEN** a table with no row matching a filter
- **WHEN** `findOne` is called with that filter
- **THEN** an empty `Optional` is returned instead of an exception