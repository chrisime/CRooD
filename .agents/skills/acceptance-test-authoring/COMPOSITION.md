# Composition Contract (Effective Spec)

This contract pins how the effective spec for a behave/cucumber run is
composed from source-of-truth specs and active change deltas. It is
`02a4647`-aligned: supersession is determined from heading deltas, and the
composition report format is shared across stacks.

## Procedure (normative)

1. Extract every `spec.md` under `openspec/specs/` and active
   `openspec/changes/*/specs/`; `openspec/changes/archive/` is excluded.
2. Determine supersession from the active delta Markdown files directly:
   - Heading format: a requirement under a `## MODIFIED Requirements` or
     `## REMOVED Requirements` section is superseded; ADDED/RENAMED are not.
   - Legacy fenced format: a `Rule:` bound to a `# @openspec: MODIFIED|REMOVED`
     marker is superseded.
3. Exclude superseded source-of-truth scenarios from the run without editing
   specs. Blank the generated `.extracted/` lines, never edit the spec.
4. Add every delta file whole.
5. Print a composition report to stderr for every left-out scenario.

## Binding Rules

| Binding | Use when |
| --- | --- |
| Line-targeted discovery | The runner filters before loading (cucumber-js `spec.feature:12:19`). |
| Pruning the extracted tree | Line selection would runtime-skip (behave). Blank `.extracted/` lines rather than editing specs. |

## Conflict Detection

Two active changes must not both supersede the same rule of the same
capability. This is a `CompositionError`; it stops the run.

## Report Format

Identical across stacks:

```text
[effective-spec] user-signup / Rule: A signup SHALL require an email address and a password
[effective-spec]   superseded by change: signup-email-verification
[effective-spec]   left out: Signing up with valid details (../openspec/specs/user-signup/spec.md:12)
[effective-spec] 1 source-of-truth scenario(s) excluded; delta versions run from openspec/changes/
```

## Invariants

- Superseded rules must not reach the runner and must not be reported as
  skipped.
- A green effective suite is the gate for sync/archive, and sync/archive must
  never change suite results.
- Specs under `openspec/changes/archive/` never execute.
- Composition is verified whenever the runner config, extractor, or
  `openspec/` tree changes.