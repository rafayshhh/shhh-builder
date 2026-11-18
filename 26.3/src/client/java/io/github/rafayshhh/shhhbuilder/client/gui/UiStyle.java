package io.github.rafayshhh.shhhbuilder.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Shared styling and UI helper for all Shhh Builder screens and HUD.
 * Uses full 32-bit ARGB colors everywhere (zero alpha is invisible in Minecraft 26.3).
 *
 * Design System:
 * - 8px spacing grid, consistent margins, 1px borders.
 * - Dark neutral panels (#141619, #1C2024).
 * - Accent: Cyan #00E5FF.
 * - Success: Green #00E676, Warning: Amber #FFB300, Error: Red #FF5252.
 * - Clear text hierarchy (title, header, body, muted, dim).
 */
public final class UiStyle {
	private UiStyle() {}

	// Grid & Spacing
	public static final int GRID = 8;
	public static final int GRID_HALF = 4;
	public static final int MARGIN = 8;
	public static final int CARD_PADDING = 6;
	public static final int BORDER_WIDTH = 1;

	// 32-bit ARGB Color Palette
	// Screen & Panels
	public static final int BG_SCREEN_OVERLAY = 0xC0101216; // Dimmed backdrop
	public static final int BG_PANEL          = 0xE6141619; // Dark neutral panel background (~90% alpha)
	public static final int BG_CARD           = 0xE61C2024; // Secondary panel / card background
	public static final int BG_CARD_HOVER     = 0xE6262B32; // Hovered card background
	public static final int BG_CARD_SELECTED  = 0xE6132832; // Selected card with dark cyan tint
	public static final int BG_CHIP           = 0xE621262D; // Small chip/badge background
	public static final int BG_KEYCAP         = 0xFF21262D; // Key-cap pill background

	// 1px Borders
	public static final int BORDER_NORMAL     = 0xFF30363D; // Subtle dark neutral border
	public static final int BORDER_LIGHT      = 0xFF485058; // Slightly brighter border/divider
	public static final int BORDER_HOVER      = 0xFF58A6FF; // Hover border
	public static final int BORDER_ACCENT     = 0xFF00E5FF; // Accent Cyan border (#00E5FF)
	public static final int BORDER_SUCCESS    = 0xFF00E676; // Success Green border
	public static final int BORDER_WARNING    = 0xFFFFB300; // Warning Amber border
	public static final int BORDER_ERROR      = 0xFFFF5252; // Error Red border

	// Accent & Semantic Colors
	public static final int COLOR_ACCENT      = 0xFF00E5FF; // Accent Cyan (#00E5FF)
	public static final int COLOR_ACCENT_HOVER= 0xFF6EFAFF; // Lighter Cyan on hover
	public static final int COLOR_ACCENT_DARK = 0xFF007799; // Deep Cyan background for primary buttons
	public static final int COLOR_SUCCESS     = 0xFF00E676; // Success Green
	public static final int COLOR_WARNING     = 0xFFFFB300; // Warning Amber
	public static final int COLOR_ERROR       = 0xFFFF5252; // Error Red

	// Typography / Text Colors
	public static final int TEXT_TITLE        = 0xFFFFFFFF; // Pure white bold headers
	public static final int TEXT_HEADER       = 0xFFF0F6FC; // Very light neutral for section headers (#F0F6FC)
	public static final int TEXT_BODY         = 0xFFE6EDF3; // Readable high-contrast light neutral body (#E6EDF3)
	public static final int TEXT_MUTED        = 0xFF8B949E; // Muted gray for metadata / hints
	public static final int TEXT_DIM          = 0xFF6E7681; // Dim text for inactive/disabled items
	public static final int TEXT_KEYCAP       = 0xFFE6EDF3; // Off-white for keycap letters
	public static final int TEXT_ACCENT       = 0xFF00E5FF; // Accent Cyan text

	/**
	 * Draws a dark neutral panel with a crisp 1px border.
	 */
	public static void drawPanel(GuiGraphicsExtractor extractor, int x, int y, int w, int h, int bgColor, int borderColor) {
		if (w <= 0 || h <= 0) return;
		extractor.fill(x, y, x + w, y + h, bgColor);
		extractor.fill(x, y, x + w, y + 1, borderColor);           // Top 1px
		extractor.fill(x, y + h - 1, x + w, y + h, borderColor);   // Bottom 1px
		extractor.fill(x, y, x + 1, y + h, borderColor);           // Left 1px
		extractor.fill(x + w - 1, y, x + w, y + h, borderColor);   // Right 1px
	}

	/**
	 * Draws an interactive card panel (with hover and selected state styles).
	 */
	public static void drawCard(GuiGraphicsExtractor extractor, int x, int y, int w, int h, boolean hovered, boolean selected) {
		int bg = selected ? BG_CARD_SELECTED : (hovered ? BG_CARD_HOVER : BG_CARD);
		int border = selected ? BORDER_ACCENT : (hovered ? BORDER_HOVER : BORDER_NORMAL);
		drawPanel(extractor, x, y, w, h, bg, border);
	}

	/**
	 * Draws a small badge (e.g. [.litematic] or [.schem]).
	 */
	public static void drawBadge(GuiGraphicsExtractor extractor, Font font, String text, int x, int y, int textColor, int bgColor, int borderColor) {
		if (font == null || text == null || text.isEmpty()) return;
		int textW = font.width(text);
		int badgeW = textW + 6;
		int badgeH = 11;
		drawPanel(extractor, x, y, badgeW, badgeH, bgColor, borderColor);
		extractor.text(font, text, x + 3, y + 2, textColor, false);
	}

	/**
	 * Draws a small key-cap label (e.g. [R], [Z], [↑]) representing a key binding.
	 */
	public static void drawKeyCap(GuiGraphicsExtractor extractor, Font font, String keyName, int x, int y) {
		drawKeyCap(extractor, font, keyName, x, y, false);
	}

	/**
	 * Draws an interactive key-cap label with reactive hover styling.
	 */
	public static void drawKeyCap(GuiGraphicsExtractor extractor, Font font, String keyName, int x, int y, boolean hovered) {
		if (font == null || keyName == null || keyName.isEmpty()) return;
		int textW = font.width(keyName);
		int capW = Math.max(12, textW + 6);
		int capH = 11;
		int bg = hovered ? 0xFF1C2D38 : BG_KEYCAP;
		int border = hovered ? COLOR_ACCENT : BORDER_LIGHT;
		int textCol = hovered ? COLOR_ACCENT : TEXT_KEYCAP;
		drawPanel(extractor, x, y, capW, capH, bg, border);
		int textX = x + (capW - textW) / 2;
		extractor.text(font, keyName, textX, y + 2, textCol, false);
	}

	/**
	 * Draws a chip tag with label (e.g. dimension chips "153 x 160 x 29").
	 */
	public static void drawChip(GuiGraphicsExtractor extractor, Font font, String label, int x, int y, int textColor, int bgColor, int borderColor) {
		if (font == null || label == null) return;
		int textW = font.width(label);
		int chipW = textW + 8;
		int chipH = 13;
		drawPanel(extractor, x, y, chipW, chipH, bgColor, borderColor);
		extractor.text(font, label, x + 4, y + 3, textColor, false);
	}

	/**
	 * Draws tutorial progress dots indicating the active step.
	 */
	public static void drawProgressDots(GuiGraphicsExtractor extractor, int currentStep, int totalSteps, int centerX, int y) {
		int dotSize = 5;
		int gap = 6;
		int totalWidth = (totalSteps * dotSize) + ((totalSteps - 1) * gap);
		int startX = centerX - (totalWidth / 2);

		for (int i = 0; i < totalSteps; i++) {
			int dx = startX + i * (dotSize + gap);
			int color = (i == currentStep) ? COLOR_ACCENT : BORDER_LIGHT;
			extractor.fill(dx, y, dx + dotSize, y + dotSize, color);
		}
	}

	/**
	 * Formats block counts with stacks representation (e.g. "2,357 (36 stacks + 53)").
	 */
	public static String formatStacks(long count) {
		if (count <= 0) return "0";
		long stacks = count / 64;
		long rem = count % 64;
		String formattedCount = String.format(Locale.ROOT, "%,d", count);
		String formattedStacks = String.format(Locale.ROOT, "%,d", stacks);

		if (stacks == 0) {
			return formattedCount;
		} else if (rem == 0) {
			return formattedCount + " (" + formattedStacks + (stacks == 1 ? " stack" : " stacks") + ")";
		} else {
			return formattedCount + " (" + formattedStacks + (stacks == 1 ? " stack" : " stacks") + " + " + rem + ")";
		}
	}

	/**
	 * Formats item requirements with stacks and shulker box calculations
	 * (e.g. "23,578 = 368 stacks + 26, about 14 shulker boxes", "128 = 2 stacks", "42").
	 */
	public static String formatNeedWithShulkers(long count) {
		if (count <= 0) return "0";
		long stacks = count / 64;
		long rem = count % 64;
		String formattedCount = String.format(Locale.ROOT, "%,d", count);
		if (stacks == 0) {
			return formattedCount;
		}

		String stackPart;
		if (rem == 0) {
			stackPart = String.format(Locale.ROOT, "%,d %s", stacks, stacks == 1 ? "stack" : "stacks");
		} else {
			stackPart = String.format(Locale.ROOT, "%,d %s + %d", stacks, stacks == 1 ? "stack" : "stacks", rem);
		}

		// A full shulker box holds 27 stacks
		if (stacks >= 27) {
			double shulkers = (double) count / (27.0 * 64.0);
			long roundedShulkers = Math.round(shulkers);
			if (roundedShulkers < 1) roundedShulkers = 1;
			return formattedCount + " = " + stackPart + ", about " + roundedShulkers + " shulker boxes";
		} else {
			return formattedCount + " = " + stackPart;
		}
	}

	/**
	 * Draws a smooth progress bar.
	 */
	public static void drawProgressBar(GuiGraphicsExtractor extractor, int x, int y, int width, int height, float progress, int fillColor) {
		extractor.fill(x, y, x + width, y + height, BG_PANEL);
		extractor.fill(x, y, x + width, y + 1, BORDER_NORMAL);
		extractor.fill(x, y + height - 1, x + width, y + height, BORDER_NORMAL);
		extractor.fill(x, y, x + 1, y + height, BORDER_NORMAL);
		extractor.fill(x + width - 1, y, x + width, y + height, BORDER_NORMAL);

		int innerW = width - 2;
		int fillW = Math.max(0, Math.min(innerW, Math.round(innerW * Math.max(0.0f, Math.min(1.0f, progress)))));
		if (fillW > 0) {
			extractor.fill(x + 1, y + 1, x + 1 + fillW, y + height - 1, fillColor);
		}
	}

	/**
	 * A custom button component that adheres strictly to the Shhh Builder design system.
	 */
	public static class StyledButton extends Button {
		public enum ButtonStyle {
			NORMAL,
			PRIMARY,
			DANGER,
			SUBTLE
		}

		private final ButtonStyle style;
		private String keyCap = null;

		public StyledButton(int x, int y, int width, int height, Component message, OnPress onPress, ButtonStyle style) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
			this.style = style;
		}

		public StyledButton(int x, int y, int width, int height, Component message, OnPress onPress) {
			this(x, y, width, height, message, onPress, ButtonStyle.NORMAL);
		}

		public StyledButton withKeyCap(String keyCap) {
			this.keyCap = keyCap;
			return this;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
			int bg;
			int border;
			int textColor;
			boolean hovered = this.isHoveredOrFocused();

			if (!this.active) {
				bg = 0x60141619;
				border = BORDER_NORMAL; // 0xFF30363D
				textColor = TEXT_MUTED; // 0xFF8B949E
			} else if (this.style == ButtonStyle.PRIMARY) {
				bg = hovered ? 0xFF0096C7 : COLOR_ACCENT_DARK;
				border = hovered ? COLOR_ACCENT_HOVER : BORDER_ACCENT;
				textColor = TEXT_TITLE;
			} else if (this.style == ButtonStyle.DANGER) {
				bg = hovered ? 0xFF991B1B : 0xFF7F1D1D;
				border = hovered ? 0xFFF87171 : BORDER_ERROR;
				textColor = TEXT_TITLE;
			} else {
				bg = hovered ? BG_CARD_HOVER : BG_CARD;
				border = hovered ? BORDER_ACCENT : BORDER_NORMAL;
				textColor = hovered ? TEXT_TITLE : TEXT_BODY;
			}

			drawPanel(extractor, this.getX(), this.getY(), this.width, this.height, bg, border);

			Font font = Minecraft.getInstance().font;
			if (font != null) {
				Component msg = this.getMessage();
				int textY = this.getY() + (this.height - font.lineHeight) / 2 + 1;

				if (this.keyCap != null && !this.keyCap.isEmpty()) {
					int keyCapW = Math.max(12, font.width(this.keyCap) + 6);
					int textMaxWidth = this.width - keyCapW - 12;
					Component displayMsg = msg;
					if (font.width(msg) > textMaxWidth && textMaxWidth > 10) {
						displayMsg = Component.literal(font.plainSubstrByWidth(msg.getString(), textMaxWidth - font.width("..")) + "..");
					}
					extractor.text(font, displayMsg, this.getX() + 6, textY, textColor, false);
					drawKeyCap(extractor, font, this.keyCap, this.getX() + this.width - keyCapW - 4, this.getY() + (this.height - 11) / 2);
				} else {
					int textMaxWidth = this.width - 8;
					Component displayMsg = msg;
					if (font.width(msg) > textMaxWidth && textMaxWidth > 10) {
						displayMsg = Component.literal(font.plainSubstrByWidth(msg.getString(), textMaxWidth - font.width("..")) + "..");
					}
					int textW = font.width(displayMsg);
					int textX = this.getX() + Math.max(4, (this.width - textW) / 2);
					extractor.text(font, displayMsg, textX, textY, textColor, false);
				}
			}
		}
	}
}
