# Privacy

DragonAltar does not collect telemetry, analytics, advertising identifiers, crash reports, or usage statistics. It does not include an update checker and does not contact external web services.

The plugin stores data locally in the Paper server's `plugins/DragonAltar` directory. Stored data can include:

- Player UUIDs
- Player accessibility settings
- Dragon Energy, selected ability, and cooldown timestamps
- Dragonborn gain and loss history
- Soul ownership, lineage, reservation, transfer, limbo, and recovery state
- Altar, event, ritual, refund, and Fractured Soul state
- Administrative audit records

Optional integrations use APIs already exposed inside the running Paper server. DragonAltar does not send their data off-server.

Server operators control retention, backups, access, and disclosure of these local files. Before sharing logs or data for support, remove player-identifying information, server paths, and unrelated secrets.
