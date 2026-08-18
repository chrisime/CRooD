#!/usr/bin/env python3
"""Synthesize Gherkin feature files from Markdown heading specs.

The heading format authors requirements and scenarios with Markdown headings:

    # <capability>

    ## ADDED Requirements

    ### Requirement: <name>
    The system SHALL ...

    #### Scenario: <name>
    - **WHEN** action
    - **THEN** outcome

Extraction synthesizes a `Feature:` (from the H1 / capability name), `Rule:`
blocks (from `### Requirement:` headings plus their body line), and `Scenario:`
blocks (from `#### Scenario:` headings plus their `- **KEYWORD**` step bullets).
HTML comments (`<!-- AC: ... -->`) are prose, not steps, and are dropped.

Legacy fenced-Gherkin files (the pre-migration format) are passed through
unchanged so that already-authored fenced deltas keep running during the
migration. Files with neither heading structure nor fences fail extraction.
"""

import re
import shutil
import sys
from pathlib import Path

GHERKIN_OPEN_RE = re.compile(r"^(`{3,})gherkin\s*$")
ANY_OPEN_RE = re.compile(r"^(`{3,})\S*\s*$")
INDENTED_GHERKIN_RE = re.compile(r"^\s+`{3,}gherkin\s*$")

H1_RE = re.compile(r"^#\s+(.+?)\s*$")
DELTA_RE = re.compile(r"^\s*##\s+(?:ADDED|MODIFIED|REMOVED|RENAMED)\s+Requirements\s*$")
REQ_RE = re.compile(r"^\s*###\s+Requirement:\s*(.+?)\s*$")
SCN_RE = re.compile(r"^\s*####\s+Scenario:\s*(.+?)\s*$")
STEP_RE = re.compile(r"^\s*-\s+\*\*([A-Z]+)\*\*\s+(.+?)\s*$")
COMMENT_RE = re.compile(r"^\s*<!--.*-->\s*$")

STEP_KEYWORDS = {
    "GIVEN": "Given",
    "WHEN": "When",
    "THEN": "Then",
    "AND": "And",
    "BUT": "But",
}


class ExtractionError(Exception):
    """A spec.md could not be extracted."""


def _feature_name(lines, md_path):
    """Feature name: the first H1 title, else the capability folder name."""
    for line in lines:
        m = H1_RE.match(line)
        if m:
            return m.group(1)
    parts = str(md_path).split("/")
    if "specs" in parts:
        idx = len(parts) - 1 - parts[::-1].index("specs")
        if idx + 1 < len(parts):
            return parts[idx + 1]
    return md_path.stem


def _synthesize_heading(lines, feature_name):
    out = ["Feature: %s" % feature_name]
    in_rule = False
    in_scenario = False
    in_fence = False
    for line in lines:
        m = REQ_RE.match(line)
        if m:
            in_rule = True
            in_scenario = False
            in_fence = False
            out.append("  Rule: %s" % m.group(1))
            continue
        m = SCN_RE.match(line)
        if m:
            in_rule = False
            in_scenario = True
            in_fence = False
            out.append("    Scenario: %s" % m.group(1))
            continue
        if H1_RE.match(line) or DELTA_RE.match(line):
            in_rule = False
            in_scenario = False
            in_fence = False
            out.append("")
            continue
        if re.match(r"^\s*```", line):
            in_fence = not in_fence
            continue
        if COMMENT_RE.match(line):
            continue
        if in_rule:
            text = line.rstrip()
            out.append("    %s" % text if text.strip() else "")
            continue
        if in_scenario:
            text = line.rstrip()
            if in_fence:
                out.append("      %s" % text if text.strip() else "")
            else:
                m = STEP_RE.match(line)
                if m and m.group(1) in STEP_KEYWORDS:
                    out.append("      %s %s" % (STEP_KEYWORDS[m.group(1)], m.group(2)))
            continue
        # top-level prose / section prose: blank
        out.append("")
    return "\n".join(out) + "\n"


def _extract_fenced(md_path, lines):
    out = []
    state = "prose"
    fence_ticks = 0
    open_line = 0
    gherkin_fences = 0
    close_re = None

    for i, line in enumerate(lines):
        if state == "prose":
            m = GHERKIN_OPEN_RE.match(line)
            if m:
                state = "gherkin"
                fence_ticks = len(m.group(1))
                close_re = re.compile(r"^`{%d,}\s*$" % fence_ticks)
                open_line = i + 1
                gherkin_fences += 1
                out.append("")
            elif INDENTED_GHERKIN_RE.match(line):
                raise ExtractionError(
                    "%s:%d: indented ```gherkin fence; gherkin fences must start at column 0"
                    % (md_path, i + 1)
                )
            else:
                m = ANY_OPEN_RE.match(line)
                if m:
                    state = "other-fence"
                    fence_ticks = len(m.group(1))
                    close_re = re.compile(r"^`{%d,}\s*$" % fence_ticks)
                    open_line = i + 1
                out.append("")
            continue

        if close_re.match(line):
            state = "prose"
            out.append("")
        else:
            out.append(line if state == "gherkin" else "")

    if state != "prose":
        raise ExtractionError("%s:%d: unclosed fence" % (md_path, open_line))
    if gherkin_fences == 0:
        raise ExtractionError("%s: no ```gherkin fences found" % md_path)
    return "\n".join(out)


def extract_file(md_path):
    md_path = Path(md_path)
    lines = re.split(r"\r?\n", md_path.read_text(encoding="utf-8"))

    has_headings = any(REQ_RE.match(line) for line in lines)
    if has_headings:
        return _synthesize_heading(lines, _feature_name(lines, md_path))

    has_fences = any(GHERKIN_OPEN_RE.match(line) for line in lines)
    if has_fences:
        return _extract_fenced(md_path, lines)

    raise ExtractionError("%s: no `### Requirement:` headings and no ```gherkin fences found" % md_path)


def _walk(root, directory, basename, found):
    if not directory.is_dir():
        return found
    for entry in sorted(directory.iterdir()):
        if entry.is_dir():
            _walk(root, entry, basename, found)
        elif entry.name == basename or (basename.startswith("*.") and entry.name.endswith(basename[1:])):
            found.append(entry.relative_to(root).as_posix())
    return found


def collect_spec_sources(openspec_dir, basename):
    openspec_dir = Path(openspec_dir)
    found = _walk(openspec_dir, openspec_dir / "specs", basename, [])
    changes_dir = openspec_dir / "changes"
    if changes_dir.is_dir():
        for entry in sorted(changes_dir.iterdir()):
            if not entry.is_dir() or entry.name == "archive":
                continue
            _walk(openspec_dir, entry / "specs", basename, found)
    return sorted(p for p in found if "changes/archive/" not in p)


def extract_all(openspec_dir=None, out_dir=None):
    here = Path(__file__).resolve().parent
    openspec_dir = Path(openspec_dir).resolve() if openspec_dir else (here / ".." / "openspec").resolve()
    out_dir = Path(out_dir).resolve() if out_dir else (here / ".extracted").resolve()

    shutil.rmtree(out_dir, ignore_errors=True)
    sources = collect_spec_sources(openspec_dir, "spec.md")

    legacy = collect_spec_sources(openspec_dir, "*.feature")
    if legacy:
        sys.stderr.write(
            "[extract-gherkin] WARNING: legacy .feature file(s) under openspec/ are ignored: %s\n"
            % ", ".join(legacy)
        )

    written = []
    for rel in sources:
        dest = out_dir / re.sub(r"spec\.md$", "spec.feature", rel)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(extract_file(openspec_dir / rel), encoding="utf-8")
        written.append(dest)
    return out_dir, written


if __name__ == "__main__":
    try:
        out, written_files = extract_all(
            sys.argv[1] if len(sys.argv) > 1 else None,
            sys.argv[2] if len(sys.argv) > 2 else None,
        )
        sys.stderr.write("[extract-gherkin] %d spec.md file(s) extracted to %s\n" % (len(written_files), out))
    except ExtractionError as err:
        sys.stderr.write("[extract-gherkin] %s\n" % err)
        sys.exit(1)
