# Game assets

Assets are grouped by purpose:

- `characters/`: player and guard sprite sheets.
- `world/`: map tiles and the TemPad collectible.
- `ui/`: menu background, bitmap font, font source and license.
- `sounds/`: music and sound effects.

`ui/hud-atlas.png` and `ui/miss-minutes-timer-empty.png` are prepared artwork
for future HUD work; they are not loaded by the game yet.

The following assets are loaded by the current version:

| File | Usage |
| --- | --- |
| `characters/loki-classic.png` | Classic Loki |
| `characters/loki-ordinary.png` | Ordinary Loki in a TVA shirt |
| `characters/sylvie.png` | Sylvie: blonde hair, broken horn, dark outfit |
| `characters/loki-alligator.png` | Alligator with small golden horns |
| `characters/b15.png`, `characters/d90.png`, `characters/c20.png`, `characters/x5.png` | Four guard types |
| `world/interior.png` | Four 48x48 tiles: floor, alternate floor, wall, alternate wall |
| `world/tempad.png` | TemPad collectible, 48x48 |
| `ui/menu-background.png` | Main menu background |
| `ui/interface.fnt`, `ui/interface.png` | Press Start 2P pixel bitmap font |

Character sprite sheets are 192x288 RGBA. Each contains nine 64x96 frames.
Rows from top to bottom: down, up, right. Columns: idle, walk A, walk B.
The game mirrors the right-facing profile to face left. The background is transparent; the foot baseline is y=88.
CharacterAnimation selects frames based on movement; animation freezes while paused.
The clone uses the current player skin with a cyan tint and its own animation.

## Canva sources

Classic Loki is built from editable shapes:
https://www.canva.com/design/DAHaVhqU250/prtAP13o427Q_y8PBYn5og/edit
Atlas page order: 1, 5, 6, 7, 8, 9, 2, 3, 4.
PNGs were scaled down by a factor of five, and the white background was made transparent.

Guards:
- B-15: https://www.canva.com/design/DAHaatmNnsY/F4zB1WRkKTQN2KzEbNcv7Q/edit
- D-90: https://www.canva.com/design/DAHaatGvseY/h0xojDwEym_4oCZWobN_8A/edit
- C-20: https://www.canva.com/design/DAHaao8juTw/O--biCeF1Jwjn5qiMJBwsA/edit
- X-5: https://www.canva.com/design/DAHaavnakI4/6929v8_tJf5fSGvPMOQhvQ/edit
Page order: 1, 2, 3, 7, 8, 9, 4, 5, 6.

Interior and TemPad:
https://www.canva.com/design/DAHaWk6-39M/npsVpeJ0YMUslvqCra_ObQ/edit
TemPad was extracted from the third cell of the previous item atlas without changing its pixels.
GameScreen draws the terminal and portal; they have no separate textures.

## Generated images

Ordinary Loki, Alligator, the standalone Sylvie sprite, and the menu background were created with the built-in
imagegen tool. They are not hand-drawn Canva artwork. After generation, the sprites were
resized, aligned, and packed. Artwork is not generated at runtime.

Sprite prompts: match the classic sprite's coarse angular pixel style,
muted green/gold palette, dark outline and simple shading; transparent background;
exact 3x3 grid, 64:96 cell aspect, DOWN/UP/RIGHT rows, idle/walk A/walk B columns;
consistent identity and centering; no text, grid or shadows.

- Ordinary: dark-haired fictional male trickster, white rolled-sleeve office shirt,
  loose dark-green tie, brown trousers, dark boots; no horns or helmet.
- Alligator: small green quadruped, tiny golden horned crown, square eyes,
  short legs and tapered tail; rectangular pixel clusters, minimal scales.
- Sylvie: fictional blonde female Norse trickster, shoulder-length blonde hair,
  asymmetric golden headpiece with one broken horn, dark-green/black adventure
  outfit, gold belt and boots; no resemblance to a real actor.

The current menu background is pixel art: a dark emerald TVA hall, brass clocks,
branching timelines, and an amber portal on the right. The center is left clear
for the skin picker and Play button.

The interface uses [Press Start 2P](https://github.com/google/fonts/tree/main/ofl/pressstart2p)
by CodeMan38 under the SIL Open Font License (see `ui/PressStart2P-OFL.txt`).
The font is rasterized at its native 8-pixel grid and enlarged with nearest-neighbor
sampling. The FNT and PNG files load directly in LibGDX; no extra runtime libraries are required.

## Sound effects

`sounds/` contains seven original synthesized arcade effects: pickup, dash,
illusion, time stop, damage, victory, and defeat. They are mono 16-bit PCM WAV
files at 44.1 kHz, generated offline without external samples or libraries.
GameScreen plays them through LibGDX Sound and releases them in dispose().

`sounds/timeline-music.wav` is an original 32-second minor-key retro synth loop.
The dash, illusion, and time-stop effects use synthesized magical sweeps,
shimmering tones, and slowing clock ticks. These are original sounds, not
samples from the Loki soundtrack. Music streams through LibGDX Music at low
volume, pauses with gameplay, and restarts for each new attempt.
