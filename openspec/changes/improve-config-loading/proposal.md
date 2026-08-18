## Source

<!-- Deliberate skip: this change does NOT originate from a Jira ticket.
     The jira-driven schema normally populates this section verbatim via the
     jira-import skill (config rule) when a change's requirements come from a
     Jira ticket. This change was identified from a codebase review; the
     jira-import / jira-grill-me skills do not apply because there is no
     ticket to load or interrogate. -->

- **Ticket:** none (codebase-review origin)
- **Summary:** Rework `CRooDConfigurationLoader` so configuration is injectable and robust
- **Description:** Not applicable (no Jira ticket).
- **Acceptance Criteria:**
  1. Not applicable (no Jira ticket).

## Why

Configuration is read eagerly from `~/crood.json` (`$HOME`) at class-load time by the singleton `CRooDConfigurationLoader`, which is surprising, non-injectable, untestable, and fails hard on malformed JSON. The config surface itself is just three booleans, with several consumers needing more (e.g. which validation framework, transient behavior, optimistic-lock regexes).

## What Changes

- **BREAKING:** Replace the static `croodConfigOfUserDir` object initialization with a first-class, injectable config loader using [Hoplite](https://github.com/sksamuel/hoplite) (`com.sksamuel.hoplite`) so library consumers control source and lifecycle; drop the handwritten `org.json` parsing entirely.
- Add graceful error handling: missing file → defaults (keep), malformed config → Hoplite's detailed failure with source path, unknown keys → ignored or warned.
- Read the config file lazily, not at class-load.
- Expand the config model incrementally to cover generator knobs currently hard-coded (validation framework, transient flag, optimistic-lock field regexes) without breaking existing keys — `CRooDConfig` becomes a Hoplite-loaded data class.
- Optional: keep format flexibility open (JSON/YAML via Hoplite modules) while defaulting to the existing `crood.json` file name and layout it already parses.

## Capabilities

### New Capabilities

- `injectable-configuration`: `CRooDConfig` loaded via Hoplite into an explicit, testable data class instead of a global `$HOME` read.

### Modified Capabilities

_(none — no existing specs exist.)_

## Impact

- Affected code: `src/main/kotlin/xyz/chrisime/crood/config/CRooDConfigurationLoader.kt`, `config/CRooDConfig.kt`, and every consumer (`codegen/*.kt`); tests in `src/test/kotlin/.../config/CRooDConfigSpec.kt`.
- API: **BREAKING** for `croodConfigOfUserDir` static access; generator wiring changes accordingly.
- Behavior kept: default config when no file or empty object.

## Decision Questions

- [x] [DQ1] · Injectable loader API shape: constructor-parameter loader vs top-level factory function vs service-provider lookup?
      Decision: Adopt Hoplite (`com.sksamuel.hoplite`) — `CRooDConfig` as a data class, loaded via `ConfigLoader`; jOOQ instantiates generators by name, so wiring reads config through an access point fed by the injected loader.
      Status: resolved
      Reason: fixes the public API contract for consumers configuring the generator.

## Open Questions

_(none open.)_