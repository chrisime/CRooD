# codegen-snapshot-suite

Golden-file tests that lock the exact generated Java and Kotlin domain output for representative schemas, so changes to the generators are reviewed against a stable baseline.

## ADDED Requirements

### Requirement: generated output is locked by golden files
The test suite SHALL include snapshot/golden-file tests that assert the exact generated Java and Kotlin domain output for representative schemas.

Feature: codegen-snapshot-suite
Rule: Generator output is pinned by golden files

#### Scenario: Kotlin domain output matches the golden file
- **GIVEN** a schema with representative tables (single-PK, composite-PK, nullable columns, optimistic-lock columns)
- **WHEN** the Kotlin domain generator runs in the test
- **THEN** the generated output exactly matches the committed golden file

#### Scenario: Java domain output matches the golden file
- **GIVEN** the same representative schema
- **WHEN** the Java domain generator runs in the test
- **THEN** the generated output exactly matches the committed golden file