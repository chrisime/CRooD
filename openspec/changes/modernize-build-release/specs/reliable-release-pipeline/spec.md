# reliable-release-pipeline

Repeatable, tag-based publishing of the CRooD artifact to GitHub Packages with real release notes, replacing the dead Bintray pipeline and the star-triggered publish workflow.

## ADDED Requirements

### Requirement: releases publish to GitHub Packages on a tag
The release workflow SHALL publish the built artifact to GitHub Packages when a version tag is pushed, using `maven-publish` with a `github` repository pointing at `packageRegistry`. It SHALL NOT trigger on repository stars.

Feature: reliable-release-pipeline
Rule: Publishing is tag-driven

#### Scenario: Publish an artifact from a version tag
- **GIVEN** the release workflow enabled for version tags
- **WHEN** a tag matching the version pattern is pushed
- **THEN** the workflow per-forms the build
- **AND** publishes the artifact to GitHub Packages

#### Scenario: Publishing does not trigger on stars
- **GIVEN** the publish workflow is configured
- **WHEN** a user stars the repository
- **THEN** no publish job is triggered

### Requirement: releases have a real body
The release-creation step SHALL produce a release with a changelog-driven body instead of the placeholder text, using `gh release create` (or the current equivalent action).

#### Scenario: Create a release with a populated body
- **GIVEN** a version tag is pushed
- **WHEN** the release is created
- **THEN** the release body contains the actual changes
- **AND** the release is not a placeholder

## REMOVED Requirements

### Requirement: Publish to a dead Bintray repository
**Reason**: Bintray was shut down by JFrog in 2021; the GitHub Actions workflow and README badge targeted a service that no longer exists.

**Migration**: Releases are published to GitHub Packages via the new tag-driven workflow; remove the Bintray badge and workflow files.