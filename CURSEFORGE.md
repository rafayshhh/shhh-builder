# Shhh Builder

A clean, lightweight schematic viewer and building assistant for Minecraft Fabric. Designed for players who want to build from schematics without dealing with complicated hotkeys or cluttered menus.

Shhh Builder is client-side only, uses zero mixins, and does not alter server gameplay or place blocks automatically.

---

### Key Features

* **In-World Holograms**: Displays translucent textured previews of your schematic right where you want to build. You can switch between textured blocks and clean wireframe outlines.
* **Performance Culling**: Interior blocks that are completely covered by surrounding blocks are hidden from rendering, keeping frame rates steady even with large builds.
* **Materials Checklist**: Press `K` to open a full materials list. It reads your current inventory (including shulker boxes if enabled), calculates how many stacks or shulkers you need, and tracks remaining items.
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
