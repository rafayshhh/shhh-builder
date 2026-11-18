# 🏗️ Shhh Builder

**A sleek, beginner-friendly client-side schematic viewer and building assistant for Minecraft Fabric.**  
*Zero mixins. Zero bytecode edits. 100% fair-play compliant.*

[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.20.1+-blue.svg)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)

---

## ✨ Why Shhh Builder?

Most schematic mods in Minecraft are overloaded with dozens of complex menus, overwhelming hotkeys, and heavy configurations. **Shhh Builder** was created to solve this:
- **Drop-and-Play Simplicity**: Drop any `.litematic` or `.schem` file into `.minecraft/schematics/`, press **`V`**, and start building.
- **Modern Dark-Mode UI**: Designed from scratch with clean typography, high-contrast text, 1px borders, and responsive split-pane layouts.
- **Zero Mixins / 100% Mod Compatibility**: Uses pure Fabric API rendering events. It never injects into Minecraft's core classes, guaranteeing flawless compatibility with **Sodium**, **Iris**, **Lithium**, and shader packs.
- **Fair-Play Certified**: Operates 100% client-side. Transmits **zero custom network packets**, performs **no automated block-placing (no printer/botting)**, and only selects items from your hotbar via vanilla client mechanics. Completely safe on multiplayer survival servers!

---

## 🚀 Key Features

### 👻 Holographic & Wireframe Previews
- **Textured Holograms**: View translucent (~45% alpha) ghost blocks with full block models and textures right inside your world.
- **Culling & Performance**: 6-neighbor internal face culling and distance LOD ensure butter-smooth 60+ FPS even with mega-schematics.
- **Fail-Safe Circuit Breaker**: If an experimental shader or unusual resource pack causes a render glitch, Shhh Builder gracefully falls back to crisp wireframes without crashing your game.

### 📋 Interactive Materials Checklist (`K`)
- **Live Inventory Scanner**: Compares required blocks against items in your main inventory and hotbar in real time.
- **Shulker Box Awareness**: Optionally counts materials tucked inside shulker boxes in your inventory.
- **Stack & Shulker Math**: See quantities broken down by exact block count, stacks + items, and full shulker boxes.
- **Deducts Placed Blocks**: Automatically tracks blocks you have already placed in the world so your "missing" count shrinks as you build!
- **One-Click Clipboard Export**: Click **[Copy List]** to copy a clean shopping list directly to your clipboard.

### 🎯 Ghost Block Pick-Block & Auto-Hotbar
- Looking at a ghost block while holding a pickaxe? Middle-click (Pick Block) instantly selects the required block from your hotbar or inventory.
- Real-time on-screen target indicator tells you exactly what block you're looking at and its height layer.

### 🍰 Layer Slicing & Visual Controls
- **Layer Modes**: Toggle between **All Layers**, **Single Layer** (build slice-by-slice), or **Up To Layer** (build from ground up).
- **In-World Placement Controls**:
  - **Rotate (`R`)**: Rotate your build 90° clockwise around its origin.
  - **Flip / Mirror (`Z`)**: Mirror the structure across the horizontal axis.
  - **Arrow Keys & Page Up/Down**: Nudge the schematic forward, backward, left, right, up, or down by 1 block without reopening the menu!

### 🎓 Interactive In-Game Tutorial
- First time using the mod? The built-in interactive tutorial walks you through all features step-by-step with interactive glowing keycaps, micro-tooltips, and clear visual demonstrations.

---

## ⌨️ Default Keybindings

All keybindings can be customized in the vanilla **Options → Controls → Key Binds** menu under the **Shhh Builder** category:

| Key | Action | Description |
| :---: | :--- | :--- |
| **`V`** | **Open Main Screen** | Browse schematics, view details, search files, and place builds. |
| **`K`** | **Materials Checklist** | Open full interactive materials manager & inventory comparison. |
| **`H`** | **Toggle Material HUD** | Toggle the compact materials overlay on your screen. |
| **`B`** | **Toggle Bounding Box** | Show or hide the cyan 3D bounding box around the placement. |
| **`G`** | **Toggle Ghost Blocks** | Quickly toggle hologram/wireframe preview visibility. |
| **`R`** | **Rotate 90°** | Rotate active placement 90 degrees clockwise. |
| **`Z`** | **Flip / Mirror** | Mirror active placement horizontally. |
| **`M`** | **Cycle Layer Mode** | Switch between All Layers, Single Layer, and Up To. |
| **`[`** / **`]`** | **Layer Down / Up** | Move the active layer slice down or up. |
| **`\`** | **Reset to All Layers** | Instantly reset layer filtering back to show all blocks. |
| **Arrows** | **Nudge N / S / W / E** | Shift schematic position by 1 block in the cardinal directions. |
| **PgUp** / **PgDn** | **Nudge Up / Down** | Shift schematic vertically by 1 block. |

---

## 📥 Installation

1. Make sure you have **Fabric Loader** (`0.20.1` or higher) installed for **Minecraft 26.3**.
2. Download **`shhh-builder-1.0.0.jar`**.
3. Place the `.jar` file into your `.minecraft/mods` folder.
4. *(Required)* Install **Fabric API** for Minecraft 26.3.
5. *(Optional)* Install **Mod Menu** to access Shhh Builder settings directly from the in-game mods list.
6. Launch the game! A sample house schematic will be deployed automatically to your `.minecraft/schematics/` folder to get you started.

---

## ❓ Frequently Asked Questions (FAQ)

<details>
<summary><b>Does Shhh Builder auto-place blocks (like a printer / bot)?</b></summary>
<b>No.</b> Shhh Builder is strictly a visual guide and material helper. It does not send packets to the server or place blocks automatically. This ensures full compliance with server anti-cheat rules and survival fair-play policies.
</details>

<details>
<summary><b>Which schematic formats are supported?</b></summary>
Both <b>.litematic</b> (Litematica format) and modern <b>.schem</b> (Sponge/WorldEdit format) files are supported out of the box!
</details>

<details>
<summary><b>Can I use this with Sodium, Iris, or Shaderpacks?</b></summary>
<b>Yes!</b> Shhh Builder uses zero mixins and standard Fabric API render pipeline stages. It is 100% compatible with Sodium, Iris shaders, Lithium, and FerriteCore.
</details>

<details>
<summary><b>Where do I put my schematic files?</b></summary>
Place them in <code>.minecraft/schematics/</code>. You can also click the <b>[Open Folder]</b> button in the bottom action bar inside the <code>V</code> menu to open the directory directly!
</details>

---

## 📜 License & Source Code

Shhh Builder is open source under the permissive [MIT License](https://opensource.org/licenses/MIT).  
Source code, issue tracking, and discussions are hosted on [GitHub](https://github.com/rafayshhh/shhh-builder).
