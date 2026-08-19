## 1. Toolchain and Dependencies

- [ ] 1.1 Raise Kotlin to a current 2.x version and align `apiVersion`/`languageVersion`/`jvmTarget` in `build.gradle.kts` (drop the `1.5` pin) in `gradle.properties` and the version catalog.
- [ ] 1.2 Bump jOOQ, the JUnit bom, kotest, and JSON versions in the version catalog to current supported releases.
- [ ] 1.3 Run `./gradlew build`; fix compiler/dependency fallout and build the examples; review generated-code differences.

## 2. GitHub Actions

- [ ] 2.1 Update `build.yml`, `publish.yml`, and `release.yml` to current major versions (`checkout@v4`, `setup-java@v4`, current gradle/wrapper-validation actions).
- [ ] 2.2 Replace `actions/create-release@v1` with `gh release create` producing a changelog-driven body.

## 3. Release Pipeline

- [ ] 3.1 Add the `github` `maven-publish` repository (`packageRegistry`) in `build.gradle.kts`; configure `permissions: packages: write`.
- [ ] 3.2 Rework `publish.yml` to trigger only on version tags (no star trigger) and publish to GitHub Packages.
- [ ] 3.3 Remove the dead Bintray workflow/references and README badge; document GitHub Packages in the README.
- [ ] 3.4 Push a `v*` tag and verify a release with a populated body and a package in GitHub Packages.

## 4. Verification Gate

- [ ] 4.1 Run `openspec validate modernize-build-release --type change --strict` and confirm exit 0.
- [ ] 4.2 Run `run_acceptance.py --lint` and `run_acceptance.py --check-ac` and confirm both pass.