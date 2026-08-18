# codegen-determinism

Predictable, documented generator decisions with no silent skips. The domain generators always produce valid output for every table, never silently skip degenerate tables, avoid duplicating language-generated members, and cache compiled optimistic-lock regexes.

## ADDED Requirements

### Requirement: generators produce output for every table
`DomainGenerator` and `KDomainGenerator` SHALL generate valid domain output for every table, including tables with only a primary key column or a single nullable second column; the previous silent skip heuristics SHALL be removed.

Feature: codegen-determinism
Rule: No silent skip heuristics

#### Scenario: Generate a domain for a table with only a primary key column
- **GIVEN** a table whose only column is an identity primary key
- **WHEN** the domain generator runs
- **THEN** a valid domain class is generated
- **AND** no skip is logged for the table

#### Scenario: Generate a domain for a table with a nullable second column
- **GIVEN** a table with a primary key column and one nullable non-key column
- **WHEN** the domain generator runs
- **THEN** a valid domain class is generated
- **AND** no skip is logged for the table

### Requirement: Kotlin generator does not duplicate data-class members
`KDomainGenerator` SHALL NOT emit `equals`/`hashCode`/`toString` for generated Kotlin `data class` output, because the language generates them; the generated output stays compilable.

#### Scenario: Kotlin data class output has no duplicate members
- **GIVEN** a table
- **WHEN** the Kotlin domain generator runs with POJO equals/hashCode and toString enabled
- **THEN** the generated `data class` contains no manually emitted `equals`, `hashCode`, or `toString` members
- **AND** the generated source compiles

### Requirement: optimistic-lock matcher caches compiled regexes
`getOptimisticLockMatcher` SHALL compile the optimistic-lock field regexes once and reuse them across calls rather than recompiling on every invocation.

#### Scenario: Repeated matcher calls reuse the compiled regex
- **GIVEN** a configured optimistic-lock field regex
- **WHEN** the matcher is invoked for many field names
- **THEN** the regex is compiled once and reused for all evaluations