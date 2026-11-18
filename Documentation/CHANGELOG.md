# Changelog

All notable changes to the "Shhh Builder" project will be documented in this file.

## [1.0.0] - Official Release - The Beginner-Friendly Holographic Schematic Viewer

**Shhh Builder v1.0.0** is the official debut release for Minecraft 26.3 on Fabric (Client-Only). It combines holographic textured previews, live in-world verification, instant ghost pick-block, exact carried item counting, an interactive materials checklist, and a sleek dark-neutral UI design.

### Summary of Features:
- **Dual Format Support**: Loads `.litematic` (Litematica) and `.schem` (Sponge v1/v2/v3) schematics from `.minecraft/schematics` with automatic sub-region merging and 6-neighbor occlusion culling.
- **Holographic In-World 3D Preview**:
  - **See-Through Textured Ghost Blocks**: Real blocks rendered via Minecraft's translucent moving-block atlas pipeline with quad alpha injection (~45% opacity default, live adjustable 10%–100%).
  - **Single Batched Submission**: Batches up to 2,000 textured blocks into a single `submitCustomGeometry` call per frame into `RenderTypes.translucentMovingBlock()` to maximize FPS.
  - **Block Borders**: Subtle thin outlines around textured blocks to distinguish individual blocks in 3D.
  - **Wireframe Mode**: MapColor-tinted outlines for low-end systems or wireframe preference.
  - **3-Strike Circuit Breaker**: Auto-falls back to wireframe outlines if rendering fails, preventing game crashes.
- **Automatic Live World Verification**:
  - Live inspection against `mc.level.getBlockState(worldPos)` every frame.
  - **Exact Match**: Automatically hides ghost blocks when placed correctly in the world.
  - **Wrong Orientation Outline**: Renders bright Amber/Orange (`0xFFFFAA00`) depth-biased wireframes over blocks placed with wrong orientation or properties.
  - **Wrong Block Outline**: Renders bright Red (`0xFFFF3333`) depth-biased wireframes over incorrect solid blocks.
- **VoxelShape Clip Crosshair Raycast & Ghost Pick-Block**:
  - Uses `VoxelShape.clip` for exact hit detection on thin blocks (panes, torches, stairs, slabs, fences).
  - Displays `"Looking at: Wrong orientation (Expected: ...)"` or `"Looking at: Wrong block (Expected: ...)"` in warning red (`0xFFFF5555`).
  - **Ghost Pick-Block**: Pressing vanilla pick-block (`keyPickItem`, Middle Click) while aiming at an unplaced preview block automatically selects the matching item from hotbar slots 0–8 without opening inventory or sending custom packets.
- **Exact Carried Item Counting Engine (`MaterialCountingEngine`)**:
  - Evaluates real 26.3 block properties to count physical carried items rather than voxels (double slabs = 2, lower door/bed halves only, stackable items, wall mounts, potted plants, cauldrons, waterlogged blocks, unobtainable blocks filtered).
- **Interactive Materials Checklist Screen (Key `K`)**:
  - Visual progress bar, real-time search box, sort cycling (Most needed, A-Z, Not done first), layer filter toggles with step buttons, 250ms throttled inventory scanning with nested shulker box inspection, shulker math tooltips (`"23,578 = 368 stacks + 26, about 14 shulker boxes"`), creative-only badges, and atomic JSON progress persistence (`config/shhh-builder-progress.json`).
- **In-Game Material HUD (Key `H`)**:
  - On-screen overlay with dual display modes (*Missing only* vs *Everything*), live crosshair pointed-block indicator, top progress tracker, and completion state (`"✔ All materials ready!"`).
- **In-World Placed Block Deduplication (`effectiveHave = inventoryHave + placedInWorld`)**:
  - Live 250ms throttled in-world placed block scanner (`PlayerInventoryScanner.scanPlacedBlocks`) that inspects the active placement in `mc.level`: when a block is placed matching the expected schematic state (`worldState == expectedState`), its carried items are evaluated via `MaterialCountingEngine.evaluate(expectedState)`.
  - Added to effective collected items (`effectiveHave = inventoryHave + placedInWorld`) in both `MaterialsScreen` and `MaterialHudElement`, guaranteeing that placing blocks in survival never increases the "Missing" material counts.
- **Professional UI Design System & Viewport Layout Overhaul**:
  - Fixed Z-index rendering order across all screens (`MaterialsScreen`, `TutorialScreen`, and `ShhhBuilderConfigScreen`): strict 5-stage pipeline (backdrop -> panel fills -> custom content -> `super.extractRenderState` -> final tooltips), ensuring all buttons render cleanly on top without dimming.
  - Enhanced disabled `StyledButton` styling (`btn.active == false`) with legible borders (`0xFF30363D`) and muted text (`0xFF8B949E`).
  - Implemented running X-accumulator (`currentX += btnWidth + spacing`) across bottom action bars in `ShhhBuilderScreen` and `MaterialsScreen` (`[Check All]`, `[Uncheck All]`, `[Copy List]`, `[Reset]`, `[Close]`), eliminating gap jumps when remainder pixels are distributed.
  - Engineered a mathematically bulletproof responsive split-pane layout for `ShhhBuilderScreen` with 12px outer margins, `PANEL_Y = 36`, and proportional 2-panel column splitting (left schematic list: `min(240, (width - 36)/3)`, right details panel occupying remaining width), ensuring panels never collapse or clip off-screen.
  - Implemented a dedicated Hotbar Clearance Buffer pinning the bottom action bar at `y = this.height - 36` (button height 22px), guaranteeing zero collision or overlap with vanilla hotbar item stacks or experience bars across all window resolutions and GUI scales (1–4).
  - Dynamically distributed all 5 action buttons (`[Place Here]`, `[Clear Build]`, `[Refresh]`, `[Open Folder]`, `[Close]`) across `width - 24` with 6px spacing (`buttonWidth = (totalWidth - 24) / 5`).
  - Added smart string truncation with `..` suffix fallback in `StyledButton` when button width is constrained on ultra-compact viewports.
  - Renamed the HUD toggle button to `"Show on HUD"` for immediate clarity.
  - Rendered a full-screen dark dimming overlay (`BG_SCREEN_OVERLAY = 0xC0101216`) in `renderBackground` ensuring in-world elements never distract from UI interactions.
  - Removed version badge from the main screen header for a clean, distraction-free "Shhh Builder" title.
  - Upgraded text contrast tokens across Details, Blocks Needed, and Position tabs to bright high-contrast tokens (`TEXT_BODY #E6EDF3` and `TEXT_HEADER #F0F6FC`) with full 32-bit ARGB alpha (`0xFF...`).
  - Completely overhauled `TutorialScreen` into an interactive showcase: animated pulsing cyan accent borders, reactive keycap pills (`[V]`, `[R]`, `[Z]`, `[K]`, `[M]`, etc.) with hover glow and micro-tooltips, floating illustration icons with dynamic vertical offset and ground shadows, clickable progress dots, and high-contrast typography.
- **Client Commands (`/shhhbuilder`)**:
  - Full client-side command suite: `/shhhbuilder`, `/shhhbuilder open`, `/shhhbuilder materials`, `/shhhbuilder checklist`, `/shhhbuilder hud`, `/shhhbuilder tutorial`, `/shhhbuilder selftest`, `/shhhbuilder clear`, and `/shhhbuilder help`.
- **Robust World-Switch & Disconnect Cleanup**:
  - `ClientPlayConnectionEvents.DISCONNECT` and `ClientLifecycleEvents.CLIENT_STOPPING` listeners thoroughly clear active placements (`PlacementManager.clear()`), reset camera & ghost renderer caches (`GhostBlockRenderer.resetOnDisconnect()`), unload active schematic sessions (`SchematicManager.setSelectedFileName(null)`), and safely flush pending checklist progress to disk via atomic file replacement (`MaterialProgressManager.flush()`).
  - Added JVM shutdown hook ensuring progress is saved even upon sudden client termination.
- **First-Run Experience**:
  - Auto-copies `sample_house.litematic` when the schematics folder is empty, shows a one-time welcome chat link, and provides an interactive 7-step tutorial card guide (`TutorialScreen`).

## [1.0.0-rc9] - Live World Verification, Ghost Pick-Block & Performance Optimization

### Added & Changed
- **Live World Verification (`GhostBlockRenderer.java`, `WorldBlockValidator.java`)**:
  - Automatically queries `mc.level.getBlockState(worldPos)` for every block inside the placement.
  - **Exact Match**: If the in-world block matches the expected schematic block state identically (`worldState == expectedState`), the preview block is skipped completely (the player already built it).
  - **Wrong State (Same Block)**: If a block of the same type is placed but with differing orientation or properties (e.g. stair or log facing the wrong direction), renders a bright Amber/Orange wireframe outline (`0xFFFFAA00`) using `RenderTypes.linesDepthBias()`.
  - **Wrong Block**: If a completely different solid block is placed in that position, renders a bright Red wireframe outline (`0xFFFF3333`) using `RenderTypes.linesDepthBias()`.
  - **Missing**: Air or fluid positions proceed to normal ghost preview rendering.
- **Crosshair Raycast with VoxelShape Clip (`MaterialHudElement.java`, `GhostBlockRaycastHelper.java`)**:
  - Upgraded crosshair targeting to use `VoxelShape.clip(eyePos, reachEndPos, worldPos)` for exact, pixel-perfect hit detection on thin blocks (panes, torches, slabs, stairs, fences).
  - When aiming at a block with wrong orientation, the HUD displays `"Looking at: Wrong orientation (Expected: <Block Name> [properties])"` in bright warning color `0xFFFF5555`.
  - When aiming at a wrong block, the HUD displays `"Looking at: Wrong block (Expected: <Block Name> [properties])"` in `0xFFFF5555`.
- **Client-Side Ghost Pick-Block (`GhostBlockRaycastHelper.java`, `ShhhBuilderClient.java`)**:
  - Intercepts vanilla pick-block (`keyPickItem`, default Middle Click) in `START_CLIENT_TICK` when aiming at an unplaced preview block within 5 blocks.
  - Resolves required item IDs via `MaterialCountingEngine.evaluate(expectedState)`.
  - Scans player hotbar slots 0-8; if a matching carried item is found, automatically switches to that slot via `player.getInventory().setSelectedSlot(slot)`.
  - Purely client-side with zero custom or non-vanilla network packets.
- **Comprehensive Unit Tests (`WorldBlockValidatorTest.java`)**:
  - 6 unit test cases verifying state comparison rules: exact match, wrong state/orientation, wrong block, missing (air/fluid), color constants, and detail string formatting against real bootstrapped Minecraft registries.
- **Batched Textured Quad Submission (`GhostBlockRenderer.java`, `GhostBlockModelHelper.java`)**:
  - Replaced individual per-block `SubmitNodeCollector.submitCustomGeometry` invocations with a single batched `submitCustomGeometry` call per frame into `RenderTypes.translucentMovingBlock()`.
  - Translated a reusable local `PoseStack.Pose` inside the geometry callback for each block offset, avoiding creating up to 2,000 separate submit nodes and pose clones per frame.
  - Precomputed 24-bit block tint RGB at model cache time in `GhostBlockModelHelper.CachedGhostModel`, eliminating all per-quad string allocations during rendering.
  - Preserved the per-block `try/catch` and 3-strike circuit breaker intact.
- **Top Slice Boundary Face Culling Fix (`GhostBlockRenderer.java`)**:
  - Added `shouldCullFace(...)` in `GhostBlockRenderer` to ensure that when `LayerMode` is `SINGLE` or `UP_TO`, faces on the active top slice boundary (`y == currentLayer`, `Direction.UP`) are NEVER culled by hidden upper layers.
  - Added unit test `testTopSliceBoundaryFacesNeverCulledInSingleOrUpTo` in `GhostBlockTest.java`.
- **Quality Presets & Config Synchronization (`ShhhBuilderConfig.java`, `ShhhBuilderConfigScreen.java`, `ConfigAndPaginationTest.java`)**:
  - Added `texturedBlockLimitButton` in the Performance section of `ShhhBuilderConfigScreen` to expose `texturedBlockLimit` (1,000 / 2,000 / 3,000 / 6,000, default 2,000).
  - Fully synchronized Quality Presets (Low / Balanced / High / Custom), `texturedDistance` (16 / 24 / 32, default 24), `texturedBlockLimit` (1,000 / 2,000 / 3,000 / 6,000, default 2,000), and `ghostBlockLimit` (5,000 / 10,000 / 20,000, default 10,000) across config, settings UI, and unit tests.
- **Fix X-Mirror Directional Facing (`PlacementManager.java`)**:
  - Replaced `BlockState.mirror(Mirror.LEFT_RIGHT)` with `BlockState.mirror(Mirror.FRONT_BACK)` so East/West directional blocks (stairs, doors, beds, chests) match the X-axis coordinate reflection (`mx = width - 1 - x`). Verified composition with clockwise/counter-clockwise rotations.
- **Fix Layer Slicing Occlusion Culling (`PlacementManager.java`, `SchematicBlockData.java`, `LitematicReader.java`, `SchemReader.java`)**:
  - Modified parse-time 6-neighbor occlusion culling to tag enclosed blocks with `occludedY` instead of permanently discarding them.
  - In `PlacementManager`, Y-neighbor culling is applied when `LayerMode == ALL`, but disabled for exposed boundary faces when slicing horizontally (`LayerMode.SINGLE` or at `currentLayer` in `LayerMode.UP_TO`), ensuring solid floors never look hollow.
  - Added unit test in `PlacementManagerTest.java` and `GhostBlockTest.java` confirming that Layer 2 in a solid 3x3x3 cube contains all 9 blocks in both `SINGLE` and `UP_TO` modes.
- **Materials Checklist Screen (`MaterialsScreen.java`)**:
  - Dedicated interactive checklist window opened via Key `K`, the header `[Materials]` button on the main screen, the `[Checklist]` button in the Details tab, or chat commands `/shhhbuilder materials` / `/shhhbuilder checklist`.
  - **Progress Bar**: Styled top progress bar showing total items checked/needed and completion percentage (`"Materials: 12/28 ready (45%)"`).
  - **Filter & Search Controls**: Real-time search box filtering items by name or ID.
  - **Sort Cycling**: Toggle between *Most needed*, *A to Z*, and *Not done first*.
  - **Filtering Toggles**:
    - *Hide done*: Collapses completed items to focus on what still needs to be gathered.
    - *This layer only*: Filters item list to the active placement layer, with step buttons `[◀]` and `[▶]` for live layer navigation.
    - *Include shulkers*: Integrates shulker box container inspection during player inventory scanning.
  - **Interactive Material Rows**:
    - Checkbox toggle for manual marking with persistent state saving.
    - Real item icons rendered via `fakeItem`.
    - Friendly localized block names with clear warning badges for creative-only blocks (`"Creative only"`).
    - Need / Have / Missing breakdown.
    - Detailed hover tooltips with stack breakdown and shulker box math (e.g. `"23,578 = 368 stacks + 26, about 14 shulker boxes"`).
  - **Action Bar Footer**: One-click actions for `Check all`, `Uncheck all`, `Copy list` (copies human-readable materials list to system clipboard), and `Reset`.
  - **Automatic Inventory Scanner (`PlayerInventoryScanner.java`)**:
    - Scans player main inventory and offhand with safe 250ms throttling to eliminate tick lag.
    - Deep inspection of nested items inside shulker boxes (using `DataComponents.CONTAINER`).
  - **Progress Persistence (`MaterialProgressManager.java`)**:
    - Persists checklist check marks per schematic key to `config/shhh-builder-progress.json` with safe atomic file replacement (`StandardCopyOption.ATOMIC_MOVE`).
- **Exact Carried Item Counting Engine (`MaterialCountingEngine.java`)**:
  - Independent, testable engine that evaluates real 26.3 block states and properties to count physical carried items rather than voxels:
    - **Slabs**: `type=double` counts as 2 slab items.
    - **Doors**: Counts only `half=lower` (1 door item per door).
    - **Beds**: Counts only `part=foot` (1 bed item per bed).
    - **Double-Height Plants & Flowers**: Counts only `half=lower` (1 item per plant), with `tall_seagrass` mapping to `seagrass`.
    - **Pistons**: Skips `piston_head` and `moving_piston`; piston bases count as 1 piston item.
    - **Multi-Amount Blocks**: Evaluates count properties (`candles`, `pickles`, `eggs`, `layers`, `flower_amount`, `segment_amount`) to multiply item requirements accurately.
    - **Wall Mounts**: Automatically maps wall variants (`wall_torch`, `wall_sign`, `wall_banner`, `redstone_wire`) to their carried inventory items.
    - **Potted Plants**: Resolves both the flower pot (1x `flower_pot`) AND the contained plant/flower.
    - **Cauldrons & Produce**: Maps water/lava/powder snow cauldrons to `cauldron`; stem crops (`pumpkin_stem`, `melon_stem`) to respective seeds.
    - **Waterlogged Blocks**: Evaluates only the host block, ignoring water voxels.
    - **Unobtainable Blocks**: Separates unobtainables (water, lava, fire, nether portal, end portal) with a clear summary count (`"N blocks have no item (water, fire...)"`).
    - **Creative-Only Items**: Badged using bundled resource `creative_only_items.txt`.
    - **Layer Indexing**: Caches item counts per Y layer for instant layer filtering.
- **Dual HUD Display Modes & Ready State (`MaterialHudElement.java`)**:
  - Added configurable HUD mode: *Missing only* (shows only items the player still needs to gather) vs *Everything* (shows all build materials).
  - Displays top progress counter (`"Materials: 12/28 ready (45%)"`).
  - Displays cheerful completion message when all materials are gathered: `"✔ All materials ready!"`.
- **Comprehensive Block Coverage & Test Suite (`BlockShowcaseGeneratorTest.java`)**:
  - Audited all 1,397 registered blocks in Minecraft 26.3 across all categories: normal solids, slabs, stairs, doors, plants, technical blocks, wall mounts, and block entities.
  - Generates full verification artifacts: `build/test-files/block_showcase.litematic`, `block_showcase.schem`, and `block_showcase_expected.txt`.
  - Added `BLOCK_COVERAGE.md` detailing exact category coverage and special mapping rules.
  - Added `/shhhbuilder selftest` output auditing material mapping engine with 0 errors.
- **Updated Tutorial**:
  - Added dedicated step "3. Collect Your Materials" in `TutorialScreen.java` detailing the Materials checklist (`K`), inventory tracking, and shulker box math.

## [1.0.0-rc8] - Professional UI Redesign: Dark Neutral Panels, Cyan Accents, Search Filtering, Step-by-Step Tutorial & Responsive Layout

### Added & Changed
- **Unified Design System (`UiStyle.java`)**:
  - Implemented a shared helper class (`UiStyle`) providing dark neutral surfaces (`#141619`, `#1C2024`), crisp 1px borders (`#30363D`), and primary Cyan accents (`#00E5FF`).
  - Standardized on full 32-bit ARGB colors everywhere (ensuring full visibility in Minecraft 26.3).
  - 8px grid spacing, consistent margins, and clear typography hierarchy (`TEXT_TITLE`, `TEXT_HEADER`, `TEXT_BODY`, `TEXT_MUTED`, `TEXT_DIM`).
  - Added `UiStyle.StyledButton` supporting `NORMAL`, `PRIMARY`, `DANGER`, and `SUBTLE` styles with custom hover states and embedded keycap pill badges.
- **Main Screen Redesign (`ShhhBuilderScreen`)**:
  - **Header**: Added mod icon (`icon.png`), bold title, version badge (`v1.0.0-rc8`), and top-right `"How to use"` and `"Settings"` shortcut buttons.
  - **Left Panel ("Schematics")**: Added a real-time search box (`EditBox`) filtering files on keystroke; interactive file cards with format badges (`.litematic` / `.schem`), dimensions & block counts, and cyan accent borders on selection; paged navigation footer.
  - **Right Panel**: Top details card with build name, author, dimension chips (`153 x 160 x 29`), and green block counts; tab switcher (`Blocks needed` vs `Position`).
  - **Blocks Needed Tab**: Scrollable list with item icons (`fakeItem`), friendly names, and stack calculations (e.g. `2,357 (36 stacks + 53)`).
  - **Position Tab**: Clean grouped controls for Move, Orientation, Layers, and Opacity, each displaying its bound key as a keycap pill badge (`[←]`, `[↑]`, `[→]`, `[PgDn]`, `[↓]`, `[PgUp]`, `[R]`, `[Z]`, `[[]`, `[]]`, `[M]`, `[\\]`).
  - **Bottom Action Bar**: Proportional 5-button bar featuring primary `"Place Here"` (cyan accent), `"Clear Build"`, `"Refresh"`, `"Open Folder"`, and `"Close"`.
  - **Cards for States**: Clear status cards for loading, empty folder, empty search results, and load errors with plain next steps.
- **Step-by-Step Tutorial Redesign (`TutorialScreen`)**:
  - Modern card container with progress dots (active dot in cyan), step badge (`STEP 1 OF 6`), title, illustration item icons (`Items.CHEST`, `COMPASS`, `STRUCTURE_BLOCK`, `RECOVERY_COMPASS`, `SCAFFOLDING`, `EXPERIENCE_BOTTLE`), and 2-3 short lines with keycaps.
  - Added `/shhhbuilder tutorial` client command.
  - Added clickable `[Open tutorial]` in the first-run chat welcome message.
  - Added a `"Skip"` option alongside `[◀ Back]`, `[Close]`, and `[Next ▶]` (or `[Finish]`).
- **Settings Screen Redesign (`ShhhBuilderConfigScreen`)**:
  - Grouped into `"Preview"`, `"Performance"`, and `"HUD"` sections with a top section tab bar.
  - Dynamic 1-line hover description bar explaining whichever setting is currently hovered or focused.
  - Added a dedicated `"Reset Defaults"` button.
  - Mouse wheel scrolling cycles through setting sections without vertical overflow.
- **HUD Redesign (`MaterialHudElement`)**:
  - Styled with dark neutral panel and 1px border matching `UiStyle`.
  - Displays schematic title, layer mode chip, pointed block indicator, item icons, and stack-formatted counts.
- **Wording Pass & Terminology**:
  - Replaced all jargon: always use "preview" (never "ghost"), "blocks" (never "voxels"), and "schematics folder" (never absolute paths).
- **Responsive Layout & Unit Tests**:
  - Added `UiStyleLayoutTest.java` verifying that Main, Settings, Tutorial, and HUD layouts remain strictly inside screen bounds with zero overlap from 640x360 to 1920x1080 across GUI scales 1 to 4.
  - All 92 unit tests passing.

## [1.0.0-rc7] - Textured Preview Options: Opacity Slider, Block Borders, Quality Presets, Universal Block Coverage & Self-Test

### Added & Changed
- **Preview Opacity Slider & Live Stepping**:
  - Added a real slider "Preview opacity" (10% to 100%, steps of 5%, default 45%) in the Mod Menu settings screen (`ShhhBuilderConfigScreen.OpacitySlider`).
  - Added live `[-]` and `[+]` stepping buttons on the Position/Placement tab (`Tab.CONTROLS` in `ShhhBuilderScreen`) with instant on-screen feedback (`"Opacity: 45%"`).
  - Alpha is computed exactly once per frame (`GhostBlockModelHelper.setPreviewAlpha`) and applied dynamically to all textured blocks, eliminating per-block floating-point overhead.
- **Block Borders**:
  - Added thin subtle border outlines around each textured block so players can clearly distinguish individual block boundaries in 3D.
  - Added settings in `ShhhBuilderConfig`:
    - `Block Borders: ON / OFF` (default ON).
    - `Border Color: Block color / White / Black / Cyan` (default Block color).
    - `Border Opacity: 25% / 50% / 75% / 100%` (default 100%).
  - Borders are drawn only for active textured blocks within the current per-frame budget.
- **Quality Presets & Textured Distance**:
  - Added a one-click `Quality Preset` button in settings:
    - **Low**: 16m textured distance, 1,000 textured limit, 5,000 ghost block limit, 32m render distance.
    - **Balanced** (default): 24m textured distance, 2,000 textured limit, 10,000 ghost block limit, 48m render distance.
    - **High**: 32m textured distance, 3,000 textured limit, 20,000 ghost block limit, 64m render distance.
    - **Custom**: Automatically displayed whenever any individual setting is customized.
  - Added `Textured Distance: 16m / 24m / 32m` cycle button.
- **Universal Block Coverage**:
  - Implemented `BlockCategoryHelper` classifying every registered block into safe rendering categories:
    - **Normal**: Full blocks, stairs, slabs, fences, walls, panes, doors, trapdoors, plants, rails, torches, lanterns, redstone parts, and multipart models render using their native baked quads.
    - **Fluids**: Water and lava render as translucent colored boxes (blue `0x3F76E4` for water, orange `0xE05810` for lava) with optional borders.
    - **Block Entities**: Blocks with no standard baked block models (chests, ender chests, beds, signs, hanging signs, banners, skulls, shulker boxes, bells, enchanting tables, conduits, decorated pots, lecterns) render as precise shaped boxes with block colors and borders.
    - **Technical Markers**: Barrier, light, structure void, and jigsaw blocks render as small clear inner markers (`0.4x0.4x0.4`), never full blocks.
    - **Tinted Blocks**: Foliage, leaves, vines, water, and redstone wire use fixed default tints without per-block world lookups.
    - **Rotations & Mirror**: All block categories respect rotation and left-right mirroring.
    - Any model lookup or rendering failure safely falls back to a colored box with a border, logged at most once per block state.
- **Client-Side Self-Test Command (`/shhhbuilder selftest`)**:
  - Iterates over all registered blocks in `BuiltInRegistries.BLOCK`.
  - Audits preview readiness, categorizes fallbacks (fluids, block entities, technical markers, air), and verifies model availability.
  - Reports findings directly to the player in plain chat and outputs the complete breakdown and fallback ID lists to the game log. Never crashes and does not draw anything in the world.
- **Responsive 2-Column Settings Layout**:
  - Re-architected `ShhhBuilderConfigScreen` with an ergonomic 2-column layout (HUD & Quality on the left, Preview & Borders on the right, Done button at bottom).
  - Added automatic 2-page pagination when GUI height is under 140px, ensuring guaranteed title clearance and button visibility across all resolutions and scales.
- **Unit Testing**:
  - Added `BlockCategoryHelperTest` and expanded `ConfigAndPaginationTest` covering alpha math, bounds clamping, preset cycling, border settings, and layout guarantees across all window resolutions and scales (87 tests passing).

## [1.0.0-rc6] - Fix Cobblestone/Stone Parsing Bug, Settings Title Layout, Path Privacy, Wording Polish & Preview Status

### Fixed & Changed
- **Fixed Cobblestone Parsed as Stone Bug**:
  - **Root Cause Found**: In Minecraft 26.3, `NbtUtils.readBlockState` expects tag keys `"id"` and `"properties"` (Mojang renamed from legacy `"Name"` and `"Properties"`). Litematica palettes and Sponge strings stored `"Name"`, causing `readBlockState` to return air, which triggered the fallback `Blocks.STONE.defaultBlockState()`. In addition, item evaluation resolved against this parsed stone state (`state.getBlock().asItem()`), converting cobblestone into `Items.STONE` (`"Stone: 23,578"`), and `PlacementManager` transformed the stone state into stone models for the 3D ghost preview.
  - **Fix Applied**: Updated `BlockStateHelper` to normalize CompoundTags with both 26.3 (`"id"`, `"properties"`) and legacy (`"Name"`, `"Properties"`) keys, and added direct registry lookup (`BuiltInRegistries.BLOCK.getOptional()`) as an additional safe fallback. Both `.litematic` and `.schem` readers now reliably produce `Blocks.COBBLESTONE` and item `"minecraft:cobblestone"`.
  - **Test Verification**: Bootstrapped Minecraft registries in `RealFilesTest.testBootstrappedCobblestoneParsing` via `SharedConstants.tryDetectVersion()` and `Bootstrap.bootStrap()`, asserting that `minecraft:cobblestone` resolves to `Blocks.COBBLESTONE.defaultBlockState()` and item `"minecraft:cobblestone"` for both formats.
- **Fixed Settings Screen Title Layout Overlap**:
  - Resolved title drawing over the first button in `ShhhBuilderConfigScreen` by positioning title vertically with guaranteed clearance above `startY` across both compact and standard modes (`titleY + font.lineHeight < firstButtonY`).
  - Added unit test `testSettingsScreenLayoutAcrossResolutionsAndScales` verifying title clearance and screen boundary conformance across window resolutions 854x480, 640x360, 1920x1080 and GUI scales 1 to 4.
- **Privacy Hardening (Never Show Absolute Path in UI)**:
  - Replaced absolute path displays with friendly text `"schematics folder"` on `ShhhBuilderScreen`, `TutorialScreen` Page 1, `/shhhbuilder list`, and `/shhhbuilder folder`. Kept absolute path strictly in diagnostic logger output (`ShhhBuilder.LOGGER.info`).
  - Added unit test `testPathPrivacyInUi`.
- **Wording Polish**:
  - Details panel: updated total count to `"Total blocks: 23,578"` (removed "(voxels)").
  - Details panel: formatted material items using friendly display names (`MaterialDisplayHelper.formatBlockName`), e.g. `"Cobblestone: 23,578"`, never `"minecraft:stone"`.
  - File list buttons: formatted file extension as a discrete tag `[.litematic]` (e.g. `▶ axe [.litematic]`), eliminating the awkward space-before-period `axe .litematic`.
- **Preview Status Line & Logging**:
  - Added one plain preview status line on the Placement tab (`Tab.CONTROLS`) and Details tab: `"Preview: textured (N blocks nearby), outlines farther away"` (or `"Preview: outlines"`).
  - Added one-time logging per placement in `GhostBlockRenderer` detailing preview style, textured blocks drawn, outline blocks drawn, and any fallback reason.

## [1.0.0-rc5] - Real See-Through Textured Ghost Blocks Default, 24-Block Limit & 3-Strike Circuit Breaker

### Added & Changed
- **Textured Ghost Blocks Default**:
  - `PreviewStyle` now defaults to `TEXTURED` out of the box, showing real see-through textured blocks (~45% opacity) with native block models, textures, and fixed full-bright illumination (`0x00F000F0`).
- **24-Block Distance & 2,000 Block Limits**:
  - Textured rendering is applied to blocks within 24 blocks of the player camera, up to the 2,000 closest blocks.
  - All remaining ghost blocks beyond 24 blocks or beyond the 2,000-block threshold seamlessly render as outlines.
- **Touching Ghost Block Face Culling**:
  - Quads for faces touching any adjacent ghost block in the current placement/layer (`PlacementManager.hasBlockAt(...)`) are skipped, preventing interior quad clutter while preserving outer shell visibility.
- **Robust 3-Strike Circuit Breaker & Single Block Fallback**:
  - If a model lookup fails for an individual block state, that specific block is drawn as an outline and logged at most once without interrupting the remaining build.
  - If the textured rendering pipeline throws repeatedly (3 times), textured mode is automatically disabled for the session, the configuration switches to outlines, and ONE clean action bar message is displayed: *"Textured preview turned off. Showing outlines."*
  - Re-selecting `Textured` in Mod Menu resets the circuit breaker.
- **Simplified Mod Menu Configuration**:
  - Streamlined Mod Menu settings to a single clean `Preview style: Textured` / `Preview style: Outlines` toggle button, ensuring optimal UI layout across both standard and compact window dimensions.
- **In-Mod Tutorial Polish**:
  - Updated Tutorial page 3 to inform players that real see-through textured blocks are active by default and can be toggled to outlines via Mod Menu.
- **Automated Verification**:
  - Pure-logic unit tests in `GhostBlockTest` and `ConfigAndPaginationTest` covering face culling with `hasBlockAt`, 24-block distance limiting, 2,000 closest-block capping, 3-strike circuit breaker trip logic, and default config values. All 76 tests passing.

### Added & Changed
- **Research & Feasibility Verdict**:
  - Investigated Minecraft 26.3 custom rendering pipeline (`SubmitNodeCollector.submitCustomGeometry`, `BakedQuad` model parts, `VertexConsumer.putBakedQuad`, and `RenderTypes.translucentMovingBlock`).
  - Documented findings in `Documentation/PREVIEW_RESEARCH.md` with honest labels (`[VERIFIED BY CODE READING]`, `[VERIFIED BY UNIT TEST]`, `[NOT VERIFIED IN THE GAME]`), concluding **FEASIBLE WITH LIMITS**.
- **Real See-Through Textured Ghost Blocks Prototype**:
  - Implemented `GhostBlockModelHelper` to resolve and cache baked quads per distinct `BlockState` across multipart/variant models.
  - Rendered using `SubmitNodeCollector.submitCustomGeometry` with `RenderTypes.translucentMovingBlock()` at ~0.45 alpha (`0x73` in ARGB) and full-bright illumination (`0x00F000F0`).
  - Added fixed biome tints for grass, foliage, water, and redstone quads.
- **Performance Guardrails**:
  - **Internal Face Culling**: Directional faces touching an adjacent solid ghost block in the schematic are skipped, drastically reducing quad count and overdraw.
  - **Distance Limit**: Textured blocks are rendered only within 20 blocks of the player (`texturedDistance = 20`); blocks beyond fall back to wireframe outlines.
  - **Block Limit**: Configurable cap (1,000 / 3,000 / 6,000, default 3,000). The closest N blocks are textured, while the remainder render as outlines.
  - **Quad Caching**: Models are indexed once per unique `BlockState` (`Map<BlockState, CachedGhostModel>`), preventing per-frame allocations or repetitive model lookups.
  - **Protective Circuit Breaker**: Wrapped custom geometry submissions and per-block draws in `try/catch`. If an error occurs, it trips the circuit breaker, disables textured preview for the session, notifies the player on the action bar (*"Textured preview turned off. Showing outlines."*), and falls back to outlines safely without crashing the game.
- **Per-Block MapColor Outlines**:
  - Updated `BlockStateHelper.getBlockColor()` to query `state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col`, ensuring each block's outline reflects its real material color (wood amber, stone gray, etc.) rather than a single hardcoded blue.
- **Targeting HUD Indicator**:
  - Added live raycasting in `MaterialHudElement` against active placement blocks up to player reach (5.0 blocks), stopping at real obstructing world blocks.
  - Displays `"Looking at: <Block Name> (layer N)"` on the HUD when the crosshair points at any ghost block.
- **Configuration & UI**:
  - Added `PreviewStyle` setting (`OUTLINES` / `TEXTURED`, default `OUTLINES`).
  - Added `texturedBlockLimit` setting (1,000 / 3,000 / 6,000, default 3,000).
  - Added cycling buttons in `ShhhBuilderConfigScreen` for both settings with support for small/compact window layouts.
  - Updated `TutorialScreen` page 3 to mention both outline and textured preview styles.
- **Automated Verification**:
  - Added pure-logic tests in `GhostBlockTest` and `ConfigAndPaginationTest` covering face culling, nearest-N selection, distance limits, alpha values, quad cache keying, and config cycling. All tests passing.

## [1.0.0-rc3] - Item-Accurate Material Counting & Voxel Separation

### Added & Changed
- **Item-Accurate Material Counting vs. Voxel Separation**:
  - Rewrote material aggregation in `LitematicReader` and `SchemReader` via `BlockStateHelper.evaluateItem()` to calculate the exact items the player needs to carry and place rather than raw 3D grid voxels:
    - **Slabs**: `type=double` counts as 2 slab items; single slabs count as 1 item.
    - **Doors**: Counts only `half=lower` as 1 door item; `half=upper` is skipped (0 items).
    - **Beds**: Counts only `part=foot` as 1 bed item; `part=head` is skipped (0 items).
    - **Tall Plants & Double-Height Flowers**: Counts only `half=lower` as 1 plant item; `half=upper` is skipped (0 items).
    - **Technical Multi-Part Blocks**: `piston_head` and `moving_piston` are excluded; a piston base counts as 1 piston item.
    - **Waterlogged Blocks**: Only the main block counts; water is never counted as an item or unobtainable block.
    - **Blocks With No Obtainable Item**: Water, lava, fire, nether/end portals, and bubble columns are excluded from required items and tracked separately.
    - **Carried Item Mapping**: Automatically maps block states to the inventory item the player carries (`wall_torch` → `torch`, `oak_wall_sign` → `oak_sign`, `redstone_wire` → `redstone`, etc.) using `block.asItem()` when bootstrapped and robust string resolution when unbootstrapped.
    - **Voxel Separation**: `totalBlocks()` is preserved as the raw non-air 3D voxel count, clearly labeled as *"Total blocks: N (voxels)"* in the Details panel.
- **UI & HUD Consistency**:
  - Details panel displays *"Total blocks: N (voxels)"*, an unobtainable blocks notice (*"N blocks have no item (water, fire...)"*), and lists items needed.
  - In-game HUD (`H`) displays item-accurate counts with proper stack calculations and item icons.
  - `/shhhbuilder select` and `/shhhbuilder place` report both blocks and items needed (e.g. *"436 blocks, 435 items"*), plus notices for unobtainable or unknown blocks.
- **Automated Verification**:
  - `SampleHouseTest`: Verified that `sample_house.litematic` requires exactly 1 door item (lower door counted, upper door skipped).
  - `RealFilesTest`: Verified that `axe.litematic` and `axe.schem` retain identical 23,578 cobblestone item and voxel counts.
  - `FuzzAndSecurityTest`: Added `testItemCountingRules()` and `testSyntheticSchematicItemCounting()`.

## [1.0.0-rc2] - Audit Follow-Up, Performance & Compatibility Hardening

### Added
- **Unknown Block Tracking & Reporting**:
  - Implemented `BlockStateHelper.isKnownBlock()` to verify whether block states exist in the active Minecraft registry.
  - Added `unknownBlocks()` metric to `LitematicData`, counted in `LitematicReader` and `SchemReader`.
  - Reported directly in plain words on the Details panel: *"Notice: N blocks are not in this game version"*, and in `/shhhbuilder select` chat output.
  - Added unit test `FuzzAndSecurityTest.testUnknownBlockDetection()`.
- **Fresh Install Retest Documentation**:
  - Added dedicated step-by-step instructions in `Documentation/TESTING.md` on how to clear config and schematics to re-verify the first-run welcome message and starter build auto-copy.

### Performance & Error Isolation
- **Section Distance Sorting Cache**:
  - Cached sorted active sections in `GhostBlockRenderer`. Distance filtering and sorting now only run when the camera enters a new 16x16x16 section (`playerSecX`, `playerSecY`, `playerSecZ`), or when placement position/distance changes, completely eliminating per-frame list re-allocation and sort overhead.
- **Per-Block Shape Error Handling**:
  - Wrapped `block.state().getShape()` in `GhostBlockRenderer` in a protective `try-catch` falling back to full cube (`Shapes.block()`).
  - Implemented single-warning logging using `ConcurrentHashMap.newKeySet()` so buggy third-party block state shapes never crash rendering and log at most once per session.

### UI & Layout Fixes
- **Responsive Layout for Small Windows**:
  - Tested and hardened `ShhhBuilderScreen` down to 640x360 and 854x480 at GUI scales 1 to 4. Proportional scaling prevents list, right details panel, and bottom action buttons from clipping off-screen.
  - Implemented adaptive 2-column layout in `ShhhBuilderConfigScreen` when screen height is under 185px, keeping all 6 buttons cleanly within screen bounds.
  - Relocated page counter on `TutorialScreen` to the subtitle header, eliminating overlap with the bottom `Close` button.

### Documentation Refinements
- **HUD Registration API**: Corrected all documentation references from `HudRenderCallback` to Fabric API's exact `HudElementRegistry.addLast(Identifier, HudElement)` in `AUDIT.md`, `ARCHITECTURE.md`, and `README.md`.
- **Wording Correction**: Removed all references to a non-existent "Materials tab"; clarified that material requirements are displayed in the "Blocks needed" section of the Details tab and on the in-game HUD (`H`).
- **Safety Limits & What Players See**: Documented the 4,096 axis limit, 10,000,000 read volume limit, 2,000,000 preview volume cap, and ghost block frame limit options (5k/10k/20k) with exact user-facing messages in `README.md`.
- **Material Counting Limitations**: Documented how double slabs, doors, beds, tall plants, and waterlogged blocks are counted (in-world voxel block states vs item crafting requirements).
- **Third-Party Compatibility**: Documented Sodium, Iris, and shader packs in `README.md` Known Issues as not tested.

## [1.0.0-rc1] - Full-Mod Audit & Release Candidate Hardening

### Added
- **System Architecture Documentation**:
  - Authored `Documentation/ARCHITECTURE.md` detailing system overview, component inventory, data flow, threading model, 26.3 rendering engine, and memory management.
- **Comprehensive Audit Report**:
  - Authored `Documentation/AUDIT.md` covering all 16 audit areas (A through P) with strict verification labels (`[VERIFIED BY UNIT TEST]`, `[VERIFIED BY CODE READING]`, `[NOT VERIFIED IN THE GAME]`) and reality gap documentation.
- **Fuzzing & Security Test Suite**:
  - Added `FuzzAndSecurityTest.java` (8 new automated tests):
    - Path traversal rejection in command line inputs (`../secret`, `subdir/nested`, `C:\Windows\...`).
    - Path traversal rejection in `SchematicManager` asynchronous loading.
    - Coordinate border boundaries (`±30,000,000`) and height limits (`minY` to `maxY`).
    - Layer validation (`all`, in-range integers, rejection of 0, negative, and out-of-range).
    - Fuzz testing against `LitematicReader` with empty and random byte arrays of varying sizes.
    - Fuzz testing against `SchemReader` with empty and random byte arrays.
    - NBT quota decompression bomb enforcement.
    - Config recovery from corrupted, truncated, or invalid JSON, plus atomic write validation.
  - Total automated test suite now stands at **67 passing tests across 12 test classes** with 0 failures and 0 skipped.

### Security & Hardening
- **Decompression Bomb Protection**:
  - Replaced `NbtAccounter.unlimitedHeap()` with `new NbtAccounter(256 * 1024 * 1024L, 512)` in `LitematicReader` and `SchemReader`. Malicious or corrupted gzip streams attempting to decompress gigabytes of heap memory are halted safely with a user-friendly `IOException`.
- **Path Traversal Guards**:
  - Hardened `SchematicManager.getSelectedSchematicData()`, `loadSchematicAsync()`, and `ShhhBuilderCommand.resolveFile()` to strictly reject any path containing `..`, `/`, or `\`, or resolving outside the normalized schematics folder.
- **Atomic Config Persistence**:
  - Updated `ShhhBuilderConfig.saveToPath()` to write first to a `.tmp` sibling file before performing an atomic move (`StandardCopyOption.ATOMIC_MOVE` with fallback to `REPLACE_EXISTING`), eliminating corrupt zero-byte config files if the game or OS halts mid-write.
- **Config Sanitization**:
  - Config values are clamped on load (`ghostRenderDistance` between 16 and 128, `ghostBlockLimit` to 5,000, 10,000, or 20,000), falling back safely to defaults upon reading corrupted JSON.
- **Dimension & Volume Safety Caps**:
  - Implemented 4096-block per-axis bounds check and a 10,000,000 block volume limit in both `LitematicReader` and `SchemReader` to prevent allocation of multi-gigabyte arrays on malicious files.
  - Implemented 65,536 maximum palette entries limit in `SchemReader`.
- **Bounded Session Cache**:
  - Implemented a 50-entry cache ceiling in `SchematicManager` to prevent unbounded memory growth during long gaming sessions.

### Fixed
- **Clean Packaging**:
  - Removed machine-specific paths across tests and documentation, replacing with platform-independent `%APPDATA%\.minecraft` or `<game folder>`.
  - Updated `.gitignore` to ignore `logs/`, `*/logs/`, and `*.log.gz`.
  - Verified jar contents contain zero test artifacts, zero mixins, and valid metadata.

### Added
- **Full Client-Side Command Set (`/shhhbuilder`)**:
  - Registered via Fabric API's `ClientCommandRegistrationCallback` in `fabric-command-api-v2` (`net.fabricmc.fabric.api.client.command.v2`).
  - 100% client-side: commands are intercepted and evaluated locally; zero packets or network traffic are sent to multiplayer servers.
  - Root command `/shhhbuilder` with 11 subcommands:
    - `/shhhbuilder help`: Lists all available commands with concise descriptions and usage hints.
    - `/shhhbuilder list`: Displays all available `.litematic` and `.schem` files in `.minecraft/schematics`, highlighting the currently active selection.
    - `/shhhbuilder select <file>`: Selects and loads a schematic file. Features smart tab-completion that suggests base names without extensions where unambiguous, and full names where ambiguous.
    - `/shhhbuilder place`: Anchors the selected schematic at the player's current foot position in the active dimension.
    - `/shhhbuilder place <x> <y> <z>`: Places the schematic at explicit world coordinates with rigorous height limit and world boundary validation.
    - `/shhhbuilder move <x> <y> <z>`: Shifts current placement by relative offsets `(dx, dy, dz)` with pre-validation against world height boundaries.
    - `/shhhbuilder rotate`: Turns the placed schematic 90° clockwise.
    - `/shhhbuilder flip`: Toggles left-to-right mirroring on/off.
    - `/shhhbuilder layer <all|number>`: Sets layer mode to all layers (`all`) or isolates a specific single layer (`1` to `buildHeight`).
    - `/shhhbuilder clear`: Clears the build preview from the world.
    - `/shhhbuilder folder`: Opens the local schematics directory in Windows Explorer.
- **Shared Architecture**:
  - All commands reuse the exact same backend logic (`PlacementManager`, `SchematicManager`, `ShhhBuilderConfig`) as GUI buttons and keybinds with zero duplicated code.
  - Commands work smoothly whether the Shhh Builder screen is open or closed.
- **Tutorial & In-Game Mentions**:
  - Updated Tutorial Screen Page 6 with line 5: *"Prefer chat commands? Type /shhhbuilder help for all commands."*
  - Added dedicated commands section to `README.md`.
- **Command Unit Tests**:
  - Added `ShhhBuilderCommandTest.java` (7 tests) verifying extension stripping, argument cleaning, smart tab-completion, file resolution, coordinate bounds checking, layer parsing, and Brigadier command tree hierarchy (59 total unit tests passing).

## [1.0.1-dev5] - Beginner-Friendliness Polish, Bundled Starter Build, & HUD Key Hints

### Added
- **Bundled Starter Schematic (`sample_house.litematic`)**:
  - Embedded a small starter house schematic (182 blocks, 7x5x7 footprint, volume 245) inside the mod jar under `assets/shhh-builder/sample/sample_house.litematic`. Built with common beginner materials: oak planks, oak logs, cobblestone, glass panes, oak stairs, doors, and a torch.
  - Automatically copies the sample house into `.minecraft/schematics` on game launch if the folder contains no `.litematic` or `.schem` files.
  - Added persistent `sampleHouseCopied` boolean flag to `ShhhBuilderConfig` so user-deleted schematics are never overwritten or re-copied.
- **HUD Key Hints Overlay**:
  - Added an optional key hint line directly below the Material List HUD: `"R turn | Z flip | [ ] layer | H blocks needed"`. Keys are dynamically queried from the player's active bindings.
  - Added `showKeyHints` toggle setting in `ShhhBuilderConfig` and a toggle button in the Mod Menu settings screen (`Key Hints on HUD: ON/OFF`).
  - Rendered with full 32-bit ARGB color (`HUD_TEXT_HINT_COLOR = 0xFFAAAAAA`, alpha `0xFF`).
- **One-Time Render Cap Notification**:
  - When the ghost block per-frame limit caps the visible wireframes, the mod displays a friendly, non-spammy action bar notification once per placement/file change: `"Showing the closest N blocks. You can raise this in Mod Menu > Shhh Builder."`.
- **Friendly Plain-Language Error Handling**:
  - Formatted file load and scan errors into clear, two-line guidance: what happened and what to do next (e.g., `"Could not read this file. It may be corrupt or from an incompatible Minecraft version. Try another schematic."`). Technical stack traces and details remain in the log file.
- **Single-Sentence Empty State Guidance**:
  - Main screen guidance instructs the user step-by-step: `"Pick a file from the list."` when files are present, and `"Now click Place Here."` when a file is selected.
- **Tutorial Updates**:
  - Updated Tutorial Page 1 and Page 2 to mention the bundled `sample_house.litematic` starter build.
- **Automated Verification**:
  - Added `SampleHouseTest.java` validating the sample house NBT structure, dimensions, palette, block counts, and `LitematicReader` compatibility.
  - Added unit tests in `ConfigAndPaginationTest.java` for `showKeyHints`, `sampleHouseCopied`, `formatFriendlyLoadError`, `formatFriendlyScanError`, and 32-bit ARGB hint color alpha (52 total unit tests passing).

### Changed
- **Beginner-Friendly Terminology**:
  - Standardized UI labels across screens and action bar: `"Show preview"` (G), `"Blocks needed"` (H), `"Flip left-right"` (Z), `"Turn 90° Clockwise"` (R), `"Clear Build"`, `"Show one layer"`, `"Show up to layer"`, and `"Show all layers"`.

## [1.0.1-dev4] - In-Mod Tutorial Screen & Beginner Guidance

### Added
- **"How to use" Button**: Added a dedicated, clearly labeled "How to use" button at the top-right of `ShhhBuilderScreen` that opens the interactive tutorial.
- **Interactive Multi-Page Tutorial Screen (`TutorialScreen`)**:
  - 6 beginner-friendly tutorial pages with Back and Next buttons, page counter (`Page X of 6`), and Close button returning to the parent screen:
    - Page 1 (Getting Started): Explains file placement, displays the real folder path, and explains Open Folder and Refresh buttons.
    - Page 2 (Pick a Build): Explains clicking files in the list, gold selection indicator, and the size/materials breakdown.
    - Page 3 (Place It in Your World): Explains player anchoring, Place Here button, cyan footprint outline, and ghost block wireframes.
    - Page 4 (Move and Turn It): Explains on-screen Placement buttons, arrow keys, Page Up/Down, and dynamic Rotate/Flip keys.
    - Page 5 (Build Floor by Floor): Explains layer mode cycling, layer up/down stepping, reset all layers, and Material List HUD shortcut.
    - Page 6 (Good to Know): Emphasizes visual aid only, right-clicking block faces, ghost/outline toggles, and Mod Menu ghost block limit tuning.
  - Keyboard navigation for tutorial pages via Arrow Left / Page Up (previous) and Arrow Right / Page Down (next).
- **Dynamic Keybind Reflection**: In-mod tutorial and first-run messages query active key mappings dynamically (falling back safely in headless test environments), keeping instructions accurate even if keys are rebound in Controls.
- **Empty State Hint**: Added an explicit guidance line in the empty list state: *"New to Shhh Builder? Click 'How to use' at the top right."*.
- **Comprehensive Tutorial Unit Tests**: Added `TutorialScreenTest.java` verifying page clamping, navigation bounds, content line limits (4–6 lines per page), and 32-bit ARGB alpha byte validation (47 total unit tests passing).

### Changed
- **First-Run Welcome Notification**: Updated first-run action bar and chat message to `"Shhh Builder: press V, then How to use."` (using the player's active open screen key).
- **HUD Title File Name Display**: Updated `MaterialHudElement` to display the schematic's actual file name in the HUD header, while keeping the internal metadata name in the details panel.

## [1.0.1-dev3] - Project-Wide Color & Alpha Audit, Category Constants, & Defensive Alpha Guards

### Added
- **Explicit Color Constants**: Defined public static ARGB color constants across renderers and GUI classes:
  - `BlockStateHelper`: `COLOR_DEFAULT` (`0xFF00E5FF`), `COLOR_WOOD` (`0xFFD7A15C`), `COLOR_GLASS` (`0xFFE0F7FA`), `COLOR_FOLIAGE` (`0xFFA5D6A7`), `COLOR_REDSTONE` (`0xFFFF8A80`), `COLOR_WATER` (`0xFF4FC3F7`), `COLOR_WOOL` (`0xFFFFE082`), and `COLOR_STONE` (`0xFF90CAF9`).
  - `BoxOutlineRenderer`: `BOX_COLOR` (`0xFF00E5FF`).
  - `MaterialHudElement`: `HUD_BACKGROUND_COLOR` (`0x90000000`), `HUD_BORDER_COLOR` (`0x40FFFFFF`), and text colors.
  - `ShhhBuilderScreen`: `COLOR_TITLE`, `COLOR_FOLDER_PATH`, `COLOR_HINT`, `COLOR_SELECTED_GREEN`, `COLOR_ERROR_TITLE`, `COLOR_ERROR_TEXT`, `COLOR_LOADING`, `COLOR_TEXT_WHITE`, `COLOR_TEXT_GRAY`, `COLOR_TEXT_MUTED`, `COLOR_MATERIALS_HEADER`, and `COLOR_MATERIALS_ITEM`.
- **Defensive Alpha Guard**: Added `BlockStateHelper.ensureAlpha(int color)` that forces the alpha byte to `0xFF` if it is zero, guaranteeing no 24-bit color can ever cause silent drops in Minecraft 26.3 rendering pipelines.
- **Direct Block ID Category Lookup**: Added `BlockStateHelper.getColorForBlockId(String blockId)` allowing category resolution and color assertions in both bootstrapped and headless unit test environments.
- **Comprehensive Color Unit Tests**: Added unit tests asserting non-zero alpha across all block category colors, footprint outline, HUD background and border, and screen elements (44 unit tests passing).

### Fixed
- **Project-Wide Color Audit**: Audited all rendering calls (`submitShapeOutline`, `fill`, `text`, `centeredText`). Confirmed and enforced non-zero alpha across all in-world wireframes, outline boxes, and HUD components.

## [1.0.1-dev2] - Custom Paged Schematic List, 26.3 ARGB Text Fix, & Ghost Block Limit

### Added
- **Custom Paged Button List**: Replaced fragile `ObjectSelectionList` with a robust paged button list (~7-8 items per page) using standard vanilla `Button` widgets. Each file gets a dedicated button displaying `baseName` + `extension` with full hover tooltips showing file path and format.
- **Visual Selection Highlighting**: Selected schematic button is highlighted with a `▶ ` prefix and bold gold/yellow text formatting.
- **Page Navigation & Mouse Wheel Paging**: Added `◀ Prev` and `Next ▶` buttons with a centered page counter (`Page X of Y`), plus smooth mouse-wheel scroll paging via `mouseScrolled()`.
- **Configurable Ghost Block Limit**: Added "Ghost Block Limit" setting to configuration (`ShhhBuilderConfig`) and Mod Menu screen (`ShhhBuilderConfigScreen`) cycling between `5,000`, `10,000` (default), and `20,000` blocks to protect frame rates on low-end PCs.
- **Automated Screen & Pagination Tests**: Added `ConfigAndPaginationTest.java` verifying configuration cycling/sanitization, pagination slicing/navigation bounds, and ARGB alpha bitmasks (40 unit tests passing).

### Changed
- **Scan-Once Lifecycle**: Scans schematics folder from disk exactly once on screen open, and once per explicit `Refresh` button click, eliminating the 4x duplicate scans on initial open.
- **Diagnostic Logging**: Logs schematic count and exact list bounds `[x, y, width, height]` once on screen open, and logs explicit scan messages when the `Refresh` button is clicked.
- **Removed Obsolete Widget**: Removed `SchematicListWidget.java` in favor of the custom paged button list.

### Fixed
- **Invisible Text and Title in Minecraft 26.3**: Discovered that `GuiGraphicsExtractor.text` in 26.3 discards any color where `ARGB.alpha(color) == 0`. Updated all text drawing calls to 32-bit ARGB with full `0xFF` alpha mask (`0xFFFFFFFF`, `0xFFAAAAAA`, `0xFF888888`, etc.), restoring screen title, folder path indicator, instruction line, and details panel.

## [1.0.1-dev] - Bug Hunt, Real-Game Audit & Distance-Prioritized Section Rendering

### Fixed
- **Root Cause of Empty List and Invisible Screen Text (Minecraft 26.3 GUI Alpha)**: In Minecraft 26.3, `GuiGraphicsExtractor.text` strictly checks `if (ARGB.alpha(color) == 0) return;`. Colors previously passed as 24-bit RGB (e.g. `0xFFFFFF`, `0xAAAAAA`, `0x888888`) have an alpha byte of `0x00`, causing Minecraft to silently drop every single text extraction call. Updated all text rendering across `ShhhBuilderScreen`, `SchematicListWidget`, `ShhhBuilderConfigScreen`, and `MaterialHudElement` to use full 32-bit ARGB values with alpha `0xFF` (`0xFFFFFFFF`, `0xFFAAAAAA`, `0xFF888888`, etc.), immediately restoring the screen title, folder path, details panel, and list entries.
- **List Widget Sizing & Diagnostic Logging**: Explicitly set widget position via `setPosition(listX, listTop)` in `ShhhBuilderScreen.init()`. Added diagnostic logging once per screen open to log entry count, widget bounds, and verify row `extractContent` execution.
- **Hover Highlight in Schematic List**: Added a subtle background highlight in `SchematicListWidget.Entry` when hovering over schematic entries (`0x1AFFFFFF`), improving visual feedback and clickability.
- **Critical Reader Null-State Bug**: In `LitematicReader`, `BlockStateHelper.isAir(state)` checked `null` states before checking raw name strings, causing unbootstrapped unit tests and unmapped block states to incorrectly treat valid blocks as air and culled. Fixed to prioritize `isAirName(rawName) || (state != null && state.isAir())`. Verified against `axe.litematic` (restoring all 23,576 visible cobblestone blocks).
- **Distance-Prioritized Ghost Block Section Rendering**: Previously, blocks exceeding the 20,000-per-frame cap were dropped in raw ascending index order (top of schematic $> Y=136$ was never drawn). Fixed by dividing blocks into 16x16x16 chunk-like `BlockSection`s and sorting sections nearest-to-camera first in `GhostBlockRenderer`.
- **Keyboard Shortcut Interception in GUIs**: Fixed key mappings being triggered while typing in chat, containers, or inventories. Key mappings are now cleanly drained and ignored whenever `client.screen != null`.
- **Stale Schematic on Delete**: Fixed `ShhhBuilderScreen` retaining a stale `selectedFileName` when the selected schematic was deleted or moved externally during refresh.
- **HUD Boundary Clamping**: Clamped `MaterialHudElement` render coordinates to viewport boundaries and truncated excessively long schematic or item names to prevent off-screen overflow.
- **Thread Safety**: Declared shared state variables in `PlacementManager` and `SchematicManager` as `volatile` to prevent memory visibility desync across render and client tick threads.
- **Single Jar Packaging**: Disabled `withSourcesJar()` in `build.gradle` so that only `shhh-builder-1.0.1-dev.jar` is produced in build outputs.

## [1.0.0] - Step 8: Initial Release

### Added
- First-run guidance notification shown once in action bar and chat when first joining a world (*"Shhh Builder: put .litematic or .schem files in the schematics folder and press V."*), stored in configuration via `firstRunHelpShown`.
- Disconnect lifecycle handler (`ClientPlayConnectionEvents.DISCONNECT`) that safely resets placement and cache when leaving a world or server, preventing cross-world ghost blocks.
- Root project documentation: comprehensive `README.md`, official `LICENSE` (MIT), and `.gitignore`.
- Full release build packaging producing `shhh-builder-1.0.0.jar`.

### Changed
- Changed default Flip (Mirror) keybind from `X` to `Z` for clean ergonomics with zero conflicts across both Survival and Creative game modes.
- Set release version to `1.0.0` in `gradle.properties` and `fabric.mod.json`.
- Configured complete metadata in `fabric.mod.json`: name, description, authors, MIT license, placeholder contact links, and client-side environment.
- Removed verbose multiline block dumps and debug console logs from `LitematicReader` and `SchemReader`, keeping concise summary logs.

## Step 7

### Added
- Sponge Schematic (`.schem`) format support via dedicated `SchemReader` parser.
- Automatic detection of Sponge schematic version formats:
  - Version 1 & 2: root-level compound format with `Palette` and `BlockData`.
  - Version 3: nested under root `"Schematic"` compound tag with `BlockPalette` and `BlockData`.
- Continuous VarInt decoding (`decodeVarIntArray`) for bit-packed block data byte arrays.
- Block state string parsing with property key-value extraction (e.g. `minecraft:oak_stairs[facing=north,half=bottom]`) into Minecraft `BlockState` objects.
- Material count breakdown with air block filtering (`minecraft:air`, `cave_air`, `void_air`).
- 6-neighbor occlusion culling and volume limit enforcement (> 2,000,000 blocks preview cap) identical to `.litematic` files.
- Seamless integration with existing downstream features: details panel, material list HUD, in-world bounding outline box, ghost hologram blocks, rotation, flip, and layer modes all work identically for `.schem` files.
- Removed the *"Reading .schem files is coming in a later update"* placeholder.
- Automated unit test suite (`SchemReaderTest`) verifying VarInt encoding/decoding, Sponge v2 and v3 parsing, error handling (empty, corrupt, missing data), and volume limit enforcement.

### Changed
- Changed default Flip (Mirror) keybind from `F` to `X` to eliminate conflict with vanilla Minecraft's "Swap Item With Offhand".
- Updated all on-screen tooltips, keybind registration, and documentation to reflect `X` as the default Flip shortcut.

## Step 6

### Added
- Layer Mode feature with three modes:
  - `All`: shows entire schematic (default).
  - `Single layer`: isolates a single Y-level for focused step-by-step building.
  - `Up to layer`: shows current level and all levels beneath it cumulatively.
- 1-based layer indexing (1 to schematic height) with bounds clamping.
- Layer mode keybinds under "Shhh Builder":
  - Cycle Mode: `M`
  - Next Layer (Up): `]`
  - Previous Layer (Down): `[`
  - Reset All Layers: `\`
- Action bar overlay notifications on layer changes (e.g. *"Layer 3 / 12 (single layer)"*, *"Layer mode: All layers"*).
- Layer mode filtering strictly for ghost block rendering while keeping the bounding outline box visible for context.
- Transformed block cache invalidation only when layer, rotation, mirroring, or schematic changes (zero per-frame rebuild overhead).
- Material list HUD display updated with an extra line indicating current layer mode and level when filtering is active (e.g. `Layer 2/4 (Single)`).
- Left-to-right Flip (Mirror) feature:
  - Default keybind `F` to toggle flip.
  - Coordinate reflection ($mx = W - 1 - x$) coordinated with rotation.
  - Vanilla block state mirroring via `BlockState.mirror(Mirror.LEFT_RIGHT)` for directional blocks (stairs, chests, doors).
- On-Screen Placement Controls panel on `ShhhBuilderScreen` with tab switcher:
  - `[Details]` tab: displays dimensions, total blocks, preview status, and materials list.
  - `[Placement]` tab: displays live position, rotation, mirror state, layer info, and clickable controls.
- Directional movement button pad on the Placement tab (Left, Fwd, Right, Down, Back, Up) moving 1 block relative to player horizontal facing.
- Rotation and flip buttons (`↶ -90°`, `Flip`, `+90° ↷`).
- Layer control buttons (`▼ Layer`, `Mode`, `Layer ▲`, `Reset: All Layers`).
- Beginner-friendly tooltips (`Tooltip.create(...)`) for all buttons describing their actions and showing keyboard shortcut keys.
- Common `Place Here` and `Clear` action buttons accessible from both Details and Placement tabs.
- Session persistence for placement coordinates, rotation, mirroring state, and layer mode across screen open/close cycles.
- Automated unit test suite (`PlacementManagerTest`, `GhostBlockTest`) verifying layer mode cycling, layer stepping bounds, mirroring state, layer filtering, and mirror coordinates with rotation.

## Step 5b

### Added
- In-world ghost block hologram preview rendering (`GhostBlockRenderer`) showing schematic blocks inside the outline box.
- Compact schematic block representation (`SchematicBlockData`) storing a unified Minecraft `BlockState` palette and pre-culled visible block array.
- 6-neighbor occlusion culling algorithm in `LitematicReader` that skips blocks completely hidden on all 6 sides by non-air blocks.
- Rotation coordinate transformations (0°, 90°, 180°, 270°) and vanilla directional block state rotation (`BlockState.rotate(Rotation)`).
- Maximum preview volume cap at 2,000,000 blocks to protect memory on gigantic schematics, while keeping outline and materials list active.
- Configurable ghost render distance (default 48 blocks) with cycle button in Mod Menu configuration screen.
- Per-frame drawing cap at 20,000 blocks with throttled action bar notification when blocks are skipped for performance.
- Precomputed cached block transform in `PlacementManager` avoiding per-frame recalculations during gameplay and nudging.
- Default keybind `G` under "Shhh Builder" controls category to toggle ghost blocks on/off with action bar overlay messages.
- Color-coded holographic wireframe styling by block category (wood/amber, stone/blue, glass/white, foliage/green, redstone/red).
- Automated unit test suite (`GhostBlockTest`) verifying 6-neighbor culling, rotation mapping, volume limits, and config cycling.

## Step 5a

### Added
- In-world placement management (`PlacementManager`) tracking block position (X, Y, Z), rotation (0°, 90°, 180°, 270°), dimension key, and visibility state.
- "Place Here" button on the Shhh Builder screen that anchors the schematic to the block under the player and closes the screen.
- "Clear" placement button to remove the active in-world placement.
- In-world 3D footprint outline box rendering using Fabric API's `LevelRenderEvents.BEFORE_GIZMOS` and Minecraft's `SubmitNodeCollector.submitShapeOutline`.
- Depth-biased bright cyan outline box (`RenderTypes.linesDepthBias()`) with automatic width and length dimension swapping at 90° and 270° rotations.
- Default keybind `B` under "Shhh Builder" controls category to toggle the outline box on and off with action bar overlay message notifications.
- Directional nudge keybinds (Arrow Up, Down, Left, Right) to shift placement by 1 block relative to player's facing direction.
- Vertical nudge keybinds (Page Up, Page Down) to move placement up or down by 1 block.
- Rotate keybind `R` to rotate placement clockwise by 90°.
- Dimension checking: outline only renders when the player is in the dimension where the schematic was placed.
- Automated unit test suite (`PlacementManagerTest`) verifying placement positioning, rotation wrapping, dimension swapping, directional nudging, and visibility toggling.

## Step 4

### Added
- In-game Material List HUD panel rendered while playing Minecraft using Fabric API's `HudElementRegistry`.
- Dynamic material rendering displaying required blocks sorted from highest to lowest count.
- Localized block display names (e.g. "Oak Planks" instead of "minecraft:oak_planks") with clean title-cased fallback.
- Small rendered block/item icon beside each material name using `GuiGraphicsExtractor.fakeItem`.
- Beginner-friendly count format: shows raw count and, for 64 or more, stacks + remainder (e.g. `212 (3 stacks + 20)`, `64 (1 stack)`).
- Truncates at top 10 materials with a subtle `+ X more` line when more block types exist.
- Title header on HUD displaying the schematic name in bold gold styling.
- Default keybind `H` under "Shhh Builder" category in Options > Controls to toggle the HUD on and off.
- Action bar overlay message feedback when pressing `H` (*"Material List HUD: Shown"* / *"Material List HUD: Hidden"*).
- Automatic suppression of HUD rendering when any screen or pause menu is open and when F1 hide-HUD mode is active.
- Mod Menu configuration screen with interactive cycle buttons for:
  - HUD Position (`Top-Left`, `Top-Right`, `Bottom-Left`, `Bottom-Right`).
  - HUD Scale (`Small (75%)`, `Normal (100%)`, `Large (125%)`).
- Persistent configuration saved to `config/shhh-builder.json`.
- "Show materials on screen" button added to the Shhh Builder screen that activates the HUD and returns to game.
- Clean semi-transparent dark HUD styling with subtle border outline and comfortable padding.
- Automated JUnit unit tests (`MaterialDisplayHelperTest`) verifying count stack calculations, block name formatting, and configuration cycling.

## Step 3

### Added
- Independent `LitematicReader` parser for `.litematic` files (gzip-compressed NBT).
- Continuous bit-stream block state unpacking handling cross-64-bit boundary packing and dynamic palette bit-widths.
- Air block filtering (`minecraft:air`, `minecraft:cave_air`, `minecraft:void_air`) excluding them from material totals.
- Metadata and region geometry aggregation: schematic name, author, enclosing dimensions (W x H x L), total non-air blocks, and materials breakdown.
- Multi-threaded asynchronous file loading (`SchematicManager.loadSchematicAsync`) preventing any game frame drops or freezing.
- Interactive details panel on `ShhhBuilderScreen` showing schematic metadata, total blocks, top 10 materials, and "+ X more" indicator.
- Loading indicator displayed while reading schematics.
- Graceful error handling for missing, empty (0-byte), corrupt, or oversized (> 50 MB) files with friendly user messages.
- Friendly placeholder notification for `.schem` files (*"Reading .schem files is coming in a later update."*).
- Session caching of parsed schematics with file timestamp and size invalidation.
- Console logging of material counts for direct verification against external tools like Litematica.
- JUnit 5 automated test suite covering bit-width calculation, boundary-crossing bit-unpacking, air filtering, and synthetic file reading.
- Updated documentation and test verification guide in `PLAN.md`.

## Step 2

### Added
- Schematic file scanning in `schematics/` directory supporting `.litematic` and `.schem` formats.
- Non-schematic file filtering (ignores `.txt`, `.png`, and any other file extensions).
- Scrollable schematic selection list widget (`SchematicListWidget`).
- File entries display the clean base file name on the left and format badge on the right.
- Left-click schematic selection with soft gold highlight and outline.
- Dynamic status line below the list showing `"Selected: <file name>"` (or `"Selected: None"`).
- "Refresh" button to rescan the schematics folder without restarting the game.
- "Open Folder" button to launch the schematics directory directly in the operating system's file explorer using `Blaze3D.openPath`.
- Friendly empty state message when no schematics are present: *"No schematics found. Put .litematic or .schem files in the schematics folder, then press Refresh."*
- Safe directory scanning error handling preventing crashes and showing user-friendly error notices.
- Session persistence for selected schematic: remembers the chosen file across screen re-openings during the game session.
- Maintained Close button, `V` key, and `Escape` key screen dismissal.
- Added comprehensive testing guide to `PLAN.md`.

## Step 1

### Added
- Initial project scaffolding for Fabric on Minecraft 26.3 with Java 25.
- Mod metadata configured in `fabric.mod.json`:
  - Mod ID: `shhh-builder`
  - Name: `Shhh Builder`
  - Author: `RafayShhh`
  - License: `MIT`
  - Environment: `client`
- Mod icon added to assets.
- Default keybind `V` registered under "Shhh Builder" controls category to open the main mod screen.
- Main UI screen with "Shhh Builder" header, user instructions, and a Close button.
- Automatic creation of the `schematics` folder in the game directory upon startup.
- Optional Mod Menu integration with a clean placeholder config screen.
- Initial project documentation (`PLAN.md`, `CHANGELOG.md`).
