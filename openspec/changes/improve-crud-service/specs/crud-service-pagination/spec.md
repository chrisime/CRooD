# crud-service-pagination

Offset/limit pagination for `CRooDService` read operations so callers can page through large result sets without loading everything.

## ADDED Requirements

### Requirement: findAll pages results with limit and offset
`findAll(limit, offset)` SHALL return at most `limit` domain records starting at `offset`, ordered consistently, so callers can retrieve a page of results.

Feature: crud-service-pagination
Rule: Pagination is deterministic

#### Scenario: Fetch the first page
- **GIVEN** a table containing more rows than the page size
- **WHEN** `findAll` is called with a limit and offset of zero
- **THEN** the result contains at most `limit` records
- **AND** the records represent the first page

#### Scenario: Fetch a later page
- **GIVEN** a table containing more rows than the page size
- **WHEN** `findAll` is called with a limit and an offset equal to a previous page boundary
- **THEN** the result contains the records of that page
- **AND** the pages do not overlap

#### Scenario: Fetch beyond the last page
- **GIVEN** a table with fewer rows than the requested offset
- **WHEN** `findAll` is called with an offset beyond the table size
- **THEN** an empty result is returned