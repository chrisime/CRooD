# Extraction Contract (Heading → Gherkin)

This contract pins how a heading-format `spec.md` is synthesized into an
executable `.feature` file. It is the `02a4647`-aligned contract: structure
comes from Markdown headings and steps are copied verbatim.

## Input Format

A spec is `openspec/specs/<capability>/spec.md` (source of truth) or
`openspec/changes/<id>/specs/<capability>/spec.md` (delta). Requirements and
scenarios are authored with headings, not fences:

```markdown
# <capability>                          `#` → Feature name

## ADDED Requirements                   `## X Requirements` → delta section marker

### Requirement: <name>                `###` → Rule
SHALL body line, plain prose.          Rule description (verbatim).

<!-- AC: TICKET-ID#N -->               Prose; dropped from the feature.

#### Scenario: <name>                  `####` → Scenario (EXACTLY 4 hashtags)
- **WHEN** action                      Step keyword + verbatim text
- **THEN** outcome
```

Two step forms are supported:

1. **Bullet steps** `- **KEYWORD** text` where KEYWORD ∈ GIVEN/WHEN/THEN/AND/
   BUT. The keyword is title-cased and the text is copied verbatim.
2. **Fenced steps** a column-0 ```gherkin ``` fence immediately inside a
   `#### Scenario:` heading; the fence's lines are copied verbatim at step
   indent.

## Synthesis Rules

- The `Feature:` name is the first `# ` H1 title; fallback is the capability
  folder name.
- A `### Requirement:` heading becomes a `Rule:`; its body lines (until the
  next heading) become the indented rule description, verbatim.
- A `#### Scenario:` heading becomes a `Scenario:`; only step lines are kept
  under it.
- HTML comments (`<!-- ... -->`) are prose and are never emitted as steps.
- Non-heading, non-step prose is blanked (never asserted as a step).

## Hard Invariants

- `#### Scenario:` MUST use exactly 4 hashtags. Using `###` or bullets breaks
  the extraction silently — this is a hard authoring error against the lint
  gate.
- Every requirement (`###`) MUST have at least one scenario (`####`); the
  native `openspec validate --strict` enforce this.
- The delta sections (`## ADDED|MODIFIED|REMOVED|RENAMED Requirements`) are
  NOT emitted; only their requirements/scenarios are.
- Legacy fenced-Gherkin files (pre-migration `# @openspec:` format) are passed
  through verbatim so already-authored fenced deltas keep running during the
  migration window.
- A file with neither `### Requirement:` headings nor ` ```gherkin ` fences is
  an extraction error.

## Line Fidelity

For heading synthesis there is no fence line-count invariant: structure is
synthesized from headings. The invariant is *content* fidelity — step texts and
rule bodies are copied verbatim, never reworded, trimmed into newlines, or
reordered.

## Edge Cases

| Case | Behavior |
| --- | --- |
| Step keyword not in the mapping | Bullet ignored (prose), not a step |
| Fenced steps with a non-gherkin info string | Fence tracked, lines blanked if outside scenario |
| H1 elsewhere | Only the first H1 sets the Feature name |
| Legacy `.feature` files under `openspec/` | Never run; extraction prints a warning |