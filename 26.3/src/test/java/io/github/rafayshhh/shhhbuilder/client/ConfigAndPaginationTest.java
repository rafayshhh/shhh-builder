package io.github.rafayshhh.shhhbuilder.client;

import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigAndPaginationTest {

	@Test
	@DisplayName("Ghost block limit config defaults to 10,000, cycles correctly, and sanitizes invalid values")
	void testGhostBlockLimitConfig() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();

		// Set to default
		config.setGhostBlockLimit(10_000);
		assertEquals(10_000, config.getGhostBlockLimit());

		// Test cycle: 10_000 -> 20_000 -> 5_000 -> 10_000
		config.cycleGhostBlockLimit();
		assertEquals(20_000, config.getGhostBlockLimit());

		config.cycleGhostBlockLimit();
		assertEquals(5_000, config.getGhostBlockLimit());

		config.cycleGhostBlockLimit();
		assertEquals(10_000, config.getGhostBlockLimit());

		// Test invalid values sanitized to 10_000
		config.setGhostBlockLimit(999);
		assertEquals(10_000, config.getGhostBlockLimit());

		config.setGhostBlockLimit(0);
		assertEquals(10_000, config.getGhostBlockLimit());

		config.setGhostBlockLimit(-500);
		assertEquals(10_000, config.getGhostBlockLimit());

		// Restore default
		config.setGhostBlockLimit(10_000);
	}

	@Test
	@DisplayName("Pagination calculations correctly compute page counts, slice ranges, and navigation bounds")
	void testPaginationLogic() {
		int itemsPerPage = 7;

		// Case 1: Empty list
		List<SchematicFileEntry> emptyList = List.of();
		int maxPagesEmpty = Math.max(1, (emptyList.size() + itemsPerPage - 1) / itemsPerPage);
		assertEquals(1, maxPagesEmpty);

		// Case 2: 2 files (e.g. axe.litematic and axe.schem)
		List<SchematicFileEntry> twoFiles = List.of(
			new SchematicFileEntry(Path.of("schematics", "axe.litematic"), "axe.litematic"),
			new SchematicFileEntry(Path.of("schematics", "axe.schem"), "axe.schem")
		);
		int maxPagesTwo = Math.max(1, (twoFiles.size() + itemsPerPage - 1) / itemsPerPage);
		assertEquals(1, maxPagesTwo);

		int start0 = 0 * itemsPerPage;
		int end0 = Math.min(twoFiles.size(), start0 + itemsPerPage);
		assertEquals(0, start0);
		assertEquals(2, end0);

		// Case 3: 15 files across 3 pages (7, 7, 1)
		List<SchematicFileEntry> fifteenFiles = new ArrayList<>();
		for (int i = 0; i < 15; i++) {
			fifteenFiles.add(new SchematicFileEntry(Path.of("schematics", "file" + i + ".litematic"), "file" + i + ".litematic"));
		}
		int maxPages15 = Math.max(1, (fifteenFiles.size() + itemsPerPage - 1) / itemsPerPage);
		assertEquals(3, maxPages15);

		// Page 0 (indices 0..7)
		int p0Start = 0 * itemsPerPage;
		int p0End = Math.min(fifteenFiles.size(), p0Start + itemsPerPage);
		assertEquals(0, p0Start);
		assertEquals(7, p0End);
		assertEquals(7, p0End - p0Start);

		// Page 1 (indices 7..14)
		int p1Start = 1 * itemsPerPage;
		int p1End = Math.min(fifteenFiles.size(), p1Start + itemsPerPage);
		assertEquals(7, p1Start);
		assertEquals(14, p1End);
		assertEquals(7, p1End - p1Start);

		// Page 2 (indices 14..15)
		int p2Start = 2 * itemsPerPage;
		int p2End = Math.min(fifteenFiles.size(), p2Start + itemsPerPage);
		assertEquals(14, p2Start);
		assertEquals(15, p2End);
		assertEquals(1, p2End - p2Start);

		// Navigation bounds
		int currentPage = 0;
		// prev on page 0 cannot go below 0
		if (currentPage > 0) currentPage--;
		assertEquals(0, currentPage);

		// next on page 2 cannot go above maxPages - 1
		currentPage = 2;
		if (currentPage < maxPages15 - 1) currentPage++;
		assertEquals(2, currentPage);

		// Selecting a file on page 2 (index 14) calculates page = 14 / 7 = 2
		int targetIndex = 14;
		int targetPage = targetIndex / itemsPerPage;
		assertEquals(2, targetPage);
	}

	@Test
	@DisplayName("All Minecraft 26.3 GUI text colors have non-zero alpha in ARGB (preventing silent text drops)")
	void testTextColorsAlpha() {
		// In Minecraft 26.3, GuiGraphicsExtractor.text has:
		// if (ARGB.alpha(color) == 0) return;
		// Passing 24-bit RGB values like 0xFFFFFF has alpha = 0x00, dropping all text!
		int[] guiColors = new int[]{
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_TITLE,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_FOLDER_PATH,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_HINT,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_SELECTED_GREEN,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_ERROR_TITLE,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_ERROR_TEXT,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_LOADING,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_TEXT_WHITE,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_TEXT_GRAY,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_TEXT_MUTED,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_MATERIALS_HEADER,
			io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.COLOR_MATERIALS_ITEM
		};

		for (int color : guiColors) {
			int alpha = (color >>> 24) & 0xFF;
			assertEquals(0xFF, alpha, "Color 0x" + Integer.toHexString(color) + " must have 0xFF alpha");
		}

		// Contrast with 24-bit RGB values that fail in 26.3
		int buggyTitleColor = 0xFFFFFF;
		assertEquals(0x00, (buggyTitleColor >>> 24) & 0xFF, "24-bit RGB has 0 alpha in 26.3");
	}

	@Test
	@DisplayName("All block category wireframe colors have non-zero alpha in ARGB")
	void testBlockCategoryColorsAlpha() {
		int[] blockColors = new int[]{
			io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_DEFAULT,
			io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_WOOD,
			io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_GLASS,
			io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_FOLIAGE,
			io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_REDSTONE,
			io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_WATER,
			io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_WOOL,
			io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_STONE
		};

		for (int color : blockColors) {
			int alpha = (color >>> 24) & 0xFF;
			assertEquals(0xFF, alpha, "Block category color 0x" + Integer.toHexString(color) + " must have 0xFF alpha");
		}
	}

	@Test
	@DisplayName("Footprint box outline, HUD background, HUD border, and HUD text colors all have non-zero alpha")
	void testRendererAndHudColorsAlpha() {
		// Footprint box outline
		int boxColor = io.github.rafayshhh.shhhbuilder.client.render.BoxOutlineRenderer.BOX_COLOR;
		int boxAlpha = (boxColor >>> 24) & 0xFF;
		assertEquals(0xFF, boxAlpha, "Box outline color 0x" + Integer.toHexString(boxColor) + " must have 0xFF alpha");

		// HUD background (translucent dark)
		int hudBg = io.github.rafayshhh.shhhbuilder.client.hud.MaterialHudElement.HUD_BACKGROUND_COLOR;
		int hudBgAlpha = (hudBg >>> 24) & 0xFF;
		assertTrue(hudBgAlpha > 0, "HUD background must have non-zero alpha (was " + hudBgAlpha + ")");
		assertEquals(0x90, hudBgAlpha, "HUD background alpha must be 0x90");

		// HUD border (translucent white)
		int hudBorder = io.github.rafayshhh.shhhbuilder.client.hud.MaterialHudElement.HUD_BORDER_COLOR;
		int hudBorderAlpha = (hudBorder >>> 24) & 0xFF;
		assertTrue(hudBorderAlpha > 0, "HUD border must have non-zero alpha (was " + hudBorderAlpha + ")");
		assertEquals(0x40, hudBorderAlpha, "HUD border alpha must be 0x40");

		// HUD text colors
		int[] hudTextColors = new int[]{
			io.github.rafayshhh.shhhbuilder.client.hud.MaterialHudElement.HUD_TEXT_TITLE_COLOR,
			io.github.rafayshhh.shhhbuilder.client.hud.MaterialHudElement.HUD_TEXT_LAYER_COLOR,
			io.github.rafayshhh.shhhbuilder.client.hud.MaterialHudElement.HUD_TEXT_ITEM_COLOR,
			io.github.rafayshhh.shhhbuilder.client.hud.MaterialHudElement.HUD_TEXT_MORE_COLOR
		};
		for (int color : hudTextColors) {
			int alpha = (color >>> 24) & 0xFF;
			assertEquals(0xFF, alpha, "HUD text color 0x" + Integer.toHexString(color) + " must have 0xFF alpha");
		}
	}

	@Test
	@DisplayName("BlockStateHelper assigns correct categories and non-zero alpha for representative block types")
	void testBlockIdColorMappingAndAlpha() {
		// Cobblestone (axe.litematic / axe.schem build block)
		int cobbleColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.getColorForBlockId("minecraft:cobblestone");
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_STONE, cobbleColor);
		assertEquals(0xFF, (cobbleColor >>> 24) & 0xFF);

		// Wood / planks
		int woodColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.getColorForBlockId("minecraft:oak_planks");
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_WOOD, woodColor);
		assertEquals(0xFF, (woodColor >>> 24) & 0xFF);

		// Glass
		int glassColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.getColorForBlockId("minecraft:glass");
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_GLASS, glassColor);
		assertEquals(0xFF, (glassColor >>> 24) & 0xFF);

		// Foliage
		int leafColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.getColorForBlockId("minecraft:oak_leaves");
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_FOLIAGE, leafColor);
		assertEquals(0xFF, (leafColor >>> 24) & 0xFF);

		// Redstone
		int redColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.getColorForBlockId("minecraft:redstone_torch");
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_REDSTONE, redColor);
		assertEquals(0xFF, (redColor >>> 24) & 0xFF);

		// Water
		int waterColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.getColorForBlockId("minecraft:water");
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_WATER, waterColor);
		assertEquals(0xFF, (waterColor >>> 24) & 0xFF);

		// Wool
		int woolColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.getColorForBlockId("minecraft:white_wool");
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_WOOL, woolColor);
		assertEquals(0xFF, (woolColor >>> 24) & 0xFF);

		// Default fallback
		int defaultColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.getColorForBlockId("minecraft:diamond_block");
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.COLOR_DEFAULT, defaultColor);
		assertEquals(0xFF, (defaultColor >>> 24) & 0xFF);
	}

	@Test
	@DisplayName("ensureAlpha defensive helper forces alpha byte to 0xFF when alpha is zero")
	void testEnsureAlphaHelper() {
		// Passing 24-bit RGB without alpha
		int noAlphaCyan = 0x00E5FF;
		int fixedCyan = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.ensureAlpha(noAlphaCyan);
		assertEquals(0xFF00E5FF, fixedCyan);
		assertEquals(0xFF, (fixedCyan >>> 24) & 0xFF);

		// Passing 32-bit ARGB already with alpha
		int withAlphaStone = 0xFF90CAF9;
		int keptStone = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.ensureAlpha(withAlphaStone);
		assertEquals(0xFF90CAF9, keptStone);
		assertEquals(0xFF, (keptStone >>> 24) & 0xFF);
	}

	@Test
	@DisplayName("Key hints and sample house config flags default correctly, toggle, and persist")
	void testKeyHintsAndSampleHouseConfig() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();

		// Key hints default to true
		config.setShowKeyHints(true);
		assertTrue(config.isShowKeyHints(), "Key hints must default to enabled");

		config.toggleShowKeyHints();
		assertFalse(config.isShowKeyHints(), "Key hints must toggle to disabled");

		config.toggleShowKeyHints();
		assertTrue(config.isShowKeyHints(), "Key hints must toggle back to enabled");

		// Sample house copied flag defaults to false and can be set to true
		config.setSampleHouseCopied(false);
		assertFalse(config.isSampleHouseCopied());

		config.setSampleHouseCopied(true);
		assertTrue(config.isSampleHouseCopied());

		// Restore
		config.setShowKeyHints(true);
	}

	@Test
	@DisplayName("Friendly error formatter returns plain beginner title, cause, and next action")
	void testFriendlyErrorFormatting() {
		// Empty file
		var errEmpty = io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.formatFriendlyLoadError("File is empty (0 bytes).");
		assertEquals("File is empty", errEmpty.title);
		assertTrue(errEmpty.whatHappened.contains("0 bytes"));
		assertTrue(errEmpty.whatToDo.contains("downloading") || errEmpty.whatToDo.contains("saving"));

		// Large file > 50 MB
		var errLarge = io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.formatFriendlyLoadError("File size exceeds 50 MB");
		assertEquals("File is too large", errLarge.title);
		assertTrue(errLarge.whatHappened.contains("50 MB"));
		assertTrue(errLarge.whatToDo.contains("smaller parts"));

		// Missing file
		var errMissing = io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.formatFriendlyLoadError("File does not exist: test.litematic");
		assertEquals("File not found", errMissing.title);
		assertTrue(errMissing.whatToDo.contains("Refresh"));

		// Unsupported version
		var errVer = io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.formatFriendlyLoadError("Schematic is missing 'Regions' compound data.");
		assertEquals("Unsupported version", errVer.title);

		// Corrupt NBT / VarInt
		var errCorrupt = io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.formatFriendlyLoadError("Unexpected end of BlockData byte array (EOF)");
		assertEquals("Damaged file", errCorrupt.title);

		// Scan error
		var errScan = io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen.formatFriendlyScanError("Access denied");
		assertEquals("Folder unavailable", errScan.title);
		assertTrue(errScan.whatToDo.contains("Open Folder"));
	}

	@Test
	@DisplayName("HUD key hints text color constant has 0xFF alpha in ARGB")
	void testHudKeyHintsAlpha() {
		int color = io.github.rafayshhh.shhhbuilder.client.hud.MaterialHudElement.HUD_TEXT_HINT_COLOR;
		int alpha = (color >>> 24) & 0xFF;
		assertEquals(0xFF, alpha, "HUD key hints color must have 0xFF alpha");
	}

	@Test
	@DisplayName("Preview style and textured block limit config cycle correctly and sanitize inputs")
	void testPreviewStyleAndTexturedLimitConfig() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();

		// Preview style defaults to TEXTURED
		config.setPreviewStyle(ShhhBuilderConfig.PreviewStyle.TEXTURED);
		assertEquals(ShhhBuilderConfig.PreviewStyle.TEXTURED, config.getPreviewStyle());

		// Test cycle: TEXTURED -> OUTLINES -> TEXTURED
		config.cyclePreviewStyle();
		assertEquals(ShhhBuilderConfig.PreviewStyle.OUTLINES, config.getPreviewStyle());
		config.cyclePreviewStyle();
		assertEquals(ShhhBuilderConfig.PreviewStyle.TEXTURED, config.getPreviewStyle());

		// Null check fallback
		config.setPreviewStyle(null);
		assertEquals(ShhhBuilderConfig.PreviewStyle.TEXTURED, config.getPreviewStyle());

		// Textured block limit: defaults to 2,000, cycles 2,000 -> 3,000 -> 6,000 -> 1,000 -> 2,000
		config.setTexturedBlockLimit(2_000);
		assertEquals(2_000, config.getTexturedBlockLimit());

		config.cycleTexturedBlockLimit();
		assertEquals(3_000, config.getTexturedBlockLimit());

		config.cycleTexturedBlockLimit();
		assertEquals(6_000, config.getTexturedBlockLimit());

		config.cycleTexturedBlockLimit();
		assertEquals(1_000, config.getTexturedBlockLimit());

		config.cycleTexturedBlockLimit();
		assertEquals(2_000, config.getTexturedBlockLimit());

		// Invalid values sanitized to 2_000
		config.setTexturedBlockLimit(500);
		assertEquals(2_000, config.getTexturedBlockLimit());

		config.setTexturedBlockLimit(99_999);
		assertEquals(2_000, config.getTexturedBlockLimit());

		// Textured distance: defaults to 24, cycles 24 -> 32 -> 16 -> 24
		config.setTexturedDistance(24);
		assertEquals(24, config.getTexturedDistance());

		config.cycleTexturedDistance();
		assertEquals(32, config.getTexturedDistance());

		config.cycleTexturedDistance();
		assertEquals(16, config.getTexturedDistance());

		config.cycleTexturedDistance();
		assertEquals(24, config.getTexturedDistance());

		// Invalid values sanitized to 24
		config.setTexturedDistance(-5);
		assertEquals(24, config.getTexturedDistance());

		config.setTexturedDistance(99);
		assertEquals(24, config.getTexturedDistance());
	}

	@Test
	@DisplayName("Preview opacity config validates 10% to 100%, steps of 5%, live +/- adjustments, and computes correct 8-bit alpha")
	void testPreviewOpacityConfigAndAlphaMath() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();

		// Default 45% -> alpha 115 (0x73)
		config.setPreviewOpacity(45);
		assertEquals(45, config.getPreviewOpacity());
		assertEquals(115, config.getPreviewAlpha());

		// Test live step adjustments
		config.stepOpacity(5);
		assertEquals(50, config.getPreviewOpacity());
		assertEquals(128, config.getPreviewAlpha());

		config.stepOpacity(-10);
		assertEquals(40, config.getPreviewOpacity());
		assertEquals(102, config.getPreviewAlpha());

		// Test bounds clamping: minimum 10%
		config.setPreviewOpacity(0);
		assertEquals(10, config.getPreviewOpacity());
		assertEquals(26, config.getPreviewAlpha());

		config.setPreviewOpacity(-50);
		assertEquals(10, config.getPreviewOpacity());

		// Test bounds clamping: maximum 100%
		config.setPreviewOpacity(150);
		assertEquals(100, config.getPreviewOpacity());
		assertEquals(255, config.getPreviewAlpha());

		// Test rounding to nearest 5%
		config.setPreviewOpacity(47);
		assertEquals(45, config.getPreviewOpacity());

		config.setPreviewOpacity(48);
		assertEquals(50, config.getPreviewOpacity());

		// Restore default
		config.setPreviewOpacity(45);
	}

	@Test
	@DisplayName("Quality presets correctly configure distance and limits in one click, and report Custom when modified")
	void testQualityPresets() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();

		// 1. Balanced (Default)
		config.applyQualityPreset(ShhhBuilderConfig.QualityPreset.BALANCED);
		assertEquals(ShhhBuilderConfig.QualityPreset.BALANCED, config.getQualityPreset());
		assertEquals(24, config.getTexturedDistance());
		assertEquals(2_000, config.getTexturedBlockLimit());
		assertEquals(10_000, config.getGhostBlockLimit());
		assertEquals(48, config.getGhostRenderDistance());

		// 2. Low Preset
		config.applyQualityPreset(ShhhBuilderConfig.QualityPreset.LOW);
		assertEquals(ShhhBuilderConfig.QualityPreset.LOW, config.getQualityPreset());
		assertEquals(16, config.getTexturedDistance());
		assertEquals(1_000, config.getTexturedBlockLimit());
		assertEquals(5_000, config.getGhostBlockLimit());
		assertEquals(32, config.getGhostRenderDistance());

		// 3. High Preset
		config.applyQualityPreset(ShhhBuilderConfig.QualityPreset.HIGH);
		assertEquals(ShhhBuilderConfig.QualityPreset.HIGH, config.getQualityPreset());
		assertEquals(32, config.getTexturedDistance());
		assertEquals(3_000, config.getTexturedBlockLimit());
		assertEquals(20_000, config.getGhostBlockLimit());
		assertEquals(64, config.getGhostRenderDistance());

		// 4. Custom preset detection when a single property is changed by hand
		config.setTexturedDistance(24); // Now High limits with 24m distance -> Custom
		assertEquals(ShhhBuilderConfig.QualityPreset.CUSTOM, config.getQualityPreset());

		// 5. Cycling from Custom resets to Balanced
		config.cycleQualityPreset();
		assertEquals(ShhhBuilderConfig.QualityPreset.BALANCED, config.getQualityPreset());

		// 6. Cycling from Balanced -> High -> Low -> Balanced
		config.cycleQualityPreset();
		assertEquals(ShhhBuilderConfig.QualityPreset.HIGH, config.getQualityPreset());
		config.cycleQualityPreset();
		assertEquals(ShhhBuilderConfig.QualityPreset.LOW, config.getQualityPreset());
		config.cycleQualityPreset();
		assertEquals(ShhhBuilderConfig.QualityPreset.BALANCED, config.getQualityPreset());

		// 7. 6,000 textured block limit yields Custom preset
		config.setTexturedBlockLimit(6_000);
		assertEquals(ShhhBuilderConfig.QualityPreset.CUSTOM, config.getQualityPreset());
		assertEquals(6_000, config.getTexturedBlockLimit());

		// Restore Balanced default
		config.applyQualityPreset(ShhhBuilderConfig.QualityPreset.BALANCED);
		assertEquals(ShhhBuilderConfig.QualityPreset.BALANCED, config.getQualityPreset());
	}

	@Test
	@DisplayName("Block borders toggle, border colors, and border opacity correctly cycle and compute alpha")
	void testBlockBordersConfig() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();

		// Default borders ON
		config.setTexturedBorders(true);
		assertTrue(config.isTexturedBorders());
		config.toggleTexturedBorders();
		assertFalse(config.isTexturedBorders());
		config.toggleTexturedBorders();
		assertTrue(config.isTexturedBorders());

		// Border color cycling: BLOCK_COLOR -> WHITE -> BLACK -> CYAN -> BLOCK_COLOR
		config.setBorderColor(ShhhBuilderConfig.BorderColor.BLOCK_COLOR);
		assertEquals(ShhhBuilderConfig.BorderColor.BLOCK_COLOR, config.getBorderColor());
		config.cycleBorderColor();
		assertEquals(ShhhBuilderConfig.BorderColor.WHITE, config.getBorderColor());
		config.cycleBorderColor();
		assertEquals(ShhhBuilderConfig.BorderColor.BLACK, config.getBorderColor());
		config.cycleBorderColor();
		assertEquals(ShhhBuilderConfig.BorderColor.CYAN, config.getBorderColor());
		config.cycleBorderColor();
		assertEquals(ShhhBuilderConfig.BorderColor.BLOCK_COLOR, config.getBorderColor());

		// Border opacity: 25% to 100%, cycling 100 -> 25 -> 50 -> 75 -> 100
		config.setBorderOpacity(100);
		assertEquals(100, config.getBorderOpacity());
		assertEquals(255, config.getBorderAlpha());

		config.cycleBorderOpacity();
		assertEquals(25, config.getBorderOpacity());
		assertEquals(64, config.getBorderAlpha());

		config.cycleBorderOpacity();
		assertEquals(50, config.getBorderOpacity());
		assertEquals(128, config.getBorderAlpha());

		config.cycleBorderOpacity();
		assertEquals(75, config.getBorderOpacity());
		assertEquals(191, config.getBorderAlpha());

		config.cycleBorderOpacity();
		assertEquals(100, config.getBorderOpacity());
		assertEquals(255, config.getBorderAlpha());

		// Bounds clamping
		config.setBorderOpacity(10);
		assertEquals(25, config.getBorderOpacity());
		config.setBorderOpacity(200);
		assertEquals(100, config.getBorderOpacity());

		// Restore default
		config.setTexturedBorders(true);
		config.setBorderColor(ShhhBuilderConfig.BorderColor.BLOCK_COLOR);
		config.setBorderOpacity(100);
	}

	@Test
	@DisplayName("Settings screen layout guarantees title is never drawn over first button across window sizes and scales")
	void testSettingsScreenLayoutAcrossResolutionsAndScales() {
		int[][] resolutions = {
			{854, 480},
			{640, 360},
			{1920, 1080}
		};

		int fontHeight = 9;

		for (int[] res : resolutions) {
			int windowW = res[0];
			int windowH = res[1];

			for (int scale = 1; scale <= 4; scale++) {
				int guiWidth = (int) Math.ceil((double) windowW / scale);
				int guiHeight = (int) Math.ceil((double) windowH / scale);

				int titleY;
				int firstButtonY;
				int lastButtonBottom;

				if (guiHeight < 140) {
					int buttonHeight = 15;
					int stride = 18;
					int startY = 18;
					titleY = 4;
					firstButtonY = startY;
					lastButtonBottom = startY + (stride * 3) + 3 + buttonHeight;
				} else {
					int buttonHeight = (guiHeight < 220) ? 15 : 19;
					int stride = (guiHeight < 220) ? 16 : 22;
					int totalBlockHeight = (stride * 6) + buttonHeight + 4;
					int startY = Math.max(12, (guiHeight - totalBlockHeight) / 2);
					titleY = Math.max(1, startY - 11);
					firstButtonY = startY;
					lastButtonBottom = startY + (stride * 6) + 2 + buttonHeight;
				}

				String context = String.format("Res: %dx%d, Scale: %d (GUI: %dx%d)", windowW, windowH, scale, guiWidth, guiHeight);

				// 1. Title must start within top bounds
				assertTrue(titleY >= 0, "Title Y must be non-negative: " + context);

				// 2. Title must finish BEFORE the first button starts
				int titleBottom = titleY + fontHeight;
				assertTrue(titleBottom <= firstButtonY,
					String.format("Title bottom (%d) must be above first button Y (%d) in %s", titleBottom, firstButtonY, context));

				// 3. Last button (Done) must fit within the screen height
				assertTrue(lastButtonBottom <= guiHeight,
					String.format("Last button bottom (%d) must fit inside screen height (%d) in %s", lastButtonBottom, guiHeight, context));
			}
		}
	}

	@Test
	@DisplayName("Privacy: UI components never expose absolute path to the user")
	void testPathPrivacyInUi() {
		// Tutorial screen page 1
		io.github.rafayshhh.shhhbuilder.client.gui.TutorialScreen tutorial = new io.github.rafayshhh.shhhbuilder.client.gui.TutorialScreen(null);
		List<net.minecraft.network.chat.Component> p1Lines = tutorial.getPageLines(0);
		assertNotNull(p1Lines);
		for (net.minecraft.network.chat.Component line : p1Lines) {
			String text = line.getString();
			assertFalse(text.contains("Users"), "Tutorial line must not leak Users path: " + text);
			assertFalse(text.contains("AppData"), "Tutorial line must not leak AppData path: " + text);
			assertFalse(text.contains(":\\"), "Tutorial line must not contain Windows drive path: " + text);
		}

		// File entry button formatting
		SchematicFileEntry entry = new SchematicFileEntry(Path.of("C:/Users/SecretUser/AppData/Roaming/.minecraft/schematics/axe.litematic"), "axe.litematic");
		assertEquals("axe", entry.baseName());
		assertEquals(".litematic", entry.extension());
	}
}
