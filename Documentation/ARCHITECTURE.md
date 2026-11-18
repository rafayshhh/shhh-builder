# Shhh Builder - System Architecture

**Target**: Minecraft 26.3, Fabric Loader 0.20.1+, Java 25  
**Version**: `1.0.0`  
**Side**: 100% Client-Side Only (No server components, zero packets transmitted)

---

## 1. Architectural Overview

Shhh Builder is a lightweight, purely client-side Minecraft schematic viewer and holographic building assistant. It reads `.litematic` and Sponge `.schem` files directly from the local `.minecraft/schematics` directory, processes 3D voxel data with 6-neighbor occlusion culling, and renders holographic wireframes and footprint bounding boxes in the game world without placing any actual server blocks or requiring any server-side plugins or permissions.

```
                    ┌───────────────────────────────────────────┐
                    │               User Inputs                 │
                    │   (Screens, Keybinds, Client Commands)   │
                    └─────────────────────┬─────────────────────┘
                                          │
                                          ▼
┌───────────────────────┐         ┌───────────────────────────────┐
│  Schematics Folder    ├────────►│      SchematicManager         │
│ (.litematic / .schem) │         │ (Async I/O, Cache, Path Safe) │
└───────────────────────┘         └───────────────┬───────────────┘
                                                  │
                                                  ▼
                                  ┌───────────────────────────────┐
                                  │       LitematicData           │
                                  │ (Dimensions, Occlusion Culling│
                                  │     Voxel Sections, Counts)   │
                                  └───────────────┬───────────────┘
                                                  │
                                                  ▼
                                  ┌───────────────────────────────┐
                                  │       PlacementManager        │
                                  │ (Anchor, Rotation, Mirror,    │
                                  │      Layer Filtering)         │
                                  └───────┬───────────────┬───────┘
                                          │               │
                     ┌────────────────────┘               └───────────────────┐
                     ▼                                                        ▼
      ┌──────────────────────────────┐                         ┌──────────────────────────────┐
      │     In-World Renderers       │                         │       In-Game 2D Overlays    │
      │  (BoxOutline & GhostBlocks)  │                         │ (Material HUD, Action Bar)   │
      │   LevelRenderEvents.BEFORE_  │                         │     HudElementRegistry.      │
      │            GIZMOS            │                         │           addLast            │
      └──────────────────────────────┘                         └──────────────────────────────┘
```

---

## 2. Component Inventory

### A. Lifecycle & Entrypoints
- `ShhhBuilder`: Implements `ModInitializer` (main entrypoint). Logs initialization and ensures core bootstrap state.
- `ShhhBuilderClient`: Implements `ClientModInitializer`. Registers client-side event listeners, render hooks, keybind callbacks, client commands, and config loading.
- `ModMenuIntegration`: Implements ModMenu's `ModMenuApi` to provide an in-game configuration screen accessible from the Mod Menu screen.

### B. Configuration & Persistence
- `ShhhBuilderConfig`: Singleton configuration manager.
  - File: `config/shhh-builder.json`.
  - Atomic File Persistence: Writes to a `.tmp` sibling file before atomic move replacement (`StandardCopyOption.ATOMIC_MOVE`), eliminating corrupt zero-byte config files on unexpected crashes.
  - Sanitization: Clamps values on load (`ghostRenderDistance` between 16 and 128, `ghostBlockLimit` to 5,000 / 10,000 / 20,000). Gracefully recovers to safe defaults on malformed JSON.

### C. File Discovery & Path Security
- `SchematicManager`: Handles schematic discovery and asynchronous disk operations.
  - Path Resolution: Uses `FabricLoader.getInstance().getGameDir().resolve("schematics").toAbsolutePath().normalize()`.
  - Path Traversal Protection: Verifies that every resolved file path strictly begins with the normalized schematics folder. Rejects `..`, `/`, and `\` attempts.
  - LRU/Bounded Cache: Caches up to 50 parsed schematics concurrently to prevent unbounded memory growth during long gaming sessions.

### D. File Readers & Voxel Processing
- `LitematicReader`: Custom NBT reader for Litematica `.litematic` files.
  - Safety Quota: Uses `new NbtAccounter(256 * 1024 * 1024L, 512)` to prevent gzip decompression bombs.
  - Dimension Checks: Validates bounds up to 4096 blocks per axis and a maximum volume of 10,000,000 blocks to prevent memory exhaustion.
  - Continuous Bit-Unpacking: Unpacks variable-width palette indices (2 to 16 bits) from continuous 64-bit long arrays.
- `SchemReader`: Custom NBT reader for Sponge Schematic format (versions v1, v2, v3).
  - Safety Quota: Uses `new NbtAccounter(256 * 1024 * 1024L, 512)`.
  - VarInt Streaming: Streams and unpacks 7-bit continuation VarInts into integer index grids with bit-shift bounds checking.
  - Palette Protection: Caps maximum palette size to 65,536 entries.
- `SchematicBlockData`: Immutable memory model holding the parsed palette, total non-air volume, and an array of visible blocks remaining after 6-neighbor occlusion culling.

### E. Placement, Transforms & Layers
- `PlacementManager`: Holds active in-world anchor (`BlockPos`), rotation (0°, 90°, 180°, 270°), mirroring (left-to-right), active dimension, and layer filtering (`ALL`, `SINGLE`, `UP_TO`).
  - Cache Invalidation: Recomputes transformed section buffers only when placement, rotation, mirroring, or layer state changes.
  - 16x16x16 Chunking: Groups visible blocks into 16x16x16 spatial sections (`BlockSection`) with precomputed geometric center coordinates.

### F. In-World Rendering Engine (Minecraft 26.3)
- `BoxOutlineRenderer`:
  - Hook: `LevelRenderEvents.BEFORE_GIZMOS`.
  - Draw Method: `SubmitNodeCollector#submitShapeOutline`.
  - Render Type: `RenderTypes.linesDepthBias()`.
  - Color: Vibrant Cyan (`0xFF00E5FF`), guaranteed full alpha (0xFF).
- `GhostBlockRenderer`:
  - Hook: `LevelRenderEvents.BEFORE_GIZMOS`.
  - **Batched Translucent Geometry**: Batches up to 2,000 textured blocks into a single `SubmitNodeCollector#submitCustomGeometry` call per frame using `RenderTypes.translucentMovingBlock()`. Inside the geometry callback, translates a reusable local `PoseStack.Pose` per block and emits cached `BakedQuad`s via `consumer.putBakedQuad(...)` with ~45% alpha (`0x73`) and precomputed 24-bit tint RGB. Eliminates thousands of per-block submit node allocations for butter-smooth framerates.
  - **Wireframe Outlines**: Depth-biased and normal line rendering (`RenderTypes.linesDepthBias()`, `RenderTypes.lines()`) tinted by block MapColor via `BlockStateHelper`.
  - **Live World Verification (`WorldBlockValidator`)**: Queries `mc.level.getBlockState(worldPos)` every frame for active placement blocks:
    - *Exact Match*: Ghost block is skipped completely (the player already built it).
    - *Wrong Orientation (Same Block)*: Renders bright Amber/Orange wireframe outline (`0xFFFFAA00`) using `RenderTypes.linesDepthBias()` (e.g. stairs, logs placed backwards).
    - *Wrong Block*: Renders bright Red wireframe outline (`0xFFFF3333`) using `RenderTypes.linesDepthBias()`.
    - *Missing*: Renders standard textured translucent or wireframe preview.
  - **Face Culling & Layer Boundary Preservation**: Touching interior faces between adjacent ghost blocks are culled. When `LayerMode` is `SINGLE` or `UP_TO`, top slice boundary faces (`y == currentLayer`, `Direction.UP`) are preserved and never culled by hidden upper layers.
  - **Performance Guardrails**: Filters sections outside `texturedDistance` (default 24m) or `ghostRenderDistance` (default 48m), sorts sections front-to-back, and caps rendering at `texturedBlockLimit` (default 2,000) and `ghostBlockLimit` (default 10,000).
  - **3-Strike Circuit Breaker**: Wraps model quad generation and submission in isolated try/catches. If rendering fails 3 times, trips the circuit breaker to fall back to outlines for the session, preventing client crashes.

### G. Targeting, Raycasting & Ghost Pick-Block
- `GhostBlockRaycastHelper`:
  - Uses `VoxelShape.clip(eyePos, reachEndPos, worldPos)` for pixel-perfect hit detection on complex and thin blocks (torches, panes, slabs, stairs, fences).
  - Supplies real-time targeted block data to the Material HUD overlay.
  - **Ghost Pick-Block**: Intercepts vanilla pick-block (`keyPickItem`, Middle Mouse Button) during `START_CLIENT_TICK`. When aiming at an unplaced preview block within 5 blocks reach, resolves the required carried item via `MaterialCountingEngine` and searches player hotbar slots 0–8. If found, switches to that slot (`player.getInventory().setSelectedSlot(slot)`). Operates 100% client-side with zero custom packets.

### H. Materials Management & Inventory Scanning
- `MaterialCountingEngine`:
  - Evaluates real 26.3 block properties to count carried items instead of voxels: double slabs = 2, lower halves only for doors/beds/tall plants, stackable items (candles, pickles, eggs, petals), wall mounts, potted plants (1 pot + 1 plant), cauldrons, waterlogged blocks, and filters unobtainable blocks (water, lava, fire, portals).
  - Caches material requirements per layer for instantaneous filtering.
- `PlayerInventoryScanner`:
  - Scans main inventory and offhand with safe 250ms throttling to eliminate tick overhead.
  - Inspects nested shulker box container items when "Include shulkers" is enabled.
- `MaterialProgressManager`:
  - Tracks manually checked items per schematic.
  - Safely persists checklist progress to `config/shhh-builder-progress.json` using atomic file replacement (`.tmp` + `StandardCopyOption.ATOMIC_MOVE`).

### I. User Interfaces & Design System (`UiStyle.java`)
- `ShhhBuilderScreen`:
  - Responsive split-pane layout with 12px outer margins, 32px header, and 26px bottom action bar reservation.
  - Hotbar safety clearance: Bottom action bar pinned at `y = this.height - 36` with 22px buttons, guaranteeing zero overlap with vanilla hotbar slots or experience bars.
  - Usable height: `this.height - 82` (with adaptive scaling on micro-viewports `< 120px`).
  - Proportional 2-column split: Left panel (min 200px, max 240px when room permits), Right panel takes remaining width.
  - Action bar: 5 buttons (`[Place Here]`, `[Clear Build]`, `[Refresh]`, `[Open Folder]`, `[Close]`) distributed dynamically across `this.width - 24` with 6px spacing.
  - Button text truncation fallback with `..` in `StyledButton` preventing text overflow.
  - Full translucent screen overlay (`0xC0101216`) dimming background world and HUD.
  - Z-index layering: Backdrop $\rightarrow$ Headers $\rightarrow$ Panel Containers $\rightarrow$ Panel Content $\rightarrow$ `super.extractRenderState(extractor, mouseX, mouseY, delta)`.
- `MaterialsScreen` (Key `K`):
  - Dedicated full-screen checklist with progress bar (`"Materials: 12/28 ready (45%)"`), instant search, sort cycling (*Most needed*, *A to Z*, *Not done first*), toggles (*Hide done*, *This layer only*, *Include shulkers*), shulker math hover tooltips, and creative-only warning badges.
- `TutorialScreen`:
  - 7-step interactive guide featuring animated pulsing cyan accent borders, reactive keycaps with hover glow and micro-tooltips, floating 3D item models with drop shadows, and clickable progress dots.
- `ShhhBuilderConfigScreen`:
  - 3-tab Mod Menu config screen (`Preview`, `Performance`, `HUD`) with custom cyan opacity slider and dynamic 1-line hover description bar.
- `MaterialHudElement` (Key `H`):
  - On-screen HUD overlay with dual display modes (*Missing only* vs *Everything*), live crosshair pointed-block indicator, top progress tracker, and completion state.

### J. Client Commands, Keybinds & Cleanup
- `ShhhBuilderCommand`:
  - Registered via Fabric API's `ClientCommandRegistrationCallback`.
  - Client-side subcommands: `help`, `list`, `select`, `place`, `move`, `rotate`, `flip`, `layer`, `clear`, `folder`, `materials`, `checklist`, `tutorial`, `selftest`.
- **Keybinds**:
  - `V` (Open Screen), `K` (Materials Checklist), `H` (Toggle HUD), `B` (Toggle Outline), `G` (Toggle Ghost), `R` (Rotate 90°), `Z` (Flip), `M` (Layer Mode), `[` / `]` (Layer Down / Up), `\` (Reset All Layers), Arrow Keys / Page Up/Down (Nudge).
- **World-Switch & Disconnect Cleanup**:
  - Listeners on `ClientPlayConnectionEvents.DISCONNECT` and `ClientLifecycleEvents.CLIENT_STOPPING` clear active placements, reset camera & ghost renderer caches, unload active schematic sessions, and safely flush checklist progress to disk.
  - JVM shutdown hook ensures progress is saved even upon sudden client termination.

---

## 3. Threading & Concurrency Model

1. **Client Render Thread**:
   - Manages all GUI screens, HUD overlays, input events, and in-world line rendering.
   - Accesses volatile placement and config state without blocking.
2. **Dedicated Background I/O Thread (`ShhhBuilder-Reader`)**:
   - A single-threaded daemon executor (`Executors.newSingleThreadExecutor`) dedicated to reading, decompressing, and parsing schematic files from disk.
   - Guarantees the game client never stutters or drops frames when a player selects a multi-megabyte schematic file.
3. **Thread Safety**:
   - State variables (`position`, `rotation`, `mirrored`, `layerMode`, `currentLayer`) in `PlacementManager` are marked `volatile`.
   - `CACHE` in `SchematicManager` is backed by `ConcurrentHashMap` with thread-safe eviction.
