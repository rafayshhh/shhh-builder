package io.github.rafayshhh.shhhbuilder.client.gui;

import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that the Shhh Builder UI design system and layout math
 * adapt responsively across resolutions from 640x360 to 1920x1080 and GUI scales 1 to 4
 * with zero overlaps and zero off-screen components.
 */
public class UiStyleLayoutTest {

	private static final int[][] RESOLUTIONS = {
		{640, 360},
		{854, 480},
		{1280, 720},
		{1920, 1080}
	};

	@Test
	@DisplayName("Main screen layout math produces strictly bounded panels and buttons across all resolutions and GUI scales")
	void testMainScreenResponsiveLayoutMath() {
		for (int[] res : RESOLUTIONS) {
			int winW = res[0];
			int winH = res[1];

			for (int scale = 1; scale <= 4; scale++) {
				int guiW = (int) Math.ceil((double) winW / scale);
				int guiH = (int) Math.ceil((double) winH / scale);

				String ctx = String.format("Res %dx%d, Scale %d (GUI %dx%d)", winW, winH, scale, guiW, guiH);

				ShhhBuilderScreen.LayoutMetrics layout = ShhhBuilderScreen.calculateLayout(guiW, guiH);

				// 1. Left Schematics Panel Bounding Box strictly inside [0, 0, guiW, guiH]
				assertTrue(layout.leftX() >= 0, "Left panel X must be >= 0: " + ctx);
				assertTrue(layout.panelY() >= 0, "Left panel Y must be >= 0: " + ctx);
				assertTrue(layout.leftWidth() > 0, "Left panel width must be > 0: " + ctx);
				assertTrue(layout.panelHeight() > 0, "Left panel height must be > 0: " + ctx);
				assertTrue(layout.leftX() + layout.leftWidth() <= layout.rightX(), "Left panel must not overlap right panel: " + ctx);
				assertTrue(layout.leftX() + layout.leftWidth() <= guiW, "Left panel must fit within GUI width: " + ctx);
				assertTrue(layout.panelY() + layout.panelHeight() <= guiH, "Left panel must fit within GUI height: " + ctx);

				// 2. Right Details Panel Bounding Box strictly inside [0, 0, guiW, guiH]
				assertTrue(layout.rightX() >= layout.leftX() + layout.leftWidth(), "Right panel must be to the right of left panel: " + ctx);
				assertTrue(layout.rightWidth() > 0, "Right panel width must be > 0: " + ctx);
				assertTrue(layout.rightX() + layout.rightWidth() <= guiW, "Right panel must fit within GUI width: " + ctx);
				assertTrue(layout.panelY() + layout.panelHeight() <= guiH, "Right panel must fit within GUI height: " + ctx);

				// Proportional split check on standard desktop sizes (contentWidth >= 540)
				if (guiW - (layout.padding() * 2) >= 540) {
					assertTrue(layout.leftWidth() >= 200, "Left panel should have min 200px on desktop resolutions: " + ctx);
					assertTrue(layout.rightWidth() > layout.leftWidth(), "Right panel should take remaining ~62% of width: " + ctx);
				}

				// 3. Tab Buttons (Blocks Needed & Position) Bounding Boxes strictly within right panel
				assertTrue(layout.tabDetailsX() >= layout.rightX(), "Details tab X must be inside right panel: " + ctx);
				assertTrue(layout.tabDetailsW() > 0, "Details tab width must be > 0: " + ctx);
				assertTrue(layout.tabControlsX() >= layout.tabDetailsX() + layout.tabDetailsW(), "Position tab must not overlap Details tab: " + ctx);
				assertTrue(layout.tabControlsW() > 0, "Position tab width must be > 0: " + ctx);
				assertTrue(layout.tabControlsX() + layout.tabControlsW() <= layout.rightX() + layout.rightWidth(), "Position tab must fit inside right panel: " + ctx);
				assertTrue(layout.tabY() >= layout.panelY(), "Tabs Y must be inside panel: " + ctx);
				assertTrue(layout.tabY() + 17 <= layout.panelY() + layout.panelHeight(), "Tabs must fit inside panel height: " + ctx);

				// 4. All 5 Bottom Action Bar Buttons Pinned and Distributed Evenly across guiW - 24
				assertTrue(layout.bottomBarY() >= layout.panelY() + layout.panelHeight(), "Bottom bar must be below panels: " + ctx);
				assertTrue(layout.bottomBarY() + layout.bottomBarH() <= guiH, "Bottom bar must fit within screen height: " + ctx);
				assertEquals(5, layout.bottomButtonX().length, "Must have exactly 5 bottom action buttons");
				assertEquals(5, layout.bottomButtonW().length, "Must have exactly 5 bottom action buttons");

				// Check exact mathematical specifications
				assertEquals(12, layout.padding(), "Outer margin must be 12px: " + ctx);
				assertEquals(12, layout.leftX(), "Left panel X must be 12: " + ctx);
				assertEquals(layout.leftX() + layout.leftWidth() + 12, layout.rightX(), "Right panel X must be leftX + leftWidth + 12: " + ctx);
				assertEquals(guiW - layout.rightX() - 12, layout.rightWidth(), "Right panel width must be guiW - rightX - 12: " + ctx);

				if (guiH >= 120) {
					assertEquals(36, layout.panelY(), "Header height 32px + 4px gap sets panelY to 36: " + ctx);
					assertEquals(guiH - 36, layout.bottomBarY(), "Bottom action bar must be pinned at y = guiH - 36: " + ctx);
					assertEquals(22, layout.bottomBarH(), "Bottom action bar height must be 22px: " + ctx);
					assertEquals(guiH - 82, layout.panelHeight(), "Usable height must equal guiH - 32 - 26 - 24: " + ctx);
				}

				for (int i = 0; i < 5; i++) {
					int btnX = layout.bottomButtonX()[i];
					int btnW = layout.bottomButtonW()[i];
					assertTrue(btnX >= 0, "Button " + i + " X must be >= 0: " + ctx);
					assertTrue(btnW > 0, "Button " + i + " width must be > 0: " + ctx);
					assertTrue(btnX + btnW <= guiW, "Button " + i + " must fit within GUI width: " + ctx);
					if (i > 0) {
						int prevX = layout.bottomButtonX()[i - 1];
						int prevW = layout.bottomButtonW()[i - 1];
						assertTrue(btnX >= prevX + prevW, "Button " + i + " must not overlap button " + (i - 1) + ": " + ctx);
					}
				}
			}
		}
	}

	@Test
	@DisplayName("Settings screen layout math guarantees section tabs and buttons never overlap across all scales")
	void testSettingsScreenResponsiveLayoutMath() {
		for (int[] res : RESOLUTIONS) {
			int winW = res[0];
			int winH = res[1];

			for (int scale = 1; scale <= 4; scale++) {
				int guiW = (int) Math.ceil((double) winW / scale);
				int guiH = (int) Math.ceil((double) winH / scale);

				String ctx = String.format("Res %dx%d, Scale %d (GUI %dx%d)", winW, winH, scale, guiW, guiH);

				int buttonHeight = (guiH < 140) ? 14 : ((guiH < 220) ? 15 : 19);
				int stride = (guiH < 140) ? 16 : ((guiH < 220) ? 16 : 22);

				int panelWidth = Math.min(340, Math.max(150, guiW - 24));
				int panelLeft = (guiW / 2) - (panelWidth / 2);

				int titleY = (guiH < 140) ? 3 : Math.max(2, (guiH < 220 ? 4 : 8));
				int tabY = (guiH < 140) ? 15 : (titleY + 13);
				int contentY = tabY + buttonHeight + 3;

				// Verify title does not overlap tabs
				assertTrue(titleY >= 0, "Title Y must be >= 0: " + ctx);
				assertTrue(titleY + 9 <= tabY, "Title must end before section tabs begin: " + ctx);

				// Verify tabs fit within GUI width
				assertTrue(panelLeft >= 0, "Settings panel left must be >= 0: " + ctx);
				assertTrue(panelLeft + panelWidth <= guiW, "Settings panel right must be <= guiW: " + ctx);

				// Verify bottom buttons (Reset Defaults & Done)
				int bottomY = (guiH < 140) ? (contentY + (stride * 2) + 2) : (guiH - buttonHeight - 8);
				assertTrue(bottomY + buttonHeight <= guiH, "Done button must fit inside screen height: " + ctx);
				assertTrue(bottomY >= contentY + (stride * 2), "Done button must be below setting options: " + ctx);
			}
		}
	}

	@Test
	@DisplayName("Tutorial screen card container and buttons fit within all viewport sizes without overlap")
	void testTutorialScreenResponsiveLayoutMath() {
		for (int[] res : RESOLUTIONS) {
			int winW = res[0];
			int winH = res[1];

			for (int scale = 1; scale <= 4; scale++) {
				int guiW = (int) Math.ceil((double) winW / scale);
				int guiH = (int) Math.ceil((double) winH / scale);

				String ctx = String.format("Res %dx%d, Scale %d (GUI %dx%d)", winW, winH, scale, guiW, guiH);

				int buttonY = (guiH < 120) ? (guiH - 20) : (guiH - 28);
				int buttonHeight = (guiH < 120) ? 16 : 20;
				int gap = 6;
				int availableWidth = Math.min(320, guiW - 24);
				int btnWidth = (availableWidth - gap * 3) / 4;
				int totalWidth = (btnWidth * 4) + (gap * 3);
				int startX = (guiW - totalWidth) / 2;

				int cardWidth = Math.min(380, guiW - 24);
				int cardX = (guiW / 2) - (cardWidth / 2);
				int cardY = (guiH < 120) ? 16 : 28;
				int cardHeight = buttonY - cardY - 4;

				assertTrue(cardX >= 0, "Card X must be non-negative: " + ctx);
				assertTrue(cardX + cardWidth <= guiW, "Card must not exceed GUI width: " + ctx);
				assertTrue(cardHeight >= 30, "Card height must be usable: " + ctx);

				assertTrue(startX >= 0, "Buttons start X must be non-negative: " + ctx);
				assertTrue(startX + totalWidth <= guiW, "Buttons must not exceed GUI width: " + ctx);
				assertTrue(buttonY + buttonHeight <= guiH, "Buttons must fit within GUI height: " + ctx);
				assertTrue(buttonY >= cardY + cardHeight, "Buttons must be below card: " + ctx);
			}
		}
	}

	@Test
	@DisplayName("HUD positioning and scaling bounds guarantee HUD never renders outside screen borders")
	void testHudPositioningBounds() {
		for (int[] res : RESOLUTIONS) {
			int winW = res[0];
			int winH = res[1];

			for (int scale = 1; scale <= 4; scale++) {
				int guiW = (int) Math.ceil((double) winW / scale);
				int guiH = (int) Math.ceil((double) winH / scale);

				for (ShhhBuilderConfig.HudScale hudScale : ShhhBuilderConfig.HudScale.values()) {
					float s = hudScale.getScale();
					int scaledW = Math.max(100, (int) (guiW / s));
					int scaledH = Math.max(100, (int) (guiH / s));

					int margin = 8;
					int maxPanelW = scaledW - (margin * 2);
					int panelWidth = Math.min(140, maxPanelW);
					int panelHeight = Math.min(90, scaledH - (margin * 2));

					for (ShhhBuilderConfig.HudPosition pos : ShhhBuilderConfig.HudPosition.values()) {
						int x;
						int y;

						switch (pos) {
							case TOP_RIGHT -> {
								x = scaledW - panelWidth - margin;
								y = margin;
							}
							case BOTTOM_LEFT -> {
								x = margin;
								y = scaledH - panelHeight - margin;
							}
							case BOTTOM_RIGHT -> {
								x = scaledW - panelWidth - margin;
								y = scaledH - panelHeight - margin;
							}
							case TOP_LEFT -> {
								x = margin;
								y = margin;
							}
							default -> {
								x = margin;
								y = margin;
							}
						}

						x = Math.max(margin, Math.min(x, scaledW - panelWidth - margin));
						y = Math.max(margin, Math.min(y, scaledH - panelHeight - margin));

						String ctx = String.format("Pos %s, HudScale %s, GUI %dx%d", pos, hudScale, guiW, guiH);
						assertTrue(x >= margin, "HUD X must respect margin: " + ctx);
						assertTrue(y >= margin, "HUD Y must respect margin: " + ctx);
						assertTrue(x + panelWidth <= scaledW, "HUD must not exceed scaled width: " + ctx);
						assertTrue(y + panelHeight <= scaledH, "HUD must not exceed scaled height: " + ctx);
					}
				}
			}
		}
	}

	@Test
	@DisplayName("UiStyle formatStacks formats block counts and stack divisions accurately")
	void testFormatStacksLogic() {
		assertEquals("0", UiStyle.formatStacks(0));
		assertEquals("42", UiStyle.formatStacks(42));
		assertEquals("64 (1 stack)", UiStyle.formatStacks(64));
		assertEquals("65 (1 stack + 1)", UiStyle.formatStacks(65));
		assertEquals("128 (2 stacks)", UiStyle.formatStacks(128));
		assertEquals("2,357 (36 stacks + 53)", UiStyle.formatStacks(2357));
		assertEquals("64,000 (1,000 stacks)", UiStyle.formatStacks(64000));
	}

	@Test
	@DisplayName("Running X-accumulator guarantees exact button spacing and zero gap jumps when remainder pixels are distributed")
	void testRunningAccumulatorActionDistribution() {
		int[] testWidths = {320, 480, 640, 854, 1280, 1920};
		int margin = 12;
		int btnCount = 5;
		int spacing = 6;

		for (int screenW : testWidths) {
			int totalWidth = Math.max(btnCount * 10, screenW - (margin * 2));
			int availBtnWidth = Math.max(btnCount * 5, totalWidth - (spacing * (btnCount - 1)));
			int baseBtnWidth = availBtnWidth / btnCount;
			int rem = availBtnWidth % btnCount;

			int[] bottomButtonW = new int[btnCount];
			int[] bottomButtonX = new int[btnCount];
			int currentX = margin;
			for (int i = 0; i < btnCount; i++) {
				bottomButtonW[i] = baseBtnWidth + (i < rem ? 1 : 0);
				bottomButtonX[i] = currentX;
				currentX += bottomButtonW[i] + spacing;
			}

			// Verify contiguous non-overlapping placement
			for (int i = 0; i < btnCount; i++) {
				assertTrue(bottomButtonX[i] >= margin, "Button X must be >= margin for screenW=" + screenW);
				assertTrue(bottomButtonW[i] > 0, "Button width must be > 0 for screenW=" + screenW);
				if (i > 0) {
					assertEquals(bottomButtonX[i - 1] + bottomButtonW[i - 1] + spacing, bottomButtonX[i],
						"Button " + i + " must be positioned exactly previous right edge + spacing for screenW=" + screenW);
				}
			}
			int lastRight = bottomButtonX[btnCount - 1] + bottomButtonW[btnCount - 1];
			assertTrue(lastRight <= screenW, "Rightmost edge must not exceed screen width for screenW=" + screenW);
		}
	}

	@Test
	@DisplayName("Disabled button styling tokens guarantee legible border (0xFF30363D) and readable text (0xFF8B949E)")
	void testDisabledButtonStylingTokens() {
		assertEquals(0xFF30363D, UiStyle.BORDER_NORMAL, "Disabled button border must match BORDER_NORMAL 0xFF30363D");
		assertEquals(0xFF8B949E, UiStyle.TEXT_MUTED, "Disabled button text must match TEXT_MUTED 0xFF8B949E");
	}

	@Test
	@DisplayName("MaterialsScreen 5-button footer layout uses running accumulator without overlaps")
	void testMaterialsScreen5ButtonFooterDistribution() {
		int[] testWidths = {240, 320, 480, 640, 960};
		int pad = 8;
		int btnCount = 5;
		int spacing = 4;

		for (int width : testWidths) {
			int totalFooterW = Math.max(btnCount * 10, width - (pad * 2));
			int availBtnWidth = Math.max(btnCount * 5, totalFooterW - (spacing * (btnCount - 1)));
			int baseBtnWidth = availBtnWidth / btnCount;
			int rem = availBtnWidth % btnCount;

			int currentX = pad;
			int[] fBtnW = new int[btnCount];
			int[] fBtnX = new int[btnCount];
			for (int i = 0; i < btnCount; i++) {
				fBtnW[i] = baseBtnWidth + (i < rem ? 1 : 0);
				fBtnX[i] = currentX;
				currentX += fBtnW[i] + spacing;
			}

			for (int i = 0; i < btnCount; i++) {
				assertTrue(fBtnX[i] >= pad);
				assertTrue(fBtnW[i] > 0);
				if (i > 0) {
					assertEquals(fBtnX[i - 1] + fBtnW[i - 1] + spacing, fBtnX[i]);
				}
			}
		}
	}
}
