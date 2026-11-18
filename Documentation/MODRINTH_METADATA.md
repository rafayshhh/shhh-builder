# Modrinth Project Metadata and Setup Guide

- Mod Name: Shhh Builder
- Mod Slug: shhh-builder
- Author: RafayShhh (https://modrinth.com/user/RafayShhh)
- Target Version: Minecraft 26.3 (Fabric)

---

## Project Details

| Field | Value to Enter | Notes |
| :--- | :--- | :--- |
| Project Name | Shhh Builder | The public display name. |
| Project Slug / ID | shhh-builder | URL will be https://modrinth.com/mod/shhh-builder |
| Project Type | Mod | Standard client mod. |
| Short Summary | Clean, client-side schematic viewer and building assistant with holograms and materials tracking. | 98 characters (under the 100-character limit). |
| Categories | Utility, Creative | Check both categories in the dashboard. |
| Client / Server Side | Client: Required, Server: Unsupported | Pure client-side mod. |
| License | MIT | Select MIT from the dropdown. |
| Source Repository | https://github.com/rafayshhh/shhh-builder | GitHub repository link. |
| Issue Tracker | https://github.com/rafayshhh/shhh-builder/issues | GitHub issues link. |
| Project Icon | Upload Documentation/showcase/icon.png | 1254x1254 square PNG. |

---

## Gallery Images

Upload the 3 screenshot files located in `Documentation/showcase/`:

### Image 1: Schematics Manager Screen
- File: `Documentation/showcase/showcase-main.jpg` (or `Assets/1.jpg`)
- Title: Schematics Browser & Placement Screen
- Description: Clean split-pane schematic picker with search filtering, placement controls, and responsive layout.

### Image 2: Interactive Materials Checklist
- File: `Documentation/showcase/showcase-materials.jpg` (or `Assets/2.jpg`)
- Title: Materials Checklist & Inventory Scanner
- Description: Real-time item tracker showing required materials, inventory counts, shulker math, and placed-block deductions.

### Image 3: Interactive In-Game Tutorial
- File: `Documentation/showcase/showcase-tutorial.jpg` (or `Assets/3.jpg`)
- Title: Step-by-Step Interactive Tutorial
- Description: Built-in 7-step guide featuring glowing accent borders, interactive keycap tooltips, and shortcut guides.

---

## Version Upload Details

| Field | Value |
| :--- | :--- |
| File to Upload | `26.3/build/libs/shhh-builder-1.0.0.jar` |
| Version Title | Shhh Builder 1.0.0 (Minecraft 26.3 Fabric) |
| Version Number | 1.0.0+mc26.3 |
| Release Channel | Release |
| Supported Game Versions | 26.3 |
| Supported Mod Loaders | Fabric |
| Dependencies | Fabric API (Required), Mod Menu (Optional) |

### Version Changelog (Copy and Paste into Modrinth)

```markdown
### Shhh Builder 1.0.0 for Minecraft 26.3 (Fabric)

Initial release for Minecraft 26.3 on Fabric.

What is included:
- In-world translucent ghost block holograms with full model textures and wireframes.
- Automatic 6-neighbor internal face culling and distance LOD for smooth frame rates on large builds.
- Interactive Materials Checklist (Key: K) with live inventory scanning, shulker box counting, and stack breakdown.
- In-world block deduction: missing material counts decrease as you place blocks in the world.
- Ghost pick-block helper: middle-click unplaced preview blocks to select or equip them from your hotbar.
- Layer slicing modes (Key: M) supporting All Layers, Single Layer, and Up To modes.
- In-world placement controls: rotate (R), flip/mirror (Z), and nudge with arrow keys without reopening menus.
- Built-in 7-step interactive tutorial screen (/shhhbuilder tutorial).
- Pure client-side event architecture with zero mixins for full compatibility with Sodium, Iris, and Lithium.
- Safe session cleanup on world switch or disconnect so ghost blocks never carry over.
```

---

## Modrinth Project Description (Copy and Paste into Modrinth)

```markdown
# Shhh Builder

A clean, lightweight schematic viewer and building assistant for Minecraft Fabric. Designed for players who want to build from schematics without dealing with complicated hotkeys or cluttered menus.

Shhh Builder is client-side only, uses zero mixins, and does not alter server gameplay or place blocks automatically.

---

## What It Does

- **In-World Holograms**: Displays translucent textured previews of your schematic right where you want to build. You can switch between textured blocks and clean wireframe outlines.
- **Performance Culling**: Interior blocks that are completely covered by surrounding blocks are hidden from rendering, keeping frame rates steady even with large builds.
- **Materials Checklist**: Press K to open a full materials list. It reads your current inventory (including shulker boxes if enabled), calculates how many stacks or shulkers you need, and tracks remaining items.
- **Build Progress Tracking**: As you place correct blocks into the world, the missing item counts decrease automatically.
- **Pick-Block Helper**: Aim at any unplaced preview block and use your middle-click (Pick Block) to automatically select that block from your hotbar or inventory.
- **Layer Slicing**: Build one layer at a time, view all layers up to your current height, or view the entire structure at once.
- **Easy Adjustments**: Rotate, mirror, or nudge your build with simple keyboard shortcuts without having to reopen the interface.
- **Built-in Tutorial**: An interactive 7-step guide walks you through the controls and features directly in-game (`/shhhbuilder tutorial`).

---

## Controls

All keybindings can be customized in **Options -> Controls -> Key Binds -> Shhh Builder**:

| Action | Default Key | Category | What it does |
| :--- | :---: | :--- | :--- |
| **Open Main Screen** | V | Shhh Builder | Browse schematics, view details, search files, and place builds |
| **Materials Checklist** | K | Shhh Builder | Open interactive materials manager & inventory comparison |
| **Toggle Material HUD** | H | Shhh Builder | Toggle compact on-screen materials overlay |
| **Toggle Bounding Box** | B | Shhh Builder | Show or hide the 3D bounding box around the placement |
| **Toggle Ghost Blocks** | G | Shhh Builder | Toggle preview hologram visibility |
| **Rotate 90°** | R | Shhh Builder | Rotate active placement 90 degrees clockwise |
| **Flip / Mirror** | Z | Shhh Builder | Mirror active placement horizontally |
| **Cycle Layer Mode** | M | Shhh Builder | Switch between All Layers, Single Layer, and Up To |
| **Layer Down / Up** | [ / ] | Shhh Builder | Move active layer slice down or up |
| **Reset Layer Filter** | \ | Shhh Builder | Reset layer view to show all layers |
| **Nudge Placement** | Arrow Keys | Shhh Builder | Move placement North, South, West, or East |
| **Vertical Nudge** | PgUp / PgDn | Shhh Builder | Shift placement Up or Down by 1 block |

---

## Installation

1. Install **Fabric Loader** (0.20.1 or newer) for **Minecraft 26.3**.
2. Install the matching version of **Fabric API**.
3. Download `shhh-builder-1.0.0.jar` and place it in your `.minecraft/mods` folder.
4. *(Optional)* Install **Mod Menu** to view and edit settings from the in-game mod list.

Put your `.litematic` or `.schem` files into `.minecraft/schematics/`. A sample house schematic is included automatically on first launch so you can try the controls right away.

---

## Server Compliance

Shhh Builder runs entirely on your local game client. It does not send custom network packets to servers and does not contain printer or automated placement features. It functions purely as a visual reference guide and hotbar helper, making it safe to use on vanilla and survival multiplayer servers.

---

## License & Source

Licensed under the MIT License.  
Source code and issue tracker: https://github.com/rafayshhh/shhh-builder
```
