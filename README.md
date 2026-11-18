# Shhh Builder

A simple, beginner-friendly schematic viewer for Fabric. Pick a file, place it in your world, and build from a clear ghost preview.

---

## Features

- **Modern, Accessible UI Design System**: Dark neutral surfaces (`#141619`), 1px borders (`#30363D`), and cyan accents (`#00E5FF`) on a clean 8px grid. Full 32-bit ARGB colors everywhere ensure zero invisible UI elements in Minecraft 26.3.
- **Bulletproof Responsive Layout & Hotbar Clearance**: Engineered with strict 12px margins, a hotbar safety buffer (pinned at `y = height - 36`, height 22px) preventing any collision with vanilla hotbar slots or experience bars, proportional 2-column splitting, dynamic 5-button action bar distribution, full screen darkening overlay (`0xC0101216`), and graceful button text ellipsis truncation (`..`).
- **Real-Time Schematic Search**: Filter schematics instantly as you type in the search box.
- **Interactive File Cards**: Each schematic is presented as a clean card showing format badges (`.litematic` / `.schem`), dimensions, and block counts, with glowing cyan borders on selection.
- **Bundled Starter Build**: Includes a small sample house (`sample_house.litematic`) automatically copied into your `schematics` folder on first run if empty, so beginners can test instantly with zero setup.
- **Dual Format Support**: Load `.litematic` and `.schem` files from a single `schematics` folder.
- **In-World Ghost Preview**: View schematic blocks as real see-through textured blocks (~45% opacity default, adjustable 10%–100%) or crisp per-block MapColor wireframes right where you want to build. Enabled by default!
- **Batched Translucent Geometry Pipeline**: Batches up to 2,000 textured blocks into a single `submitCustomGeometry` call per frame into `RenderTypes.translucentMovingBlock()` using a reusable local `PoseStack.Pose` and precomputed tint RGB, eliminating per-block submit node allocations for butter-smooth framerates.
- **Customizable Block Borders**: Thin subtle border lines around textured blocks (with configurable color: Block color / White / Black / Cyan, and 25%–100% opacity) so blocks remain distinct and easy to place.
- **Automatic Live World Verification (Diff Highlighting)**: Live comparison against `mc.level.getBlockState(worldPos)` every frame:
  - **Exact Match**: Preview block is automatically hidden because you already built it.
  - **Wrong Orientation (Same Block)**: Bright Amber/Orange wireframe outline (`0xFFFFAA00`) highlights blocks placed facing the wrong direction (e.g. stairs, logs, observers).
  - **Wrong Block**: Bright Red wireframe outline (`0xFFFF3333`) highlights incorrect solid blocks.
- **VoxelShape Clip Raycasting & Ghost Pick-Block**:
  - Uses `VoxelShape.clip` for exact hit detection on thin blocks (torches, panes, stairs, slabs, fences).
  - When pointing at an incorrect block, the HUD displays `"Looking at: Wrong orientation (Expected: ...)"` or `"Looking at: Wrong block (Expected: ...)"` in warning red (`0xFFFF5555`).
  - **Ghost Pick-Block**: Pressing vanilla pick-block (Middle Click) while aiming at an unplaced preview block within 5 blocks automatically searches hotbar slots 0–8 for the matching item and selects that slot without opening inventory or sending custom network packets!
- **Universal Block Support**: Seamless handling for all block categories: full blocks, slabs, stairs, doors, plants, torches, fluids (translucent water/lava boxes), block entities (chests, beds, signs), and technical markers (barrier, light, structure void).
- **Materials Checklist Screen (`K`)**: Complete interactive materials manager accessible via key **`K`**, the **Materials** header button, or `/shhhbuilder materials`. Features a top progress bar (`"Materials: 12/28 ready (45%)"`), instant search box, sort cycling (*Most needed*, *A to Z*, *Not done first*), toggles (*Hide done*, *This layer only* with step buttons `[◀]`/`[▶]`, *Include shulkers*), automatic player inventory scanning (with shulker box container inspection), shulker math hover tooltips (`"23,578 = 368 stacks + 26, about 14 shulker boxes"`), creative-only warning badges, and persistent progress saving in `config/shhh-builder-progress.json`.
- **Exact Carried Item Counting Engine**: Comprehensive counting engine (`MaterialCountingEngine`) evaluating real 26.3 block properties to count carried items rather than voxels: slabs (double = 2), doors (lower half only), beds (foot part only), tall plants (lower half only, `tall_seagrass` -> `seagrass`), piston bases = 1 (piston heads & moving pistons skipped), stackables (candles, pickles, eggs, snow layers, pink petals, wildflowers, leaf litter), wall mounts (`wall_torch` -> `torch`, `wall_sign` -> `sign`, `wall_banner` -> `banner`, `redstone_wire` -> `redstone`), potted plants (1 pot + 1 plant), cauldrons -> cauldron, and unobtainables (water, fire, portals -> 0 items). Caches counts per layer for instant filtering. Full coverage verified across all 1,397 registered blocks in [BLOCK_COVERAGE.md](Documentation/BLOCK_COVERAGE.md).
- **Targeting HUD Indicator & Dual HUD Modes**: Shows `"Looking at: <Block Name> (layer N)"` in real time when pointing your crosshair at any ghost block within reach. Material HUD (`H`) supports configurable display modes (*Missing only* or *Everything*) and displays progress percentage or `"✔ All materials ready!"` when complete.
- **Footprint Outline**: Bright bounding box showing the exact dimensions and position of the build.
- **Material List & Stack Calculations**: "Blocks needed" tab and HUD display item icons, friendly localized names, and item counts with stack math (e.g. `2,357 (36 stacks + 53)`).
- **Physical Keycap Control Labels**: Position tab and tutorial display controls with clean keycap pills (`[←]`, `[↑]`, `[→]`, `[PgDn]`, `[R]`, `[Z]`, `[M]`, etc.) showing your actual bound keys.
- **Step-by-Step Interactive Showcase Tutorial**: Beautiful 7-step guide featuring animated pulsing cyan accent borders, reactive keycaps with hover glow and micro-tooltips, floating 3D item models with drop shadows, and clickable progress dots accessible via `/shhhbuilder tutorial`, the header **How to use** button, or the first-run chat link.
- **Grouped Settings with Hover Bar**: Settings screen organized into `Preview`, `Performance`, and `HUD` sections with custom cyan opacity slider and a dynamic 1-line description of whatever setting is hovered or focused.
- **Quality Presets & Safe Fallbacks**: One-click Quality Presets (*Low*, *Balanced*, *High*, or *Custom*) to tune textured distance (16/24/32m) and block limits for any PC.
- **Robust World-Switch & Disconnect Cleanup**: Automatic listeners for `DISCONNECT` and `CLIENT_STOPPING` clear active placements, reset renderer caches, and safely flush checklist progress to disk via atomic file replacement.
- **Responsive Layout**: Designed and tested to fit flawlessly from 640x360 to 1920x1080 across GUI scales 1 through 4 with zero overlap.

---

## Installation

1. Install **[Fabric Loader](https://fabricmc.net/)** for Minecraft 26.3.
2. Download and place the **[Fabric API](https://modrinth.com/mod/fabric-api)** jar into your `.minecraft/mods` folder.
3. *(Optional)* Download and place **[Mod Menu](https://modrinth.com/mod/modmenu)** into your `.minecraft/mods` folder to access in-game settings.
4. Download **`shhh-builder-1.0.0.jar`** and place it into your `.minecraft/mods` folder. (Ensure any older versions are removed).
5. Launch the game! A `schematics` folder will be created automatically in your `.minecraft` directory.

---

## How to Use

1. Put your `.litematic` or `.schem` files into the `schematics` folder in your game directory.
2. In a world, press **`V`** to open the schematic browser and select a file (or click **How to use** in the top right for a quick interactive guide).
3. Press **`K`** (or click **Materials**) to open the checklist and track which items you need to gather.
4. Click **Place Here** to position the build at your feet, then adjust it using the on-screen buttons or keybinds.

---

## Keybinds

All keybinds can be customized in Minecraft's **Options → Controls → Key Binds → Shhh Builder** menu.

| Key | Default Action |
| --- | --- |
| `V` | Open Shhh Builder schematic browser |
| `K` | Open Materials Checklist screen |
| `H` | Toggle Material List HUD |
| `B` | Toggle Footprint Outline Box |
| `G` | Toggle Ghost Blocks Wireframe |
| `R` | Rotate placement 90° clockwise |
| `Z` | Flip (mirror) placement left-to-right |
| `M` | Cycle layer mode (*All* → *Single layer* → *Up to layer*) |
| `[` | Previous layer (step down 1 level) |
| `]` | Next layer (step up 1 level) |
| `\` | Reset layer mode to show all layers |
| `Arrow Keys` | Move placement horizontally (Forward / Backward / Left / Right) |
| `Page Up` / `Page Down` | Move placement vertically (Up / Down 1 block) |

---

## Client-Side Commands (`/shhhbuilder`)

Shhh Builder provides a full set of client-side chat commands that operate with zero server communication. You can control builds entirely from chat without opening the screen:

Type **`/shhhbuilder help`** in chat at any time for the built-in command overview.

| Command | Description |
| --- | --- |
| `/shhhbuilder help` | Display list of all client commands and usage examples |
| `/shhhbuilder tutorial` | Open the interactive step-by-step tutorial card |
| `/shhhbuilder materials` | Open the Materials Checklist screen (alias `/shhhbuilder checklist`) |
| `/shhhbuilder list` | List all `.litematic` and `.schem` files in your schematics folder |
| `/shhhbuilder select <file>` | Select and load a schematic (with smart tab-completion) |
| `/shhhbuilder place` | Place the selected build right where you are standing |
| `/shhhbuilder place <x> <y> <z>` | Place the selected build at exact coordinates |
| `/shhhbuilder move <x> <y> <z>` | Move placement by relative offsets (dx, dy, dz) |
| `/shhhbuilder rotate` | Turn placement 90° clockwise |
| `/shhhbuilder flip` | Flip placement left-to-right |
| `/shhhbuilder layer <all\|number>` | Switch layer mode to all or isolate a specific layer |
| `/shhhbuilder clear` | Clear the build preview from your world |
| `/shhhbuilder folder` | Open the schematics folder in Windows Explorer |
| `/shhhbuilder selftest` | Audit all registered blocks for ghost preview support and item mapping |

> [!NOTE]
> All `/shhhbuilder` commands are processed strictly on the client. Nothing is ever sent across the network or to multiplayer servers.

---

## Supported Formats

- **`.litematic`** (Litematica format): Full support for single and multi-region schematics, bit-packed palettes, enclosure metadata, and negative region sizes.
- **`.schem`** (Sponge Schematic Specification): Full support for Sponge v2 and v3 specifications, LEB128 VarInt-encoded block arrays, nested structures, and palette containers.

---

## Configuration

Settings are saved automatically to `config/shhh-builder.json`. If you have [Mod Menu](https://modrinth.com/mod/modmenu) installed, you can configure everything in-game by opening **Mods → Shhh Builder → Settings**:
- **Preview Opacity**: Real slider from 10% to 100% (default 45%, adjustable live on the Position tab via `[-]` and `[+]`).
- **Block Borders**: Toggle subtle boundary lines around textured blocks, with customizable color (Block color, White, Black, Cyan) and opacity (25% to 100%).
- **Quality Presets**: Switch in one click between *Low*, *Balanced*, and *High* (or *Custom*).
- **Textured Distance**: Limit full 3D texturing to 16m, 24m, or 32m from the player.
- **HUD Settings**: Position, scale (75% / 100% / 125%), and key hint line toggle.

---

## Known Issues & Limitations

- **Axis Dimension Limit (4,096 blocks)**: Schematics with any dimension (width, height, or length) exceeding 4,096 blocks are rejected at load time to prevent memory overflow. The player will see a red error in the Details panel: *"Region dimensions exceed safety limit of 4096 blocks per axis."*
- **Read Volume Limit (10,000,000 blocks)**: Schematics with a total volume exceeding 10,000,000 blocks are rejected during reading before large memory allocations occur. The player will see a red error in the Details panel: *"Schematic volume exceeds safety limit of 10,000,000 blocks."*
- **Preview Volume Safeguard (2,000,000 blocks)**: Schematics with total volume between 2,000,000 and 10,000,000 blocks disable in-world ghost block wireframes to prevent memory spikes and client lag, while keeping the cyan footprint box and material list active. The player will see a gold notice in the Details panel: *"Preview: Outline only (volume > 2M)"*.
- **Ghost Block Per-Frame Limit (5,000 / 10,000 / 20,000 blocks)**: To preserve client FPS, ghost rendering prioritizes 16x16x16 sections closest to the camera first and caps rendering at the configured limit (default: 10,000 blocks; options: 5,000 / 10,000 / 20,000 via Mod Menu settings). When a build exceeds this limit within render distance, the closest blocks are drawn and the player sees an action bar notification: *"Showing the closest 10,000 blocks. You can raise this in Mod Menu > Shhh Builder."*
- **Item-Accurate Material Counts**: Material lists on the HUD, Details panel ("Blocks needed"), and commands count the actual **items** the player needs to carry and place:
  - **Slabs**: `type=double` counts as 2 slab items.
  - **Doors**: Counts only `half=lower` (1 door item per door).
  - **Beds**: Counts only `part=foot` (1 bed item per bed).
  - **Tall plants & double-height flowers**: Counts only `half=lower` (1 item per plant).
  - **Technical blocks**: `piston_head` and `moving_piston` are excluded; a piston base counts as 1 piston item.
  - **Waterlogged blocks**: Only the main block counts, never the water.
  - **Blocks with no obtainable item**: Water, lava, fire, nether/end portals, and bubble columns are excluded from required items and shown on a separate note: *"N blocks have no item (water, fire...)"*.
  - **Carried item resolution**: Block states like wall torches, wall signs, wall banners, and redstone wire automatically map to the carried inventory item (`torch`, `sign`, `banner`, `redstone`).
  - **Block count separation**: The total 3D non-air block count is preserved as a separate figure (*"Total blocks: N"*).
- **Unknown / Unregistered Blocks**: Block states from missing mods or different Minecraft versions are identified, counted, and reported directly to the player in the Details panel (*"N blocks are not in this game version"*), and rendered using safe stone wireframe placeholders.
- **Rendering Mod Compatibility (Sodium, Iris, Shaders)**: Sodium, Iris, and custom third-party shader packs have **not been tested**. Shader packs that suppress or redirect vanilla line rendering passes (`RenderTypes.lines()` or `RenderTypes.linesDepthBias()`) may hide wireframes or outline boxes.
- **Client-Side Only**: Shhh Builder is strictly a visual aid. It **never** places blocks, breaks blocks, automates player actions, or sends packets to servers. Always respect multiplayer server rules.

---

## Credits

- File format readers were implemented from public specifications and documentation:
  - Litematic format: Public documentation by the [Litemapy](https://litemapy.readthedocs.io/) project.
  - Sponge Schematic format: Official [SpongePowered Schematic Specification](https://github.com/SpongePowered/Schematic-Specification).
- In-world rendering is implemented using Fabric API's `LevelRenderEvents.BEFORE_GIZMOS` event and Minecraft's `SubmitNodeCollector.submitShapeOutline` API into `RenderTypes.linesDepthBias()` and `RenderTypes.lines()`.
- In-game material HUD is registered using Fabric API's `HudElementRegistry.addLast`.
- Shhh Builder was written from scratch and is not affiliated with, endorsed by, or derived from Litematica, WorldEdit, or Mojang.

---

## License

This project is licensed under the [MIT License](LICENSE).
