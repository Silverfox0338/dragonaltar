# Changelog

## 1.4.23 - 2026-08-09

### 26.x compatibility

- Added compilation targets for the latest stable Paper 26.1.2 and 26.2 APIs
  under Java 25 while retaining the Paper 1.21 minimum-API release build under
  Java 21.
- Added live server-start smoke tests for Paper 26.1.2 and 26.2. CI downloads the
  latest stable server build from Paper's official downloads service, installs
  the minimum-API-built DragonAltar JAR, waits for startup, verifies that the
  plugin enabled without linkage errors, and shuts the server down.
- Replaced deprecated `Attribute.valueOf(...)` compatibility calls with Bukkit
  registry lookups supporting both the original `generic.*` attribute keys and
  the current keys used by later 1.21 and 26.x servers.
- Kept Java 21 bytecode and `api-version: '1.21'` so one JAR can serve the 1.21
  line on Java 21 and the 26.x line on Java 25.
- Kept public API contract `3.0`; no API interface, model, event, or add-on hook
  changed for 26.x compatibility.

### Test matrix

- Full tests, formatting, static analysis, API boundary checks, and packaging:
  Paper 1.21 API on Java 21.
- Compile guard: Paper 1.21.11 API on Java 21.
- Compile guards: Paper 26.1.2 and 26.2 APIs on Java 25.
- Runtime startup: latest stable Paper 26.1.2 and 26.2 servers on Java 25.
- Local smoke validation passed on Paper 26.1.2 build 74 and Paper 26.2 build
  111: DragonAltar enabled and both servers reached ready state without linkage
  errors.
- Completed live Paper and Purpur gameplay validation across the supported 1.21
  and 26.x targets, including abilities, rituals, persistence, integrations,
  shutdown, and restart behavior.

## 1.4.22 - 2026-08-09

### Compatibility

- Expanded the supported server range from Paper 1.21.4 to Paper and Purpur
  1.21 through 1.21.11 on Java 21.
- Changed the compile-time Paper dependency to the oldest supported 1.21 API so
  accidental use of APIs introduced by later 1.21 releases now fails during the
  build instead of failing when the plugin runs on an older server.
- Added a small runtime compatibility layer for the Bukkit attribute rename
  introduced during the 1.21 release line. Dragonborn health, Frostveil movement
  speed, Stoneheart armor toughness, fractured-soul combat attributes, ability
  health calculations, and dragon diagnostics now resolve both the original
  `GENERIC_*` names and their newer replacements.
- Kept the packaged plugin descriptor at `api-version: '1.21'`, allowing the same
  JAR to load throughout the supported 1.21 server family.

### API, build, and documentation

- Published the matching `com.dragonaltar:dragonaltar-api:1.4.22` Maven artifact.
  The runtime API contract remains `3.0`; this release makes no breaking public
  interface, event, model, or add-on hook changes.
- Compiled the public API and the external add-on consumer fixture against the
  minimum Paper 1.21 API so add-ons do not inherit an unnecessary 1.21.4 floor.
- Added a CI compile pass against Paper 1.21.11 while retaining the full release
  verification against the minimum Paper 1.21 API.
- Updated the README, installation guide, administrator guide, add-on development
  examples, and wiki home page to document the complete compatibility range.
- Updated add-on Maven examples to compile against the minimum 1.21 Paper API.
- Verified the compatibility build with all 98 plugin tests, the API boundary
  test, external add-on consumer compilation, formatting checks, release-JAR
  inspection, and SpotBugs analysis with no findings.

## 1.4.21 - 2026-08-08

- Added API 2.2 per-item soul-loss policies for leaving, unequipping, dropping,
  or destroying registered soul-bound equipment.
- Reconciled offline losses on join and add-on re-registration, including safe
  full-inventory and death/keep-inventory behavior without item duplication.
- Promoted the public contract to API 3.0 and replaced deprecated methods whose
  bytecode signatures exposed plugin implementation types with API-owned event,
  soul, eligibility, and cast models.
- Split `dragonaltar-api` into a standalone Maven artifact containing only
  supported `com.dragonaltar.api` types, with an API-boundary test and external
  add-on consumer fixture.
- Added the GitHub Packages publication workflow and synchronized API artifact
  versions with DragonAltar plugin releases.

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
