# Team: SE-2534
# Members: Ailbekov Maksat, Nurlybek Anuar
# SDP_Final

TVA mission simulator with Loki variants. Java Swing final project for SDP.

Requires JDK 21 or newer. No external libraries.

On Windows, run `run.cmd`. Or run from the project folder:

```powershell
javac -encoding UTF-8 -d out src/*.java
New-Item -ItemType Directory -Force out/assets | Out-Null
Copy-Item assets/*.png out/assets
java -cp out Main
```

## Gameplay

Choose a mission and difficulty, select Loki and equipment, then choose one action per scene.
Four variants are available: Ordinary, Classic, Alligator and Sylvie (magic 65 / combat 45 / diplomacy 55).
Equipment includes the magic amplifier, the diplomacy amulet (+15 to diplomacy) and the time shield.
After confirming an action, use Next to continue. Finish three scenes with instability at 20 or below.
Reaching 100 ends the attempt immediately. Successful missions unlock Asgard, then the Void.

The Swing interface includes headquarters, mission setup, loadout, encounters, results and archive,
with illustrated location cards, character portraits and a live instability monitor.

Completed attempts and unlocked locations are saved automatically after a mission and on exit.
The save file is `%USERPROFILE%/.loki-tva/campaign.xml`; hover over the footer to see its full path.
An unfinished mission is discarded when you confirm leaving it. It does not enter the archive.
If the save cannot be read, the game warns you and leaves that file untouched. Move the damaged
file elsewhere and restart to create a fresh campaign. Write failures are shown in the window;
you can retry on exit or explicitly choose to leave without saving the latest results.

## Design patterns

| Pattern | Classes | Use |
| --- | --- | --- |
| Builder | `Mission.Builder` | Assemble and validate mission settings |
| Factory Method | `LokiCreator` and its four creators | Create different Loki variants through an overridden method |
| Decorator | `EquipmentDecorator`, `MagicAmplifier`, `DiplomacyAmulet`, `TimeShield` | Add magic power, diplomacy and reduce failure penalties |
| Strategy | `ActionStrategy`, `NegotiateStrategy`, `IllusionStrategy`, `CombatStrategy` | Resolve actions using different hero stats |
| Facade | `MissionControlFacade` | Start missions, resolve turns, unlock locations and store results |
| Observer | `Timeline`, `TimelineListener`, `TimelineMonitor`, `EventLog` | Update the Swing monitor and event log when instability changes |

`GameWindow` sends actions to the facade. The chosen strategy reads the equipped hero's stats.
The facade changes the timeline, which notifies the monitor and log.
Gameplay calculations can run without opening a window.

## Checks

Run `check.cmd` for the headless logic and artwork checks.
Run `check.cmd --ui` to also exercise all six Swing screens, a Sylvie mission and a campaign restart.
The UI check uses its own hidden window and temporary save; renders are written to `out/ui/`.
Manual PowerShell commands:

```powershell
javac -encoding UTF-8 -Xlint:all -Werror -d out src/*.java tests/*.java
New-Item -ItemType Directory -Force out/assets | Out-Null
Copy-Item assets/*.png out/assets
java -ea -Djava.awt.headless=true -cp out GameChecks
```

The checks cover all six patterns, campaign progression, failed attempts, retry, shield penalties,
observer subscription/removal, limits 0-100, duplicate actions and immutable archived results.
They also verify the Sylvie creator, the diplomacy amulet (55 -> 70, other stats unchanged),
its combination with the amplifier and shield, and that Sylvie passes the Standard diplomacy
threshold in a scene without a modifier.
Save checks cover archive round trips, UTF-8 mission names, unlocked locations, early collapse,
discarded attempts and damaged files. `CampaignStore` uses the JDK XML Properties format and
replaces the save via a temporary file; the six gameplay patterns keep their existing roles.

For a successful first mission: Standard difficulty, Classic Loki, Magic amplifier,
then Illusion / Illusion / Negotiate. Instability: 70 -> 45 -> 20 -> 0.
