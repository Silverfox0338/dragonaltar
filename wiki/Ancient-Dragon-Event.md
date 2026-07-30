# The Ancient Dragon Event

The official event uses Paper's vanilla Ender Dragon respawn API. DragonAltar places and protects four event crystals, starts the real respawn sequence, identifies the resulting dragon, and follows that one canonical dragon through death and altar awakening.

Test dragons created by developer commands are separately tagged. They cannot awaken the official altar unless staff deliberately promote one through a confirmed developer operation.

## Before starting

Run:

```text
/dragon event preview
```

The preview shows the configured world and fountain, nearby player count, ScaledEnderDragon detection, altar validation, protection status, the three eventual souls, and every failed preflight check.

The official start is player-only:

```text
/dragon event start
/dragon event confirm-start <token>
```

This start token is separate from the general `/dragon confirm <token>` system.

## Start preflight

The start is refused if any of these conditions is true:

- The event is not in its untouched start state
- Any Dragon Soul already exists
- Saved altar setup is invalid
- The fountain is missing or not a valid End fountain
- The four crystal points are missing or invalid
- Internal protection is required but disabled or not configured with same-world corners
- An Ender Dragon battle is already running
- A respawn sequence is already in progress
- A dragon is already alive in the fountain world
- A crystal site is occupied
- Another plugin cancels the prepare event

The `event.nearby-player-radius` value is informational for the preview and nearby broadcast context. It does not replace fountain validation.

## Lifecycle

| State | Meaning |
|---|---|
| `NOT_STARTED` | The official event has not begun |
| `PREPARING` | Preflight passed and the start is being prepared |
| `SUMMONING` | Four protected crystals are driving the vanilla respawn |
| `ACTIVE` | The canonical Ancient Dragon is alive |
| `DEATH_SEQUENCE` | Its death was accepted and delayed reward handling began |
| `DEFEATED` | The canonical dragon is dead |
| `ALTAR_AWAKENING` | The altar awakening delay and animation are running |
| `ALTAR_ACTIVE` | The altar is open and unclaimed souls are available |
| `COMPLETED` | All initial souls have been claimed and the altar is dormant |
| `ABORTED` | The official run was deliberately or safely stopped |
| `RECOVERY_REQUIRED` | Persisted state and loaded entities need a staff decision |

The normal path is:

```text
NOT_STARTED
PREPARING
SUMMONING
ACTIVE
DEATH_SEQUENCE
DEFEATED
ALTAR_AWAKENING
ALTAR_ACTIVE
COMPLETED
```

## Canonical dragon tracking

DragonAltar accepts a naturally spawned default dragon only when it appears:

- In the configured fountain world
- Within the configured event radius on x and z
- During the official summoning window

It tags the accepted dragon with the official event session and stores its UUID. Deaths from other dragons do not progress the event.

Use:

```text
/dragon event dragon-info
/dragon event locate
```

`dragon-info` reports the loaded canonical dragon's UUID, health, maximum health, and phase. `locate` reports its loaded block coordinates.

## Dragon death and awakening

When the canonical dragon dies, DragonAltar:

1. Records how completion was observed.
2. Waits `event.scaled-dragon-reward-delay-ticks`, 40 ticks by default.
3. Marks the event defeated.
4. Waits `event.altar-awakening-delay-ticks`, 80 ticks by default.
5. Creates Akuma, Rev, and Lamari as unclaimed souls if they do not already exist.
6. Awakens the altar and restores its displays.

The event becomes complete after all three initial souls have holders. The altar then becomes dormant and its claim displays disappear.

## ScaledEnderDragon

DragonAltar still starts a vanilla respawn so ScaledEnderDragon can observe and scale the dragon normally. If DragonAltar sees an `sed kill` command within 10 seconds before the canonical death callback, it records that completion method; otherwise it records combat.

Remove obtainable dragon egg rewards from ScaledEnderDragon's rewards configuration. DragonAltar's altar egg is visual and must remain distinct from reward items.

## Restart recovery

On load, unsafe mid-transition states are not guessed:

- `PREPARING`, `SUMMONING`, and `DEATH_SEQUENCE` enter recovery-required handling
- `ACTIVE` without the stored canonical dragon also requires recovery
- Defeated or awakening states resume their delayed altar path

`/dragon event recover` evaluates durable soul state and loaded official entities:

| Evidence found | Recovery result |
|---|---|
| One or more Dragon Souls | Restore the altar; active if claims remain, dormant if none remain |
| Exactly one matching official dragon | Restore `ACTIVE` |
| Exactly four matching event crystals | Resume summoning |
| Multiple matching dragons | Require manual repair |
| One to three matching event crystals | Require manual repair |
| No souls, dragon, or crystals | Abort safely |

`/dragon event rescan` and `/dragon event recover` both use the general destructive confirmation system because they may rewrite canonical identity or event state.

Do not clear entities first and hope recovery guesses correctly. Inspect with:

```text
/dragon event status
/dragon event dragon-info
/dragon event locate
/dragon system entities
/dragon system health
```

Then see [Troubleshooting](Troubleshooting) for a safe decision order.
