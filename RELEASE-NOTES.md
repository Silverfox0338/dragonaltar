# DragonAltar 1.4.18 Release Notes

DragonAltar brings a persistent three-soul Dragonborn storyline to Paper 1.21.4. Server staff configure a physical altar and End fountain, run one protected vanilla dragon respawn event, and release Akuma, Rev, and Lamari into a long-running cycle of rituals, inheritance, reincarnation, instability, and recovery.

The release includes distinct frost, hunt, and stone ability kits; full-energy ultimates; pair resonances; Dragon Trinity; shared persistent cooldowns; exact ritual refunds; accessibility controls; optional PlaceholderAPI, WorldEdit, and ScaledEnderDragon support; and a public Bukkit service and event API.

Version 1.4.16 also introduces the player-facing Dragon Soul history archive and the expanded destructive-command confirmation system. Administrative soul provenance remains available in private lineage metadata and audit logs while player-facing history uses neutral event descriptions.

Version 1.4.17 makes administrative custody fully private in the player history interface. Admin and developer holders are omitted from current-holder, previous-holder, caller, killer, and transfer participant displays while their identities remain available in private lineage metadata and audit records.

Version 1.4.18 adds the noncommercial DragonAltar license and API 2.0 for free,
independent add-ons. Add-ons can inspect immutable public event, ritual, soul,
eligibility, ability, energy, selection, and cooldown state; open the safe soul
history interface; and register namespaced custom abilities that remain inside
the normal DragonAltar energy and cooldown pipeline. Private staff custody and
raw lineage are not exposed through the snapshot API.

Requirements:

- Paper 1.21.4
- Java 21

Install the JAR in `plugins`, start once, complete `/dragon setup begin`, save the guided setup, and require `/dragon setup validate` to report ready before starting the official event.

Important release gates:

- Complete [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) on a live Paper server.
- Replace publishing and support placeholders before creating a listing.
