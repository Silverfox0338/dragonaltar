# Command Reference

The primary label is `/dragon`. `/dragonaltar` is an alias.

Every command first checks `dragonaltar.use`, including administrator and developer branches. The permission column below lists the additional permission.

Sender types:

- Player: must be run in game
- Either: player or console
- Mixed: console works only when the noted fallback location is already configured

An online player argument must match the player's current name exactly.

## Player commands

| Syntax | Sender | Extra permission | Purpose |
|---|---|---|---|
| `/dragon` | Either | None | Event, setup, and personal Dragonborn status |
| `/dragon status` | Either | None | Same status summary |
| `/dragon help` | Either | None | Player GUI in game, compact help on console |
| `/dragon abilities` | Either | None | Player-specific ability ids in game, whole registry on console |
| `/dragon focus` | Player | None | Restore a missing owned Focus when an inventory slot is free |
| `/dragon settings` | Player | None | Open settings GUI |
| `/dragon settings menu` | Player | None | Open settings GUI |
| `/dragon settings effects <full\|reduced\|minimal>` | Player | None | Set animation effect density |
| `/dragon settings hud <on\|off>` | Player | None | Toggle action-bar HUD and Rev Heat bar |
| `/dragon settings selector <locked\|sneak-scroll>` | Player | None | Set Focus scroll behavior |
| `/dragon settings slowfall <on\|off>` | Player | None | Toggle the Dragonborn Slow Falling passive |
| `/dragon history` | Player | None | Open public soul-history GUI |
| `/dragon history` | Console | None | Print operational soul history in chat form; keep the output private |
| `/dragon history <player>` | Either | None | Print that player's public Dragonborn history |
| `/dragon refunds` | Player | None | Retry exact-item ritual refunds |

Individual sound, title, particle, and screen-effect toggles are available in the settings GUI, not as direct subcommands.

## Confirmation commands

| Syntax | Sender | Extra permission | Purpose |
|---|---|---|---|
| `/dragon confirm <token>` | Either | Permission of original operation | Execute the sender-bound pending operation |
| `/dragon cancel` | Either | None beyond base command access | Discard the sender's pending operation |
| `/dragon event confirm-start <token>` | Player | `dragonaltar.admin.event` | Confirm the separate official event start preview |
| `/dragon dev confirm <token>` | Either | `dragonaltar.developer` | Alias for general confirmation inside an enabled developer branch |

General confirmation tokens are single-use, expire, and are rejected after relevant state changes.

## Official event commands

Additional permission: `dragonaltar.admin.event`.

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon event status` | Either | Show official event state | Read-only |
| `/dragon event preview` | Either | Run preflight and show the start summary | Read-only |
| `/dragon event start` | Player | Run preflight and issue an event-start token | No state change until `confirm-start` |
| `/dragon event confirm-start <token>` | Player | Start the protected vanilla respawn | Event-specific confirmation |
| `/dragon event abort` | Either | Mark the official event aborted and remove official crystals | General confirmation |
| `/dragon event rescan` | Either | Rescan loaded official dragons and crystals | General confirmation |
| `/dragon event recover` | Either | Choose recovery from durable souls and loaded official entities | General confirmation |
| `/dragon event locate` | Either | Show loaded canonical dragon coordinates | Read-only |
| `/dragon event dragon-info` | Either | Show canonical UUID, health, maximum health, and phase | Read-only |

## Guided setup commands

Additional permission: `dragonaltar.setup`. All setup commands are player-only.

Point commands use the player's current location when coordinates are omitted. The coordinate form is `<x> <y> <z> [yaw] [pitch]`.

| Syntax | Purpose | Safety |
|---|---|---|
| `/dragon setup begin` | Start or replace a 15-minute staging session | Staged only |
| `/dragon setup status` | Show saved setup status and staged paths | Read-only |
| `/dragon setup setaltarcenter [coordinates]` | Stage altar center | Staged only |
| `/dragon setup setritualcenter [coordinates]` | Stage ritual center | Staged only |
| `/dragon setup setegg [coordinates]` | Stage egg display anchor | Staged only |
| `/dragon setup setinteraction [coordinates]` | Stage altar interaction block | Staged only |
| `/dragon setup setarrival [coordinates]` | Stage Ascension arrival point | Staged only |
| `/dragon setup setfountain [coordinates]` | Stage End fountain center | Staged only |
| `/dragon setup setcrystal <north\|south\|east\|west> [coordinates]` | Stage one crystal site | Staged only |
| `/dragon setup setpedestal <id>` | Stage a pedestal at the current location | Staged only |
| `/dragon setup removepedestal <id>` | Stage removal of one pedestal | Staged only |
| `/dragon setup pos1 [coordinates]` | Stage internal protection corner 1 | Staged only |
| `/dragon setup pos2 [coordinates]` | Stage internal protection corner 2 | Staged only |
| `/dragon setup validate` | Show saved validation plus staged path list | Read-only |
| `/dragon setup preview` | Show staged particles and coordinates to the player | Read-only |
| `/dragon setup save` | Overwrite saved paths with staged values | General confirmation |
| `/dragon setup cancel` | Discard staged changes | Immediate, no saved change |

## Altar commands

Additional permission: `dragonaltar.admin.altar`.

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon altar status` | Either | Saved setup report and unclaimed count | Read-only |
| `/dragon altar validate` | Either | Same saved validation report | Read-only |
| `/dragon altar teleport` | Player | Teleport to saved altar center | Immediate |
| `/dragon altar preview` | Either | Play altar-awaken animation at altar center | Cosmetic |
| `/dragon altar awaken` | Either | Play awakening and force altar active | Production gate and confirmation |
| `/dragon altar activate` | Either | Force altar active and recover displays | Production gate and confirmation |
| `/dragon altar deactivate` | Either | Force altar dormant and remove displays | Production gate and confirmation |
| `/dragon altar dormancy` | Either | Alias for deactivate | Production gate and confirmation |

### Egg display

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon altar egg spawn` | Either | Preview at least one remaining soul and recover display | Immediate preview state |
| `/dragon altar egg reset` | Either | Clear preview count, reconcile, and reset transform | Immediate |
| `/dragon altar egg teleport-to-config` | Either | Same behavior as reset | Immediate |
| `/dragon altar egg remove` | Either | Remove altar egg and recipe displays | General confirmation |
| `/dragon altar egg inspect` | Either | Describe current display entities and duplicates | Read-only |
| `/dragon altar egg animate <idle\|awaken\|claim\|deplete\|animation-id>` | Either | Play a configured animation at the egg | Cosmetic |
| `/dragon altar egg count <0-3>` | Either | Set a temporary displayed remaining-soul count | Immediate preview state |

`egg remove` calls the combined display removal path, so the recipe display is removed too.

### Recipe display

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon altar recipe spawn` | Either | Clear preview override and reconcile displays | Immediate |
| `/dragon altar recipe refresh` | Either | Same behavior as spawn | Immediate |
| `/dragon altar recipe preview` | Either | Same behavior as spawn | Immediate |
| `/dragon altar recipe inspect` | Either | Describe egg, recipe, and duplicate state | Read-only |
| `/dragon altar recipe move [x y z]` | Player | Save a new offset from egg to current or exact location | General confirmation |
| `/dragon altar recipe remove` | Either | Remove recipe display only | General confirmation |

The move target must be in the egg display's world.

## Ritual commands

`status` and `inspect` require only `dragonaltar.use`. Mutations require `dragonaltar.admin.ritual`.

| Syntax | Sender | Permission | Purpose | Safety |
|---|---|---|---|---|
| `/dragon ritual status` | Either | Base only | Show active initial ritual or inactive | Read-only |
| `/dragon ritual inspect` | Either | Base only | Same active ritual output | Read-only |
| `/dragon ritual start <player>` | Either | `dragonaltar.admin.ritual` | Start the ordinary ritual pipeline for an online target | General confirmation |
| `/dragon ritual stop` | Either | `dragonaltar.admin.ritual` | Cancel active ritual and request refund | General confirmation |
| `/dragon ritual fail` | Either | `dragonaltar.admin.ritual` | Same behavior as stop | General confirmation |
| `/dragon ritual refund` | Either | `dragonaltar.admin.ritual` | Same behavior as stop | General confirmation |
| `/dragon ritual complete` | Either | `dragonaltar.admin.ritual` | Force the active ritual to complete | Production gate and confirmation |
| `/dragon ritual reserve <soul-id> <player>` | Either | `dragonaltar.admin.ritual` | Reserve an existing soul for an online player | General confirmation |
| `/dragon ritual release <soul-id>` | Either | `dragonaltar.admin.ritual` | Release a soul reservation | General confirmation |

The reserve and release commands use the persisted soul id, not the public display name.

## Soul and player administration

The bare `/dragon admin` requires `dragonaltar.admin`, is player-only, and opens the admin GUI.

All commands in this section require `dragonaltar.admin.souls`.

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon admin list` | Either | List internal soul state and holder UUIDs for trusted diagnostics | Read-only, do not publish output |
| `/dragon admin inspect <player>` | Either | Inspect an online player's held soul | Read-only |
| `/dragon admin refunds inspect <player>` | Either | Count pending exact-item refund entries | Read-only |
| `/dragon admin refunds give <player>` | Either | Retry refund delivery for an online player | Immediate |
| `/dragon admin grant <player> [soul-id]` | Either | Assign a specified soul, or first holderless soul | General confirmation |
| `/dragon admin remove <player> [Akuma\|Rev\|Lamari]` | Either | Remove the online player's held soul, optionally verifying its name | General confirmation |
| `/dragon admin transfer <from> <to>` | Either | Move the source player's held soul to an online target | General confirmation |
| `/dragon admin transfer-soul <soul-id> <to>` | Either | Assign a specific persisted soul to an online target | General confirmation |
| `/dragon admin make-pending <soul-id>` | Either | Put a soul into pending transfer | General confirmation |
| `/dragon admin reincarnate <soul-id>` | Either | Select a random eligible online recipient | General confirmation |
| `/dragon admin fix-passives <player>` | Either | Reapply current Dragonborn passive state | Immediate |
| `/dragon admin repair <player>` | Either | Reapply passives, restore Focus, clear ability cache, and fill energy | Immediate |
| `/dragon soul force-remove <player> <Akuma\|Rev\|Lamari>` | Either | Verify the named held soul, then remove it | General confirmation |
| `/dragon soul force-transfer <Akuma\|Rev\|Lamari> <from> <to>` | Either | Verify the named held soul, then transfer it | General confirmation |

The force forms are safer when a human is responding to a live incident because they refuse to run if the named soul and current source no longer match.

The persisted ids currently map as follows:

| Public name | Persisted id |
|---|---|
| Akuma | `soul-1` |
| Rev | `soul-2` |
| Lamari | `soul-3` |

Use public names in player-facing text and the persisted ids only where these administrative or developer commands require them.

## Ability administration

Additional permission: `dragonaltar.admin.abilities`. Targets must be online.

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon admin ability select <player> <ability-id>` | Either | Select an available ability for the player | Immediate |
| `/dragon admin ability cast <player> <ability-id>` | Either | Select, then use the normal cast pipeline | Immediate; normal safeguards still apply |
| `/dragon admin energy view <player>` | Either | Show current energy | Read-only |
| `/dragon admin energy fill <player>` | Either | Fill to configured maximum | Immediate |
| `/dragon admin energy set <player> <value>` | Either | Set energy through the normal bounded change path | Immediate |
| `/dragon admin cooldown view <player>` | Either | Show stored cooldown timestamps | Read-only |
| `/dragon admin cooldown clear <player> [ability-id]` | Either | Clear one ability or all cooldowns | General confirmation |

Use `all` or omit the ability id to clear all cooldowns.

## Protection commands

| Syntax | Sender | Permission | Purpose | Safety |
|---|---|---|---|---|
| `/dragon protection status` | Either | `dragonaltar.admin.protection` | Show enabled and configured status | Read-only |
| `/dragon protection validate` | Either | `dragonaltar.admin.protection` | Same status path in 1.4.19 | Read-only |
| `/dragon protection enable` | Either | `dragonaltar.admin.protection` | Enable `internal-protection.enabled` | Immediate |
| `/dragon protection disable` | Either | `dragonaltar.admin.protection` | Disable `internal-protection.enabled` | General confirmation |
| `/dragon protection setpos1` | Player | `dragonaltar.admin.protection` | Save current location as corner 1 | General confirmation |
| `/dragon protection setpos2` | Player | `dragonaltar.admin.protection` | Save current location as corner 2 | General confirmation |
| `/dragon protection visualize` | Player | `dragonaltar.admin.protection` | Draw a particle line between the saved corners | Cosmetic |
| `/dragon protection bypass` | Player | `dragonaltar.protection.bypass` | Toggle the player's session bypass | Immediate |

When `internal-protection.required-for-event` is true, the official event preflight also requires valid same-world corners. See [Altar Setup](Altar-Setup).

## System commands

Additional permission: `dragonaltar.admin.system`.

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon system status` | Either | Version, event state, and soul count | Read-only |
| `/dragon system health` | Either | Validate soul invariants and summarize config, event, and soul count | Read-only |
| `/dragon system validate` | Either | Validate all six editable configuration files | Read-only |
| `/dragon system save` | Either | Flush queued persistence writes | Immediate |
| `/dragon system reload` | Either | Reload configuration and rebuild services | Can disable plugin on validation failure |
| `/dragon system integrations` | Either | List enabled optional plugin names | Read-only |
| `/dragon system tasks` | Either | Show central task service presence | Read-only |
| `/dragon system entities` | Either | Count loaded displays, End Crystals, and Ender Dragons | Read-only |
| `/dragon system cleanup` | Either | Remove test dragons and reconcile displays | General confirmation |
| `/dragon system version` | Either | Print plugin name and version | Read-only |

## Developer commands

Additional permission: `dragonaltar.developer`.

The whole branch is available only when:

- `server-mode: BETA` and `developer.enabled-in-beta: true`, or
- Production destructive commands have been explicitly enabled

Without the production override, even developer diagnostics are refused in Production.

### Developer event

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon dev event start-force` | Player | Spawn an isolated direct test dragon | Immediate test entity |
| `/dragon dev event spawn-direct` | Player | Spawn an isolated direct test dragon | Immediate test entity |
| `/dragon dev event spawn-vanilla` | Either | Start an isolated vanilla test respawn at saved sites | Immediate test sequence |
| `/dragon dev event simulate-spawn` | Player | Play visual spawn particles | Cosmetic |
| `/dragon dev event simulate-death` | Mixed | Play visual death and awakening only | Cosmetic; player fallback if altar center is absent |
| `/dragon dev event simulate-sed-kill` | Mixed | Play visual ScaledEnderDragon kill handling | Cosmetic; player fallback if altar center is absent |
| `/dragon dev event promote-test-dragon` | Player | Promote a nearby tagged test dragon to canonical | General confirmation |
| `/dragon dev event clear-crystals` | Either | Remove official and test event crystals | General confirmation |
| `/dragon dev event clear-dragons` | Either | Remove tagged test dragons | General confirmation |
| `/dragon dev event dump` | Either | Show event state, session UUID, and dragon UUID | Read-only, do not publish output |

### Developer altar and ritual

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon dev altar displays` | Either | Inspect plugin display state | Read-only |
| `/dragon dev altar remove-duplicates` | Either | Reconcile saved egg and recipe displays | Immediate |
| `/dragon dev altar repair-displays` | Either | Same reconciliation path | Immediate |
| `/dragon dev ritual recipe-check <player>` | Either | Show offering plan for online player | Read-only |
| `/dragon dev ritual recipe-plan <player>` | Either | Same offering plan path | Read-only |
| `/dragon dev ritual recipe-consume <player>` | Either | Consume a diagnostic plan | General confirmation |
| `/dragon dev ritual recipe-refund <player>` | Either | Retry pending refund | Immediate |
| `/dragon dev ritual give-recipe <player>` | Either | Add configured recipe materials to inventory | Immediate mutation |
| `/dragon dev ritual test-elytra <player>` | Either | Explain acceptable Elytra candidates and selection | Read-only |

### Developer eligibility

| Syntax | Sender | Purpose |
|---|---|---|
| `/dragon dev eligibility check <player>` | Either | Show all eligibility booleans for an online player |
| `/dragon dev eligibility explain <player>` | Either | Same result as check |
| `/dragon dev eligibility list` | Either | List eligible online players |
| `/dragon dev eligibility choose` | Either | Randomly choose and print one eligible online player without assigning a soul |

### Developer soul

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon dev soul list` | Either | Validate and list internal soul state | Read-only, do not publish output |
| `/dragon dev soul duplicate-check` | Either | Same validation and listing path | Read-only |
| `/dragon dev soul dump <soul-id>` | Either | Print restricted internal soul diagnostics | Keep the output private |
| `/dragon dev soul create <soul-id>` | Either | Create a missing canonical soul | Immediate mutation |
| `/dragon dev soul delete <soul-id>` | Either | Delete a soul record | General confirmation |
| `/dragon dev soul assign <soul-id> <player>` | Either | Assign a soul to an online player | General confirmation |
| `/dragon dev soul unassign <soul-id>` | Either | Put a soul into pending state | General confirmation |
| `/dragon dev soul setstate <soul-id> <state>` | Either | Force an allowed internal state | General confirmation |
| `/dragon dev soul repair` | Either | Run conservative soul repair | Immediate |

Allowed `setstate` values handled by the service are `UNCLAIMED`, `TRANSFER_PENDING`, `FRACTURED`, `MOTHER_SOUL_LIMBO`, `DISABLED`, and `UNCREATED`. Held and reserved states require their dedicated assignment or reservation paths.

### Developer animation and input

| Syntax | Sender | Purpose |
|---|---|---|
| `/dragon dev animation list` | Either | List configured animation ids |
| `/dragon dev animation play <id> [player]` | Mixed | Play at target player or saved altar center |
| `/dragon dev animation stop` | Player | Stop the last tracked preview session for that player |
| `/dragon dev animation <altar-awaken\|egg-idle\|egg-claim\|egg-deplete\|soul-depart\|soul-arrive\|ritual-start\|ritual-complete> [player]` | Player or explicit target | Play a named preview |
| `/dragon dev animation pvp-transfer <victim> <killer>` | Either | Play departure and PvP transfer previews |
| `/dragon dev animation natural-transfer <victim> <recipient>` | Either | Play departure and natural transfer previews |
| `/dragon dev input status <player>` | Either | Show selected ability and energy |
| `/dragon dev input simulate-scroll <player> [up]` | Either | Cycle ability, backward only when final argument is `up` |
| `/dragon dev input simulate-cast <player>` | Either | Use normal cast pipeline |
| `/dragon dev input simulate-swap <player>` | Either | Cycle ability category |
| `/dragon dev input reset <player>` | Either | Attempt to select Wings |

### Developer data and performance

| Syntax | Sender | Purpose | Safety |
|---|---|---|---|
| `/dragon dev data backup` | Either | Copy known runtime data to a timestamped backup | Immediate |
| `/dragon dev data restore <name>` | Either | Restore known files from a direct named backup | General confirmation |
| `/dragon dev data dump` | Either | Show event, soul, and backup diagnostic summary | Restricted output |
| `/dragon dev data save` | Either | Flush persistence queue | Immediate |
| `/dragon dev data validate` | Either | Validate soul invariants | Read-only |
| `/dragon dev data reload` | Either | Reload runtime data from disk | Immediate, operationally sensitive |
| `/dragon dev data dump-player <player>` | Either | Print settings, soul, and energy | Restricted output |
| `/dragon dev data dump-soul <soul-id>` | Either | Print internal soul object | Restricted output |
| `/dragon dev data clear-cache` | Either | Clear ability caches | Immediate |
| `/dragon dev perf <status\|particles\|tasks\|entities>` | Either | Print the same online, entity, and display count summary | Read-only |

### Developer resets

All reset commands require general confirmation.

| Syntax | Result |
|---|---|
| `/dragon dev reset souls` | Delete all three soul records and holder bindings |
| `/dragon dev reset event` | Reset official event state and event crystals for Beta |
| `/dragon dev reset players` | Clear settings, energy, selections, cooldowns, and player history |
| `/dragon dev reset altar` | Delete saved altar, fountain, crystal, pedestal, and protection locations |
| `/dragon dev reset history` | Delete soul and player Dragonborn history |
| `/dragon dev reset everything` | Reset souls, consequences, players, ability caches, event, and altar configuration |

> Reset commands are not recovery shortcuts. Take a matching full backup first. Several reset results can be undone only by restoring that backup.

See [Administrator Guide](Administrator-Guide), [Permissions](Permissions), and [Troubleshooting](Troubleshooting).
