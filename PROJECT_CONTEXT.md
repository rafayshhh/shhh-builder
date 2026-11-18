# Project Context: Shhh Builder

**Repository**: `Shhh Builder`  
**Current Version**: `1.0.0`  
**Target Platform**: Minecraft 26.3, Fabric Loader `0.20.1+`, Fabric API `0.166.2+26.3`, Java 25 (`Java 25.0.5`), Gradle `9.8.2` / Loom `1.19.4`  
**Environment**: 100% Client-Side Only (`ClientModInitializer`) — Zero server network packets, zero mixins.

---

## 1. Executive Summary & Purpose

**Shhh Builder** is a modern, lightweight, beginner-friendly schematic viewer and holographic building assistant designed specifically for Minecraft 26.3 on Fabric.

### What It Does:
- **Discovers and loads** both `.litematic` (Litematica) and `.schem` (Sponge v1/v2/v3) schematic files from the local `.minecraft/schematics` directory.
- **Renders an in-world holographic 3D preview**:
  - **Textured Ghost Blocks** (default): Real see-through blocks (~45% opacity, adjustable 10%–100%) rendered via Minecraft's translucent moving-block atlas pipeline with custom quad alpha injection and universal fallback for all blocks.
  - **Block Borders**: Subtle thin outlines around textured blocks so adjacent blocks are immediately distinguishable in 3D.
  - **Wireframe Outlines**: Clean MapColor-tinted outlines for players who prefer a wireframe mode or on lower-end systems.
  - **Footprint Bounding Box**: Bright Cyan (`#00E5FF`) box outline showing the exact bounding coordinates of the build.
- **Transforms placements live in-world**: Move (arrow keys, PageUp/PageDown), rotate 90° (`R`), flip/mirror left-to-right (`Z`), and cycle layer-by-layer modes (`M`, `[`, `]`, `\`).
- **Automatic Live World Verification (`GhostBlockRenderer`, `WorldBlockValidator`)**:
  - Live inspection against `mc.level.getBlockState(worldPos)` on every frame.
  - **Exact Match**: If the placed block matches the expected schematic block state identically, the ghost preview block is skipped completely (the player already built it).
  - **Wrong State (Same Block)**: If a block of the same type is placed with differing orientation or properties (e.g. stair or log placed backwards), Shhh Builder draws a bright Amber/Orange wireframe outline (`0xFFFFAA00`) using `RenderTypes.linesDepthBias()`.
  - **Wrong Block**: If an incorrect solid block is placed, Shhh Builder draws a bright Red wireframe outline (`0xFFFF3333`) using `RenderTypes.linesDepthBias()`.
  - **Missing**: Air or fluid positions continue to render normal translucent or wireframe ghost previews.
- **Crosshair Raycast with VoxelShape Clip (`GhostBlockRaycastHelper`, `MaterialHudElement`)**:
  - Uses `VoxelShape.clip(eyePos, reachEndPos, worldPos)` for exact hit detection on thin blocks (glass panes, torches, stairs, slabs, fences).
  - Displays `"Looking at: Wrong orientation (Expected: Oak Stairs [north, bottom])"` in bright warning color `0xFFFF5555`.
  - Displays `"Looking at: Wrong block (Expected: ...)"` in `0xFFFF5555`.
- **Client-Side Ghost Pick-Block (`GhostBlockRaycastHelper`, `ShhhBuilderClient`)**:
  - When the player presses vanilla pick-block (`keyPickItem`, default Middle Click) while looking at an unplaced preview block within 5 blocks, searches the player's hotbar (slots 0-8) for a matching carried item (via `MaterialCountingEngine` item mapping) and switches to that slot (`player.getInventory().setSelectedSlot(slot)`). Zero network packets sent.
- **Exact Carried Item Counting Engine (`MaterialCountingEngine`)**: Evaluates real 26.3 block properties to count the physical items a player must carry (not voxels). Correctly counts double slabs as 2, lower door/bed halves only, stackable items (candles, pickles, eggs, petals), wall mounts, potted plants (pot + flower), cauldrons, waterlogged blocks, and filters out unobtainable blocks (water, lava, fire, portals).
- **Interactive Materials Checklist Screen (`K`)**: Complete materials manager with progress bar, real-time search box, sort cycling (Most needed, A-Z, Not done first), toggles (Hide done, This layer only with step buttons, Include shulkers), 250ms throttled inventory scanning with deep shulker box container inspection, shulker math hover tooltips (`"23,578 = 368 stacks + 26, about 14 shulker boxes"`), creative-only badges, and atomic JSON persistence (`config/shhh-builder-progress.json`).
- **In-Game Material HUD (`H`)**: On-screen overlay with dual display modes (*Missing only* vs *Everything*), live crosshair pointed-block indicator (`"Looking at: Oak Planks (layer 3)"`), top progress tracker, and completion state (`"✔ All materials ready!"`).
- **Professional UI Design System (`UiStyle.java`)**: Dark neutral panels (`#141619`), 1px borders (`#30363D`), cyan accents (`#00E5FF`), full 32-bit ARGB colors everywhere, bright high-contrast text tokens (`TEXT_BODY #E6EDF3`, `TEXT_HEADER #F0F6FC`), responsive split-pane layout (38%/62% split, min 200px), pinned auto-scaling bottom action bar at `y = height - 36` (height 22px) guaranteeing zero collision with vanilla hotbar slots or experience bars, full screen dimming overlay (`0xC0101216`), button text ellipsis truncation (`..`), and responsive layouts verified from 640x360 to 1920x1080 across GUI scales 1–4.
- **Robust World-Switch & Disconnect Cleanup**: `ClientPlayConnectionEvents.DISCONNECT` and `ClientLifecycleEvents.CLIENT_STOPPING` listeners thoroughly clear active placements (`PlacementManager.clear()`), reset camera & ghost renderer caches (`GhostBlockRenderer.resetOnDisconnect()`), unload active schematic sessions (`SchematicManager.setSelectedFileName(null)`), and safely flush pending checklist progress to disk via atomic file replacement (`MaterialProgressManager.flush()`). Includes JVM shutdown hook fallback.
- **Batched Translucent Geometry Rendering**: Batches up to 2,000 textured blocks into a single `submitCustomGeometry` call per frame into `RenderTypes.translucentMovingBlock()` using a reusable local `PoseStack.Pose` and precomputed tint RGB, eliminating per-block submit node allocations for 60+ FPS performance.
- **Built-in First-Run Experience & Interactive Showcase**: Automatically copies a starter sample house (`sample_house.litematic`) on first run if the schematics folder is empty, shows a one-time welcome chat notice, and provides an interactive 7-step tutorial card guide (`TutorialScreen`) featuring animated glowing cyan borders, floating illustration icons with dynamic shadows, reactive keycap pills (`[V]`, `[R]`, `[Z]`, `[K]`, `[M]`, etc.) with hover glow and micro-tooltips, and clickable progress step dots.

---

## 2. Directory Tree & File Inventory

```
Shhh Builder/
├── .gitignore
├── LICENSE                              # MIT License
├── README.md                            # Main player-facing documentation
├── PROJECT_CONTEXT.md                   # THIS FILE (comprehensive context for AI agents)
├── Assets/
│   └── icon.png                         # Mod logo / icon (Structure Block motif)
├── Documentation/
│   ├── ARCHITECTURE.md                  # Detailed architectural overview and data flow
│   ├── AUDIT.md                         # Comprehensive audit report with verification tags
│   ├── BLOCK_COVERAGE.md                # Exhaustive coverage of all 1,397 registered blocks
│   ├── CHANGELOG.md                     # Full release history from v1.0.0-rc1 to v1.0.0-rc9
│   ├── PLAN.md                          # Implementation roadmap and design history
│   ├── PREVIEW_RESEARCH.md              # Decompiler & API research for translucent block rendering
│   ├── TESTING.md                       # In-game testing guide across all features
│   └── UI_STYLE.md                      # UI design system specification and layout matrix
└── 26.3/                                # Root of the Gradle / Fabric mod project
    ├── build.gradle                     # Loom build configuration
    ├── gradle.properties                # Minecraft 26.3, Loom 1.19.4, version=1.0.0
    ├── settings.gradle
    ├── gradlew / gradlew.bat
    ├── build/libs/
    │   └── shhh-builder-1.0.0.jar       # Current compiled production mod artifact
    ├── src/
    │   ├── main/
    │   │   ├── java/io/github/rafayshhh/shhhbuilder/
    │   │   │   └── ShhhBuilder.java     # ModInitializer (main entrypoint, logs initialization)
    │   │   └── resources/
    │   │       ├── fabric.mod.json      # Mod metadata, entrypoints, dependencies
    │   │       └── assets/shhh-builder/
    │   │           ├── icon.png         # Bundled mod icon (16x16 / 64x64)
    │   │           ├── data/
    │   │           │   └── creative_only_items.txt  # 18 creative/operator item IDs
    │   │           ├── lang/
    │   │           │   └── en_us.json   # Localization strings for keys, commands, tutorial
    │   │           └── sample/
    │   │               └── sample_house.litematic   # Bundled starter build
    │   ├── client/
    │   │   ├── java/io/github/rafayshhh/shhhbuilder/client/
    │   │   │   ├── LayerMode.java       # Enum: ALL, SINGLE, UP_TO
    │   │   │   ├── PlacementManager.java# In-world coordinates, rotation, mirroring, layer state
    │   │   │   ├── SchematicFileEntry.java # Record: path, name, format tag, size, block count
    │   │   │   ├── SchematicManager.java# Async file scanning, caching, path traversal protection
    │   │   │   ├── ShhhBuilderClient.java # ClientModInitializer, keybinds, event hooks
    │   │   │   ├── command/
    │   │   │   │   └── ShhhBuilderCommand.java # Client-side /shhhbuilder commands
    │   │   │   ├── config/
    │   │   │   │   └── ShhhBuilderConfig.java  # Config singleton, JSON load/save, clamping
    │   │   │   ├── gui/
    │   │   │   │   ├── MaterialsScreen.java    # Full checklist screen (Key K, search, progress)
    │   │   │   │   ├── ShhhBuilderConfigScreen.java # 3-tab Mod Menu config screen
    │   │   │   │   ├── ShhhBuilderScreen.java  # Main 2-panel schematic browser
    │   │   │   │   ├── TutorialScreen.java     # 7-step interactive tutorial card
    │   │   │   │   └── UiStyle.java            # Shared design system, ARGB colors, StyledButton
    │   │   │   ├── hud/
    │   │   │   │   ├── GhostBlockRaycastHelper.java # VoxelShape clip raycast & ghost pick-block
    │   │   │   │   ├── MaterialDisplayHelper.java # Formatting stacks, shulkers, item icons
    │   │   │   │   └── MaterialHudElement.java    # On-screen HUD overlay (HudElementRegistry)
    │   │   │   ├── integration/
    │   │   │   │   └── ModMenuIntegration.java    # ModMenuApi implementation
    │   │   │   ├── material/
    │   │   │   │   ├── MaterialCountingEngine.java# Exact item counting logic & layer caching
    │   │   │   │   ├── MaterialProgressManager.java# JSON progress persistence (checked items)
    │   │   │   │   └── PlayerInventoryScanner.java # 250ms throttled inventory & shulker scan
    │   │   │   ├── render/
    │   │   │   │   ├── BoxOutlineRenderer.java    # Footprint cyan bounding box renderer
    │   │   │   │   ├── GhostBlockModelHelper.java # Baked quad extraction, tint RGB & alpha
    │   │   │   │   └── GhostBlockRenderer.java    # In-world 3D block preview renderer (batched)
    │   │   │   └── schematic/
    │   │   │       ├── BlockCategoryHelper.java   # Normal, Fluid, Block Entity, Technical
    │   │   │       ├── BlockStateHelper.java      # State parsing, NbtUtils compatibility, tints
    │   │   │       ├── LitematicData.java         # Immutable parsed schematic data model
    │   │   │       ├── LitematicReader.java       # NBT reader for .litematic files
    │   │   │       ├── SchematicBlockData.java    # Raw block grid & occlusion culling
    │   │   │       ├── SchemReader.java           # NBT reader for Sponge .schem files
    │   │   │       └── WorldBlockValidator.java   # Live world comparison (exact/orient/wrong)
    │   │   └── resources/
    │   │       └── shhh-builder.client.mixins.json # Client mixins config (clean, 0 mixins)
    │   └── test/
    │       └── java/io/github/rafayshhh/shhhbuilder/client/
    │           ├── ConfigAndPaginationTest.java   # Config clamping, pagination math
    │           ├── FuzzAndSecurityTest.java       # Fuzzing, path traversal, bomb safety
    │           ├── GhostBlockTest.java            # Section chunking, coordinate transforms
    │           ├── PlacementManagerTest.java      # Rotations, mirroring, layer modes
    │           ├── TutorialScreenTest.java        # Tutorial page bounds & navigation
    │           ├── WorldSwitchAndDisconnectTest.java # World disconnect & shutdown cleanup
    │           ├── command/ShhhBuilderCommandTest.java # Client command syntax & safety
    │           ├── gui/UiStyleLayoutTest.java     # Responsive UI layout across 8 viewports
    │           ├── hud/MaterialDisplayHelperTest.java  # Stacks, shulker math, names
    │           ├── material/MaterialCountingEngineTest.java # Exact counting rules & persistence
    │           └── schematic/
    │               ├── BlockCategoryHelperTest.java # Universal block classification
    │               ├── BlockShowcaseGeneratorTest.java # 1,397 block verification & generator
    │               ├── LitematicReaderTest.java   # Litematic NBT unpacking & metadata
    │               ├── RealFilesTest.java         # Real-world axe.litematic / axe.schem test
    │               ├── SampleHouseTest.java       # sample_house.litematic parsing
    │               ├── ScanAllBlocksTest.java     # BuiltInRegistries.BLOCK scanner
    │               ├── SchemReaderTest.java       # Sponge LEB128 VarInt streaming
    │               ├── SyntheticAxeTest.java      # Synthetic benchmark parsing
    │               └── WorldBlockValidatorTest.java # Live world verification unit tests
```

---

## 3. Architecture & Core Subsystems

```
                               ┌────────────────────────────────────────────────────────┐
                               │                      User Inputs                       │
                               │  - Screens: ShhhBuilderScreen (V), MaterialsScreen (K) │
                               │  - In-World Keybinds: R (rotate), Z (flip), M (layers) │
                               │  - Client Commands: /shhhbuilder <cmd>                 │
                               └───────────────────────────┬────────────────────────────┘
                                                           │
                                                           ▼
┌───────────────────────────────┐              ┌────────────────────────┐
│      Schematics Folder        │              │    SchematicManager    │
│  (.litematic and .schem)      ├─────────────►│ - Path Traversal Guard │
│  - Auto-copies sample_house   │              │ - 50-Entry LRU Cache   │
└───────────────────────────────┘              │ - Async File I/O       │
                                               └───────────┬────────────┘
                                                           │
                                                           ▼
                                               ┌────────────────────────┐
                                               │   Schematic Parsers    │
                                               │ - LitematicReader      │
                                               │ - SchemReader          │
                                               │ - MaterialCounting     │
                                               │ - 6-Neighbor Culling   │
                                               └───────────┬────────────┘
                                                           │
                                                           ▼
                                               ┌────────────────────────┐
                                               │    PlacementManager    │
                                               │ - Anchor (BlockPos)    │
                                               │ - Rotations (0..270°)  │
                                               │ - Mirroring (X flip)   │
                                               │ - Layers (ALL/SGL/UPTO)│
                                               │ - Raycast Targeting    │
                                               └─────┬────────────┬─────┘
                                                     │            │
                     ┌───────────────────────────────┘            └───────────────────────────────┐
                     ▼                                                                            ▼
      ┌─────────────────────────────┐                                              ┌─────────────────────────────┐
      │     In-World 3D Preview     │                                              │      2D Overlays & HUD      │
      │   (LevelRenderEvents.       │                                              │   (HudElementRegistry.      │
      │      BEFORE_GIZMOS)         │                                              │         addLast)            │
      ├─────────────────────────────┤                                              ├─────────────────────────────┤
      │ - GhostBlockRenderer        │                                              │ - MaterialHudElement        │
      │   * Textured Quads (trans-  │                                              │   * Missing Only vs All     │
      │     lucent moving block)    │                                              │   * Looking at: <Block>     │
      │   * Block Borders (subtle)  │                                              │   * Progress & "All ready!" │
      │   * Outlines (MapColor)     │                                              │ - PlayerInventoryScanner    │
      │ - BoxOutlineRenderer        │                                              │   * 250ms throttled scan    │
      │   * Cyan bounding box       │                                              │   * Shulker box container   │
      └─────────────────────────────┘                                              └─────────────────────────────┘
```

---

## 4. Key Subsystem Details

### 4.1 Schematic Readers & Security Safeguards (`LitematicReader`, `SchemReader`)
- **Decompression Bomb Protection**: Uses `new NbtAccounter(256 * 1024 * 1024L, 512)` to prevent gzip bombs from exhausting memory.
- **Safety Bounds**:
  - Maximum axis dimension: `4,096` blocks.
  - Maximum total read volume: `10,000,000` blocks.
  - Maximum Sponge palette size: `65,536` entries.
- **Bit-Unpacking**: `LitematicReader` continuously unrolls variable-width palette indices (2 to 16 bits per block) across 64-bit `long[]` arrays, handling non-multiple-of-64 boundaries, multiple regions, and negative coordinate offsets.
- **VarInt Streaming**: `SchemReader` reads Sponge v1/v2/v3 LEB128 VarInt block arrays with bit-shift bounds guards (`shift >= 35`).
- **Occlusion Culling**: Fast 6-neighbor spatial culling identifies blocks enclosed on all 6 sides by solid opaque neighbors. Instead of permanently discarding them, they are tagged with `occludedY = true` in `VisibleBlock`. In `PlacementManager`, they are culled when `LayerMode == ALL` or when below the active layer in `UP_TO` mode, but kept visible when exposed on the top boundary in `SINGLE` and `UP_TO` modes, ensuring sliced floors never look hollow.
- **Section Chunking**: Visible blocks are partitioned into `16x16x16` spatial sections (`BlockSection`) with precalculated center points for distance sorting.

### 4.2 Placement & Transformations (`PlacementManager`)
- **State**: Anchor position (`BlockPos`), rotation (0°, 90°, 180°, 270°), mirrored (boolean, flips along the build's X axis), layer mode (`ALL`, `SINGLE`, `UP_TO`), and current layer index.
- **Dimension Swapping**: When rotated 90° or 270°, bounding box width and length swap cleanly.
- **Block State Transformations**: Coordinates and directional block properties (stairs, doors, chests, repeaters) are rotated or mirrored using vanilla `BlockState.rotate()` and `BlockState.mirror(Mirror.FRONT_BACK)` (which applies `OctahedralGroup.INVERT_X`, matching coordinate reflection `mx = width - 1 - x`).
- **Targeting Raycast**: Computes which ghost block the player's crosshair is pointing at in real time, displaying `"Looking at: <Block Name> (layer N)"` on the HUD.

### 4.3 In-World 3D Preview Engine (`GhostBlockRenderer`, `GhostBlockModelHelper`)
- **Render Hook**: Fabric API's `LevelRenderEvents.BEFORE_GIZMOS`.
- **Pipeline Overload & Batched Submission**: Batches all textured blocks within the camera render budget into a **SINGLE** `SubmitNodeCollector.submitCustomGeometry` call per frame into `RenderTypes.translucentMovingBlock()`. Inside the geometry callback, a reusable local `PoseStack.Pose` translates to each block offset and emits cached baked quads via `consumer.putBakedQuad`, eliminating up to 2,000 submit node allocations and pose clones per frame.
- **Top Slice Boundary Face Culling**: `shouldCullFace(...)` skips face culling for `Direction.UP` on the active slice boundary (`y == currentLayer` in `LayerMode.SINGLE` or `LayerMode.UP_TO`), ensuring exposed slice floors are never culled by hidden upper layers while still culling interior touching faces with `PlacementManager.hasBlockAt`.
- **Precomputed Block Tint**: 24-bit block tint RGB is precalculated during model caching (`CachedGhostModel.tintRgb()`), eliminating string conversions and allocations (`toString()`, `toLowerCase()`) during per-quad rendering.
- **Dynamic Opacity**: Alpha is computed once per frame (`GhostBlockModelHelper.setPreviewAlpha`) and injected dynamically into model vertex colors.
- **Block Borders**: Thin wireframe lines drawn around textured blocks using `SubmitNodeCollector.submitShapeOutline` into `RenderTypes.lines()`. Players can configure border color (Block color, White, Black, Cyan) and opacity (25%–100%).
- **Section Budget & Prioritization**: Sections are sorted front-to-back relative to the player camera. Rendering is strictly capped at the configured limits (Quality Presets: Low, Balanced, High, Custom; textured distance: 16/24/32m; textured limit: 1k/2k/3k/6k; ghost limit: 5k/10k/20k).
- **Universal Block Fallback (`BlockCategoryHelper`)**:
  - `FLUID`: Water and lava render as translucent tinted boxes.
  - `BLOCK_ENTITY`: Chests, beds, signs, banners, skulls render shaped boxes using block MapColors.
  - `TECHNICAL`: Barriers, structure voids, light blocks render small clear center markers.
  - `NORMAL`: Standard baked block quads.

### 4.4 Exact Carried Item Counting Engine (`MaterialCountingEngine`)
Calculates the **items a player must carry in their inventory to construct the build**:
- **Slabs**: `type=double` counts as 2 of that slab item. Single slabs count as 1.
- **Doors**: Counts only `half=lower` (1 door item per door). Upper half is skipped.
- **Beds**: Counts only `part=foot` (1 bed item per bed). Head part is skipped. Resolves to colored bed item.
- **Double-Height Plants**: Counts only `half=lower` (1 item). `tall_seagrass` maps to `minecraft:seagrass`.
- **Pistons**: `piston_head` and `moving_piston` are excluded; piston bases count as 1 piston item.
- **Stackable Multi-Amount Blocks**: Evaluates count properties (`candles`, `pickles`, `eggs`, `layers`, `flower_amount`, `segment_amount`) to multiply required item counts.
- **Wall Mounts**: Automatically maps wall variants (`wall_torch`, `wall_sign`, `wall_banner`, `redstone_wire`) to their carried inventory items.
- **Potted Plants**: Counts as **2 items**: 1 `flower_pot` plus 1 of the contained plant item.
- **Cauldrons**: Water, lava, and powder snow cauldrons map to 1 `cauldron`.
- **Waterlogged Blocks**: Only the host block state is evaluated; water is ignored.
- **Unobtainable Blocks**: Survival-unobtainable blocks (`water`, `lava`, `fire`, `nether_portal`, `end_portal`, `bubble_column`) count as 0 items and are reported in a separate count: *"N blocks have no item (water, fire...)"*.
- **Creative-Only Items**: Items like `barrier`, `structure_void`, `light`, `command_block` are tagged with `"Creative only"` badges loaded from `creative_only_items.txt`.
- **Per-Layer Caching**: Caches exact counts per Y layer for instant single-layer checklist filtering.

### 4.5 Inventory Scanner & Persistence (`PlayerInventoryScanner`, `MaterialProgressManager`)
- **Inventory Scanner**: Background scan throttled to **250ms** (`4 Hz`). Inspects player main inventory, offhand, and recursively inspects nested items inside shulker boxes using `DataComponents.CONTAINER`.
- **Progress Manager**: Saves checked item IDs per schematic key into `config/shhh-builder-progress.json`. Uses `StandardCopyOption.ATOMIC_MOVE` with `.tmp` fallback for crash-safe persistence.

### 4.6 UI Design System & Screens (`UiStyle`, `ShhhBuilderScreen`, `MaterialsScreen`)
- **Design System (`UiStyle.java`)**:
  - Dark neutral panels (`0xE6141619`), 1px borders (`0xFF30363D`), primary Cyan accent (`0xFF00E5FF`).
  - Full 32-bit ARGB colors everywhere (ensures visibility in 26.3).
  - 8px grid spacing, consistent typography hierarchy (`TEXT_TITLE`, `TEXT_HEADER`, `TEXT_BODY`, `TEXT_MUTED`, `TEXT_DIM`).
  - `UiStyle.StyledButton`: Custom styled buttons with embedded keycap pill badges (`.withKeyCap("R")`).
- **Main Screen (`ShhhBuilderScreen`)**:
  - Left panel: Real-time search box, file cards with format badges (`.litematic` / `.schem`), dimensions, block count, pagination footer.
  - Right panel: Details card (author, size chips, total blocks), tab switcher (`Blocks needed` vs `Position`).
  - Position tab: Move controls, Orientation (-90°, Flip, +90°), Layers (down, mode, up), and Opacity live stepper (`[-] [Opacity: 45%] [+]`).
  - Action bar: `[Place Here]` (Primary), `[Clear Build]` (Danger), `[Refresh]`, `[Open Folder]`, `[Close]`.
- **Materials Checklist Screen (`MaterialsScreen`, Key `K`)**:
  - Header with title, shortcut hint, and progress bar (`"Materials: 12/28 ready (45%)"`).
  - Search box, sort cycling (Most needed, A-Z, Not done first), toggles (Hide done, This layer only with step buttons `[◀]`/`[▶]`, Include shulkers).
  - Checkbox, `fakeItem` icons, Need/Have/Missing counts, creative-only warning badges.
  - Hover tooltips with stack breakdowns and shulker box math (`"23,578 = 368 stacks + 26, about 14 shulker boxes"`).
  - Footer actions: `Check all`, `Uncheck all`, `Copy list` (to clipboard), `Reset`.
- **Tutorial Screen (`TutorialScreen`)**:
  - 7-step modern card guide with progress dots, step badge (`STEP 1 OF 7`), 3D item icon illustrations, and keycap badges.
- **Settings Screen (`ShhhBuilderConfigScreen`)**:
  - Tab switcher: `[Preview]`, `[Performance]`, `[HUD]`.
  - Dynamic 1-line hover description bar explaining whichever setting is currently hovered or focused.
  - `[Reset Defaults]` and `[Done]`.

---

## 5. Critical Minecraft 26.3 API Nuances & Gotchas

Any AI developer modifying this codebase must be aware of these Minecraft 26.3 facts:

1. **32-Bit ARGB Color Requirement**:
   - In Minecraft 26.3, passing a 24-bit hex color (`0x00RRGGBB` or `0xRRGGBB` without alpha) results in an alpha of `0x00` (100% transparent/invisible).
   - **Rule**: Every color constant must explicitly specify the alpha byte (e.g. `0xFFFFFFFF` for white, `0xFF00E5FF` for cyan).
2. **Screen Event Signatures in 26.3**:
   - Mouse click: `public boolean mouseClicked(MouseButtonEvent event, boolean isFocused)`
   - Key press: `public boolean keyPressed(KeyEvent event)`
   - Mouse scroll: `public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)`
   - Screen transition: `mc.gui.setScreen(screen)` and `mc.gui.screen()`.
3. **NbtUtils Tag Normalization**:
   - In Minecraft 26.3, `NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag)` expects `"id"` (string) and `"properties"` (compound).
   - Legacy Litematica and Sponge formats use `"Name"` and `"Properties"`.
   - `BlockStateHelper.parseBlockState` normalizes both schemas before resolving.
4. **Color Collections**:
   - Colored blocks (beds, banners, candles) in 26.3 are grouped in `ColorCollection<Block>` rather than standalone fields.
   - Lookups use `BuiltInRegistries.BLOCK.getValue(Identifier.parse(...))` to ensure universal resolution.
5. **Shulker Box Container Inspection**:
   - Item container data in 26.3 is accessed via `stack.get(DataComponents.CONTAINER)`.
6. **SubmitNodeCollector Exception Trapping**:
   - The 26.3 render graph will hard-crash the game client if an unhandled exception escapes from `submitCustomGeometry` or `submitShapeOutline`.
   - **Rule**: Every per-block quad iteration, custom geometry submission, and section iteration MUST be wrapped in `try/catch (Throwable t)`.

---

## 6. Keybinds & Command Reference

### Default Keybinds
All keybinds are registered under the `"Shhh Builder"` category in Minecraft's Controls menu:
- **`V`**: Open Shhh Builder schematic browser (`ShhhBuilderScreen`)
- **`K`**: Open Materials Checklist screen (`MaterialsScreen`)
- **`H`**: Toggle Material HUD overlay
- **`B`**: Toggle Footprint Outline bounding box
- **`G`**: Toggle Ghost Blocks wireframe/preview
- **`R`**: Rotate placement 90° clockwise
- **`Z`**: Flip (mirror) placement left-to-right
- **`M`**: Cycle layer mode (`All` $\rightarrow$ `Single layer` $\rightarrow$ `Up to layer`)
- **`[`**: Step down 1 layer
- **`]`**: Step up 1 layer
- **`\`**: Reset layer mode to `All Layers`
- **`Arrow Keys`**: Move placement horizontally
- **`Page Up` / `Page Down`**: Move placement vertically

### Client Commands (`/shhhbuilder`)
100% client-side execution via Fabric `ClientCommands`:
- `/shhhbuilder help` — List all client commands
- `/shhhbuilder tutorial` — Open interactive tutorial
- `/shhhbuilder materials` / `/shhhbuilder checklist` — Open materials checklist
- `/shhhbuilder list` — List all schematics in folder
- `/shhhbuilder select <file>` — Select a schematic with tab-completion
- `/shhhbuilder place [x y z]` — Place build at player or coordinates
- `/shhhbuilder move <dx dy dz>` — Move placement by offset
- `/shhhbuilder rotate` — Rotate 90° clockwise
- `/shhhbuilder flip` — Mirror left-to-right
- `/shhhbuilder layer <all|N>` — Set layer mode
- `/shhhbuilder clear` — Clear active placement
- `/shhhbuilder folder` — Open schematics folder in Windows Explorer
- `/shhhbuilder selftest` — Audits all 1,397 registered blocks for preview and material mapping

---

## 7. Testing & Verification Status

- **Automated Test Suite**: 17 unit test classes in `26.3/src/test/java`, comprising 100+ assertions.
- **Full Test Run**: `BUILD SUCCESSFUL in 1m 2s` with **0 failures**.
- **Block Showcase**: `BlockShowcaseGeneratorTest` verifies exact parity across all 1,397 Minecraft 26.3 blocks, producing:
  - `build/test-files/block_showcase.litematic`
  - `build/test-files/block_showcase.schem`
  - `build/test-files/block_showcase_expected.txt`
- **Responsive Layout Verification**: `UiStyleLayoutTest` verifies that all screens fit without element clipping or overlap across 8 standard viewports (640x360 to 1920x1080) and GUI scales 1 to 4.

---

## 8. Development & Build Instructions

To compile and verify the project:
```powershell
cd 26.3
# Run full unit test suite:
.\gradlew.bat test

# Run specific material engine test:
.\gradlew.bat test --tests "io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngineTest"

# Build production mod jar:
.\gradlew.bat build

# Launch development client:
.\gradlew.bat runClient
```
The final production jar is produced at `26.3/build/libs/shhh-builder-1.0.0.jar`.
