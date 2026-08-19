## 1. Hoplite Configuration Loading

- [ ] 1.1 Add the Hoplite dependency to the version catalog and `implementation`; pin a version compatible with the current Kotlin toolchain (or record the module dependency on modernize-build-release).
- [ ] 1.2 Convert `CRooDConfigurationLoader` to expose `loadConfiguration(file)` / `loadConfiguration(source)` via Hoplite; delete the `org.json` parser and the eager `croodConfigOfUserDir` val.
- [ ] 1.3 Keep `CRooDConfig`/`Annotations`/`Frameworks` as Hoplite-mapped data classes with defaults matching `defaultConfiguration`.

## 2. Injectable Wiring

- [ ] 2.1 Point `CRooDGenerator.configuration` at the injected access point so no generator path reads `$HOME` implicitly.

## 3. Error Handling and Tests

- [ ] 3.1 Missing/unreadable source returns `defaultConfiguration`; malformed content raises an exception naming the source path; unknown keys are ignored or warned (configure Hoplite decoder policy).
- [ ] 3.2 Update `CRooDConfigSpec` for the loader API; add scenarios for default-on-missing, path-in-error-on-malformed, and unknown-keys tolerance.
- [ ] 3.3 Run `./gradlew test` and re-run the generator examples to confirm default config behavior.

## 4. Verification Gate

- [ ] 4.1 Run `openspec validate improve-config-loading --type change --strict` and confirm exit 0.
- [ ] 4.2 Run `run_acceptance.py --lint` and `run_acceptance.py --check-ac` and confirm both pass.