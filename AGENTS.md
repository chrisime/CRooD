# AGENTS.md

- For OpenSpec propose/apply/verify/archive workflows, use the local `openspec-git-discipline` skill to enforce proposal commits before apply and merge-before-archive discipline.

- Before verifying or archiving OpenSpec changes, always enumerate all active changes with `openspec list --json` (never rely on memory or conversation context alone) and handle every one of them.
