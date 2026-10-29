# Team: SE-2534
# Members: Ailbekov Maksat, Nurlybek Anuar
# SDP_Final

TVA mission simulator with Loki variants. Java Swing final project for SDP.

Requires JDK 21 or newer. No external libraries.

On Windows, run `run.cmd`. Or run from the project folder:

```sh
javac -encoding UTF-8 -d out src/*.java
java -cp out Main
```

## Gameplay

Choose a mission and difficulty, select Loki and equipment, then choose one action per scene.
After confirming an action, use Next to continue. Finish three scenes with instability at 20 or below.
Reaching 100 ends the attempt immediately. Successful missions unlock Asgard, then the Void.

The first Swing interface includes headquarters, mission setup, loadout, encounters, results and archive.
It currently uses text and controls; character art and scene backgrounds are not added yet.
Progress and the archive last until the application closes.

## Design patterns

| Pattern | Classes | Use |
| --- | --- | --- |
| Builder | `Mission.Builder` | Assemble and validate mission settings |
| Factory Method | `LokiCreator` and its three creators | Create different Loki variants through an overridden method |
| Decorator | `EquipmentDecorator`, `MagicAmplifier`, `TimeShield` | Add magic power and reduce failure penalties |
| Strategy | `ActionStrategy`, `NegotiateStrategy`, `IllusionStrategy`, `CombatStrategy` | Resolve actions using different hero stats |
| Facade | `MissionControlFacade` | Start missions, resolve turns, unlock locations and store results |
| Observer | `Timeline`, `TimelineListener`, `TimelineMonitor`, `EventLog` | Update the Swing monitor and event log when instability changes |

`GameWindow` sends actions to the facade. The chosen strategy reads the equipped hero's stats.
The facade changes the timeline, which notifies the monitor and log.
Gameplay calculations can run without opening a window.

## Checks

Run `check.cmd`, or:

```sh
javac -encoding UTF-8 -Xlint:all -Werror -d out src/*.java tests/*.java
java -ea -Djava.awt.headless=true -cp out GameChecks
```

The checks cover all six patterns, campaign progression, failed attempts, retry, shield penalties,
observer subscription/removal, limits 0-100, duplicate actions and immutable archived results.

For a successful first mission: Standard difficulty, Classic Loki, Magic amplifier,
then Illusion / Illusion / Negotiate. Instability: 70 -> 45 -> 20 -> 0.
