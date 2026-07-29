# Release Checklist

## Legal and listing

- [ ] Intended license selected and `LICENSE` added
- [ ] Author or organization confirmed
- [ ] Project, source, issue, and support URLs confirmed
- [ ] Publishing placeholders removed
- [ ] Privacy statement reviewed

## Clean installation

- [ ] Paper 1.21.4 starts with Java 21
- [ ] DragonAltar 1.4.16 enables without warnings or exceptions
- [ ] Six editable YAML files and all runtime directories are created
- [ ] No soul, event crystal, altar display, or task appears before setup requires it
- [ ] `/dragon setup begin`, save, preview, and validation work

## Upgrade and rollback

- [ ] Existing customized YAML values survive the upgrade
- [ ] Missing defaults merge and version fields advance
- [ ] Rev and resonance migrations preserve customized values
- [ ] Soul ownership, lineage, cooldowns, pending transfers, refunds, and consequences survive restart
- [ ] A stopped full-directory backup restores successfully with the previous JAR

## Commands and permissions

- [ ] Every player command works with `dragonaltar.use`
- [ ] Every administrative branch checks its documented child permission
- [ ] Setup and developer permissions behave as documented
- [ ] Destructive confirmation tokens reject wrong sender, operation, arguments, expiry, and reuse
- [ ] Production mode blocks destructive developer actions by default

## Ancient Dragon and altar

- [ ] Fountain and four cardinal crystal positions validate
- [ ] Vanilla respawn beams and canonical dragon tracking work
- [ ] ScaledEnderDragon timing and rewards work when installed
- [ ] Canonical death creates exactly Akuma, Rev, and Lamari
- [ ] Egg and recipe displays recover without duplication
- [ ] Shutdown and restart leave no unwanted displays or crystals

## Rituals, transfer, and recovery

- [ ] Initial recipe selection and exact serialized refunds work
- [ ] Full-inventory refunds persist and `/dragon refunds` retrieves them
- [ ] Forced removal validates structure, callers, offerings, target, and recipient
- [ ] PvP, natural death, Dragonborn-killer, logout, and pending-join transfers work
- [ ] Restart during event, ritual, transfer, limbo, and fracture recovers safely
- [ ] Instability, Fractured Soul, backfire, and limbo behavior matches configuration

## Abilities and resonances

- [ ] Akuma passives, Trail, Hush, Absolute Zero, Brittle, and Shatter work
- [ ] Rev passives, Heat, Hunt, Inferno Mark, Rampage, Rend recast, Wrath, Inferno's Wrath, and Predator's Claim work
- [ ] Lamari passives, Fault, Reckoning, Bulwark, charge, and reflection work
- [ ] Wings and Roar work for all three
- [ ] Shared ultimate cooldown persists through restart
- [ ] Thermal Convergence, Volcanic Aegis, Glacial Bastion, and Dragon Trinity work
- [ ] Shared resonance cooldown applies to all participants and persists through restart
- [ ] No ability penetrates walls, targets spectators, or creates a real explosion
- [ ] Temporary terrain, displays, combat state, flight, and tasks clean up on death, logout, soul loss, reload, and disable

## Accessibility and performance

- [ ] HUD, full, reduced, and minimal effects work independently
- [ ] Sounds, titles, screen effects, passive particles, and animation particles respect settings
- [ ] Essential Rev state remains readable with minimal effects
- [ ] Target and particle caps hold in dense multiplayer testing
- [ ] No permanent task, UUID cache growth, display leak, entity leak, or temporary-block leak is observed
- [ ] Tick time remains acceptable during every ultimate and resonance

## Artifact

- [ ] `mvn clean package` succeeds
- [ ] Every automated test passes
- [ ] JAR metadata reports DragonAltar 1.4.16, API 1.21, and the correct main class
- [ ] JAR contains only plugin classes and required resources
- [ ] Paper, WorldEdit, PlaceholderAPI, and ScaledEnderDragon classes are not bundled
- [ ] SHA-256 checksum recorded and matches the delivered JAR
