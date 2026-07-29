# Permissions

| Permission | Purpose | Default |
|---|---|---|
| `dragonaltar.use` | Player commands and Focus controls | Everyone |
| `dragonaltar.admin` | Event, altar, ritual, soul, system, and GUI administration | Operator |
| `dragonaltar.admin.event` | Granular event administration child | Operator |
| `dragonaltar.admin.altar` | Granular altar administration child | Operator |
| `dragonaltar.admin.ritual` | Granular ritual administration child | Operator |
| `dragonaltar.admin.souls` | Granular soul administration child | Operator |
| `dragonaltar.admin.abilities` | Granular ability and energy administration child | Operator |
| `dragonaltar.admin.protection` | Granular protection administration child | Operator |
| `dragonaltar.admin.system` | Granular system administration child | Operator |
| `dragonaltar.setup` | Guided altar and fountain setup | Operator |
| `dragonaltar.developer` | Beta diagnostics, simulation, repair, backup, and reset tools | Operator |
| `dragonaltar.protection.bypass` | Per-player altar protection bypass toggle | Operator |
| `dragonaltar.eligibility.required` | Optional eligibility permission suitable for configuration | Everyone |
| `dragonaltar.eligibility.excluded` | Exclude a player from reincarnation and ritual selection | Nobody unless assigned |

`dragonaltar.admin` declares the granular administration nodes as children. Each command branch checks its corresponding child node, so LuckPerms may grant narrowly scoped access without granting the parent or GUI. Operator status does not bypass production destructive-command safety and does not permanently bypass altar protection.
