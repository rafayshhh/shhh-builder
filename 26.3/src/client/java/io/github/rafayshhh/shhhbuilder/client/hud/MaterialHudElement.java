package io.github.rafayshhh.shhhbuilder.client.hud;

import io.github.rafayshhh.shhhbuilder.client.LayerMode;
import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import io.github.rafayshhh.shhhbuilder.client.SchematicManager;
import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.gui.UiStyle;
import io.github.rafayshhh.shhhbuilder.client.material.MaterialProgressManager;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MaterialHudElement implements HudElement {
	// HUD Colors in 32-bit ARGB (using UiStyle palette with test-verified alpha)
	public static final int HUD_BACKGROUND_COLOR = 0x90141619; // Translucent dark neutral panel (alpha = 0x90)
	public static final int HUD_BORDER_COLOR     = 0x40FFFFFF; // Subtle 1px border (alpha = 0x40)
	public static final int HUD_TEXT_TITLE_COLOR = UiStyle.TEXT_TITLE;
	public static final int HUD_TEXT_LAYER_COLOR = UiStyle.COLOR_ACCENT;
	public static final int HUD_TEXT_ITEM_COLOR  = UiStyle.TEXT_BODY;
	public static final int HUD_TEXT_MORE_COLOR  = UiStyle.TEXT_MUTED;
	public static final int HUD_TEXT_HINT_COLOR  = UiStyle.TEXT_MUTED;

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
		if (!config.isHudEnabled()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();

		// 6. Must not draw while a menu or screen is open
		if (mc.gui != null && mc.gui.screen() != null) {
			return;
		}

		// 6. Must hide when the F1 hide-HUD key is used
		if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.isHidden()) {
			return;
		}

		// 8. If no schematic is selected or loaded, draw nothing
		LitematicData data = SchematicManager.getSelectedSchematicData();
		if (data == null) {
			return;
		}

		renderHud(extractor, mc, config, data);
	}

	private void renderHud(GuiGraphicsExtractor extractor, Minecraft mc, ShhhBuilderConfig config, LitematicData data) {
		Font font = mc.font;
		if (font == null) {
			return;
		}

		// Title line: schematic file name (Requirement 7)
		String fileName = SchematicManager.getSelectedFileName();
		String titleStr;
		if (fileName != null && !fileName.isBlank()) {
			titleStr = fileName;
		} else if (data.name() != null && !data.name().isBlank() && !data.name().equalsIgnoreCase("unnamed")) {
			titleStr = data.name();
		} else {
			titleStr = "Blocks needed";
		}
		Component titleComp = Component.literal(titleStr).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);

		// Layer mode indicator if not in "All" mode
		Component layerComp = null;
		if (PlacementManager.getLayerMode() != LayerMode.ALL) {
			String layerModeName = PlacementManager.getLayerMode() == LayerMode.SINGLE ? "Single" : "Up to";
			layerComp = Component.literal("Layer " + PlacementManager.getCurrentLayer() + "/" + data.height() + " (" + layerModeName + ")")
				.withStyle(ChatFormatting.AQUA);
		}

		// "Looking at: <Block Name> (layer N)" indicator when pointing at a ghost block within reach
		Component lookingAtComp = findLookingAtGhostBlock(mc);
		int lookingAtHeight = lookingAtComp != null ? 10 : 0;

		// Exact material counts from MaterialCountingEngine
		Map<String, Long> requiredCounts;
		if (PlacementManager.getLayerMode() == LayerMode.SINGLE) {
			requiredCounts = data.getMaterialCountsForLayer(PlacementManager.getCurrentLayer() - 1);
		} else {
			requiredCounts = data.itemCounts();
		}

		// Checklist & inventory integration
		String schematicKey = MaterialProgressManager.buildKey(fileName, 0, 0);
		Map<String, Integer> inventoryCounts = Collections.emptyMap();
		if (mc.player != null) {
			inventoryCounts = io.github.rafayshhh.shhhbuilder.client.material.PlayerInventoryScanner.scanInventory(
				mc, config.isIncludeShulkerBoxes(), false
			);
		}

		// In-world placed blocks scan (throttled 250ms)
		Map<String, Integer> placedCounts = Collections.emptyMap();
		if (mc.level != null && PlacementManager.hasPlacement()) {
			int layerFilter = (PlacementManager.getLayerMode() == LayerMode.SINGLE)
				? (PlacementManager.getCurrentLayer() - 1)
				: -1;
			placedCounts = io.github.rafayshhh.shhhbuilder.client.material.PlayerInventoryScanner.scanPlacedBlocks(
				mc, layerFilter, false
			);
		}

		int totalTypes = 0;
		int readyTypes = 0;
		long totalItems = 0;
		long readyItems = 0;

		record HudItemEntry(String itemId, long need, int have, long missing, boolean isDone) {}
		List<HudItemEntry> allEntries = new ArrayList<>();

		for (Map.Entry<String, Long> entry : requiredCounts.entrySet()) {
			String itemId = entry.getKey();
			long need = entry.getValue();
			if (need <= 0) continue;

			int invHave = inventoryCounts.getOrDefault(itemId, 0);
			int placedHave = placedCounts.getOrDefault(itemId, 0);
			int have = invHave + placedHave; // effectiveHave = inventoryHave + placedInWorld
			boolean isChecked = MaterialProgressManager.isChecked(schematicKey, itemId);
			boolean isDone = isChecked || (config.isAutoCheckFromInventory() && have >= need);
			long missing = isDone ? 0 : Math.max(0, need - have);

			totalTypes++;
			totalItems += need;
			if (isDone) {
				readyTypes++;
				readyItems += need;
			} else {
				readyItems += Math.min(have, need);
			}

			allEntries.add(new HudItemEntry(itemId, need, have, missing, isDone));
		}

		// Progress line indicator
		Component progressComp = null;
		int progressHeight = 0;
		if (totalTypes > 0) {
			int pct = totalItems > 0 ? (int) Math.round((double) readyItems * 100.0 / (double) totalItems) : 0;
			progressComp = Component.literal("Materials: " + readyTypes + "/" + totalTypes + " ready (" + pct + "%)")
				.withStyle(readyTypes == totalTypes ? ChatFormatting.GREEN : ChatFormatting.AQUA);
			progressHeight = 10;
		}

		// Filter items based on HudShowMode setting
		List<HudItemEntry> displayList;
		if (config.getHudShowMode() == ShhhBuilderConfig.HudShowMode.MISSING_ONLY) {
			displayList = allEntries.stream()
				.filter(e -> !e.isDone() && e.missing() > 0)
				.sorted((a, b) -> Long.compare(b.missing(), a.missing()))
				.toList();
		} else {
			displayList = allEntries.stream()
				.sorted((a, b) -> Long.compare(b.need(), a.need()))
				.toList();
		}

		Component allDoneComp = null;
		int allDoneHeight = 0;
		if (config.getHudShowMode() == ShhhBuilderConfig.HudShowMode.MISSING_ONLY && readyTypes == totalTypes && totalTypes > 0) {
			allDoneComp = Component.literal("✔ All materials ready!").withStyle(ChatFormatting.GREEN);
			allDoneHeight = 12;
		}

		int countToShow = Math.min(displayList.size(), 10);
		int remaining = displayList.size() - countToShow;

		int padding = 6;
		int margin = 8;

		float scale = config.getHudScale().getScale();
		int scaledScreenWidth = Math.max(100, (int) (extractor.guiWidth() / scale));
		int scaledScreenHeight = Math.max(100, (int) (extractor.guiHeight() / scale));
		int maxAllowedWidth = Math.max(120, scaledScreenWidth - (margin * 2) - (padding * 2));

		// Truncate title if wider than screen allows
		if (font.width(titleComp) > maxAllowedWidth) {
			String truncated = font.plainSubstrByWidth(titleStr, maxAllowedWidth - font.width("...")) + "...";
			titleComp = Component.literal(truncated).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
		}

		// Calculate required content dimensions
		int titleWidth = font.width(titleComp);
		int maxLineWidth = titleWidth;
		if (layerComp != null) {
			int layerWidth = font.width(layerComp);
			if (layerWidth > maxLineWidth) {
				maxLineWidth = Math.min(layerWidth, maxAllowedWidth);
			}
		}
		if (progressComp != null) {
			int progressWidth = font.width(progressComp);
			if (progressWidth > maxLineWidth) {
				maxLineWidth = Math.min(progressWidth, maxAllowedWidth);
			}
		}
		if (allDoneComp != null) {
			int allDoneWidth = font.width(allDoneComp);
			if (allDoneWidth > maxLineWidth) {
				maxLineWidth = Math.min(allDoneWidth, maxAllowedWidth);
			}
		}
		if (lookingAtComp != null) {
			int lookingAtWidth = font.width(lookingAtComp);
			if (lookingAtWidth > maxLineWidth) {
				maxLineWidth = Math.min(lookingAtWidth, maxAllowedWidth);
			}
		}

		for (int i = 0; i < countToShow; i++) {
			HudItemEntry entry = displayList.get(i);
			String displayName = MaterialDisplayHelper.formatBlockName(entry.itemId());
			long displayCount = (config.getHudShowMode() == ShhhBuilderConfig.HudShowMode.MISSING_ONLY) ? entry.missing() : entry.need();
			String countStr = MaterialDisplayHelper.formatCount(displayCount);
			int countWidth = font.width(": " + countStr);
			int maxNameWidth = maxAllowedWidth - 20 - countWidth;
			if (maxNameWidth > 20 && font.width(displayName) > maxNameWidth) {
				displayName = font.plainSubstrByWidth(displayName, maxNameWidth - font.width("...")) + "...";
			}
			int lineWidth = 20 + font.width(displayName) + countWidth;
			if (lineWidth > maxLineWidth) {
				maxLineWidth = Math.min(lineWidth, maxAllowedWidth);
			}
		}

		String moreText = null;
		if (remaining > 0) {
			moreText = "+ " + remaining + " more";
			int moreWidth = font.width(moreText);
			if (moreWidth > maxLineWidth) {
				maxLineWidth = Math.min(moreWidth, maxAllowedWidth);
			}
		}

		// Key hint line on HUD when placed (Requirement 3)
		Component hintComp = null;
		int hintHeight = 0;
		if (config.isShowKeyHints() && PlacementManager.hasPlacement()) {
			String rotKey = "R";
			String flipKey = "Z";
			String layerDownKey = "[";
			String layerUpKey = "]";
			String hudKey = "H";
			if (net.minecraft.client.Minecraft.getInstance() != null) {
				rotKey = io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.getKeyName(io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.ROTATE_KEY, "R");
				flipKey = io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.getKeyName(io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.FLIP_KEY, "Z");
				layerDownKey = io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.getKeyName(io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.LAYER_DOWN_KEY, "[");
				layerUpKey = io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.getKeyName(io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.LAYER_UP_KEY, "]");
				hudKey = io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.getKeyName(io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient.TOGGLE_HUD_KEY, "H");
			}
			String hintStr = rotKey + " turn | " + flipKey + " flip | " + layerDownKey + " " + layerUpKey + " layer | " + hudKey + " blocks needed";
			hintComp = Component.literal(hintStr).withStyle(ChatFormatting.GRAY);
			int hintWidth = font.width(hintComp);
			if (hintWidth > maxLineWidth) {
				maxLineWidth = Math.min(hintWidth, maxAllowedWidth);
			}
			hintHeight = 11;
		}

		int panelWidth = maxLineWidth + (padding * 2);
		int lineHeight = 18;
		int titleHeight = 10;
		int titleGap = 4;
		int layerHeight = layerComp != null ? 10 : 0;
		int panelHeight = padding + titleHeight + layerHeight + progressHeight + (lookingAtComp != null ? (lookingAtHeight + 2) : 0) + titleGap + (countToShow * lineHeight) + (allDoneComp != null ? allDoneHeight : 0) + (remaining > 0 ? 12 : 0) + hintHeight + padding;

		int x;
		int y;

		switch (config.getHudPosition()) {
			case TOP_RIGHT -> {
				x = scaledScreenWidth - panelWidth - margin;
				y = margin;
			}
			case BOTTOM_LEFT -> {
				x = margin;
				y = scaledScreenHeight - panelHeight - margin;
			}
			case BOTTOM_RIGHT -> {
				x = scaledScreenWidth - panelWidth - margin;
				y = scaledScreenHeight - panelHeight - margin;
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

		// Ensure panel coordinates stay strictly within viewport boundaries
		x = Math.max(margin, Math.min(x, scaledScreenWidth - panelWidth - margin));
		y = Math.max(margin, Math.min(y, scaledScreenHeight - panelHeight - margin));

		// Apply HUD scaling
		extractor.pose().pushMatrix();
		extractor.pose().scale(scale, scale);

		// Dark neutral panel with 1px border
		UiStyle.drawPanel(extractor, x, y, panelWidth, panelHeight, HUD_BACKGROUND_COLOR, HUD_BORDER_COLOR);

		int drawX = x + padding;
		int drawY = y + padding;

		// 4. Title line with schematic name (full 0xFF alpha required in Minecraft 26.3)
		extractor.text(font, titleComp, drawX, drawY, HUD_TEXT_TITLE_COLOR);
		drawY += titleHeight;

		// Layer mode indicator line
		if (layerComp != null) {
			extractor.text(font, layerComp, drawX, drawY, HUD_TEXT_LAYER_COLOR);
			drawY += layerHeight;
		}

		// Progress line
		if (progressComp != null) {
			extractor.text(font, progressComp, drawX, drawY, HUD_TEXT_TITLE_COLOR);
			drawY += progressHeight;
		}

		// Looking at ghost block indicator line
		if (lookingAtComp != null) {
			extractor.text(font, lookingAtComp, drawX, drawY, HUD_TEXT_TITLE_COLOR);
			drawY += lookingAtHeight + 2;
		}
		drawY += titleGap;

		// 1-4. Material lines
		for (int i = 0; i < countToShow; i++) {
			HudItemEntry entry = displayList.get(i);
			String itemId = entry.itemId();
			long count = (config.getHudShowMode() == ShhhBuilderConfig.HudShowMode.MISSING_ONLY) ? entry.missing() : entry.need();

			String displayName = MaterialDisplayHelper.formatBlockName(itemId);
			String countStr = MaterialDisplayHelper.formatCount(count);

			// 2. Block/Item icon
			ItemStack stack = MaterialDisplayHelper.getItemStack(itemId);
			if (!stack.isEmpty()) {
				extractor.fakeItem(stack, drawX, drawY - 1);
			}

			// 2, 3. Display name and count
			int textX = drawX + 20;
			int textY = drawY + 4;
			ChatFormatting countColor = entry.isDone() ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
			extractor.text(
				font,
				Component.literal(displayName + ": ").append(Component.literal(countStr).withStyle(countColor)),
				textX,
				textY,
				HUD_TEXT_ITEM_COLOR
			);

			drawY += lineHeight;
		}

		if (allDoneComp != null) {
			extractor.text(font, allDoneComp, drawX + 2, drawY + 2, HUD_TEXT_TITLE_COLOR);
			drawY += allDoneHeight;
		}

		// 4. "+ X more" indicator
		if (remaining > 0 && moreText != null) {
			extractor.text(
				font,
				Component.literal(moreText).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY),
				drawX + 4,
				drawY + 2,
				HUD_TEXT_MORE_COLOR
			);
			drawY += 12;
		}

		// Key hints line at bottom of HUD (Requirement 3)
		if (hintComp != null) {
			extractor.text(
				font,
				hintComp,
				drawX,
				drawY + 3,
				HUD_TEXT_HINT_COLOR
			);
		}

		extractor.pose().popMatrix();
	}

	public static Component findLookingAtGhostBlock(Minecraft mc) {
		GhostBlockRaycastHelper.GhostHit hit = GhostBlockRaycastHelper.raycast(mc);
		return GhostBlockRaycastHelper.formatLookingAtComponent(hit);
	}
}
