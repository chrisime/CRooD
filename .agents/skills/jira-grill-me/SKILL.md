---
name: jira-grill-me
description: Use during the proposal phase of the jira-driven schema to interrogate a Jira ticket's Acceptance Criteria and scope before specs. Owns the two-phase review - Phase 0 Decision Questions (DQ) gating the AC interpretation, then Phase 1 Feedback Questions (FQ) - plus handle assignment, the default-answer heuristic, and the DQ state machine. Reference this skill from schema instructions instead of inlining the review procedure.
---

# Jira Grill-Me

## Overview

Grill-Me is the jira-driven interrogation step that turns a loaded Jira ticket
(the proposal's `## Source`) into a complete, gap-free requirement statement
before specs begin. It runs in two gated phases and works entirely on the
proposal file plus a conversation with the user. It does not fetch or write to
Jira — the jira-import skill owns all ticket I/O; this skill owns the
interrogation logic.

## Workflow

Run the two phases in order; Phase 1 must not start until Phase 0 is clear.

### Phase 0 — Decision Questions (DQ)

1. Detect fundamental / scope decisions that gate the AC interpretation (e.g.
   which system or target repo a ticket targets).
2. Record each open DQ in the proposal's `## Decision Questions` section
   (before `## Open Questions`) using the template checkbox format:
   `[DQx] · <question>`, its own monotonic numbering, separate from `[FQx]`
   and `[ACx]`, plus a Status line (`awaiting-answers` | `resolved`).
3. A DQ is a question about a decision, not an AC gap: it stays a DQ, is never
   promoted to an AC, and carries no AC contract.
4. **DQ gate:** do NOT generate any Feedback Questions until no DQs are open.

### Phase 1 — Feedback Questions (FQ)

1. Only after the DQ gate is clear, document every unresolved AC requirement
   gap under the proposal's `## Open Questions`.
2. Use the checkbox format defined in the template: AC reference, stable
   `[FQx]` handle assigned at Grill-Me time, gap, recommendation, reason, and
   an optional "communicated to Jira" note.

## Handles

Handles are stable process identities assigned at Grill-Me time, never renamed,
independent of block position and AC numbering:

- `[DQx]` — decision questions (own numbering, Phase 0).
- `[FQx]` — feedback questions (AC gaps, Phase 1).
- `[ACx]` — AC annotations in `## Source`, replacing the provisional
  `ACx-TBD` / `AC5-DELETED` handles. The `<!-- AC: TICKET-ID#N -->` anchors in
  the specs stay unchanged — they are workflow-host-local and never reach Jira.
  (In the specs artifact instruction these map onto the AC numbering:
  `[AC3]` provisional ↔ `AC3-TBD`, `[AC5] DELETED` ↔ `AC5-DELETED`.)

## Default Answer Heuristic

When a gap depends on a decision only the ticket owner can make, offer the
default answer "needs to be asked by the ticket owner". If the user takes it,
record the gap as open with no extra marker — the AC keeps its provisional
`[ACx]` handle and its stable `[FQx]` identity, and the reason line reads
exactly "Reason: needs to be asked by the ticket owner". The re-import
recognizes the gap by that exact reason (no separate tag, no new field) and,
once the owner answers, folds the reply in and ticks the box like any other
open question.

## DQ State Machine

One-way state transitions for a DQ:

- `awaiting-answers` → `resolved` on a PO answer in the ticket OR a team
  decision documented as a `Decision:` note (when the PO is unreachable, the
  team decides so the workflow never blocks).
- `resolved` → `resolved` — a later re-import NEVER flips it back.
- `awaiting-answers` → `awaiting-answers` — a re-import with an unchanged
  ticket leaves the DQ open as-is.
- There is no second severity level of `awaiting-answers`.

## Relationship To Other Steps

- **jira-import** owns ticket fetching, re-import, and export; Grill-Me owns
  the interrogation. After Grill-Me, the export of still-open questions is a
  jira-import concern.
- **Schema instruction** references this skill for the review procedure and
  keeps the gate policy (when the proposal blocks on open questions) inline.