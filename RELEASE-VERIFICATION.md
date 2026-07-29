# Release Verification

Release version: 1.4.17

Artifact: `target/DragonAltar-1.4.17.jar`

Artifact size: 467,971 bytes, 457.00 KiB

SHA-256: `214644f656f22cb24c321432b8174ee481aecef6d964831f655d80a236c0de6c`

Checksum file: `target/DragonAltar-1.4.17.jar.sha256`

## Automated verification

- `mvn clean package`: passed
- Test suites: 28
- Tests: 83
- Failures: 0
- Errors: 0
- Skipped: 0
- Main compiler warnings: 0
- Test compiler warnings: 0
- Compiled Java class version: 65, Java 21, through Maven `release`
- Reproducibility: deterministic archive timestamps remain configured; repeat-build hash comparison was not rerun for this workspace build
- JAR entries: 215
- JAR namespaces: plugin classes only under `com/dragonaltar`
- Bundled resources: `abilities.yml`, `altar.yml`, `animations.yml`, `config.yml`, `messages.yml`, `plugin.yml`, and `ritual.yml`
- Bundled Paper, WorldEdit, PlaceholderAPI, ScaledEnderDragon, or JUnit classes: none
- UTF-8 validation: every source, resource, test, workflow, script, and Markdown text file passed strict UTF-8 decoding
- Player-facing em dash or en dash scan: no matches
- Personal path scan: no matches
- Credential pattern scan: no credential material found

The local Maven runtime was JDK 25.0.3 and compiled with `--release 21`. The generated manifest records `Target-Java-Version: 21`. The repository CI workflow is configured for Temurin Java 21, but it was not executed by a hosted CI service in this workspace.

## Metadata verification

- Plugin name: DragonAltar
- Main class: `com.dragonaltar.DragonAltarPlugin`
- API version: 1.21
- Command: `/dragon`
- Alias: `/dragonaltar`
- Optional dependencies: PlaceholderAPI, WorldEdit, ScaledEnderDragon
- Maven release version and filtered `plugin.yml` version: 1.4.17

## Remaining manual and legal gates

- Run the full [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) on Paper 1.21.4 with Java 21.
- Test optional integrations on the intended server builds.
- Confirm startup, shutdown, restart, chunk, display, inventory, ritual, transfer, and multiplayer performance behavior live.
- Select and add the intended license before public distribution.
- Replace publishing, identity, project, issue, and support placeholders.
- Establish source provenance or import the snapshot into a reviewed Git repository.
