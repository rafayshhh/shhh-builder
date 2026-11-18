# Shhh Builder v1.0.0 Release Notes

**Minecraft 26.3 Fabric • Client-Only • Zero Mixins**

We are thrilled to announce the official **v1.0.0** release of **Shhh Builder**! 

Shhh Builder is a clean, modern, beginner-friendly schematic viewer and building companion designed for seamless survival building without clutter or complex keybind mazes.

---

### 🚀 Highlights

- **Holographic & Wireframe Previews**:
  - Translucent (~45% alpha) textured 3D ghost blocks rendered directly in the game world.
  - Automatic 6-neighbor internal face culling and distance LOD for butter-smooth FPS.
  - Fail-safe render circuit breaker that steps down to wireframes if a shader error occurs.
- **Interactive Materials Manager**:
  - Live inventory and shulker box scanning with stack and shulker math.
  - In-world block deduction: missing counts automatically reduce as you place blocks in the world.
  - One-click **[Copy List]** button for shopping lists.
- **Smart Hotbar & Pick-Block**:
  - Middle-click ghost block targeting selects or equips the required item from your hotbar or inventory.
  - Real-time on-screen HUD with current block target and height layer info.
- **Layer Slicing & Precision Controls**:
  - Cycle between **All Layers**, **Single Layer**, and **Up To** modes (`M`).
  - Move slice up/down with `[` and `]`.
  - Rotate (`R`), flip (`Z`), and nudge with arrow keys without reopening menus.
- **7-Step Interactive In-Game Tutorial**:
  - Interactive walkthrough with glowing cyan cards, reactive keycaps, and hover tooltips.
- **Fair-Play Certified**:
  - Purely client-side visual guidance and standard client slot selection. Sends zero network packets, does zero auto-printing, and never gets you banned on multiplayer servers.
- **Zero Mixins**:
  - 100% pure Fabric API event hooks with zero bytecode injection. Flawless compatibility with Sodium, Iris, Lithium, and VulkanMod.

---

### 📦 Files & Cryptographic Checksums

| File | Size | SHA-256 |
| :--- | :---: | :--- |
| **`shhh-builder-1.0.0.jar`** | 1,442,968 bytes | `6CCC223528DF5CC025A22DBEE9BDA2CCFB6A7E64353CB6F63ECE0CB8160D5DCD` |

**SHA-512**:
```text
68B29A7F472DD5E467AC26695E14393AEC3915C7548AD6EFA6139998E0E134627D05D530F630772832430D610A3FD3203480BBFFE2B1C11F2A0EC18141895623
```
