# Shhh Builder UI Style Guide & Design System

This document specifies the professional design system, color palette, component specifications, and responsive layout rules implemented in **Shhh Builder** for Minecraft 26.3.

---

## 1. Design Philosophy

- **Dark Neutral Panels with 1px Borders**: Inspired by modern toolkits and IDEs, windows and overlay cards use dark neutral panels with crisp 1px borders rather than noisy textures.
- **Single Accent Color**: Vibrant Cyan (`#00E5FF`, `0xFF00E5FF`) is used sparingly and intentionally for active elements, selected cards, progress indicators, and primary action buttons.
- **Full 32-bit ARGB Everywhere**: In Minecraft 26.3, colors with zero alpha (`0x00RRGGBB`) render completely invisible. Every color definition explicitly defines its full 8-bit alpha channel (`0xFF` for opaque elements, `0xE6` / `0x90` / `0x40` for deliberate translucency).
- **8px Spacing Grid**: All panel margins, widget paddings, and card gaps adhere to an 8px grid (8px, 16px, 24px) with 4px half-grid subdivisions.
- **Zero Jargon & Clean Terminology**:
  - Always use **"preview"**, never **"ghost"**.
  - Always use **"blocks"**, never **"voxels"**.
  - Always refer to the storage location as **"schematics folder"**, never printing raw filesystem paths (`C:\Users\...` or `/home/...`) to the UI.

---

## 2. Color Palette (32-bit ARGB)

### 2.1 Surfaces & Panels
| Token | Hex ARGB | CSS / Hex | Purpose |
|---|---|---|---|
| `BG_SCREEN_OVERLAY` | `0xC0101216` | `#101216` @ 75% | Screen backdrop dimming overlay |
| `BG_PANEL` | `0xE6141619` | `#141619` @ 90% | Main dark container panel background |
| `BG_CARD` | `0xE61C2024` | `#1C2024` @ 90% | Card item background (unselected) |
| `BG_CARD_HOVER` | `0xE6262B32` | `#262B32` @ 90% | Card background on mouse hover |
| `BG_CARD_SELECTED` | `0xE6132832` | `#132832` @ 90% | Card background when selected (subtle cyan tint) |
| `BG_CHIP` | `0xE621262D` | `#21262D` @ 90% | Dimension chip, format tag, and badge background |
| `BG_KEYCAP` | `0xFF21262D` | `#21262D` | Keyboard shortcut pill background |

### 2.2 Borders (1px)
| Token | Hex ARGB | CSS / Hex | Purpose |
|---|---|---|---|
| `BORDER_NORMAL` | `0xFF30363D` | `#30363D` | Standard 1px panel/card border |
| `BORDER_LIGHT` | `0xFF485058` | `#485058` | Internal dividers, key-cap borders |
| `BORDER_HOVER` | `0xFF58A6FF` | `#58A6FF` | Soft blue border on hovered elements |
| `BORDER_ACCENT` | `0xFF00E5FF` | `#00E5FF` | Primary Cyan accent border (selected cards & tabs) |
| `BORDER_SUCCESS` | `0xFF00E676` | `#00E676` | Success green border |
| `BORDER_WARNING` | `0xFFFFB300` | `#FFB300` | Warning amber border |
| `BORDER_ERROR` | `0xFFFF5252` | `#FF5252` | Error red border |

### 2.3 Semantic & Accent Colors
| Token | Hex ARGB | CSS / Hex | Purpose |
|---|---|---|---|
| `COLOR_ACCENT` | `0xFF00E5FF` | `#00E5FF` | Primary Cyan accent |
| `COLOR_ACCENT_HOVER` | `0xFF6EFAFF` | `#6EFAFF` | Lighter cyan highlight on hover |
| `COLOR_ACCENT_DARK` | `0xFF007799` | `#007799` | Solid cyan base for primary action buttons |
| `COLOR_SUCCESS` | `0xFF00E676` | `#00E676` | Success status, placed indicators, block counts |
| `COLOR_WARNING` | `0xFFFFB300` | `#FFB300` | Warnings, unobtainable block alerts, loading |
| `COLOR_ERROR` | `0xFFFF5252` | `#FF5252` | Error titles, destructive action buttons |

### 2.4 Typography Hierarchy
| Token | Hex ARGB | Role | Formatting |
|---|---|---|---|
| `TEXT_TITLE` | `0xFFFFFFFF` | Screen titles, active card names | Bold, pure white |
| `TEXT_HEADER` | `0xFFF0F6FC` | Section headers, panel titles, essential labels | Crisp off-white |
| `TEXT_BODY` | `0xFFE6EDF3` | Body text, button labels, descriptions, metadata | High-contrast readable light neutral (#E6EDF3) |
| `TEXT_MUTED` | `0xFF8B949E` | Secondary metadata, authors, page counters | Muted cool gray |
| `TEXT_DIM` | `0xFF6E7681` | Disabled buttons, hints, subtle tags | Dim cool gray |
| `TEXT_KEYCAP` | `0xFFE6EDF3` | Key shortcut characters inside key-caps | Clean white |

---

## 3. UI Component Specifications (`UiStyle.java`)

All components are rendered through the centralized `UiStyle` class (`io.github.rafayshhh.shhhbuilder.client.gui.UiStyle`).

### 3.1 Panels & Cards (`drawPanel`, `drawCard`)
- Drawn using 5 fill commands: 1 central rectangle for background, plus 4 1px border lines (top, bottom, left, right).
- Guarantees pixel-perfect 1px borders across all GPU drivers and GUI scales without rasterization gaps.

### 3.2 Badges & Chips (`drawBadge`, `drawChip`)
- **Format Badges**: Rendered at the right of file cards (`[.litematic]` in cyan/blue, `[.schem]` in purple).
- **Dimension Chips**: Rendered on the Details card (`153 x 160 x 29`, `23,578 blocks`).
- Fixed height: 11px for badges, 13px for chips; text vertically centered.

### 3.3 Key-Caps (`drawKeyCap`)
- Pill-style representation of keyboard shortcuts (`[V]`, `[R]`, `[Z]`, `[M]`, `[↑]`, `[PgUp]`).
- Dark background (`#21262D`) with subtle 1px border (`#485058`) and off-white centered text.
- Supports interactive hover state: highlights with cyan glow border (`#00E5FF`), dark cyan background (`#1C2D38`), and rich micro-tooltips explaining the shortcut.

### 3.4 Styled Buttons (`UiStyle.StyledButton`)
Extends vanilla `Button` while replacing default button sprites with custom dark neutral panels:
- `ButtonStyle.NORMAL`: Dark neutral card background (`#1C2024`) with 1px border (`#30363D`), accent cyan border on hover.
- `ButtonStyle.PRIMARY`: Accent cyan background (`#007799`) with cyan border (`#00E5FF`). Used for primary actions like `"Place Here"` and tutorial `"Next ▶"`.
- `ButtonStyle.DANGER`: Deep red background (`#7F1D1D`) with red border (`#FF5252`). Used for `"Clear Build"` and `"Reset Defaults"`.
- `ButtonStyle.SUBTLE`: Transparent border until hovered; used for `"Skip"` and secondary controls.
- **Embedded Keycaps**: `.withKeyCap("R")` automatically renders the button label left-aligned and a keycap badge right-aligned.

### 3.5 Progress Dots (`drawProgressDots`)
- 7x7px square dots separated by 8px gaps.
- Active step is filled with `COLOR_ACCENT` (`#00E5FF`), inactive steps with `BORDER_LIGHT` (`#485058`), and hovered dots with `COLOR_ACCENT_HOVER`.
- Fully interactive: clicking on any dot immediately jumps to that tutorial step.

### 3.6 Stack-Aware Formatter (`formatStacks`)
- Automatically divides raw item counts by 64:
  - $< 64$: `"42"`
  - Exactly 64: `"64 (1 stack)"`
  - Exact multiple: `"128 (2 stacks)"`
  - Remainder: `"2,357 (36 stacks + 53)"`

### 3.7 Progress Bar (`drawProgressBar`)
- Horizontal progress bar with dark background (`BG_CHIP`), 1px border (`BORDER_LIGHT`), and filled progress in `COLOR_SUCCESS` (`#00E676`) or `COLOR_ACCENT` (`#00E5FF`).
- Used in `MaterialsScreen` to display material gathering completion percentage.

### 3.8 Shulker Math Formatter (`formatNeedWithShulkers`)
- Computes stacks and full shulker boxes (27 stacks = 1,728 items) for high-quantity materials:
  - `< 64`: `"42"`
  - $< 1,728$: `"384 (6 stacks)"`
  - $\ge 1,728$: `"23,578 = 368 stacks + 26, about 14 shulker boxes"`

---

## 4. Screen-by-Screen Layout Specifications

### 4.1 Main Screen (`ShhhBuilderScreen`)
- **Header**:
  - Mod icon (16x16 icon at top left at `x = 12, y = 8`).
  - Title: `"Shhh Builder"` in bold white text at `x = 34, y = 12` (clean title without version badge).
  - Top-right buttons: `"Materials"` (Key K), `"How to use"`, and `"Settings"` right-aligned with 12px right margin.
- **Mathematical Viewport Partitioning**:
  - `margin = 12` (12px outer screen padding).
  - `headerHeight = 32`, placing main panel containers at `panelY = 36`.
  - `actionbarHeight = 26` reserved at bottom, with action bar buttons pinned at `y = this.height - 36` (height 22px), guaranteeing zero collision with the vanilla hotbar region, experience bar, or item stacks.
  - Usable panel height: `usableHeight = this.height - headerHeight - actionbarHeight - 24 = this.height - 82` (with adaptive scaling on micro-viewports `< 120px` to guarantee zero negative dimensions).
  - Proportional 2-column grid:
    - Left panel (Schematics list): `x = 12`, `y = 36`, width = `Math.min(240, availableWidth / 3)` (enforcing minimum 200px on desktop resolutions, max 240px), height = `usableHeight`.
    - Right panel (Details / Tabs): `x = leftX + leftWidth + 12`, `y = 36`, width = `this.width - rightX - 12`, height = `usableHeight`.
- **Left Panel ("Schematics")**:
  - Real-time search box (`EditBox`) with `"Search builds..."` placeholder.
  - File cards:
    - Primary line: Schematic base name + format badge.
    - Secondary line: Cached dimensions & total block count (or file type).
    - Selected card highlighted with 1px cyan border and dark cyan fill.
  - Footer: `[◀ Prev]` `Page X / Y` `[Next ▶]` navigation.
  - Empty states: Clear cards for empty folder and no search matches.
- **Right Panel**:
  - Top Details Card: Build name, author, dimension chips, and total blocks.
  - Tab Switcher: `[Blocks needed]` vs `[Position]`.
  - **"Blocks needed" Tab**: Scrollable list of items with icons (`fakeItem`), names, and stack counts; notices for unobtainable/unknown blocks; `[Checklist]` button opening `MaterialsScreen`; `"Show on HUD"` button.
  - **"Position" Tab**: Grouped controls for Move (`Left`, `Fwd`, `Right`, `Down`, `Back`, `Up`), Orientation (`-90°`, `Flip`, `+90°`), Layers (`▼ Layer`, `Mode`, `Layer ▲`, `Show All`), and Opacity live stepper (`[-] [Opacity: 45%] [+]`), each with real keycaps.
- **Pinned & Auto-Scaling Bottom Action Bar**:
  - Pinned securely at `y = this.height - 36` with height 22px.
  - Total width: `totalWidth = this.width - 24`.
  - 5 buttons distributed evenly with 6px spacing: `[Place Here]` (Primary), `[Clear Build]` (Danger), `[Refresh]`, `[Open Folder]`, `[Close]`. Dynamic button widths and remainder distribution ensure zero clipping, zero gaps, and zero overlaps across all resolutions and scales.
- **Screen Dimming Backdrop & Clean Z-Index**:
  - Draws `extractor.fill(0, 0, this.width, this.height, BG_SCREEN_OVERLAY)` first to dim background world and HUD elements.
  - Render pipeline order: Backdrop $\rightarrow$ Headers $\rightarrow$ Panel Containers $\rightarrow$ Panel Content $\rightarrow$ `super.extractRenderState(extractor, mouseX, mouseY, delta)` ensures all buttons, search boxes, and tooltips render cleanly on top with zero z-fighting.
  - Button text ellipsis truncation: When button text exceeds button width without keycaps, `StyledButton` automatically truncates using `font.plainSubstrByWidth` with `..` fallback so text never spills over button borders.

### 4.2 Tutorial Screen (`TutorialScreen`)
- 7-step modern guide with progress dots and step badge (`STEP 1 OF 7`).
- Large illustration item icon (`fakeItem`) at top-right of the card.
- 2 to 3 short, friendly lines per page with inline keycaps:
  - Step 1: Getting Started (`CHEST`)
  - Step 2: Pick a Build (`COMPASS`)
  - Step 3: Collect Your Materials (`BUNDLE` - Materials checklist `K`, inventory scanning, shulker math)
  - Step 4: Place in World (`STRUCTURE_BLOCK`)
  - Step 5: Move and Turn (`RECOVERY_COMPASS`)
  - Step 6: Build by Layers (`SCAFFOLDING`)
  - Step 7: Tips & Controls (`EXPERIENCE_BOTTLE`)
- Navigation: `[◀ Back]`, `[Skip]`, `[Close]`, and `[Next ▶]` (or `[Finish]`).

### 4.3 Settings Screen (`ShhhBuilderConfigScreen`)
- Section Tab Switcher at top: `[Preview]`, `[Performance]`, `[HUD]`.
- Active section shows 3 to 5 options in clean cards.
- Bottom 1-line hover description bar: Dynamically describes the hovered setting.
- Actions: `[Reset Defaults]` and `[Done]`.
- Mouse wheel scroll cycles between sections.

### 4.4 Materials Checklist Screen (`MaterialsScreen`)
- **Header**: Schematic title, Keybind shortcut hint (`K`), and close button.
- **Progress Bar**: Visual bar and text (`"Materials: 12/28 ready (45%)"` or `"✔ All materials ready!"`).
- **Toolbar**: Real-time search box, sort cycle button, `Hide done` toggle, `Layer` filter with `[◀]` / `[▶]` step buttons, and `Shulkers` inclusion toggle.
- **Scrollable Item Rows**: Checkbox, 3D `fakeItem` icon, friendly block name, creative-only warning badge, and Have / Need counts. Hover tooltips provide full stack and shulker box breakdowns.
- **Action Bar Footer**: `Check all`, `Uncheck all`, `Copy list`, and `Reset`.

### 4.5 In-Game HUD (`MaterialHudElement`)
- Dark neutral card panel matching `UiStyle` with 1px border.
- Schematic title in bold gold/white.
- Layer mode chip (when not in "All" mode).
- Pointed block indicator (`"Looking at: Oak Planks (layer 3)"`).
- Item icons, friendly block names, and stack counts.
- Toggleable keybind hints line at bottom.

---

## 5. Responsive Viewport Matrix

All screens are verified by layout-math unit tests (`UiStyleLayoutTest.java`) across standard Minecraft viewport resolutions and GUI scales:

| Resolution | GUI Scale | Scaled Dimensions | Main Screen | Config Screen | Tutorial Screen | HUD |
|---|---|---|---|---|---|---|
| **640x360** | Scale 1 | 640 x 360 | Full 2-panel | 2-column cards | Full card | Fits corners |
| **640x360** | Scale 2 | 320 x 180 | Balanced panels | Tabbed cards | Compact card | Fits corners |
| **640x360** | Scale 3 | 214 x 120 | 50/50 panels | Ultra-compact tabs | Compact card | Scaled bounds |
| **854x480** | Scale 2 | 427 x 240 | Full 2-panel | 2-column cards | Full card | Fits corners |
| **854x480** | Scale 4 | 214 x 120 | 50/50 panels | Ultra-compact tabs | Compact card | Scaled bounds |
| **1280x720** | Scale 3 | 427 x 240 | Full 2-panel | 2-column cards | Full card | Fits corners |
| **1920x1080**| Scale 3 | 640 x 360 | Full 2-panel | 2-column cards | Full card | Fits corners |
| **1920x1080**| Scale 4 | 480 x 270 | Full 2-panel | 2-column cards | Full card | Fits corners |

**Guarantees**:
- Panel widths are never negative or compressed below usability limits.
- Title and header texts never overlap with interactive buttons.
- Bottom action bars never clip off-screen.
- Zero elements render outside the viewport.
