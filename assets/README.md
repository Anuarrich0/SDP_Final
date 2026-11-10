# Game assets

This directory contains the assets loaded by the current version.

| File | Usage |
| --- | --- |
| `loki-classic.png` | Classic Loki |
| `loki-ordinary.png` | Ordinary Loki in a TVA shirt |
| `sylvie.png` | Sylvie: blonde hair, broken horn, dark outfit |
| `loki-alligator.png` | Alligator with small golden horns |
| `b15.png`, `d90.png`, `c20.png`, `x5.png` | Four guard types |
| `interior.png` | Four 48×48 tiles: floor, alternate floor, wall, alternate wall |
| `tempad.png` | TemPad collectible, 48×48 |
| `menu-background.png` | Main menu background |
| `ui/interface.fnt`, `ui/interface.png` | Bitmap font with Cyrillic support |

Character sprite sheets are 192×288 RGBA. Each contains nine 64×96 frames.
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
GameScreen draws the terminal, portal, and NULL markers; they have no separate textures.

## Generated images

Ordinary Loki, Alligator, the standalone Sylvie sprite, and the menu background were created with the built-in
imagegen tool. They are not hand-drawn Canva artwork. After generation, the sprites were
resized, aligned, and packed. Artwork is not generated at runtime.

Sprite prompts: match the classic sprite's coarse angular pixel style,
muted green/gold palette, dark outline and simple shading; transparent background;
exact 3×3 grid, 64:96 cell aspect, DOWN/UP/RIGHT rows, idle/walk A/walk B columns;
consistent identity and centering; no text, grid or shadows.

- Ordinary: dark-haired fictional male trickster, white rolled-sleeve office shirt,
  loose dark-green tie, brown trousers, dark boots; no horns or helmet.
- Alligator: small green quadruped, tiny golden horned crown, square eyes,
  short legs and tapered tail; rectangular pixel clusters, minimal scales.
- Sylvie: fictional blonde female Norse trickster, shoulder-length blonde hair,
  asymmetric golden headpiece with one broken horn, dark-green/black adventure
  outfit, gold belt and boots; no resemblance to a real actor.

Background prompt: original cinematic environment illustration for a time-travel
adventure game; empty retro-futurist time bureaucracy office, amber circular portal,
brass elevators, analog clocks, teak desks, case files, green consoles, branching
timeline; emerald and burnt-orange light, painterly realism, landscape 16:9;
quiet dark left side, no people, text, logos, watermark or UI elements.

The interface font was prepared from the installed Segoe UI font. The FNT and PNG files
are ready to load in LibGDX; no additional libraries are required.
