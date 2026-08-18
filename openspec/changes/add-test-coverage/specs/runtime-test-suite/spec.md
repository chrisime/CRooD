# runtime-test-suite

A repeatable unit + integration test harness covering the `CRooDService` CRUD paths, `PrimaryKey`, and the `GenericExt` reflection helpers. Service tests run against Testcontainers PostgreSQL for dialect fidelity.

## ADDED Requirements

### Requirement: service CRUD paths are covered by integration tests
The test suite SHALL exercise the `CRooDService` operations (`create`, `findById`, `update`, `delete`, select-count, and `truncate`) against a Testcontainers PostgreSQL database so jOOQ dialect behavior (including last-id/returning semantics) is verified.

Feature: runtime-test-suite
Rule: Service behavior is verified against a real PostgreSQL dialect

#### Scenario: Create and find a record
- **GIVEN** a Testcontainers-backed `CRooDService`
- **WHEN** a record is created and fetched by id
- **THEN** the persisted record matches the created values
- **AND** the created primary key is returned

#### Scenario: Update one or more records
- **GIVEN** an existing record in the table
- **WHEN** an update is executed
- **THEN** the record reflects the new values

#### Scenario: Delete a record
- **GIVEN** an existing record in the table
- **WHEN** it is deleted by id
- **THEN** the row is removed
- **AND** the delete count is `1`

#### Scenario: Select the row count
- **GIVEN** a table with a known number of rows
- **WHEN** the count is selected
- **THEN** the returned count matches

#### Scenario: Truncate the table
- **GIVEN** a table with existing rows
- **WHEN** the table is truncated
- **THEN** all rows are removed

### Requirement: PrimaryKey and GenericExt helpers are covered by unit tests
The suite SHALL unit-test `PrimaryKey.equal` (single-key, wrong-arity, type-mismatch) and the `GenericExt` helpers (`newInstance`, `getClassAtIndex`, `asType`) without a database.

#### Scenario: PrimaryKey matches a single column
- **GIVEN** a single-column primary key field
- **WHEN** `equal` is called with the matching id
- **THEN** a matching `Condition` is produced

#### Scenario: PrimaryKey rejects wrong arity
- **GIVEN** a single-column primary key field
- **WHEN** `equal` is called with a mismatched number of values
- **THEN** the call fails with a clear message

#### Scenario: GenericExt resolves reified type arguments
- **GIVEN** a class hierarchy with parameterized superclasses
- **WHEN** `getClassAtIndex` and `newInstance` are invoked
- **THEN** the resolved types and instances are correct

## REMOVED Requirements

### Requirement: Tests limited to configuration-only coverage
**Reason**: The single existing config spec does not protect the service, generators, `PrimaryKey`, or reflection helpers; the runtime bug in `GenericExt` survived because of this gap.

**Migration**: The new runtime test suite and codegen snapshot suite provide coverage of the core paths.