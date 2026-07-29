# Changelog

## 1.4.16 - 2026-07-29

First public changelog baseline. DragonAltar was a private project before 1.4.16, so earlier private releases are intentionally not reconstructed here.

- Finalized Paper 1.21.4 and Java 21 release metadata.
- Produces the `DragonAltar-1.4.16.jar` distribution name.
- Added the player-facing Dragon Soul history overview and paginated per-soul timelines.
- Added live holder, prior-holder, transfer, fracture, limbo, and return-time details.
- Added private administrator provenance to soul lineage without exposing staff involvement in public history.
- Expanded destructive-command previews with affected players/souls, outcome, cooldown/history impact, reversibility, cancellation, state-drift rejection, and confirming-admin audit records.
- Added guarded force-remove and force-transfer commands that verify both player and public soul name.
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
