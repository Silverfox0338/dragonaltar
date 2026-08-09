# DragonAltar 1.4.22 Release Notes

DragonAltar brings a persistent three-soul Dragonborn storyline to Paper and Purpur 1.21 through 1.21.11. Server staff configure a physical altar and End fountain, run one protected vanilla dragon respawn event, and release Akuma, Rev, and Lamari into a long-running cycle of rituals, inheritance, reincarnation, instability, and recovery.

The release includes distinct frost, hunt, and stone ability kits; full-energy ultimates; pair resonances; Dragon Trinity; shared persistent cooldowns; exact ritual refunds; accessibility controls; optional PlaceholderAPI and ScaledEnderDragon support; and a public Bukkit service and event API.

Version 1.4.16 introduced the player-facing Dragon Soul history archive and the expanded destructive-command confirmation system. Administrative soul provenance remains available in private lineage metadata and audit logs while player-facing history uses neutral event descriptions.

Version 1.4.17 made administrative custody fully private in the player history interface. Admin and developer holders are omitted from current-holder, previous-holder, caller, killer, and transfer participant displays while their identities remain available in private lineage metadata and audit records.

Version 1.4.18 added the noncommercial DragonAltar license and API 2.0 for free,
independent add-ons. Add-ons can inspect immutable public event, ritual, soul,
eligibility, ability, energy, selection, and cooldown state; open the safe soul
history interface; and register namespaced custom abilities that remain inside
the normal DragonAltar energy and cooldown pipeline. Private staff custody and
raw lineage are not exposed through the snapshot API.

## What changed in 1.4.22

The plugin now supports the complete Paper and Purpur 1.21 release line on Java
21. It compiles against the minimum Paper 1.21 API and resolves both generations
of Bukkit attribute names used across these releases. CI also compiles against
Paper 1.21.11 to protect both ends of the supported range.

The matching `com.dragonaltar:dragonaltar-api:1.4.22` artifact is published for
add-on developers. The independent runtime API contract remains `3.0`, with no
breaking public interface, event, model, or add-on hook changes in this release.

## Earlier 1.4.21 changes

API 2.2 adds `DragonAddonItem.onSoulLoss()` with `NONE`, `UNEQUIP`, `DROP`, and
`DESTROY` policies. DragonAltar safely handles online and offline soul loss,
full inventories, player death, keep-inventory servers, and item definitions
returning after an add-on reload.

## Earlier 1.4.20 changes

API 2.1 adds registered soul-bound add-on items, a shared
`dragonaltar:soul_bound_item` marker, central ownership and callback checks for
all six player equipment slots, and the cancellable
`DragonAddonItemEquipEvent`. Add-ons can react to later soul movement through
the soul ids already carried by transfer and Dragonborn loss events.

## Earlier 1.4.19 changes

This release removes settings and internal helpers that were left behind after
their features changed. WorldEdit and schematic support are no longer declared
because DragonAltar does not use them. Inactive ability sections, unused display
settings, old Focus switches, Elytra durability options, unused messages, and
dead helper classes are also gone.

Internal altar protection can now be required before the Ancient Dragon event
starts. Existing servers that used the former protection switch are migrated to
the active setting.

Player and staff messages now appear in the situations they describe. This
includes player-only commands, incomplete setup, event start, event recovery,
and ritual start failures. The Mother Soul ritual now takes its caller and
weakness-potion counts directly from the caller-pad layout.

Event recovery now uses the same decision rules covered by the automated tests.
The compatibility soul-transfer event also fires from the real transfer path
and can cancel the transfer.

Requirements:

- Paper or Purpur 1.21-1.21.11
- Java 21

Install the JAR in `plugins`, start once, complete `/dragon setup begin`, save the guided setup, and require `/dragon setup validate` to report ready before starting the official event.

Important release gates:

- Complete live validation on the intended Paper or Purpur 1.21.x server with
  Java 21 before production use.
