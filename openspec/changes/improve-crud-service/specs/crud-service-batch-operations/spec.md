# crud-service-batch-operations

Batched insert and bulk-delete operations on `CRooDService`. Multi-row `create` must not degrade to N+1 sequential inserts, and bulk deletes must be available for ID collections.

## ADDED Requirements

### Requirement: create writes multiple records in a single batched insert
`create(Collection<D>)` SHALL write all records in one batched insert operation instead of issuing one insert per record.

Feature: crud-service-batch-operations
Rule: Batch inserts are atomic single statements, not loops

#### Scenario: Create multiple records in one batch
- **GIVEN** a `CRooDService` for a table with an auto-incrementing primary key
- **WHEN** `create` is called with a collection of several domain records
- **THEN** all records are persisted
- **AND** a single batched insert statement is executed

#### Scenario: Create an empty collection does nothing
- **GIVEN** a `CRooDService`
- **WHEN** `create` is called with an empty collection
- **THEN** no insert is executed and the return value is `0`

#### Scenario: Create a single record
- **GIVEN** a `CRooDService` for a table with an auto-incrementing primary key
- **WHEN** `create` is called with a single domain record
- **THEN** the record is persisted
- **AND** the generated primary key is returned

### Requirement: delete removes multiple records by id in one statement
`deleteByIds(Collection<ID>)` SHALL remove all rows matching the given ids in a single statement and return the number of deleted rows.

#### Scenario: Delete several records by their ids
- **GIVEN** a table containing several rows with known ids
- **WHEN** `deleteByIds` is called with the collection of ids
- **THEN** all matching rows are deleted
- **AND** the number of deleted rows is returned

#### Scenario: Delete ids that do not exist
- **GIVEN** a table with no rows matching the given ids
- **WHEN** `deleteByIds` is called with those ids
- **THEN** no rows are deleted and the return value is `0`
