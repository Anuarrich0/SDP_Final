# Loki TVA Escape

A top-down game built with Java 21 and LibGDX 1.14.0 using Maven.
Loki must collect items, activate the TVA terminal, and escape through a portal
while avoiding four guards with Pac-Man-inspired behavior.

## Running

Install JDK 21 and Maven and add them to PATH. Open the root `pom.xml` as a Maven project.
On Windows, run this from the project root:

```powershell
.\run.cmd
```

Maven downloads dependencies on the first run. To verify the build:

```powershell
mvn clean package
```

The JAR is generated in `target/`. The game uses PNG files from `assets/`, so
launch it with `run.cmd`, which sets the working directory through Maven.

## Menu and controls

Choose Classic Loki, Ordinary Loki, Sylvie, or Alligator and press Play or Enter.
Your choice changes the appearance of the player and clone. All skins have the
same gameplay stats. The selection persists when restarting or returning to the
menu, but is not saved to disk. The HUD shows health, collected items, time, difficulty, and ability cooldowns.

| Key | Action |
| --- | --- |
| WASD / arrow keys | Choose a direction; Loki keeps moving after the key is released |
| Space | Dash; 2-second cooldown |
| E | Moving clone; 7-second cooldown |
| Q | Freeze guards for 1.5 seconds; 12-second cooldown |
| F | Activate the nearby terminal after collecting three TemPads |
| Esc | Pause, with options to resume or return to the menu |
| R | Start a new attempt |

The next turn is buffered until a passage is available. Reversing direction takes
effect immediately. Dashing follows passages and cannot pass through walls.

## How to win

Collect all small dots, four power orbs, and three TemPads on the 25x19 map.
After collecting the TemPads, approach the cyan computer marked TERMINAL and
press F. The EXIT is marked by an orange ring; its position depends on the level. It turns green
when all items are collected and the terminal is active. Enter it to win.

A power orb makes guards vulnerable for 6 seconds. Touching a vulnerable guard
sends it back to the house. Each dot awards 10 points, each orb awards 50, and
consecutive guards award 200/400/800/1600. A normal collision removes 20 health
and returns the characters to their starting positions. Collected items remain
collected. Reaching zero health ends the game in defeat.

The clone runs through passages, turns, and uses the tunnel automatically.
All normal guards temporarily chase it instead of Loki, even in SCATTER mode.
The clone cannot collect items and lasts 4.25 seconds. Vulnerable guards keep
moving randomly. Normal behavior resumes when the clone disappears.

The side tunnel connects the edges of the middle row. Guards slow down in the
three outermost cells at each end. Collision detection accounts for the wraparound.
Q lets Loki safely pass frozen guards; the release queue and mode timers also
pause.

## Guards

- B-15 chases the target's current cell.
- D-90 intercepts the target ahead of its position.
- C-20 flanks the target relative to B-15's position.
- X-5 retreats to its corner when the target is close.

SCATTER/CHASE modes alternate for 7/20/7/20/5/20/5 seconds, followed by permanent
chase mode. Power orbs pause this schedule. B-15 starts outside the house, D-90
leaves first, C-20 leaves after 30 dots, and X-5 after another 60. If no dots are
collected for 4 seconds, the next guard leaves. After losing health, shared
thresholds of 7/17/32 dots apply. Captured guards return to the house and leave
again without another wait. Loki and actively chasing guards cannot enter
through the house door.

This is a TVA adaptation rather than an arcade emulator. Health, abilities, and
objectives are retained; the original release-queue bugs and ROM random number
generator are not reproduced. Reference: [The Pac-Man Dossier](https://pacman.holenet.info/).

## Code and design patterns

| Path | Purpose |
| --- | --- |
| `src/loki/GameLauncher.java` | `main` entry point and LWJGL3 window |
| `src/loki/game/` | Menu, game screen, skins, movement, and animation |
| `src/loki/builder/Level.java` | Map Builder; the three mazes are defined in `tva(levelNumber)` |
| `src/loki/decorator/` | Decorator: `MagicAmplifier` extends clone duration; `TimeShield` reduces damage |
| `src/loki/factory/` | Factory Method: each selected skin uses its own `LokiCreator` |
| `src/loki/strategy/` | Strategy: `ChaseStrategy` and `ScatterStrategy` |
| `src/loki/facade/GameSession.java` | Facade for rules, abilities, items, and the release queue |
| `src/loki/observer/GameListener.java` | State Observer; the screen reacts to victory and defeat |
| `assets/` | Textures and font in use; sources are listed in `assets/README.md` |

Characters have a name, magic power, agility, and collision damage. Magic power
affects clone duration; agility affects speed. Factories set magic power to 75
and agility to 30. The amplifier adds 15 magic power, and the shield reduces
damage from 30 to 20. All four skin factories create identical stats, so changing
appearance does not change the difficulty.
Development history is preserved in Git. The `target/` directory is generated by the build.

Six patterns are used: Decorator, Factory Method, Builder, Strategy, Observer, and Facade.

## Difficulty progression

Complete three different mazes: Easy, Medium, then Hard. Each has its own
corridors, item positions, terminal, and exit. Normal guard
speed increases from 140 to 160 to 180. Clearing a round automatically starts
the next with fresh health, items, score, and timer. The third clear wins the game.
R and Try Again replay the current round; Play from the menu and replaying a
completed game start at Easy. The HUD shows the current round and difficulty.
