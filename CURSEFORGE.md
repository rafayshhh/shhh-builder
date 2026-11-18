# Shhh Builder

**A sleek, beginner-friendly client-side schematic viewer and building assistant for Minecraft Fabric.**  
*Zero mixins. Zero bytecode injection. 100% fair-play compliant.*

---

### 🌟 Overview

Tired of clunky, overly complex schematic tools with endless sub-menus? **Shhh Builder** brings modern UI design and effortless simplicity to schematic building in Minecraft.

Simply place your `.litematic` or `.schem` files into `.minecraft/schematics/`, press **`V`**, and start building with translucent 3D holographic ghost blocks guiding your way.

---

### ✨ Features

* **👻 Translucent Holographic Previews**: Preview structures directly in your world with realistic textures, MapColor wireframes, and 6-neighbor internal occlusion culling.
* **📋 Real-Time Materials Checklist (`K`)**: Automatically calculates needed materials, scans your inventory & shulker boxes, and tracks missing items.
* **📉 In-World Placement Deduction**: As you place blocks into your world, missing material counts dynamically decrease!
* **🎯 Middle-Click Pick Block**: Point at any ghost block and press Pick Block to automatically select or swap to the correct item from your hotbar or inventory.
* **🍰 Layer Slicing (`M`, `[`, `]`)**: Build complex multi-level structures slice by slice with **Single Layer** and **Up To** visualization modes.
* **🔄 Easy In-World Transforms**: Rotate with **`R`**, flip/mirror with **`Z`**, and nudge with the **Arrow Keys** & **PgUp/PgDn** without reopening menus.
* **🎓 Built-In Interactive Tutorial**: Learn all shortcuts and features with a guided 7-step walkthrough right in your game.
* **⚡ High Performance & Low Memory**: Zero garbage collection pauses with mutable vector operations, distance-based LOD, and a crash-safe rendering circuit breaker.
* **🛡️ Zero Mixins / Fair-Play Certified**: Sends zero packets and never auto-places blocks. 100% safe on multiplayer survival servers and fully compatible with **Sodium** and **Iris**.

---

### ⌨️ Default Keybindings

* **`V`**: Open Schematics Browser & Placement Screen
* **`K`**: Open Interactive Materials Checklist
* **`H`**: Toggle On-Screen Materials HUD
* **`B`**: Toggle 3D Bounding Box Outline
* **`G`**: Toggle Ghost Block Holograms
* **`R`**: Rotate Placement 90°
* **`Z`**: Flip / Mirror Placement
* **`M`**: Cycle Layer Mode (All / Single / Up To)
* **`[` / `]`**: Move Active Layer Down / Up
* **`\`**: Reset Layer Filter to All
* **Arrow Keys**: Nudge Placement Horizontally
* **Page Up / Page Down**: Nudge Placement Vertically

*(All keybindings can be rebound under Options → Controls → Key Binds → Shhh Builder)*

---

### 📦 Requirements & Installation

1. Install **Fabric Loader** (`0.20.1+`) for Minecraft **26.3**.
2. Install **Fabric API** for 26.3.
3. *(Optional)* Install **Mod Menu** to access in-game configuration.
4. Drop `shhh-builder-1.0.0.jar` into your `.minecraft/mods` directory and enjoy building!

---

### 📄 License

Shhh Builder is licensed under the permissive **MIT License**.  
Source code & bug tracker: [GitHub Repository](https://github.com/rafayshhh/shhh-builder)
