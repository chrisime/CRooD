# <!-- capability name -->

<!-- Optional prose: context, rationale, links. Structure is Markdown headings;
     steps may be `- **WHEN**`/`- **THEN**` bullets, or column-0 ```gherkin
     fences inside a `#### Scenario:` heading. -->

## ADDED Requirements

### Requirement: <!-- requirement name -->
<!-- requirement description: use SHALL/MUST. Plain prose, never inside a fence. -->

#### Scenario: <!-- scenario name -->
- **WHEN** <!-- action -->
- **THEN** <!-- expected outcome -->

Or, for fence-authored steps:

#### Scenario: <!-- scenario name -->

```gherkin
Given <!-- precondition -->
When <!-- action -->
Then <!-- expected outcome -->
```