# Shhh Builder - In-Game Testing Guide (Release 1.0.0)

Welcome to the beginner-friendly testing guide for **Shhh Builder v1.0.0**! This guide is written for players of all experience levels to verify every feature of the mod directly in the Minecraft client.

---

## 1. Setup & Installation

1. Locate your Minecraft installation folder:
   - **Windows**: `%APPDATA%\.minecraft` (usually `C:\Users\<YourUsername>\AppData\Roaming\.minecraft`)
   - **macOS**: `~/Library/Application Support/minecraft`
   - **Linux**: `~/.minecraft`
2. Open the `mods` folder inside `.minecraft`:
   - If older `shhh-builder-*.jar` files are present, **delete them**.
   - Copy **`shhh-builder-1.0.0.jar`** into your `mods` folder.
3. Launch Minecraft 26.3 with Fabric Loader (0.20.1 or newer).
4. On the title screen, click **Mods** (via Mod Menu) and confirm:
   - **Shhh Builder** is listed.
   - Version reads **`1.0.0`**.

---

## 2. Fresh Install & First-Run Retest Procedure

Shhh Builder includes two smart one-time startup actions:
1. **Sample Build Auto-Copy**: `sample_house.litematic` is copied into `.minecraft/schematics` **only if** the folder contains zero `.litematic` or `.schem` files AND the `sampleHouseCopied` flag is `false` in config.
2. **First-Run Welcome Message**: The message `"Shhh Builder: press V, then How to use."` displays in chat/action bar **only once** (tracked by `firstRunHelpShown: true` in config).

### How to re-test the fresh-install experience from scratch:
1. Close Minecraft.
2. Temporarily move any existing files out of `.minecraft/schematics` (so the folder is empty).
3. Delete the configuration file at `.minecraft/config/shhh-builder.json`.
4. Launch Minecraft and join a world.
5. Verify that:
   - The welcome message appears in chat and on the action bar.
   - Opening the Shhh Builder screen (`V`) shows `sample_house.litematic` automatically present in the list.
   - Rejoining the world does NOT repeat the welcome message.

---

## 3. Test 0: First Launch, Welcome Message & In-Mod Tutorial

1. Create and enter a single-player Creative Superflat world.
2. When the world loads, look at your chat and action bar:
   - You will see: `"Shhh Builder: press V to open, or [Open tutorial]."`
   - Hover over `[Open tutorial]`: a tooltip shows `"Click to open the step-by-step tutorial"`.
   - Click `[Open tutorial]`: it executes `/shhhbuilder tutorial` and directly opens the tutorial!
3. Alternatively, press **V** to open the main Shhh Builder screen.
4. In the top-right corner, find the **`How to use`** button.
5. Click **`How to use`**:
   - The screen transitions to the interactive 7-step tutorial card.
   - **Visuals**: Dark neutral panel (`#141619`), 1px border (`#30363D`), cyan active progress dots, step badge (`STEP 1 OF 7`), title, rendered 3D item icon illustration, and concise guidance with keycap pills.
   - **Step 1 (Getting Started)**: Item icon `CHEST`. Mentions your `schematics folder`, `Open Folder` and `Refresh` (without leaking personal filesystem paths).
   - **Step 2 (Pick a Build)**: Item icon `COMPASS`. Explains the schematic search box, format badges (`.litematic` / `.schem`), cyan card selection border, and the right panel tabs.
   - **Step 3 (Collect Your Materials)**: Item icon `BUNDLE`. Explains the Materials checklist (`[K]`), progress bar, inventory scanning, and shulker box math.
   - **Step 4 (Place in World)**: Item icon `STRUCTURE_BLOCK`. Explains primary `Place Here` button, the bright cyan bounding box, and real see-through textured blocks.
   - **Step 5 (Move and Turn)**: Item icon `RECOVERY_COMPASS`. Explains arrow keys, Page Up/Down, and shows your actual keybinds with styled keycap badges (`[R]`, `[Z]`).
   - **Step 6 (Build by Layers)**: Item icon `SCAFFOLDING`. Explains layer modes (`[M]`), layer adjusting (`[[ ]` and `[]]`), layer reset (`[\\]`), and the Material HUD (`[H]`).
   - **Step 7 (Tips & Controls)**: Item icon `EXPERIENCE_BOTTLE`. Explains that Shhh Builder is strictly a visual aid, notes toggles (`[G]` for preview blocks, `[B]` for outline box), and explains right-clicking blocks against existing faces.
6. Test navigation controls:
   - Click `[Next ▶]` (or press **Right Arrow** / **Page Down**) to advance.
   - Click `[◀ Back]` (or press **Left Arrow**) to go back.
   - Click `[Skip]` or `[Close]` (or press **Esc**) to cleanly return to the main screen.
   - On Step 7, the next button turns into `[Finish]`, which closes the tutorial and returns you to the main screen.

---

## 4. Test 1: Sample House Auto-Copy (Beginner Out-of-the-Box Experience)

1. If your `.minecraft/schematics` directory was empty, check the schematic list on the Shhh Builder screen:
   - **`sample_house`** will appear automatically in the list with tag `[.litematic]`!
2. Click **`sample_house`**:
   - The file is selected with a gold `▶` prefix.
   - The right-hand panel (Details tab) shows the build properties: `7 x 5 x 7`, `Total blocks: 137`, Author: `Shhh Builder`.
   - Scroll down on the Details tab: the **Blocks needed** section lists required materials with friendly names (Oak Planks, Cobblestone, Glass, Oak Stairs, Torch, Oak Door - never raw IDs). Notice that **Oak Door** shows exactly **1 door item** needed (not 2 blocks), because the lower door half is counted as 1 item and the upper half is skipped. Slabs with `type=double` count as 2 slab items. If water/lava/fire are present, a separate notice shows: *"N blocks have no item (water, fire...)"*.
3. Click **`Place Here`**:
   - The screen closes.
   - An in-world cyan bounding box appears right where you were standing, with holographic colored wireframes showing the small house!

---

## 5. Test 2: Real Large Test Files (`axe.litematic` and `axe.schem`)

1. Copy your test files (`axe.litematic` and `axe.schem`) into your `.minecraft/schematics` folder:
   - You can click the **`Open Folder`** button on the Shhh Builder screen to open the folder in Windows Explorer.
2. On the Shhh Builder screen, click **`Refresh`**:
   - Both files appear in the list!
3. Click **`axe.litematic`**:
   - Details: `153 x 160 x 29`, `23,578 blocks`, Region: `Main`.
   - Blocks needed: `23,578 Cobblestone (368 stacks + 26)`.
4. Click **`Place Here`**:
   - The footprint bounding box appears.
   - Holographic wireframes render the closest blocks up to your configured block limit (default 10,000 blocks).
   - If the build contains more blocks than the limit, a friendly action bar message appears: `"Showing the closest 10,000 blocks. You can raise this in Mod Menu > Shhh Builder."`
5. Repeat with **`axe.schem`**: verify that it loads with identical dimensions and block counts.

---

## 6. Test 3: In-World Movement, Rotation & Mirroring

1. With the build placed in the world, use your keyboard controls:
   - **Arrow Keys**: Press Up, Down, Left, Right to nudge the build horizontally by 1 block.
   - **Page Up / Page Down**: Nudge the build vertically by 1 block.
   - **R**: Rotates the build 90° clockwise. The cyan bounding box and wireframes swap width and length cleanly.
   - **Z**: Flips the build left-to-right (mirroring).
2. Notice the action bar: every keypress displays immediate feedback (e.g. `Moved: +1 X`, `Rotation: 90°`, `Mirrored: Yes`).

---

## 6. Test 4: Floor-by-Floor Layer Modes

1. Press **M** to cycle layer mode:
   - Mode cycles from `All Layers` -> `Single Layer` -> `Up to Layer` -> `All Layers`.
2. When on `Single Layer` or `Up to Layer`:
   - Press **`]`** to move up one layer.
   - Press **`[`** to move down one layer.
   - Only the specified layer(s) are drawn in the world.
3. Press **`\`**: resets layer mode back to `All Layers` instantly.

---

## 7. Test 5: Interactive Materials Checklist Screen (`K`)

1. Press **`K`** (or click **`[Materials]`** in the main screen header, or **`[Checklist]`** in the Details tab, or run `/shhhbuilder materials`):
   - The dedicated **Materials Checklist** screen opens with the current build's name in the header.
2. Verify top progress bar:
   - Shows progress text: `"Materials: X/Y ready (Z%)"` with a visual progress bar.
   - When all items are gathered/checked, shows `"✔ All materials ready!"`.
3. Test search and filtering:
   - Type in the search box: the item list filters in real time.
   - Click **`Sort: ...`** to cycle between *Most needed*, *A to Z*, and *Not done first*.
   - Click **`Hide done: OFF / ON`**: completed items collapse when ON.
   - Click **`Layer: All`** / **`Layer N`**: use `[◀]` and `[▶]` to step through individual layers.
   - Click **`Shulkers: Included / Ignored`**: toggle whether nested shulker boxes in your inventory are counted.
4. Test item rows and tooltips:
   - Hover over an item row: tooltip shows exact stack breakdown and shulker box math (e.g. `"23,578 = 368 stacks + 26, about 14 shulker boxes"`).
   - If holding items in inventory, "Have" updates live.
   - Click an item row to toggle its checkbox on/off.
5. Test footer action buttons:
   - Click **`Check all`**: marks every item in the list as completed.
   - Click **`Uncheck all`**: unchecks all items.
   - Click **`Copy list`**: copies a clean, human-readable text list to your clipboard (paste into Notepad to verify).
   - Click **`Reset`**: clears saved progress for this schematic.
6. Verify persistence:
   - Check off a few items, close the screen (`Esc`), and reopen with `K`: your check marks remain intact! Progress is safely saved to `.minecraft/config/shhh-builder-progress.json`.

---

## 8. Test 6: In-Game Material HUD & Dual Display Modes

1. Press **H**:
   - A compact HUD overlay appears on-screen showing the top materials needed for your current placement.
   - In Mod Menu settings, test **`HUD shows: Missing only`** vs **`HUD shows: Everything`**. In *Missing only* mode, items you already have in inventory or marked done disappear from the HUD!
2. In single layer mode: HUD dynamically reflects the materials for the active layer!
3. Press **H** again to toggle the HUD off.

---

## 9. Test 7: Client-Side Commands (`/shhhbuilder`)

Open your in-game chat and test the `/shhhbuilder` command set:

1. `/shhhbuilder help` - Shows a concise overview of all commands.
2. `/shhhbuilder tutorial` - Opens the step-by-step interactive tutorial.
3. `/shhhbuilder materials` - Opens the Materials Checklist screen (alias: `/shhhbuilder checklist`).
4. `/shhhbuilder list` - Lists all schematics found in `.minecraft/schematics`.
5. `/shhhbuilder select <file>` - Selects a schematic (supports Tab-completion without extensions!).
6. `/shhhbuilder place` - Places the selected schematic at your current player position.
7. `/shhhbuilder place 0 64 0` - Places the schematic at exact coordinates `X=0, Y=64, Z=0`.
8. `/shhhbuilder move 5 0 -5` - Nudges the placement relative to its current spot.
9. `/shhhbuilder rotate` - Rotates 90° clockwise.
10. `/shhhbuilder flip` - Toggles left-to-right mirroring.
11. `/shhhbuilder layer 5` - Switches to single layer mode for layer 5.
12. `/shhhbuilder layer all` - Resets to show all layers.
13. `/shhhbuilder folder` - Opens your schematics folder in Windows Explorer.
14. `/shhhbuilder clear` - Clears the current in-world placement.
15. `/shhhbuilder selftest` - Audits all 1,397 registered blocks for ghost preview support and material item mappings, reporting: `"Material engine: tested 1397 blocks, 1482 item mappings, 0 errors."`.

*Safety test*: Try typing `/shhhbuilder place 40000000 100 0` or `/shhhbuilder select ../secret.litematic`. Verify that helpful client error messages are displayed without crashing.

---

## 10. Test 8: Mod Menu Configuration Screen

1. Press **Esc** -> **Options** -> **Mods** (or **Mods** from the main menu).
2. Click **Shhh Builder** and click the settings/gear icon:
   - **Preview Style**: Click to toggle between `Textured` (default) and `Outlines`.
   - **HUD Enabled**: On / Off.
   - **HUD Position**: Click to cycle Top-Left -> Top-Right -> Bottom-Left -> Bottom-Right.
   - **HUD Scale**: Click to cycle Small (75%) -> Normal (100%) -> Large (125%).
   - **Ghost Render Distance**: Click to cycle 24 -> 32 -> 48 -> 64 blocks (for outlines).
   - **Ghost Block Limit**: Click to cycle 5,000 -> 10,000 -> 20,000 blocks (for outlines).
   - **Show Key Hints**: On / Off.
3. Changes save automatically to `config/shhh-builder.json` with safe atomic file replacement.

---

## 10. Test 8: Real See-Through Textured Ghost Blocks & Preview Fallback (1.0.0-rc5)

1. **Verify Real See-Through Textured Blocks (Default Out-of-the-Box Mode)**:
   - Place `sample_house.litematic` in the world (`V` -> select `sample_house.litematic` -> `Place Here`).
   - Notice that blocks render **textured by default**:
     - Oak Planks, Cobblestone, Glass, Oak Stairs, Torch, and Door appear with their true in-game textures and 3D shapes.
     - Blocks are holographic and see-through with ~45% opacity (`0x73` alpha). You can see mobs, terrain, and blocks behind them.
     - Fixed full-bright light (`PACKED_FULL_BRIGHT = 0x00F000F0`) is applied, ensuring blocks remain clearly visible in darkness or inside caverns.
     - Fixed natural tints are rendered for grass, foliage, and water.
2. **Verify Touching Ghost Block Face Culling**:
   - Walk inside or around the placed `sample_house.litematic`.
   - Observe walls and floors: any face that touches another ghost block in the placement is skipped.
   - Interior partitions between touching solid blocks do not produce internal quad lattices or criss-cross z-fighting.
3. **Verify Distance (24 blocks) & Block Limit (2,000 blocks) Fallback**:
   - Place a large build like `axe.litematic` (over 20,000 blocks).
   - Stand near the handle of the axe:
     - The closest blocks (within 24 blocks Euclidean distance, up to the 2,000 closest blocks) render as see-through textured blocks.
     - Blocks farther than 24 blocks seamlessly render as colored wireframe outlines.
     - As you walk along the build, blocks dynamically transition between textured and outline representations without frame drops or sorting stalls.
4. **Looking-At HUD Raycast Test**:
   - Ensure the Material HUD is visible (press **H** if hidden).
   - Walk within 5 blocks of a ghost block and point your crosshair directly at it.
   - Look at the top line of the Material HUD:
     - Displays: `"Looking at: <Block Name> (layer N)"` (e.g. `Looking at: Oak Planks (layer 1)`).
5. **Switching to Outlines Mode**:
   - Press **Esc** -> **Options** -> **Mods** -> **Shhh Builder** settings.
   - Toggle **Preview Style** to `Outlines`.
   - Return to the world: all ghost blocks render as crisp wireframes colored by their vanilla MapColor (oak is warm amber, stone is gray, foliage is green).
6. **3-Strike Circuit Breaker Safety**:
   - If an unexpected error occurs in the textured pipeline:
     - First 2 errors log warnings to the game console and fall back to outlines for the offending block/frame.
     - On the 3rd error, the circuit breaker automatically trips, disables textured preview for the session, flips the config to Outlines, and shows ONE plain action bar message:
       `"Textured preview turned off. Showing outlines."`
   - Re-opening Mod Menu and setting Preview Style back to `Textured` resets the circuit breaker.

---

## 11. Test 9: rc6 Regression Verifications (Cobblestone vs Stone, Settings Layout, Privacy, Preview Status)

1. **Cobblestone vs Stone Verification**:
   - Place `axe.schem` or `axe.litematic` (which contains Cobblestone blocks).
   - Verify on HUD: Displays `Cobblestone: 23578` (or proper count), NOT `Stone`.
   - Verify in Details Panel: Displays `Cobblestone: 23,578`, NOT `minecraft:stone`.
   - Verify in 3D Preview: Blocks render with Cobblestone texture/model, NOT smooth stone.
2. **Settings Screen Title Layout**:
   - Open Settings screen at different GUI scales (1 to 4) and resolutions (854x480, 1920x1080).
   - Verify that "Shhh Builder Settings" header text has clear spacing above the first button ("Preview Style") and never overlaps.
3. **Path Privacy in UI**:
   - Open main screen, open Tutorial screen (Page 1), run `/shhhbuilder list` and `/shhhbuilder folder`.
   - Verify that absolute filesystem paths (such as `C:\Users\...`) are NEVER printed to chat, action bar, or screens. Only friendly phrase `"schematics folder"` is shown.
4. **Preview Status Line & Logging**:
   - In the main screen under Placement tab and Details tab, verify status text:
     `Preview: textured (N blocks nearby), outlines farther away` (or `Preview: outlines`).
   - Check game console log on first frame after placing a build:
     `Ghost preview active for placement at [x, y, z]: style=textured, textured=N, outlines=M, reason: normal (within 24 blocks distance and 2000 block cap)`.

---

## 12. Test 10: rc7 Textured Preview Options & Self-Test Checklist

1. **Preview Opacity Slider & Live Stepping**:
   - Open Settings screen: Drag "Preview Opacity" slider. Verify it steps smoothly by 5% increments between 10% and 100% (default 45%).
   - Open Main Screen -> Position Tab (`Tab.CONTROLS`): Locate the opacity control row `[-] [Opacity: 45%] [+]`.
   - Click `[-]` and `[+]`. Verify opacity adjusts live in 5% increments without closing the screen or reopening settings.
   - Observe ghost blocks in the world: confirm textured transparency visibly increases/decreases.
2. **Block Borders**:
   - In Settings screen, toggle "Textured Borders: ON/OFF" (default ON).
   - Cycle "Border Color": `Block color` -> `White` -> `Black` -> `Cyan`.
   - Cycle "Border Opacity": `25%` -> `50%` -> `75%` -> `100%` (default 100%).
   - In world with borders ON: Verify thin boundary lines appear around textured blocks so adjacent identical blocks (e.g. cobblestone or smooth stone walls) can be visually separated.
3. **Quality Presets & Textured Distance**:
   - In Settings screen, click "Quality Preset: Balanced". Verify it cycles to `High` -> `Low` -> `Balanced`.
   - Check preset values:
     - `Low`: 16m distance, 500 max textured, 1000 max total blocks.
     - `Balanced`: 24m distance, 2000 max textured, 4000 max total blocks.
     - `High`: 32m distance, 5000 max textured, 10000 max total blocks.
   - Manually cycle "Textured Distance" (16m -> 24m -> 32m) or "Max Textured Blocks": verify preset label automatically changes to `Custom`.
4. **Universal Block Categories Rendering**:
   - Place a schematic containing diverse block types:
     - **Normal & Multipart Models**: Stairs, slabs, fences, walls, panes, doors, trapdoors, torches, lanterns, redstone repeaters/comparators render with their authentic 3D models and textures.
     - **Fluids**: Water renders as a translucent blue box (`#3F76E4`), Lava renders as a translucent orange box (`#E05810`).
     - **Block Entities**: Chests, Ender Chests, Beds, Signs, Banners, Skulls, Shulker Boxes, Bells, Enchanting Tables render as shaped colored boxes with borders matching their voxel bounding boxes.
     - **Technical & Invisible Blocks**: Barriers, Light blocks, Structure Voids, Jigsaws render as small centered marker boxes (0.3 to 0.7 cube), never full opaque or distracting cubes.
     - **Tinted Blocks**: Grass blocks, foliage, leaves, vines, and water render with clean default foliage/water tints.
5. **Self-Test Command (`/shhhbuilder selftest`)**:
   - Run `/shhhbuilder selftest` in chat.
   - Verify client-side chat feedback:
     - Plain text report summarizing: Total registered blocks tested, blocks using normal baked models, block entities using shape fallback, fluids using fluid box fallback, and technical markers.
     - Confirmation that 0 blocks crashed and 0 unexpected failures occurred.
   - Check game console log: Confirm detailed breakdown listing exact block categories and any fallback IDs for verification.

---

## 13. Test 11: UI Redesign Checklist & Usability Verification (1.0.0-rc8)

1. **Design System & Visual Consistency (`UiStyle`)**:
   - Verify dark neutral surfaces (`#141619` for main panels, `#1C2024` for cards/chips).
   - Verify crisp 1px borders (`#30363D` default, `#00E5FF` cyan accent for selections/primary focus).
   - Verify clear text hierarchy: bold white titles, readable headers, light gray body text, and muted secondary labels (`#8B949E`).
   - Confirm that all UI elements, backgrounds, text, and borders render with full 32-bit ARGB colors (no invisible 24-bit zero-alpha elements in 26.3).

2. **Main Screen Header**:
   - Open main screen with **V**.
   - Header shows mod icon (`icon.png`) and clean bold `"Shhh Builder"` title without any distracting version badge.
   - Top-right corner displays `[How to use]` and `[Settings]` buttons with subtle borders.

3. **Schematic Search & File Cards (Left Panel)**:
   - Search box: Type in the search box (e.g., `axe` or `sample`). The file list filters in real time.
   - File cards: Each card displays the file name, a format badge (`.litematic` or `.schem`), dimensions (`W x H x L`), and total blocks in muted text.
   - Selection: Clicking a file highlights its card with a glowing cyan border (`#00E5FF`).
   - Empty search state: Typing a query that matches no files displays a clean card: *"No schematics match your search."*
   - Pagination: If more than 6 files are present, pagination controls (`◀ Page X/Y ▶`) appear neatly in the footer.

4. **Details Card & Tabs (Right Panel)**:
   - With a schematic selected, the top details card displays the build name, author, dimension chips (e.g. `[153 x 160 x 29]`), and green block count chip.
   - Tab switcher allows toggling between `"Blocks needed"` and `"Position"`.

5. **"Blocks Needed" Tab**:
   - Scrollable material list with a dark track.
   - Each entry shows the rendered item icon, friendly localized item name, and count formatted with stack calculations (e.g., `2,357 (36 stacks + 53)`).
   - If unobtainable blocks exist (water, lava, fire, etc.), a separate card shows: *"N blocks have no item (water, fire...)"*.
   - Action buttons at the bottom of the tab: `[Checklist]` (opens the full materials manager) and `[Show on HUD]` (enables the on-screen overlay).

6. **"Position" Tab & Keycap Labels**:
   - Controls are cleanly grouped into Move, Orientation, Layers, and Opacity.
   - Each button displays its bound key as a physical keycap pill badge (`[←]`, `[↑]`, `[→]`, `[PgDn]`, `[R]`, `[Z]`, `[M]`, etc.).
   - Live opacity row shows `[-]`, current opacity percentage (e.g., `[Opacity: 45%]`), and `[+]`. Clicking adjusts opacity in real time.

7. **Pinned Bottom Action Bar & Hotbar Clearance**:
   - Five proportional buttons pinned securely at `y = this.height - 36` with height 22px:
     - Primary `[Place Here]` highlighted in cyan accent.
     - `[Clear Build]` (red danger accent when active).
     - `[Refresh]`.
     - `[Open Folder]`.
     - `[Close]`.
   - Guaranteed zero collision with vanilla hotbar slots, experience bars, or player inventory hand items underneath.
   - On narrow screens or high GUI scales, buttons distribute available width evenly without clipping or falling off-screen, with graceful ellipsis truncation (`..`) on long button text.

8. **Settings Screen Sections & Hover Bar**:
   - Open Settings from main screen or Mod Menu.
   - Top tab bar lets you switch between `[Preview]`, `[Performance]`, and `[HUD]`.
   - Opacity slider features a dark track, filled bar, and glowing cyan thumb handle.
   - Dynamic hover bar: Hovering or focusing any setting updates a 1-line description at the bottom explaining what that setting does in plain English.
   - Bottom buttons: `[Reset Defaults]` and `[Done]`.

9. **Material HUD**:
   - Press **H** in-world.
   - HUD appears in a sleek dark neutral card matching `UiStyle`.
   - Shows schematic title, active layer badge, pointed block indicator, item icons, and required item counts with stack math.
   - Key hints line (`[R] turn | [Z] flip | [[ ]] layer | [H] hide`) toggles cleanly via settings.

10. **Responsive Layout Stress Test**:
    - Test window resolutions: 640x360, 854x480, 1280x720, 1920x1080.
    - Test GUI scales: 1, 2, 3, and 4 (including Auto).
    - Verify that no buttons collide, no text overlaps, and all controls remain fully clickable and within screen bounds.

---

## 11. Live World Verification & Ghost Pick-Block Test Procedure

1. **Exact Match Verification**:
   - Place a schematic in your world (`V` -> `[Place Here]`).
   - Place the exact required block with matching orientation (e.g. Oak Stairs facing North).
   - Observe that the ghost preview block disappears immediately (exact match).
2. **Wrong Orientation Wireframe**:
   - Place the expected block type, but with the wrong orientation (e.g. Oak Stairs facing South instead of North).
   - Observe that Shhh Builder draws a bright Amber/Orange wireframe outline (`0xFFFFAA00`) using depth-biased rendering over the block.
   - Look at the block with your crosshair:
     - The HUD displays: `"Looking at: Wrong orientation (Expected: Oak Stairs [north, bottom])"` in bright red/warning (`0xFFFF5555`).
3. **Wrong Block Wireframe**:
   - Place a completely different solid block (e.g. Cobblestone where Oak Planks are expected).
   - Observe that Shhh Builder draws a bright Red wireframe outline (`0xFFFF3333`) using depth-biased rendering over the block.
   - Look at the block with your crosshair:
     - The HUD displays: `"Looking at: Wrong block (Expected: Oak Planks)"` in bright warning (`0xFFFF5555`).
4. **Ghost Pick-Block (Vanilla Key)**:
   - Aim your crosshair at an unplaced preview block within 5 blocks reach.
   - Press vanilla Pick-Block (default Middle Mouse Button).
   - Verify that Shhh Builder searches your hotbar (slots 0-8) and instantly switches to the matching item without opening inventory or sending custom network packets.
   - If aiming at a normal world block (not a ghost block), verify vanilla pick-block works completely untouched.

---

## 12. World-Switch & Disconnect Cleanup Test Procedure

1. **Multiplayer / Singleplayer World Disconnect**:
   - In a world, place any schematic (`V` -> `[Place Here]`), rotate it (`R`), or change layers (`[` / `]`).
   - Open checklist (`K`) and mark some items as collected.
   - Press `Escape` -> `Save and Quit to Title` (or `Disconnect`).
   - Check `.minecraft/config/shhh-builder-progress.json`:
     - Confirm all marked checklist items were safely flushed to disk via atomic file replacement.
2. **Joining a New World**:
   - Join another world or multiplayer server.
   - Confirm that zero ghost blocks, wireframe outlines, or bounding boxes are rendered at the old coordinates.
   - Press `V`: confirm the placement position is cleared and starts completely fresh.
