# composite-primary-keys

Runtime condition-building and generated constructors for tables with composite primary keys. `PrimaryKey`, `CRooDService`, and both code generators handle multiple key columns; a typed `CompositeKey` value class is generated per composite-PK table.

## ADDED Requirements

### Requirement: PrimaryKey builds conditions for composite keys
`PrimaryKey.equal` SHALL build a `Condition` matching all primary-key columns positionally from their component values when the table has a composite key, in addition to the existing single-column behavior.

Feature: composite-primary-keys
Rule: Composite keys are matched positionally against the key columns

#### Scenario: Build a condition for a composite key
- **GIVEN** a table with a primary key spanning two columns `(a, b)`
- **WHEN** `equal` is called with the two component values for `a` and `b`
- **THEN** a `Condition` is returned that matches rows where `a` and `b` equal the given values

#### Scenario: Build a condition for a single-column key
- **GIVEN** a table with a single-column primary key
- **WHEN** `equal` is called with the single id value
- **THEN** a `Condition` is returned that matches the row with that id

#### Scenario: Reject a mismatch between components and key columns
- **GIVEN** a table with a composite primary key spanning two columns
- **WHEN** `equal` is called with a different number of component values
- **THEN** a failure is raised explaining the expected number of key columns

### Requirement: CRooDService reads and deletes by composite key
`CRooDService` SHALL accept a composite key (typed `CompositeKey`) in `findById`, `existsById`, `findOptionalById`, and `deleteById`, and use it to build the correct multi-column condition.

#### Scenario: Find a record by composite key
- **GIVEN** a table with a composite primary key and a row at known component values
- **WHEN** `findById` is called with the composite key
- **THEN** the matching domain record is returned

#### Scenario: Delete a record by composite key
- **GIVEN** a table with a composite primary key and a row at known component values
- **WHEN** `deleteById` is called with the composite key
- **THEN** the row is deleted and the delete count is returned

### Requirement: generators produce composite-key constructors and a CompositeKey value class
`DomainGenerator.generateCompositeKeyConstructor` and `KDomainGenerator.generateCompositeKeyConstructor` SHALL generate a constructor that sets all key columns for tables with composite primary keys, and a typed `CompositeKey` value class SHALL be generated alongside the domain for such tables.

#### Scenario: Kotlin generator emits a composite-key data class
- **GIVEN** a table with a composite primary key
- **WHEN** the Kotlin domain generator runs
- **THEN** the generated domain has a constructor accepting all key columns
- **AND** a typed `CompositeKey` value class is generated

#### Scenario: Java generator emits a composite-key constructor
- **GIVEN** a table with a composite primary key
- **WHEN** the Java domain generator runs
- **THEN** the generated POJO has a secondary constructor accepting all key columns

#### Scenario: Single-column tables are unaffected
- **GIVEN** a table with a single-column primary key
- **WHEN** either generator runs
- **THEN** the existing single-column domain output is preserved