# Changelog

## 1.4.21 - 2026-08-08

- Added API 2.2 per-item soul-loss policies for leaving, unequipping, dropping,
  or destroying registered soul-bound equipment.
- Reconciled offline losses on join and add-on re-registration, including safe
  full-inventory and death/keep-inventory behavior without item duplication.

## 1.4.20 - 2026-08-08

- Added API 2.1 registration and canonical PDC tagging for soul-bound add-on items.
- Added shared soul ownership and add-on callback checks across all six player
  equipment slots, with a cancellable equip event and configurable denial text.
- Documented item lifecycle handling and the soul ids carried by transfer and
  Dragonborn loss events.

## 1.4.19 - 2026-07-29

- Removed the unused WorldEdit dependency, soft dependency, schematic settings,
  setup checks, and related documentation.
- Removed old configuration options that no longer controlled gameplay,
  including inactive abilities, display motion settings, Focus protection
  switches, Elytra durability switches, and the forced-removal participant
  count.
- Made `internal-protection.required-for-event` an active event-start
  requirement and migrated the former protection switch.
- Fired the compatibility soul-transfer event from the real transfer path while
  keeping both transfer events cancellable.
- Connected the player-only, setup warning, event start, recovery warning, and
  ritual error messages to the places where players and staff need them.
- Removed the unused message prefix and egg hologram text.
- Made the Mother Soul caller count and required weakness potions follow the
  caller-pad layout automatically.
- Connected the tested recovery decision rules to live event recovery.
- Removed unused ritual planning code, old combat tracking code, persistence
  helpers, getters, and their tests.
- Updated the README, release documents, configuration reference, and wiki to
  match the supported 1.4.19 behavior.

## 1.4.18 - 2026-07-29

- Added the DragonAltar noncommercial license and a separate permission for free
  independent add-ons with required Silverfox0338 ownership credit.
- Added API 2.0 immutable soul, event, ritual, eligibility, ability, cooldown, and
  player-action views.
- Redacted private staff custody from public API snapshots and retired raw soul
  access that could expose hidden lineage.
- Added namespaced custom ability registration with energy, cooldown, soul, and
  server-thread validation.
- Added automatic add-on cleanup on plugin disable and contained failures from
  third-party ability callbacks.
- Added a complete Maven and Bukkit add-on example.

## 1.4.17 - 2026-07-29

- Removed administrative and developer holders from every player-facing soul-history field.
- Added private-custody metadata so admin test transfers remain auditable without entering the public holder chain.
- Kept compatibility with 1.4.16 entries by recognizing the recorded admin actor and current operator status.
- Displays privately held souls as dormant with no public holder until they return to a normal player.

## 1.4.16 - 2026-07-29

First public changelog baseline. DragonAltar was a private project before 1.4.16, so earlier private releases are intentionally not reconstructed here.
*(Forgot to bump up the number in earlier commits sry yall ._.)*

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
