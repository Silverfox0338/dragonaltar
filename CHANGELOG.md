# Changelog

## 1.0.0 - 2026-07-28

First public release candidate.

- Finalized Paper 1.21.4 and Java 21 release metadata.
- Removed SNAPSHOT labeling and produced the `DragonAltar-1.0.0.jar` distribution name.
- Preserved Akuma, Rev, Lamari, their passives and abilities, all ultimates, all resonances, rituals, transfers, recovery, accessibility, and persistence behavior.
- Preserved Rev's Heat, Hunt, Inferno Mark, Rampage, Rend recast, Wrath field, and Predator's Claim design.
- Preserved persistent shared ultimate and resonance cooldowns.
- Added hard runtime target-radius and entity-count ceilings.
- Excluded invalid, dead, removed, armor-stand, and spectator targets from shared ability queries.
- Fixed final regenerated Dragon Energy persistence at logout and shutdown.
- Added explicit ritual, animation, temporary-flight, display, terrain, task, and cache cleanup.
- Improved persistence failure reporting and reload validation.
- Expanded packaged YAML, metadata, cooldown, lifecycle, and combat-bound tests.
- Added release, privacy, publishing, upgrade, rollback, and manual verification documentation.
- Added Maven and repository hygiene for reproducible release builds.
