# Commands

The primary command is `/dragon`; `/dragonaltar` is an alias.

## Players

`status`, `history [player]`, `abilities`, `focus`, `settings`, `settings effects <full|reduced|minimal>`, `settings hud <on|off>`, `settings selector <locked|sneak-scroll>`, `settings slowfall <on|off>`, and `help`.

`/dragon settings` opens the accessibility menu. `/dragon help` opens the player help menu. Staff can run `/dragon admin repair <player>` to reapply passives, restore a missing Focus, clear stale ability caches, and refill energy.

## Setup and operations

- `setup begin|status|setegg|setinteraction|setritualcenter|setarrival|setfountain|validate|preview|save|cancel`
- Setup point commands accept optional exact coordinates: `setegg|setinteraction|setritualcenter|setarrival|setfountain <x> <y> <z> [yaw] [pitch]`. Crystal points use `setcrystal <north|south|east|west> [x y z [yaw] [pitch]]`. Without coordinates, the player's current location is recorded.
- `event status|preview|start|confirm-start|abort|recover|rescan|locate|dragon-info`
- `altar status|awaken|dormancy|activate|deactivate|preview|validate|teleport`; `altar egg spawn|remove|reset|teleport-to-config|inspect|animate`; `altar recipe spawn|remove|refresh|inspect|preview|move [x y z]`
- `altar recipe move` places the floating recipe display at your exact current location. Optional `x y z` coordinates place it precisely in your current world. The new offset from the egg is saved immediately.
- `refunds`; `admin refunds inspect|give <player>`
- `dev altar displays|remove-duplicates|repair-displays`; `dev ritual recipe-check|recipe-plan|recipe-consume|recipe-refund|give-recipe|test-elytra <player>`
- `ritual status|inspect|start|stop|complete|fail|refund|reserve|release`
- `protection status|enable|disable|bypass|setpos1|setpos2|validate|visualize` (optional internal-protection utility only)
- `system status|health|validate|save|reload|integrations|tasks|entities|cleanup|version`

## Administration and development

`admin list|inspect|grant|remove|transfer|transfer-soul|reincarnate|make-pending|fix-passives` plus the `ability`, `cooldown`, and `energy` branches.

The `/dragon dev` tree contains isolated direct/vanilla test dragons, animation previews, soul repair, eligibility explanation, input simulation, data backup/restore, performance inspection, and reset branches. Test dragons carry PDC tags and cannot awaken the official altar unless an administrator explicitly confirms `promote-test-dragon`. Production mode blocks developer mutations unless `safety.allow-destructive-commands-in-production` is explicitly enabled.

Destructive operations issue sender-bound, operation-bound, argument-bound, expiring, single-use tokens. Confirm with `/dragon confirm <token>`; `/dragon dev confirm <token>` remains accepted for developer workflows.

Commands displayed here describe the command contract. Vanilla respawn recognition, ScaledEnderDragon timing, and entity animation behavior still require the live-server checklist before production.
