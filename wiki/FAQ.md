# Frequently Asked Questions

## Player questions

### How many Dragonborn can exist?

Three. Akuma, Rev, and Lamari are the only canonical souls, and one player cannot hold more than one.

### Can I choose which soul the initial ritual gives me?

No. The initial ritual reserves the next unclaimed soul. The target-selection GUI belongs to the separate Mother Soul removal ritual.

### Can I keep my Focus in an Ender Chest?

No. The Focus is owner-bound and protected from storage, trade, sale, dropping, and duplication. Run `/dragon focus` with one free inventory slot if it is missing.

### Does Minimal effects make abilities weaker?

No. It changes presentation only. Damage, defense, durations, energy, cooldowns, targeting, and ritual results stay the same.

### Why did scrolling stop changing my hotbar slot?

The default selector mode is Locked. While holding the Focus, scrolling cycles abilities and cancels the slot change. Use `/dragon settings selector sneak-scroll` if you want normal scrolling unless sneaking.

### Why can I not cast Lamari's Fault?

It requires Bukkit's flying state. Use Wings first or another legitimate source of flight. Elytra gliding alone is not the same state.

### Why can I not kill a Fractured Soul?

Current Dragonborn are blocked from landing the killing blow. A non-Dragonborn player must finish it.

### Where are my interrupted ritual items?

Free inventory space and run `/dragon refunds`. Overflow stays pending safely.

### What does Dormant mean?

It is the privacy-safe public label for a soul without a public holder that is waiting, reserved, choosing, silenced, not yet awakened, or otherwise unavailable. It does not reveal internal maintenance context.

## Administrator questions

### Does DragonAltar build the altar?

No. Build the physical altar first, then record its locations through the guided setup.

### Is the altar egg a real dragon egg block?

No. It is a persistent `BlockDisplay`. Players cannot pick it up.

### Why does the event say internal protection is required?

When `internal-protection.required-for-event` is true, protection must be enabled and both cuboid corners must be configured in the same world. Use `/dragon protection enable`, `setpos1`, and `setpos2`, then run `/dragon event preview` again.

### Can the console start the official event?

It can preview it, but `/dragon event start` and `confirm-start` are player-only.

### Can the console run administrative commands?

Most can. Commands that need the sender's world, location, GUI, or personal preview are player-only. The [Commands](Commands) page marks every sender type.

### Do operators bypass production safety?

No. Forced state changes and the entire developer branch remain blocked in Production unless the explicit destructive override is enabled.

### Do confirmation tokens survive state changes?

No. A token is rejected if relevant state differs from the preview. Run the original command again and review a fresh preview.

### Is pending transfer data loss?

No. Pending souls are retained and can be assigned after the countdown, on an eligible join, or during startup recovery.

### Can I edit `souls.yml` to fix a holder?

Do not edit runtime data while Paper is running. Prefer supported repair and confirmed transfer commands. For offline data repair, stop the server, take a full backup, and validate the entire connected state afterward.

### What should I back up?

The whole `plugins/DragonAltar` directory while Paper is stopped, plus the matching plugin JAR. Restoring only one data file or only the JAR can leave incompatible state.

## Server owner questions

### Does DragonAltar contact the internet?

No. It has no telemetry, analytics, update checker, crash upload, or outbound service call.

### Can I sell access to DragonAltar features?

Not under the included license. Paid, premium, subscription, paywalled, or purchase-gated DragonAltar features require separate written permission from Silverfox0338.

### Can a server accept donations?

The repository license allows genuinely voluntary donations only when payment gives no DragonAltar feature, content, access, priority, advantage, or other benefit.

### Can I use a free independent add-on?

Yes, if it follows the published API and all license conditions, remains free, includes the required notice, does not bundle DragonAltar, and does not imply endorsement.

DragonAltar is owned by Silverfox0338.

## Add-on developer questions

### What API version ships with DragonAltar 1.4.22?

API contract `3.0`. The Maven artifact version matches the plugin release:
`com.dragonaltar:dragonaltar-api:1.4.22`.

### How do I get the API?

Add the GitHub Packages repository and the provided Maven dependency
`com.dragonaltar:dragonaltar-api:1.4.22`. Configure `~/.m2/settings.xml` with a
classic GitHub token carrying `read:packages`, declare `depend: [DragonAltar]`,
and load `DragonAltarApi.class` from Bukkit's `ServicesManager` during
`onEnable`. See [Add-on Development](Add-on-Development#maven-setup) for the
copy-paste configuration.

### Can I use `DragonAltarPlugin` or `DragonAltarApiImpl`?

No. Use the `DragonAltarApi` service interface and documented `com.dragonaltar.api` records, add-on hooks, and events.

### Can I read DragonAltar YAML directly?

No. Runtime files are not an API, can change, and contain information that is not suitable for public output.

### When are energy and cooldown charged for a custom ability?

Only after `canUse` and `activate` return success. An exception or failed result does not charge them.

### Can my custom ability be an ultimate?

Yes. Override `ultimate()` to return true. The cast then requires a full bar and uses the shared persistent ultimate cooldown.

### Are custom callbacks asynchronous?

No. They run on the server thread. Keep them fast and schedule slow work properly.

### Can I charge for my add-on?

No. The add-on and every DragonAltar-related feature must remain free. Paid or premium DragonAltar features are prohibited.

See [Add-on Development](Add-on-Development) and [API Reference](API-Reference).
