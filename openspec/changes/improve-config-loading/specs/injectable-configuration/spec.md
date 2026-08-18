# injectable-configuration

`CRooDConfig` is loaded via Hoplite (`com.sksamuel.hoplite`) into an explicit, testable data class instead of an eager global `$HOME` read. Consumers control the configuration source and lifecycle; missing or malformed configuration is handled gracefully.

## ADDED Requirements

### Requirement: configuration is loaded through an injectable entry point
The generator SHALL NOT read `~/crood.json` implicitly at class-load time. Instead, configuration SHALL be supplied through an injected `CRooDConfig` instance or a Hoplite-backed loader that library consumers control.

Feature: injectable-configuration
Rule: No class-load-time reads of the user home directory

#### Scenario: Generator uses explicitly supplied configuration
- **GIVEN** a `CRooDConfig` instance with `annotations.useJakarta` enabled
- **WHEN** the generator runs with that configuration
- **THEN** it emits Jakarta validation annotations
- **AND** no configuration file is read from the user home directory

#### Scenario: Missing configuration file defaults
- **GIVEN** a configured source path that does not exist
- **WHEN** the configuration is loaded
- **THEN** the default `CRooDConfig` is returned
- **AND** no error is raised

#### Scenario: Malformed configuration fails with a clear message
- **GIVEN** a configuration source with invalid content
- **WHEN** the configuration is loaded
- **THEN** a clear exception is raised that identifies the source path

#### Scenario: Unknown keys do not break loading
- **GIVEN** a configuration source with extra, unsupported keys
- **WHEN** the configuration is loaded
- **THEN** the supported keys are parsed
- **AND** the unknown keys are ignored or surfaced as a warning

### Requirement: CRooDConfig is a Hoplite-loaded data class
`CRooDConfig` SHALL be supported as a Hoplite-loaded data class so standard Kotlin config files (JSON/YAML) map directly onto it, replacing the handwritten `org.json` parsing.

#### Scenario: JSON configuration maps onto CRooDConfig
- **GIVEN** a JSON source with `annotations` and `frameworks` objects
- **WHEN** the configuration is loaded via Hoplite
- **THEN** the resulting `CRooDConfig` reflects the source values