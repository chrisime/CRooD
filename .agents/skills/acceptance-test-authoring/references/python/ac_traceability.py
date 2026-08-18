#!/usr/bin/env python3
"""Jira-only AC traceability and coverage gate for the acceptance harness.

Runs via `run_acceptance.py --check-ac`. Enforces:

  - AC-to-scenario traceability per spec file: every `<!-- AC: TICKET-ID#N -->`
    comment maps to at least one `#### Scenario:` in the same file.
  - AC-comment placement: in heading-format files, an AC comment must sit
    inside a requirement block (after `### Requirement:`, above a mapped
    `#### Scenario:`), because the native `openspec archive` merge silently
    drops prose before the requirement heading. Placement outside a block is an
    error.
  - AC coverage gate per active change with specs: every AC listed in the
    change's proposal `## Source` maps to an `<!-- AC: TICKET-ID#N -->`
    comment in the change's spec files, and every such comment references an
    AC that exists in `## Source` (no orphan comments).
"""

import re
import sys
from pathlib import Path

AC_RE = re.compile(r"<!--\s*AC:\s*([^\s]+)\s*-->")
AC_FULL_RE = re.compile(r"<!--\s*AC:\s*([^\s#]+)#(\d+)\s*-->")
TICKET_RE = re.compile(r"^\s*-\s*\*\*Ticket:\*\*\s*(.+?)\s*$")
AC_HEADING_RE = re.compile(r"^\s*-\s*\*\*Acceptance Criteria:\*\*\s*$")
AC_LINE_RE = re.compile(r"^\s*(\d+)\.\s")

REQ_HEADING_RE = re.compile(r"^\s*###\s+Requirement:\s*")
SCENARIO_HEADING_RE = re.compile(r"^\s*####\s+Scenario:\s*")
SECTION_HEADING_RE = re.compile(r"^\s*#{2,3}\s+")
FENCE_OPEN_RE = re.compile(r"^```gherkin\s*$")
FENCE_CLOSE_RE = re.compile(r"^```\s*$")


def _is_heading_file(path):
    return any(REQ_HEADING_RE.match(line) for line in re.split(r"\r?\n", path.read_text(encoding="utf-8")))


def check_ac_traceability(spec_path):
    """Each AC comment must be followed (in this file) by at least one scenario.

    Also, in heading-format files, each AC comment must be inside a requirement
    block: after its `### Requirement:` heading and before the enclosing section
    ends.
    """
    lines = re.split(r"\r?\n", spec_path.read_text(encoding="utf-8"))
    total_ac = sum(1 for line in lines if AC_RE.search(line))
    total_scenarios = sum(1 for line in lines if SCENARIO_HEADING_RE.match(line)) if _is_heading_file(spec_path) else 0
    issues = []

    if total_ac and total_scenarios < total_ac:
        issues.append(
            "%s: %d Jira AC comment(s) but only %d scenario(s); every AC must map to at least one scenario"
            % (spec_path, total_ac, total_scenarios)
        )

    if _is_heading_file(spec_path):
        issues.extend(_check_placement(spec_path, lines))
    return issues


def _check_placement(spec_path, lines):
    issues = []
    in_requirement = False
    for i, line in enumerate(lines):
        if REQ_HEADING_RE.match(line):
            in_requirement = True
            continue
        if SECTION_HEADING_RE.match(line) and not SCENARIO_HEADING_RE.match(line):
            in_requirement = False
            continue
        if not AC_RE.search(line):
            continue
        if not in_requirement:
            issues.append(
                "%s:%d: AC comment outside a requirement block; place `<!-- AC: ... -->` inside the block, after `### Requirement:` and before the mapped `#### Scenario:`"
                % (spec_path, i + 1)
            )
    return issues


def _source_ac_numbers(proposal_path):
    """Return (ticket_id, {ac_number: line_number}) from a proposal's ## Source section.

    The ticket id is the `**Ticket:**` value (stripped of an HTML comment
    placeholder); AC numbers come from the numbered lines under the
    `**Acceptance Criteria:**` heading. Returns (None, {}) when the proposal
    has no ticket or no AC list (e.g. a tooling change without a ticket).
    """
    lines = re.split(r"\r?\n", proposal_path.read_text(encoding="utf-8"))
    ticket = None
    in_ac = False
    acs = {}
    for i, line in enumerate(lines):
        t = TICKET_RE.match(line)
        if t:
            ticket = t.group(1).strip()
            m = re.search(r"<!--\s*([^>]+?)\s*-->", ticket)
            if m:
                ticket = m.group(1).strip()
            continue
        if AC_HEADING_RE.match(line):
            in_ac = True
            continue
        if in_ac:
            if re.match(r"^##\s", line):
                break
            m = AC_LINE_RE.match(line)
            if m:
                acs[int(m.group(1))] = i + 1
    return ticket, acs


def check_ac_coverage(openspec_dir, change_dir):
    """Coverage gate for one active change with specs.

    Every AC listed in the change's proposal `## Source` must map to an
    `<!-- AC: TICKET-ID#N -->` comment in the change's spec files, and every
    such comment must reference an AC that exists in `## Source` (no orphans).
    """
    proposal_path = change_dir / "proposal.md"
    if not proposal_path.exists():
        return []
    ticket, acs = _source_ac_numbers(proposal_path)
    if not ticket or not acs:
        return []

    change_specs = sorted(
        p for p in (change_dir / "specs").rglob("*.md") if "changes/archive/" not in p.as_posix()
    )
    if not change_specs:
        return []

    referenced = {}  # ac_number -> list of spec files
    for spec_path in change_specs:
        lines = re.split(r"\r?\n", spec_path.read_text(encoding="utf-8"))
        for line in lines:
            m = AC_FULL_RE.search(line)
            if m and m.group(1) == ticket:
                referenced.setdefault(int(m.group(2)), []).append(spec_path.name)

    issues = []
    for ac_num in sorted(acs):
        if ac_num not in referenced:
            issues.append(
                "%s:%d: AC %s#%d listed in ## Source has no <!-- AC: %s#%d --> comment in the change's specs"
                % (proposal_path, acs[ac_num], ticket, ac_num, ticket, ac_num)
            )
    for ac_num, files in sorted(referenced.items()):
        if ac_num not in acs:
            issues.append(
                "AC comment %s#%d in %s has no matching AC in ## Source (orphan)"
                % (ticket, ac_num, ", ".join(sorted(set(files))))
            )
    return issues


def run_ac_checks(openspec_dir=None, change_filter=None):
    """Run the AC checks; return an issue list. exit_code = 1 if any issue."""
    openspec_dir = Path(openspec_dir).resolve() if openspec_dir else Path.cwd() / "openspec"
    issues = []

    spec_mds = sorted(
        p
        for p in [*openspec_dir.joinpath("specs").rglob("spec.md")]
        if "changes/archive/" not in p.as_posix()
    )
    for spec_path in spec_mds:
        issues.extend(check_ac_traceability(spec_path))

    changes_dir = openspec_dir / "changes"
    if changes_dir.is_dir():
        for entry in sorted(changes_dir.iterdir()):
            if not entry.is_dir() or entry.name == "archive":
                continue
            if change_filter and entry.name != change_filter:
                continue
            issues.extend(check_ac_coverage(openspec_dir, entry))

    return issues


def main(argv):
    openspec_dir = argv[0] if argv else None
    return _report(run_ac_checks(openspec_dir))


def _report(issues):
    if not issues:
        sys.stderr.write("[check-ac] ok\n")
        return 0
    sys.stderr.write("[check-ac] %d issue(s):\n" % len(issues))
    for issue in issues:
        for line in issue.split("\n"):
            sys.stderr.write("  " + line + "\n")
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))