# improve-config-loading — Design

## Context

`CRooDConfigurationLoader` (`src/main/kotlin/xyz/chrisime/crood/config/CRooDConfigurationLoader.kt`) is a singleton `object` whose `croodConfigOfUserDir` **eagerly reads `$HOME/crood.json` at object-initialization time** (line 8), i.e. at class-load when first referenced. Consequences:

- Non-injectable: `CRooDGenerator.configuration` (`CRooDGenerator.kt:28-29`) is a synthetic `val` reading the user-dir config; consumers cannot supply their own source.
- Untestable: config behavior depends on the machine's `$HOME` (the existing spec passes a path but still goes through the eager object).
- Fragile: malformed JSON throws from `org.json` inside the singleton init, and there is no graceful missing-file handling beyond returning the default.

`CRooDConfig` is a two-field data class (`CRooDConfig(annotations, frameworks)`) with hand-parsed `org.json` mapping (`enableTransient`/`enableJakarta`/`isMicronaut` keys).

Per DQ1 the loader is reworked around Hoplite (`com.sksamuel.hoplite`): `CRooDConfig` becomes a Hoplite-loaded data class, the handwritten `org.json` parsing is dropped, and wiring injects the loader where jOOQ instantiates the generator by name.

## Goals / Non-Goals

**Goals:**
- No class-load-time read of `$HOME`; config arrives via an injected `CRooDConfig` or a Hoplite-backed loader the consumer controls.
- `CRooDConfig` is a Hoplite-loaded data class mapping from JSON/YAML keys (`annotations.{useTransient,useJakarta}`, `frameworks.isMicronaut`).
- Graceful handling: missing/unreadable file → defaults; malformed config → clear exception naming the source path; unknown keys → ignored or warned, not fatal.
- Lazy config read (not at object init).
- Existing key names and the `crood.json` default layout keep parsing the same files.

**Non-Goals:**
- No expansion of the config model into new generator knobs in this change (the proposal lists it as incremental; the specs only require the existing surface).
- No change to how jOOQ's `Generator` SPI instantiates generator classes; wiring adapts to it rather than replacing it.

## Decisions

### D1: `CRooDConfig` becomes a Hoplite-loaded data class
Keep `CRooDConfig(annotations: Annotations, frameworks: Frameworks)` (and nested `Annotations`/`Frameworks`) as a data class with Hoplite-friendly defaults, and load it via a Hoplite `ConfigLoader` from a source the caller provides (file path, resource, or string). Delete the `org.json` `JSONTokener`/`JSONObject` mapping.

- **Rationale:** DQ1 resolved Hoplite; the data-class shape is what Hoplite documents (Kotlin data-class mapping), and the existing `CRooDConfigSpec` assertions carry over unchanged for the happy-path JSON.
- **Alternatives considered:**
  - Keep `org.json` parsing, just make it lazy → rejected in DQ1: the hand-rolled mapping is exactly the fragile/untestable part Hoplite replaces.
  - Jackson/YAML-only → rejected: Hoplite gives JSON+YAML from one source, matching the "format flexibility" goal.

### D2: Injectable entry point, no eager read
Remove `croodConfigOfUserDir` from the singleton and replace the eager `val` with an explicit loader API: a `loadConfiguration(source: ...)`/`loadConfiguration(config: CRooDConfig)` entry point. The generator wiring gains an injectable access point that carries the consumer-supplied `CRooDConfig` (or a loader) into the generator, so no generator path reads `$HOME` implicitly.

- **Rationale:** The spec's rule is "no class-load-time reads of the user home directory" and the injectable entry-point requirement; jOOQ instantiates generators by class name, so the wiring must feed the injected config through a stable access point rather than a static feild.
- **Alternatives considered:**
  - Service-provider lookup for config → rejected in DQ1: an implicit discovery mechanism recreates the hidden-global problem.
  - Constructor-parameter loader into generators → blocked by jOOQ's name-based instantiation, which cannot pass constructor args; the access point is the pragmatic seam.

### D3: Graceful, source-identifying error handling
Missing/unreadable file → return `defaultConfiguration`. Malformed content → raise a clear exception that names the source path (Hoplite failures include decoding details; wrap to add the path). Unknown keys → Hoplite's default unknown-key behavior is configured to ignore or log a warning rather than throw.

- **Rationale:** Matches the spec scenarios verbatim (missing → defaults; malformed → path-identifying error; unknown keys → ignored/warned).
- **Alternatives considered:** fail-fast on unknown keys → rejected by spec; permissive parsing for library config is the documented behavior.

### D4: Lazy read via the loader boundary
Loading happens only when a consumer invokes the loader (or supplies config); the class-load path performs no I/O.

- **Rationale:** Removes the class-load surprise entirely; parity with the default-behavior spec when invoked on a missing path.

## Risks / Trade-offs

- [BREAKING for `croodConfigOfUserDir` static access] -> All in-repo consumers (`CRooDGenerator.kt:29`) update to the injected access point; external consumers migrate to the new loader API (versioned release note).
- [Hoplite default behaviors vary by version] -> Config loader construction pins the decoder settings (unknown-key policy disabled-by-default; the error wrapper test asserts the path appears).
- [jOOQ name-based instantiation cannot inject via constructor] -> The access point carries the injected config; documented in the generator interface so scripted generator configs are unaffected.
- [Existing `enableTransient`/`enableJakarta` key aliases] -> Hoplite maps `annotations.useTransient`/`useJakarta`; the JSON test fixture keeps loading identical keys, so the read is backward-compatible for well-formed files.

## Migration Plan

1. Add Hoplite dependency to the version catalog and `implementation`.
2. Convert `CRooDConfigurationLoader` to expose `loadConfiguration(file)` / `loadConfiguration(source: String)` via Hoplite; delete the `org.json` path and the `croodConfigOfUserDir` eager val.
3. Point `CRooDGenerator.configuration` at the injected access point (no `$HOME` read).
4. Update `CRooDConfigSpec` for the new loader API; add scenarios for missing path (default), malformed JSON (path in exception), and unknown keys (ignored/warned).
5. Run `./gradlew test`; run the generator examples to confirm default config behavior.
6. Rollback: revert loader/generator/spec to the singleton `org.json` form; drop the Hoplite dependency.

## Open Questions

- Exact Hoplite dependency/module version for the current catalog (`kotlin 1.7.20`) — resolved at implementation; if Hoplite needs a newer Kotlin, the toolchain bump (modernize-build-release) must land first, switching this change's dependency addition accordingly.