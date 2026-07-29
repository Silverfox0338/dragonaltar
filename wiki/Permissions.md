# Permissions

Every `/dragon` command begins with `dragonaltar.use`. Granting a granular administrator node without `dragonaltar.use` will still leave the command blocked.

## Complete table

| Permission | Default | Parent | Purpose |
|---|---|---|---|
| `dragonaltar.use` | Everyone | None | Player commands, Focus controls, altar interaction, and the base gate for all staff commands |
| `dragonaltar.admin` | Operators | None | Bare admin GUI and parent for the seven granular admin nodes |
| `dragonaltar.admin.event` | Operators | `dragonaltar.admin` | Official event preview, start, inspection, abort, rescan, and recovery |
| `dragonaltar.admin.altar` | Operators | `dragonaltar.admin` | Altar state, teleport, preview, egg display, and recipe display |
| `dragonaltar.admin.ritual` | Operators | `dragonaltar.admin` | Initial ritual mutation, reservation, release, cancellation, and forced completion |
| `dragonaltar.admin.souls` | Operators | `dragonaltar.admin` | Soul ownership, transfer, reincarnation, repair, and refund administration |
| `dragonaltar.admin.abilities` | Operators | `dragonaltar.admin` | Ability selection and casting, energy, and cooldown administration |
| `dragonaltar.admin.protection` | Operators | `dragonaltar.admin` | Optional internal protection status, corners, and switch commands |
| `dragonaltar.admin.system` | Operators | `dragonaltar.admin` | Validation, health, save, reload, integrations, task, entity, cleanup, and version commands |
| `dragonaltar.setup` | Operators | None | Guided setup staging and save |
| `dragonaltar.developer` | Operators | None | Beta diagnostics, test entities, direct data tools, backups, repair, and resets |
| `dragonaltar.protection.bypass` | Operators | None | Toggle a personal session bypass for the internal protected region |
| `dragonaltar.eligibility.required` | Everyone | None | Ready-made permission name that can be placed in `eligibility.required-permission` |
| `dragonaltar.eligibility.excluded` | Nobody | None | Exclude a player from automated eligibility and ritual selection |

`dragonaltar.admin` declares all seven `dragonaltar.admin.*` nodes as children with value `true`. It does not include setup, developer access, or the protection bypass.

The granular nodes also default to operator independently. If you use a permission manager, set explicit values for non-operator groups rather than relying on operator status.

## LuckPerms examples

Grant ordinary command and Focus access:

```text
/lp group default permission set dragonaltar.use true
```

Create an event operator without soul or developer access:

```text
/lp group eventoperator permission set dragonaltar.use true
/lp group eventoperator permission set dragonaltar.admin.event true
/lp group eventoperator permission set dragonaltar.admin.system true
```

Create a setup builder:

```text
/lp group altarbuilder permission set dragonaltar.use true
/lp group altarbuilder permission set dragonaltar.setup true
/lp group altarbuilder permission set dragonaltar.admin.altar true
```

Allow a trusted builder to bypass internal protection:

```text
/lp user <player> permission set dragonaltar.use true
/lp user <player> permission set dragonaltar.protection.bypass true
```

Exclude a player from becoming a random or ritual-selected holder:

```text
/lp user <player> permission set dragonaltar.eligibility.excluded true
```

Make a custom required eligibility node:

```yaml
eligibility:
  required-permission: myserver.dragonaltar.eligible
```

```text
/lp group member permission set myserver.dragonaltar.eligible true
```

The shipped `dragonaltar.eligibility.required` node does nothing by itself because `eligibility.required-permission` is blank by default.

## Recommended separation

- Give players only `dragonaltar.use`
- Give routine event staff `admin.event` and `admin.system`
- Give altar builders `setup` and `admin.altar`
- Restrict `admin.souls` to people authorized to change persistent holders
- Restrict `developer` to server owners during Beta testing or repair
- Grant `protection.bypass` per person, not to a broad staff group
- Keep `dragonaltar.eligibility.excluded` explicit and reviewable

Permission does not bypass production safety, confirmation tokens, ordinary eligibility checks, or Focus ownership.

See [Commands](Commands) for the exact branch mapping.
