# CurseForge Project Metadata and Setup Guide

- Mod Name: Shhh Builder
- Author: RafayShhh (https://curseforge.com/members/rafayshhh)
- Target Version: Minecraft 26.3 (Fabric)

---

## Project Details

| Setting Field | Value to Enter | Notes |
| :--- | :--- | :--- |
| Project Name | Shhh Builder | Display name on CurseForge. |
| Category | Primary: Utility & QoL<br>Secondary: Map and Information, Creative Mode | Select these in the project creation form. |
| Short Description / Summary | A clean, beginner-friendly client-side schematic viewer and building assistant for Minecraft 26.3 Fabric. Built with zero mixins, in-world holograms, live materials tracking, and fair-play client controls. | 206 characters (CurseForge requires between 100 and 250 characters). |
| Project Visibility | Public | Standard public release. |
| Environment | Client | Pure client-side mod. |
| License | MIT License | Choose MIT from the dropdown. |
| Source Code Link | https://github.com/rafayshhh/shhh-builder | GitHub repository link. |
| Issues / Bug Tracker Link | https://github.com/rafayshhh/shhh-builder/issues | GitHub issues link. |
| Project Logo / Avatar | Upload Documentation/showcase/icon.png | 1254x1254 PNG (CurseForge requires at least 400x400). |

---

## Screenshots (Images Tab)

Upload the 3 screenshot files located in `Documentation/showcase/`:

1. `showcase-main.jpg` (or `Assets/1.jpg`)
   - Title: Schematics Manager Screen
   - Description: Clean split-pane schematic picker with search filtering, placement controls, and responsive layout.
2. `showcase-materials.jpg` (or `Assets/2.jpg`)
   - Title: Interactive Materials Checklist
   - Description: Real-time item tracker showing required materials, inventory counts, shulker math, and placed-block deductions.
3. `showcase-tutorial.jpg` (or `Assets/3.jpg`)
   - Title: Interactive In-Game Tutorial
   - Description: Built-in 7-step guide featuring glowing accent borders, interactive keycap tooltips, and shortcut guides.

---

## File Upload (Files Tab)

| Field | Value |
| :--- | :--- |
| Upload Binary | `26.3/build/libs/shhh-builder-1.0.0.jar` |
| Display Name | Shhh Builder 1.0.0 (Fabric 26.3) |
| Release Type | Release |
| Supported Minecraft Version | Select 26.3 |
| Mod Loader | Select Fabric |
| Java Version | Java 25 |

### Dependencies:
Under the **Dependencies** section for the uploaded file:
- **Fabric API**: Set as **Required Dependency**
- **Mod Menu**: Set as **Optional Dependency**

### File Changelog:
```markdown
### Shhh Builder 1.0.0 for Minecraft 26.3 (Fabric)

Initial release for Minecraft 26.3 on Fabric.

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

## CurseForge Description (Copy and Paste into CurseForge)

```markdown
# Shhh Builder

A clean, lightweight schematic viewer and building assistant for Minecraft Fabric. Designed for players who want to build from schematics without dealing with complicated hotkeys or cluttered menus.

Shhh Builder is client-side only, uses zero mixins, and does not alter server gameplay or place blocks automatically.

---

### Key Features

* **In-World Holograms**: Displays translucent textured previews of your schematic right where you want to build. You can switch between textured blocks and clean wireframe outlines.
* **Performance Culling**: Interior blocks that are completely covered by surrounding blocks are hidden from rendering, keeping frame rates steady even with large builds.
* **Materials Checklist**: Press K to open a full materials list. It reads your current inventory (including shulker boxes if enabled), calculates how many stacks or shulkers you need, and tracks remaining items.
* **Build Progress Tracking**: As you place correct blocks into the world, the missing item counts decrease automatically.
* **Pick-Block Helper**: Aim at any unplaced preview block and use your middle-click (Pick Block) to automatically select that block from your hotbar or inventory.
* **Layer Slicing**: Build one layer at a time, view all layers up to your current height, or view the entire structure at once.
* **Easy Adjustments**: Rotate, mirror, or nudge your build with simple keyboard shortcuts without having to reopen the interface.
* **Built-in Tutorial**: An interactive 7-step guide walks you through the controls and features directly in-game (`/shhhbuilder tutorial`).

---

### Default Controls

* **V**: Open the main schematic manager
* **K**: Open the materials checklist
* **H**: Toggle the on-screen materials HUD
* **B**: Toggle the 3D bounding box
* **G**: Toggle preview visibility
* **R**: Rotate the build 90 degrees
* **Z**: Mirror / flip the build horizontally
* **M**: Cycle layer mode (All, Single layer, Up to current layer)
* **[** / **]**: Move the active layer down or up
* **\\**: Reset layer view to show all layers
* **Arrow keys**: Move placement North, South, West, or East
* **Page Up / Page Down**: Move placement Up or Down

*(All controls can be rebound in Options -> Controls -> Key Binds -> Shhh Builder)*

---

### Installation

1. Install **Fabric Loader** (0.20.1 or newer) for **Minecraft 26.3**.
2. Install the matching version of **Fabric API**.
3. Download `shhh-builder-1.0.0.jar` and place it in your `.minecraft/mods` folder.
4. *(Optional)* Install **Mod Menu** to view and edit settings from the in-game mod list.

Put your `.litematic` or `.schem` files into `.minecraft/schematics/`. A sample house schematic is included automatically on first launch so you can try the controls right away.

---

### Server Compliance

Shhh Builder runs entirely on your local game client. It does not send custom network packets to servers and does not contain printer or automated placement features. It functions purely as a visual reference guide and hotbar helper, making it safe to use on vanilla and survival multiplayer servers.

---

### License & Source

Licensed under the MIT License.  
Source code and issue tracker: https://github.com/rafayshhh/shhh-builder
```
