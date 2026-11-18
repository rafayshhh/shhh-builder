# Shhh Builder - Development Plan

## Overview
A simple, beginner-friendly schematic viewer for Minecraft 26.3 Fabric.
Allows players to load schematic files, view them as in-world ghost holograms, and build layer-by-layer with ease.

---

## Roadmap

- [x] **Step 1: Project Setup & Core Screen**
  - [x] Set up Fabric project for Minecraft 26.3 and verify compilation
  - [x] Configure mod metadata in `fabric.mod.json` (ID: `shhh-builder`, Author: `RafayShhh`, License: `MIT`, Client-side only)
  - [x] Add mod icon from Assets
  - [x] Implement 'V' keybind under "Shhh Builder" category in Options > Controls
  - [x] Create base schematic screen with title and placeholder guidance
  - [x] Auto-create `schematics` folder in Minecraft game directory on startup
  - [x] Add optional Mod Menu integration with placeholder config screen
  - [x] Add documentation (`PLAN.md`, `CHANGELOG.md`)
  - [x] Test client launch with `gradlew runClient`

- [x] **Step 2: File Browser**
  - [x] Scan and list `.litematic` and `.schem` files from the `schematics/` directory
  - [x] Display file name without extension and type badge in a scrollable list
  - [x] Click-to-select schematic with highlight and "Selected: <file name>" text
  - [x] Add "Refresh" button to rescan directory
  - [x] Add "Open Folder" button to open schematics directory in OS file explorer
  - [x] Friendly empty state message and error handling
  - [x] Persist selected file name during runtime session

- [x] **Step 3: Litematic Parser & Metadata**
  - [x] Create `LitematicReader` clean-room parser for gzip-compressed NBT
  - [x] Bit-unpacking across 64-bit boundaries (minimum 2 bits per block)
  - [x] Exclude air blocks (`air`, `cave_air`, `void_air`) from counts and totals
  - [x] Parse metadata (name, author, enclosing dimensions, volume) with region fallbacks
  - [x] Asynchronous background reading with "Loading..." indicator
  - [x] Details panel displaying name, author, dimensions, total blocks, top 10 materials, and "+ X more" indicator
  - [x] Safe error handling (corrupt, empty, or oversized files > 50 MB)
  - [x] Friendly notice for `.schem` files (*"Reading .schem files is coming in a later update."*)
  - [x] Session result caching with file modification timestamp and size verification
  - [x] Comprehensive JUnit unit tests for unpacking logic, boundary crossing, and air filtering
  - [x] Console logging of material counts for direct verification against Litematica

- [x] **Step 4: Material List HUD**
  - [x] In-game HUD panel showing required materials sorted by count from highest to lowest
  - [x] Friendly block display names (e.g. "Oak Planks") with localized registry lookups and clean fallback
  - [x] Small rendered block/item icon next to each material using `extractor.fakeItem`
  - [x] Friendly stack count formatting (raw count, and for 64 or more, stacks + remainder: e.g. "212 (3 stacks + 20)")
  - [x] Shows at most 10 lines, with a `+ X more` line when more materials exist
  - [x] Title line displaying schematic name
  - [x] Toggle keybind `H` under "Shhh Builder" category (defaults to ON when a schematic is selected)
  - [x] Action bar overlay notification when pressing `H` ("Material List HUD: Shown" / "Material List HUD: Hidden")
  - [x] HUD hides while any screen or menu is open and when F1 hide-HUD is active
  - [x] Mod Menu configuration screen with HUD position (`Top-Left`, `Top-Right`, `Bottom-Left`, `Bottom-Right`) and HUD scale (`Small`, `Normal`, `Large`)
  - [x] Persistent JSON configuration file (`config/shhh-builder.json`) with sensible defaults
  - [x] Draws nothing when no schematic is selected or loaded
  - [x] "Show materials on screen" button in Shhh Builder screen that turns the HUD on and returns to game
  - [x] Clean semi-transparent dark background styling with subtle border and padding
  - [x] Automated unit test suite (`MaterialDisplayHelperTest`) verifying formatting and config cycling

- [x] **Step 5a: World Placement & Outline Box**
  - [x] In-memory placement position (X, Y, Z coordinates), rotation (0, 90, 180, 270), and dimension tracking in `PlacementManager`
  - [x] "Place Here" button on Shhh Builder screen that anchors the schematic to the block under the player and closes the screen
  - [x] "Clear" placement button to reset placement
  - [x] In-world box outline rendering using Fabric API's `LevelRenderEvents.BEFORE_GIZMOS` and Minecraft's `SubmitNodeCollector.submitShapeOutline`
  - [x] Depth-biased bright cyan outline box (`RenderTypes.linesDepthBias()`) respecting 90° and 270° dimension swapping
  - [x] Default keybind `B` to toggle outline visibility on/off with action bar notifications
  - [x] Rebindable nudge keybinds (Arrow Up/Down/Left/Right, Page Up/Down) to move placement by 1 block relative to player facing direction, plus `R` to rotate (+90°)
  - [x] Hides outline if no placement exists, if toggle is off, or if player is in a different dimension
  - [x] Unit test suite (`PlacementManagerTest`) verifying placement, rotation swapping, and nudging

- [x] **Step 5b: In-World Ghost Hologram Blocks**
  - [x] Compact schematic block storage: unified palette of Minecraft `BlockState`s and pre-culled visible block array in `SchematicBlockData`
  - [x] Maximum volume cap for preview (2,000,000 blocks); displays friendly notification if volume is exceeded while keeping outline and materials list active
  - [x] Placement rotation applied to block coordinates and vanilla directional block states (`BlockState.rotate(Rotation)`)
  - [x] 6-neighbor occlusion culling (skips blocks completely surrounded by non-air blocks on all 6 sides)
  - [x] In-world holographic wireframe rendering using Fabric API's `LevelRenderEvents.BEFORE_GIZMOS` and `SubmitNodeCollector.submitShapeOutline`
  - [x] Configurable ghost render distance (default 48 blocks) with cycle button in Mod Menu configuration screen
  - [x] Frame performance cap at 20,000 blocks with throttled action bar notification if blocks are skipped
  - [x] Precomputed cached block transform in `PlacementManager` avoiding per-frame coordinate recalculation
  - [x] Keybind `G` to toggle ghost preview on/off with action bar notifications
  - [x] Automated unit test suite (`GhostBlockTest`) verifying 6-neighbor culling, rotation mapping, volume limits, and config cycling

- [x] **Step 6: Hologram Controls & Manipulation**
  - [x] Layer mode: All (default), Single layer (one Y level), and Up to layer (cumulative) with 1-based indexing
  - [x] Layer keybinds under "Shhh Builder": Cycle Mode (`M`), Next Layer (`]`), Previous Layer (`[`), Reset All (`\`)
  - [x] Action bar overlay notifications on layer changes (e.g. *"Layer 3 / 12 (single layer)"*, *"Layer mode: All layers"*)
  - [x] Layer mode filtering applies to ghost blocks while keeping full footprint outline box visible
  - [x] Cached visible-block transformation in `PlacementManager` recomputed only when layer, rotation, mirror, or schematic changes
  - [x] Material list HUD displays current layer information when mode is not "All" (e.g. `Layer 2/4 (Single)`)
  - [x] Flip (mirror) support left-to-right via `X` keybind (rebound from `F` to avoid conflict with vanilla offhand swap) and on-screen button, using vanilla `BlockState.mirror(Mirror.LEFT_RIGHT)` and rotation coordination ($mx = W - 1 - x$, then rotate)
  - [x] On-screen Placement Controls panel with tab switcher (`[Details]` vs `[Placement]`) on `ShhhBuilderScreen`
  - [x] Live on-screen display of position, rotation, mirror state, and active layer
  - [x] Interactive directional movement buttons (Left, Fwd, Right, Down, Back, Up) relative to player horizontal facing
  - [x] Interactive rotation (`↶ -90°`, `+90° ↷`) and flip buttons
  - [x] Interactive layer buttons (`▼ Layer`, `Mode`, `Layer ▲`, `Reset: All Layers`)
  - [x] Clean layout with tooltips teaching keybind shortcuts and zero button crowding
  - [x] Session persistence for placement, rotation, mirroring, and layer mode
  - [x] Automated unit test suite (`PlacementManagerTest`, `GhostBlockTest`) verifying layer filtering, layer stepping, mirroring, and rotation coordinate mapping

- [x] **Step 7: Sponge Schematic (.schem) Support**
  - [x] Custom `SchemReader` parsing Sponge Schematic Specification formats (v1, v2, v3)
  - [x] Automatic format detection: root-level compound (v1/v2) vs nested `"Schematic"` compound tag (v3)
  - [x] Continuous VarInt byte stream decoding (`decodeVarIntArray`) for packed `BlockData` arrays
  - [x] Flexible palette parsing supporting `BlockPalette` (v3) and `Palette` (v1/v2)
  - [x] Robust block state parsing from strings (e.g. `minecraft:oak_stairs[facing=north,half=bottom]`) with property key-value extraction and Minecraft block registration
  - [x] Material counting (excluding air variants) and 6-neighbor occlusion culling identical to `.litematic`
  - [x] Safety and error handling for 0-byte, corrupt, missing-palette, missing-data, or oversized (> 2,000,000 blocks) schematics
  - [x] Full integration into `SchematicManager`: details panel, material list HUD, bounding box, ghost preview, rotation, flip, and layer modes all work seamlessly
  - [x] Removed placeholder message ("Reading .schem files is coming in a later update")
  - [x] Automated unit test suite (`SchemReaderTest`) testing VarInt encoding/decoding, v2/v3 synthetic schematics, error handling, and volume limits

- [x] **Step 8: Polish & Release Preparation**
  - [x] Rebound Flip keybind to `Z` (completely unassigned in vanilla Minecraft)
  - [x] Set release version to `1.0.0` in `gradle.properties` and `fabric.mod.json`
  - [x] Verified `fabric.mod.json` metadata (MIT license, author RafayShhh, contact links, client environment, dependencies)
  - [x] Cleaned up debug logging and multiline console dumps across readers
  - [x] Comprehensive stability review: world disconnect listener (`ClientPlayConnectionEvents.DISCONNECT`) to safely clear placements across sessions/worlds
  - [x] First-run help message displayed once on first world join and saved to configuration (`firstRunHelpShown`)
  - [x] Safety and fair-play declaration: purely client-side visual tool, never modifies blocks, never transmits packets to servers
  - [x] Added `README.md`, `LICENSE` (MIT), and `.gitignore` in the project root
  - [x] Compiled production release jar (`build/libs/shhh-builder-1.0.0.jar`)
  - [x] Documented release checklist in `PLAN.md` and release notes in `CHANGELOG.md`

- [x] **Step 9: Real-Game Testing, Bug Hunt & Audit (v1.0.1-dev)**
  - [x] Full 13-area codebase audit with strict verification tagging (`Documentation/AUDIT.md`)
  - [x] Ground-truth validation with real `axe.litematic` and `axe.schem` (23,578 blocks, 23,576 visible blocks)
  - [x] Fixed `LitematicReader` null-state check in `BlockStateHelper.isAir`
  - [x] Implemented 16x16x16 `BlockSection` distance-prioritized sorting for 20,000 block per-frame cap
  - [x] Prevented mod shortcuts from firing when GUI or chat is active (`client.screen != null`)
  - [x] Handled deleted schematic on refresh in `ShhhBuilderScreen`
  - [x] Added viewport boundary clamping to `MaterialHudElement`
  - [x] Added `volatile` modifiers for cross-thread state visibility
  - [x] Single jar packaging (`shhh-builder-1.0.1-dev.jar`) with sources jar disabled
  - [x] Created beginner-friendly in-game test guide (`Documentation/TESTING.md`)

- [x] **Step 10: In-Mod Tutorial Screen & Beginner Guidance (v1.0.1-dev4)**
  - [x] "How to use" button on `ShhhBuilderScreen` (top right, clearly visible)
  - [x] Dedicated 6-page `TutorialScreen` with Back/Next navigation, page counter, and Close button returning to main screen
  - [x] Dynamic key mapping resolution with headless fallback for unit test stability
  - [x] 32-bit ARGB colors (alpha = 0xFF for text) preventing silent drops in Minecraft 26.3
  - [x] First-run welcome message updated to `"Shhh Builder: press V, then How to use."`
  - [x] Empty state tutorial hint line pointing to "How to use" button
  - [x] Schematic file name displayed in HUD title instead of internal metadata name
  - [x] Unit test suite (`TutorialScreenTest`) covering page bounds, content limits, and ARGB alpha (47 tests passing)

- [x] **Step 11: Beginner-Friendliness Polish, Bundled Sample House, & HUD Key Hints (v1.0.1-dev5)**
  - [x] Bundled starter schematic `sample_house.litematic` (182 blocks, 7x5x7, common blocks) inside jar resources
  - [x] Auto-copy sample house to `schematics/` on startup when empty, tracked by `sampleHouseCopied` flag in config
  - [x] Optional beginner key hints line on Material List HUD (`"R turn | Z flip | [ ] layer | H blocks needed"`)
  - [x] Mod Menu setting toggle for HUD key hints (`showKeyHints`)
  - [x] One-time non-spammy action bar notification when 20k render cap skips blocks
  - [x] Plain-language, friendly two-line error messages for load and scan failures
  - [x] Single-sentence empty state guidance ("Pick a file from the list.", "Now click Place Here.")
  - [x] Beginner-friendly terminology across all buttons, tooltips, and action bar messages
  - [x] Tutorial pages updated to mention bundled sample build
  - [x] Unit test suites updated with `SampleHouseTest` and `ConfigAndPaginationTest` expansions (52 tests passing)

- [x] **Step 12: Client-Side Command Set (/shhhbuilder) (v1.0.1-dev6)**
  - [x] Client-side command registration via Fabric API's `ClientCommandRegistrationCallback` (`net.fabricmc.fabric.api.client.command.v2`)
  - [x] Root command `/shhhbuilder` with 11 subcommands (`help`, `list`, `select`, `place`, `place <x y z>`, `move <x y z>`, `rotate`, `flip`, `layer`, `clear`, `folder`)
  - [x] Smart tab-completion suggesting base names without extension where unambiguous and full names where ambiguous
  - [x] 100% client-side execution with zero packets sent to servers
  - [x] Coordinate and height bounds validation against `level.getMinY()` and `level.getMaxY()`
  - [x] Layer validation against loaded schematic height
  - [x] Reused shared backend logic from `PlacementManager` and `SchematicManager`
  - [x] Tutorial Screen Page 6 and README updated to mention `/shhhbuilder help`
  - [x] Unit test suite `ShhhBuilderCommandTest` (7 tests, 59 total tests passing)

- [x] **Step 13: Full-Mod Audit & Release Candidate Hardening (1.0.0-rc1)**
  - [x] Comprehensive 16-area codebase audit (Areas A through P) with verification labels
  - [x] NBT decompression bomb safety (`NbtAccounter` 256 MB quota) in `LitematicReader` and `SchemReader`
  - [x] Path traversal guards in file selection, command arguments, and async loading
  - [x] Atomic configuration writes (`.tmp` + `StandardCopyOption.ATOMIC_MOVE`)
  - [x] Configuration sanitization (clamping `ghostRenderDistance` and `ghostBlockLimit`) and corrupt JSON recovery
  - [x] Dimensions capped at 4096 and total volume capped at 10,000,000 blocks
  - [x] Sponge palette size capped at 65,536 entries
  - [x] Bounded schematic cache (50 entries)
  - [x] Dedicated security and fuzzing test suite (`FuzzAndSecurityTest.java`, 67 total tests passing)
  - [x] Authored `Documentation/ARCHITECTURE.md`, updated `Documentation/AUDIT.md`, `Documentation/TESTING.md`, `README.md`, `CHANGELOG.md`
  - [x] Bumped version to `1.0.0-rc1` across `gradle.properties` and `fabric.mod.json`

- [x] **Step 14: Audit Follow-Up, Performance & Compatibility Hardening (1.0.0-rc2)**
  - [x] Unknown block tracking and reporting (`unknownBlocks()` metric in `LitematicData`, Details panel notice, and `/shhhbuilder select` notice)
  - [x] Corrected HUD API documentation references to `HudElementRegistry.addLast`
  - [x] Section distance sorting cache in `GhostBlockRenderer` to eliminate per-frame allocations and sort overhead
  - [x] Protective per-block shape error handling with single warning logger
  - [x] Responsive layout for small windows down to 640x360 and 854x480 at scale 4
  - [x] Fresh install retest documentation in `Documentation/TESTING.md`

- [x] **Step 15: Item-Accurate Material Counting & Voxel Separation (1.0.0-rc3)**
  - [x] Implement `BlockStateHelper.evaluateItem()` for item vs voxel counting rules (double slabs, doors, beds, tall plants, pistons, waterlogged)
  - [x] Filter unobtainable blocks (water, lava, fire, portals) and report separately
  - [x] Separate 3D voxel count ("Total blocks: N (voxels)") from carried item requirements
  - [x] Update Details panel, Material HUD, and `/shhhbuilder` commands to show item-accurate figures
  - [x] Automated unit test suite with 100% passing tests

- [x] **Step 16: Real See-Through Textured Ghost Blocks, MapColor Outlines & Targeting HUD (1.0.0-rc4)**
  - [x] Research 26.3 rendering pipeline (`SubmitNodeCollector.submitCustomGeometry`, `BakedQuad` model parts, `VertexConsumer.putBakedQuad`, `RenderTypes.translucentMovingBlock`)
  - [x] Document technical findings and feasibility verdict in `Documentation/PREVIEW_RESEARCH.md`
  - [x] Implement `GhostBlockModelHelper` with distinct `BlockState` quad caching
  - [x] Implement textured ghost block rendering via `submitCustomGeometry` with ~0.45 alpha (`0x73`), full-bright light, and biome tints
  - [x] Implement performance guardrails: internal face culling, 20-block distance limit, configurable block limit (1k/3k/6k, default 3k)
  - [x] Implement protective circuit breaker: try/catch around custom geometry, fallback to outlines, action bar alert
  - [x] Implement per-block MapColor wireframe outlines
  - [x] Implement "Looking at: <Block Name> (layer N)" line on HUD overlay with live raycasting
  - [x] Add configuration options in `ShhhBuilderConfig` and `ShhhBuilderConfigScreen`
  - [x] Add unit tests in `GhostBlockTest` and `ConfigAndPaginationTest` (76 total tests passing)

- [x] **Step 17: Textured Ghost Blocks Default, 24-Block Distance, 2,000 Block Limit & 3-Strike Circuit Breaker (1.0.0-rc5)**
  - [x] Enable real see-through textured blocks by default (`previewStyle = PreviewStyle.TEXTURED`)
  - [x] Apply textured rendering to blocks within 24 blocks of player camera, up to 2,000 closest blocks (all others stay as outlines)
  - [x] Skip quads for faces touching any adjacent ghost block in the current placement/layer (`hasBlockAt`)
  - [x] Maintain fixed even light (`PACKED_FULL_BRIGHT = 0x00F000F0`) and fixed tints for grass, leaves, water
  - [x] Preserve real shapes and textures for glass, leaves, flowers, doors, stairs, slabs, torches
  - [x] Cache quads per `BlockState` with zero per-frame list re-allocation or per-frame sorting
  - [x] Streamline Mod Menu options to single "Preview style: Textured / Outlines" toggle
  - [x] Implement 3-strike circuit breaker: per-block model failure falls back to outline and logs once; 3 repeated pipeline errors disable textured mode for session and show action bar alert
  - [x] Update documentation (README, Tutorial page 3, CHANGELOG, TESTING, AUDIT)
  - [x] Automated unit test suite with 100% passing tests (76 passing) and single release artifact (`shhh-builder-1.0.0-rc5.jar`)

- [x] **Step 18: Release 1.0.0 Final Release, Live World Verification, Ghost Pick-Block & Viewport Overhaul**
  - [x] Live World Verification (`WorldBlockValidator`, `GhostBlockRenderer`): queries `mc.level.getBlockState(worldPos)` every frame; exact match skips ghost rendering, wrong state/orientation renders bright Amber/Orange (`0xFFFFAA00`) depth-biased wireframes, wrong block renders bright Red (`0xFFFF3333`) depth-biased wireframes.
  - [x] Crosshair Raycast & Ghost Pick-Block (`GhostBlockRaycastHelper`, `MaterialHudElement`): upgraded raycast to `VoxelShape.clip` for thin blocks; HUD warning display for wrong state/block; intercept vanilla pick-block (`keyPickItem`, Middle Click) within 5 blocks to automatically select matching carried items from hotbar slots 0–8.
  - [x] Batched Translucent Geometry: unified into a single `submitCustomGeometry` call per frame into `RenderTypes.translucentMovingBlock()`, precomputed 24-bit tint RGB at cache time, preserving top-slice boundary faces (`y == currentLayer`, `Direction.UP`) from being culled.
  - [x] Robust World-Switch & Disconnect Cleanup: `ClientPlayConnectionEvents.DISCONNECT`, `ClientLifecycleEvents.CLIENT_STOPPING`, and JVM shutdown hook flush pending checklist progress via atomic file replacement (`MaterialProgressManager.flush()`), reset placement transforms (`PlacementManager.clear()`), and purge ghost render caches.
  - [x] Mathematical Viewport Partitioning & Hotbar Collision Clearance:
    - 12px outer margins (`MARGIN = 12`), `PANEL_Y = 36`, header height 32px, action bar height 26px.
    - Proportional 2-panel grid: left schematics list (`min(240, (width - 36)/3)`), right details card & tabs occupying remaining width.
    - Pinned bottom action bar at `y = height - 36` (height 22px), providing a dedicated Hotbar Clearance Buffer guaranteeing zero collision with vanilla hotbar slots across all window sizes and GUI scales (1–4).
    - Dynamic 5 buttons across `width - 24` with 6px spacing (`buttonWidth = (totalWidth - 24) / 5`).
    - Smart text ellipsis truncation fallback (`..`) in `StyledButton` for compact viewports.
    - Renamed HUD toggle to `"Show on HUD"`.
    - Full-screen dark dimming overlay (`BG_SCREEN_OVERLAY = 0xC0101216`).
    - Removed version badge from the main screen header for a clean, distraction-free "Shhh Builder" title.
    - Overhauled `TutorialScreen` into an interactive 7-page visual showcase with pulsing cyan accent borders, reactive keycap pills with hover glow and micro-tooltips, and floating illustration icons.
  - [x] Version Bump & Release Artifact: bumped version to `1.0.0` in `gradle.properties` and `fabric.mod.json`, verified MIT license, and verified 100% test pass rate across 118 unit tests generating clean `shhh-builder-1.0.0.jar`.

---

## Testing

### How to Launch
From the `26.3` directory, run:
```powershell
.\gradlew runClient
```

### Verification Checklist

#### Step 1 Verification
1. **Mod Menu & Metadata**: On the main title screen, open **Mods**, locate **Shhh Builder**, and click **Configure** to verify the placeholder config screen.
2. **Keybind**: In-game or in **Options > Controls > Key Binds**, find the **"Shhh Builder"** category with default keybind `V`.
3. **Schematics Directory**: Verify that `run/schematics` is automatically created on startup.

#### Step 2 Verification
1. **Empty Folder State**:
   - Ensure the `run/schematics` folder is empty.
   - In-game, press `V`.
   - Verify the message: *"No schematics found. Put .litematic or .schem files in the schematics folder, then press Refresh."* is displayed.
2. **File Scanning & Filtering**:
   - Click **Open Folder** to open the schematics directory in Windows Explorer.
   - Add test files (e.g. `house.litematic`, `tower.schem`, and `readme.txt`).
   - Click **Refresh** in-game.
   - Verify that `house` (.litematic) and `tower` (.schem) appear in the list, while `readme.txt` is ignored.
3. **Selection & Highlighting**:
   - Click on `house`.
   - Verify that the item is highlighted in soft gold with Minecraft's selection outline.
   - Verify that the text below the list updates to: *"Selected: house.litematic"*.
4. **Session Persistence**:
   - Close the screen (using the **Close** button, pressing `V`, or pressing `Escape`).
   - Press `V` again to reopen.
   - Verify that `house.litematic` is still selected and highlighted.
5. **Safe Error Handling**:
   - The screen will display friendly error text without crashing if directory permissions or access fail.

#### Step 3 Verification
1. **Automated Unit Tests**:
   - Run `.\gradlew test` from the `26.3` directory.
   - Verifies bit-width determination, boundary-crossing bit-unpacking, air block filtering, and synthetic `.litematic` generation and parsing.
2. **Metadata & Block Breakdown Display**:
   - Launch the client with `.\gradlew runClient` and press `V`.
   - Select `starter_house.litematic`.
   - Check the right details panel:
     - Name: `Starter House`
     - Author: `RafayShhh`
     - Size: `5 x 4 x 5`
     - Total blocks: `83`
     - Materials list showing top block types (e.g. `minecraft:oak_planks: 53`, `minecraft:stone_bricks: 25`, `minecraft:glass: 4`, `minecraft:torch: 1`).
     - Air blocks (`minecraft:air`) are excluded.
3. **Console Logging**:
   - Inspect the game console/terminal output. Notice the clean `=== Loaded Litematic: Starter House ===` log with full block statistics for easy comparison against Litematica.
4. **Asynchronous Non-Blocking Loading**:
   - Selecting a file displays a brief `"Loading..."` indicator while background threads read and parse the file, ensuring the game never freezes.
5. **Session Caching**:
   - Close the screen and reopen it with `V`.
   - The schematic details are instantly displayed from memory without re-reading the disk.
6. **.schem File Notice**:
   - Click `windmill.schem`.
   - Verify that the right details panel displays: *"Reading .schem files is coming in a later update."*
7. **Error Handling**:
   - Select an empty or 0-byte file (e.g. `big_castle.litematic`).
   - Verify that the screen cleanly shows: *"Could not load schematic: File is empty (0 bytes)."* without any crashes.

#### Step 4 Verification
1. **Automated Unit Tests**:
   - Run `.\gradlew test` from the `26.3` directory.
   - Verifies count formatting (e.g. `53` -> `"53"`, `64` -> `"64 (1 stack)"`, `212` -> `"212 (3 stacks + 20)"`), block name title casing fallback, and config default values / cycling.
2. **In-Game Material List HUD**:
   - Launch the game with `.\gradlew runClient`.
   - Open a world or join a test world.
   - Press **V** to open Shhh Builder and select `starter_house.litematic`.
   - Click the new **"Show materials on screen"** button (or click Close / press V).
   - Verify the HUD panel appears in the top-left corner showing:
     - Title: `Starter House` in bold gold text.
     - Each line with a small item/block icon, the friendly display name (e.g. `Oak Planks`, `Stone Bricks`, `Glass`, `Torch`), and count in green.
     - Semi-transparent dark background with neat border and padding.
3. **Toggle Keybind (H)**:
   - In-game, press **H**.
   - Verify the HUD disappears and an action bar message appears: *"Material List HUD: Hidden"*.
   - Press **H** again.
   - Verify the HUD reappears with message: *"Material List HUD: Shown"*.
   - Check **Options > Controls > Key Binds** to verify "Toggle Material List HUD" is listed under the "Shhh Builder" category.
4. **F1 & Menu Hiding**:
   - With the HUD visible, press **F1** (hide GUI). Verify the material list HUD hides along with the rest of Minecraft's HUD. Press **F1** again to restore.
   - Press **Escape** (pause menu) or open your inventory with **E**. Verify the HUD does not render while a menu/screen is open.
5. **No Schematic Selected**:
   - If no schematic is selected or loaded, verify that the HUD draws nothing on screen.
6. **Mod Menu Configuration**:
   - Open the pause menu or title screen, navigate to **Mods > Shhh Builder > Configure**.
   - Click the **HUD Position** button to cycle through: `Top-Left` -> `Top-Right` -> `Bottom-Left` -> `Bottom-Right`.
   - Click the **HUD Scale** button to cycle through: `Normal (100%)` -> `Large (125%)` -> `Small (75%)`.
   - Click **Done**.
   - Return to the game and verify the HUD updates its position and scale immediately.
   - Inspect `run/config/shhh-builder.json` to verify the settings are saved in clean JSON.

#### Step 5a Verification
1. **Automated Unit Tests**:
   - Run `.\gradlew test` from the `26.3` directory.
   - Verifies placement creation, rotation normalization (0°, 90°, 180°, 270°), dimension swapping for rotated footprints, directional nudging relative to facing angle, and visibility toggling.
2. **"Place Here" Button**:
   - Launch the game with `.\gradlew runClient` and open a world.
   - Stand on a block, press **V**, and select `starter_house.litematic`.
   - Verify that the **Place Here** button is active (and **Clear** is disabled when no placement exists).
   - Click **Place Here**.
   - The screen should close, and a bright cyan outline box should appear in the world starting at the block you stood on.
3. **Footprint Outline Box**:
   - Inspect the outline box: it should match the schematic's dimensions ($5 \times 4 \times 5$ for `starter_house.litematic`).
   - The lines use depth bias so they remain clearly visible against terrain.
4. **Keybind B (Toggle Outline)**:
   - Press **B**.
   - The outline box disappears, and the action bar displays: *"Schematic Outline: Hidden"*.
   - Press **B** again.
   - The outline box reappears, and the action bar displays: *"Schematic Outline: Shown"*.
5. **Keybind R (Rotate)**:
   - Look at the outline and press **R**.
   - The schematic rotates clockwise by 90°.
   - If using a rectangular schematic (e.g. $10 \times 4 \times 5$), verify that 90° and 270° swap the width and length ($5 \times 4 \times 10$).
   - The action bar displays: *"Schematic Rotated: 90°"* (then 180°, 270°, 0°).
6. **Nudge Keybinds (Movement)**:
   - Look in a direction (e.g., North, South, East, or West).
   - Press **Arrow Up**: outline box moves 1 block forward in the direction you are facing.
   - Press **Arrow Down**: outline moves 1 block backward.
   - Press **Arrow Left**: outline moves 1 block left.
   - Press **Arrow Right**: outline moves 1 block right.
   - Press **Page Up**: outline moves 1 block upward.
   - Press **Page Down**: outline moves 1 block downward.
   - Each press displays an action bar confirmation with the new coordinates (e.g. *"Nudged North to (X, Y, Z)"*).
7. **Controls Menu**:
   - Open **Options > Controls > Key Binds**.
   - Verify that under the **"Shhh Builder"** category all new keys are listed and rebindable:
     - *Toggle Schematic Outline* (`B`)
     - *Rotate Schematic* (`R`)
     - *Nudge Forward* (`Up`)
     - *Nudge Backward* (`Down`)
     - *Nudge Left* (`Left`)
     - *Nudge Right* (`Right`)
     - *Nudge Up* (`Page Up`)
     - *Nudge Down* (`Page Down`)
8. **Clear Placement**:
   - Press **V** to open the Shhh Builder screen.
   - Notice the status line under details showing current placement coordinates and rotation: `Placement: X, Y, Z (rot°)`.
   - Click **Clear**.
   - Notice the placement status changes to `Placement: None`, and the outline is removed from the world.
9. **Dimension Separation**:
   - Place a schematic in the Overworld.
   - Travel to the Nether.
   - Verify that the Overworld outline box is not rendered in the Nether.

#### Step 5b Verification
1. **Automated Unit Tests**:
   - Run `.\gradlew test` from the `26.3` directory.
   - Verifies 6-neighbor occlusion culling (center block of solid 3x3x3 cube is culled, hollow cube is preserved), rotation coordinate transformations (0°, 90°, 180°, 270°), volume limit enforcement (> 2,000,000 blocks), and config ghost render distance cycling.
2. **In-World Ghost Hologram**:
   - Launch the game with `.\gradlew runClient` and open a world.
   - Select `starter_house.litematic` and click **Place Here**.
   - Observe inside the cyan outline box: all non-air blocks are rendered as crisp, color-coded holographic wireframes:
     - Wood/planks/roof in warm amber (`#D7A15C`).
     - Stone bricks/floor in soft cyan-blue (`#90CAF9`).
     - Glass windows in ice white-cyan (`#E0F7FA`).
     - Torch in warm red/orange (`#FF8A80`).
3. **Toggle Ghost Blocks (G)**:
   - Press **G**.
   - Ghost blocks hide, and the action bar displays: *"Ghost Preview: Hidden"*.
   - Notice that the bounding box outline (toggled by **B**) remains visible.
   - Press **G** again.
   - Ghost blocks reappear with message: *"Ghost Preview: Shown"*.
4. **Rotation with Directional Blocks (R)**:
   - Press **R** to rotate the schematic by 90°.
   - Observe the ghost blocks rotate accordingly. Directional blocks (stairs, logs, torches) rotate to match their new orientation.
5. **Nudge Movement (Arrow Keys & Page Up/Down)**:
   - Use the arrow keys or Page Up/Down to move the placement.
   - Observe the ghost blocks move cleanly together with the outline box.
6. **Mod Menu Ghost Render Distance**:
   - Open **Mods > Shhh Builder > Configure**.
   - Click the **Ghost Render Distance** button to cycle: `24 blocks` -> `32 blocks` -> `48 blocks` -> `64 blocks`.
   - Click **Done**, return to the world, and walk away from the schematic to verify blocks beyond the distance setting are culled smoothly.
7. **Volume Limit & Safety**:
   - For schematics exceeding 2,000,000 blocks, ghost preview is cleanly disabled with a descriptive notice, while outline box and materials list continue to function smoothly.

#### Step 6 Verification
1. **Automated Unit Tests**:
   - Run `.\gradlew test` from the `26.3` directory.
   - Verifies layer mode cycling (All -> Single -> Up To -> All), layer stepping clamping ([1, maxHeight]), left-to-right mirroring state, layer filtering (All mode includes all blocks, Single layer includes only matching Y, Up To includes Y and lower), and mirror coordinates with rotation.
2. **On-Screen Placement Tab & Live Status**:
   - Launch the game with `.\gradlew runClient` and open a world.
   - Press **V** to open Shhh Builder and select `starter_house.litematic`.
   - Click the **[Placement]** tab button in the upper right.
   - Observe live status lines:
     - `Pos: Not placed in world` (red) or coordinates `Pos: X, Y, Z` (aqua) once placed.
     - `Rot: 0° | Mirror: No`.
     - `Layer: All layers`.
3. **On-Screen Directional Movement**:
   - Click **Place Here** at your current feet position, then press **V** and return to the **[Placement]** tab.
   - Click **◄ Left**, **▲ Fwd**, **Right ►**, **Down ▼**, **▼ Back**, **Up ▲**.
   - Observe the live position coordinates update, and when closing the screen, the in-world schematic has moved by 1 block relative to your player facing direction.
4. **On-Screen Rotation & Flip (Mirror)**:
   - Click **↶ -90°** or **+90° ↷** on the Placement tab.
   - Click **Flip**. Notice the button changes to `Flip: ON` and the status line updates to `Mirror: Yes`.
   - Close the screen and observe the ghost hologram: the structure is mirrored left-to-right, with directional blocks (stairs, doors) mirrored appropriately.
   - Press **X** in-game (rebound from F): observe action bar confirmation *"Schematic Flipped: Mirrored"* / *"Schematic Flipped: Normal"*.
5. **Layer Modes (All, Single, Up To)**:
   - In-game or on the screen, press **M** (or click **Mode** button).
   - Mode switches from *All layers* to *Single layer*, defaulting to layer 1.
   - Action bar shows: *"Layer 1 / 4 (single layer)"*.
   - In the world, observe only the floor layer (layer 1) is visible as ghost blocks. The cyan footprint outline remains full height for spatial orientation.
   - Press **]** (or click **Layer ▲**): advances to layer 2 (walls/windows), then layer 3, then layer 4 (roof).
   - Press **[** (or click **▼ Layer**): moves back down layer by layer.
   - Press **M** again: switches to *Up to layer* mode.
   - Observe layer 1 and layer 2 are shown simultaneously, with upper layers hidden.
   - Press **\** (or click **Reset: All Layers**): immediately resets to showing all layers with notification *"Layer mode: All layers"*.
6. **Material List HUD Layer Indicator**:
   - Ensure HUD is visible (**H**).
   - In *Single* or *Up to* mode, observe the extra layer indicator line on the HUD (e.g. `Layer 2/4 (Single)`).
   - In *All* mode, the extra line is hidden to keep the HUD clean.
7. **Tooltips & Keybind Ergonomics**:
   - Hover over every button on the screen: tooltips clearly describe what each button does and state its keyboard shortcut.
   - Test all shortcuts in Options > Controls > Key Binds under the "Shhh Builder" category. All keybinds can be rebound if desired.

#### Step 7 Verification
1. **Automated Unit Tests**:
   - Run `.\gradlew test` from the `26.3` directory.
   - Verifies VarInt encoding/decoding, Sponge v2 format reading, Sponge v3 nested format reading, empty file handling, corrupt file rejection, missing data handling, and 2M volume limit enforcement.
2. **Loading Sponge (.schem) Schematics**:
   - Place any `.schem` file into the `schematics/` folder.
   - Launch Minecraft with `.\gradlew runClient` and press **V**.
   - Select the `.schem` file from the list.
   - Verify that the previous *"Reading .schem files is coming in a later update"* notice is gone.
   - Observe that the details panel immediately displays the schematic name, author, dimensions, total non-air blocks, and materials breakdown just like `.litematic` files.
3. **In-World Hologram & Material HUD**:
   - Click **Place Here** to position the `.schem` schematic in the world.
   - Observe the bright cyan bounding box outline matching the schematic's Width, Height, and Length.
   - Observe the ghost wireframe blocks rendered inside the outline, including directional blocks (e.g., stairs, torches).
   - Observe the Material List HUD (**H**) displaying required blocks and stack counts.
4. **Hologram Controls with .schem**:
   - Test rotation (**R** or on-screen button): turns the `.schem` blocks by 90°.
   - Test flip (**Z** or on-screen button): mirrors the `.schem` blocks left-to-right.
   - Test nudge movement (arrow keys or on-screen button pad): shifts the placement by 1 block.
   - Test layer modes (**M**, **]**, **[**, **\** or on-screen buttons): isolates single layers or builds cumulatively.

#### Step 8 Verification & Release Checklist
1. **Automated Unit Tests**:
   - Run `.\gradlew test` from the `26.3` directory.
   - All tests must pass (Litematic unpacking, 6-neighbor culling, volume limits, HUD helpers, layer modes, mirroring, VarInt decoding, and Sponge v2/v3 parsing).
2. **Keybind Verification**:
   - Verify that **Z** toggles flip/mirroring in-game without conflicting with vanilla offhand swapping (**F**).
   - In Options > Controls > Key Binds, confirm all keys are listed under the "Shhh Builder" category.
3. **First-Run Help Notification**:
   - Delete or edit `config/shhh-builder.json` to have `"firstRunHelpShown": false`.
   - Join a world and verify that the message: *"Shhh Builder: put .litematic or .schem files in the schematics folder and press V."* appears in the action bar and chat.
   - Rejoin or open a different world and verify the message does not appear again.
4. **Stability & Edge Cases**:
   - Join a world with no schematic placed or selected (verify no crashes).
   - Place a schematic, switch dimensions (e.g. Overworld to Nether), and verify ghost blocks cleanly hide.
   - Disconnect from world back to title screen and verify placement is safely cleared.
   - Select a schematic, delete the file from the `schematics` folder in Explorer, return to game, and verify the mod handles the missing file gracefully without crashing.
   - Open Mod Menu configuration while a schematic is placed and change HUD position, HUD scale, and Ghost render distance (verify live updates with no errors).
5. **Release Distribution**:
   - Run `.\gradlew build` in the `26.3` directory.
   - Verify that the production artifact is `26.3/build/libs/shhh-builder-1.0.0.jar`.
   - Verify that no test files or debug schematics are included in the archive.
   - Verify that root documentation (`README.md`, `LICENSE`, `.gitignore`) is complete and up to date.

#### Step 18 Verification (v1.0.0 Final Release)
1. **Automated Unit Tests**:
   - Run `.\gradlew test` from the `26.3` directory.
   - 100% pass rate across all 118 unit tests in 19 test suites (`WorldBlockValidatorTest`, `WorldSwitchAndDisconnectTest`, `UiStyleLayoutTest`, `MaterialCountingEngineTest`, `GhostBlockTest`, `PlacementManagerTest`, `FuzzAndSecurityTest`, etc.).
2. **Live World Verification**:
   - Exact match: blocks placed correctly in the world are hidden from the ghost preview.
   - Wrong orientation (same block): renders Amber/Orange (`0xFFFFAA00`) depth-biased outline.
   - Wrong block: renders bright Red (`0xFFFF3333`) depth-biased outline.
   - HUD crosshair (`VoxelShape.clip`) shows `"Looking at: Wrong orientation / Wrong block"`.
3. **Ghost Pick-Block**:
   - Aiming at unplaced ghost block within 5 blocks and pressing Middle Click (`keyPickItem`) selects matching carried item from hotbar slots 0–8.
4. **Viewport Layout Math & Hotbar Clearance**:
   - 12px outer margins, `PANEL_Y = 36`, header height 32px, action bar pinned at `y = height - 36` (height 22px).
   - Dedicated Hotbar Clearance Buffer completely prevents overlap with vanilla hotbar across all resolutions (640x360 to 1920x1080) and GUI scales (1–4).
   - `UiStyleLayoutTest` verifies that panel bounds, tab buttons, and all 5 bottom buttons stay strictly within screen bounds.
5. **Interactive Tutorial Screen**:
   - 7 tutorial pages with pulsing cyan accent border, reactive keycap pills (`[V]`, `[R]`, `[Z]`, `[K]`, `[M]`, etc.) with hover glow and micro-tooltips, and floating illustration icons.
6. **Session Disconnect Cleanup**:
   - Disconnecting or stopping client clears active placements and flushes checklist progress atomically.
