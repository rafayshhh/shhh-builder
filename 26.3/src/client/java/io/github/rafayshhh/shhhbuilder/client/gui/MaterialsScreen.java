package io.github.rafayshhh.shhhbuilder.client.gui;

import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import io.github.rafayshhh.shhhbuilder.client.SchematicFileEntry;
import io.github.rafayshhh.shhhbuilder.client.SchematicManager;
import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine;
import io.github.rafayshhh.shhhbuilder.client.material.MaterialProgressManager;
import io.github.rafayshhh.shhhbuilder.client.material.PlayerInventoryScanner;
import io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Materials checklist screen for Shhh Builder.
 * Provides a searchable, sortable list of required items with stack & shulker math,
 * live player inventory comparison, checkbox progress persistence, layer filtering,
 * and plain text list copying.
 */
public class MaterialsScreen extends Screen {

	public enum SortMode {
		MOST_NEEDED("Most needed"),
		ALPHABETICAL("A to Z"),
		NOT_DONE_FIRST("Not done first");

		private final String displayName;

		SortMode(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public SortMode next() {
			SortMode[] vals = values();
			return vals[(this.ordinal() + 1) % vals.length];
		}
	}

	public record MaterialRow(
		String itemId,
		ItemStack stack,
		String displayName,
		long need,
		int have,
		long missing,
		boolean isChecked,
		boolean isDone,
		boolean isCreativeOnly
	) {}

	private final Screen parent;
	private String schematicKey = "unknown";
	private String schematicFileName = null;
	private LitematicData schematicData = null;

	private EditBox searchBox;
	private UiStyle.StyledButton sortButton;
	private UiStyle.StyledButton hideDoneButton;
	private UiStyle.StyledButton layerFilterButton;
	private UiStyle.StyledButton prevLayerButton;
	private UiStyle.StyledButton nextLayerButton;
	private UiStyle.StyledButton shulkersButton;

	private UiStyle.StyledButton checkAllButton;
	private UiStyle.StyledButton uncheckAllButton;
	private UiStyle.StyledButton copyListButton;
	private UiStyle.StyledButton resetProgressButton;
	private UiStyle.StyledButton closeButton;
	private UiStyle.StyledButton openMainButton;

	private SortMode sortMode = SortMode.MOST_NEEDED;
	private boolean hideDone = false;
	private boolean thisLayerOnly = false;
	private int currentLayer = 0;
	private int scrollOffset = 0;
	private int maxScroll = 0;

	private String copyFeedback = null;
	private long copyFeedbackTime = 0;

	private final List<MaterialRow> filteredRows = new ArrayList<>();
	private int totalItemTypes = 0;
	private int readyItemTypes = 0;
	private long totalItemsCount = 0;
	private long readyItemsCount = 0;

	public MaterialsScreen(Screen parent) {
		super(Component.literal("Materials Checklist"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		loadActiveSchematic();

		int pad = 8;
		int headerH = (this.height < 140) ? 22 : 28;
		int footerH = (this.height < 140) ? 20 : 26;
		int buttonH = (this.height < 140) ? 16 : 20;

		// Toolbar row
		int toolY = headerH + 2;
		int searchW = Math.max(70, Math.min(130, this.width / 4));
		this.searchBox = new EditBox(this.font, pad, toolY, searchW, buttonH, Component.literal("Search"));
		this.searchBox.setHint(Component.literal("Search..."));
		this.searchBox.setResponder(s -> updateFilteredList());
		this.addRenderableWidget(this.searchBox);

		int curX = pad + searchW + 4;
		int sortW = Math.max(65, Math.min(90, (this.width - curX - pad) / 3));
		this.sortButton = new UiStyle.StyledButton(curX, toolY, sortW, buttonH, Component.literal("Sort: " + sortMode.getDisplayName()), b -> {
			sortMode = sortMode.next();
			b.setMessage(Component.literal("Sort: " + sortMode.getDisplayName()));
			updateFilteredList();
		});
		this.addRenderableWidget(this.sortButton);
		curX += sortW + 4;

		int toggleW = Math.max(55, Math.min(75, (this.width - curX - pad) / 2));
		this.hideDoneButton = new UiStyle.StyledButton(curX, toolY, toggleW, buttonH, Component.literal("Hide Done: " + (hideDone ? "ON" : "OFF")), b -> {
			hideDone = !hideDone;
			b.setMessage(Component.literal("Hide Done: " + (hideDone ? "ON" : "OFF")));
			updateFilteredList();
		});
		this.addRenderableWidget(this.hideDoneButton);
		curX += toggleW + 4;

		// Layer filter controls
		int maxLayers = schematicData != null ? schematicData.height() : 1;
		int layerBtnW = Math.max(75, Math.min(105, this.width - curX - pad - 36));
		this.layerFilterButton = new UiStyle.StyledButton(curX, toolY, layerBtnW, buttonH, getLayerButtonText(), b -> {
			thisLayerOnly = !thisLayerOnly;
			b.setMessage(getLayerButtonText());
			updateLayerNavButtons();
			updateFilteredList();
		});
		this.addRenderableWidget(this.layerFilterButton);
		curX += layerBtnW + 2;

		this.prevLayerButton = new UiStyle.StyledButton(curX, toolY, 16, buttonH, Component.literal("◀"), b -> {
			if (currentLayer > 0) {
				currentLayer--;
				if (layerFilterButton != null) layerFilterButton.setMessage(getLayerButtonText());
				updateFilteredList();
			}
		});
		this.addRenderableWidget(this.prevLayerButton);
		curX += 18;

		this.nextLayerButton = new UiStyle.StyledButton(curX, toolY, 16, buttonH, Component.literal("▶"), b -> {
			if (currentLayer < maxLayers - 1) {
				currentLayer++;
				if (layerFilterButton != null) layerFilterButton.setMessage(getLayerButtonText());
				updateFilteredList();
			}
		});
		this.addRenderableWidget(this.nextLayerButton);

		updateLayerNavButtons();

		// Bottom Action Bar: 5 buttons distributed evenly across this.width - (pad * 2)
		// with running currentX accumulator ([Check All], [Uncheck All], [Copy List], [Reset], [Close])
		int footerY = this.height - footerH + 2;
		int btnCount = 5;
		int spacing = 4;
		int totalFooterW = Math.max(btnCount * 10, this.width - (pad * 2));
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

		this.checkAllButton = new UiStyle.StyledButton(fBtnX[0], footerY, fBtnW[0], buttonH, Component.literal("Check All"), b -> {
			List<String> ids = filteredRows.stream().map(MaterialRow::itemId).toList();
			MaterialProgressManager.checkAll(schematicKey, ids);
			updateFilteredList();
		});
		this.addRenderableWidget(this.checkAllButton);

		this.uncheckAllButton = new UiStyle.StyledButton(fBtnX[1], footerY, fBtnW[1], buttonH, Component.literal("Uncheck All"), b -> {
			MaterialProgressManager.uncheckAll(schematicKey);
			updateFilteredList();
		});
		this.addRenderableWidget(this.uncheckAllButton);

		this.copyListButton = new UiStyle.StyledButton(fBtnX[2], footerY, fBtnW[2], buttonH, Component.literal("Copy List"), b -> {
			copyListToClipboard();
		});
		this.addRenderableWidget(this.copyListButton);

		this.resetProgressButton = new UiStyle.StyledButton(fBtnX[3], footerY, fBtnW[3], buttonH, Component.literal("Reset"), b -> {
			MaterialProgressManager.resetProgress(schematicKey);
			updateFilteredList();
		}, UiStyle.StyledButton.ButtonStyle.DANGER);
		this.addRenderableWidget(this.resetProgressButton);

		this.closeButton = new UiStyle.StyledButton(fBtnX[4], footerY, fBtnW[4], buttonH, Component.literal("Close"), b -> onClose());
		this.addRenderableWidget(this.closeButton);

		// "Open Main Screen" button for empty state
		this.openMainButton = new UiStyle.StyledButton(
			this.width / 2 - 60, this.height / 2 + 15, 120, 20,
			Component.literal("Pick a Schematic"),
			b -> {
				if (this.minecraft != null) {
					this.minecraft.gui.setScreen(new ShhhBuilderScreen());
				}
			},
			UiStyle.StyledButton.ButtonStyle.PRIMARY
		);
		this.openMainButton.visible = (schematicData == null);
		this.addRenderableWidget(this.openMainButton);

		updateFilteredList();
	}

	private Component getLayerButtonText() {
		if (!thisLayerOnly) {
			return Component.literal("All Layers");
		}
		int maxL = schematicData != null ? schematicData.height() : 1;
		return Component.literal("Layer " + (currentLayer + 1) + "/" + maxL);
	}

	private void updateLayerNavButtons() {
		if (prevLayerButton != null) prevLayerButton.visible = thisLayerOnly;
		if (nextLayerButton != null) nextLayerButton.visible = thisLayerOnly;
	}

	private void loadActiveSchematic() {
		this.schematicFileName = SchematicManager.getSelectedFileName();
		if (schematicFileName != null && !schematicFileName.isBlank()) {
			Path file = SchematicManager.getSchematicsDirectory().resolve(schematicFileName);
			if (Files.exists(file)) {
				try {
					long size = Files.size(file);
					long modified = Files.getLastModifiedTime(file).toMillis();
					this.schematicKey = MaterialProgressManager.buildKey(schematicFileName, size, modified);
				} catch (Throwable ignored) {
					this.schematicKey = schematicFileName;
				}
				SchematicManager.CachedSchematic cached = SchematicManager.getCached(file);
				if (cached != null) {
					this.schematicData = cached.data;
				}
			} else {
				this.schematicKey = schematicFileName;
			}
			if (this.schematicData == null) {
				this.schematicData = SchematicManager.getLoadedData();
			}
		} else {
			this.schematicData = null;
			this.schematicKey = "none";
		}

		// Sync layer with placement if placed
		if (PlacementManager.hasPlacement()) {
			this.currentLayer = Math.max(0, PlacementManager.getCurrentLayer() - 1);
		}
	}

	private void updateFilteredList() {
		filteredRows.clear();
		totalItemTypes = 0;
		readyItemTypes = 0;
		totalItemsCount = 0;
		readyItemsCount = 0;

		if (schematicData == null) {
			return;
		}

		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
		boolean autoCheck = config.isAutoCheckFromInventory();
		boolean includeShulkers = config.isIncludeShulkerBoxes();

		// Live player inventory scan
		Map<String, Integer> inventoryCounts = Collections.emptyMap();
		if (this.minecraft != null && this.minecraft.player != null) {
			inventoryCounts = PlayerInventoryScanner.scanInventory(this.minecraft, includeShulkers, false);
		}

		// In-world placed blocks scan (throttled 250ms)
		Map<String, Integer> placedCounts = Collections.emptyMap();
		if (this.minecraft != null && this.minecraft.level != null && PlacementManager.hasPlacement()) {
			int layerFilter = thisLayerOnly ? currentLayer : -1;
			placedCounts = PlayerInventoryScanner.scanPlacedBlocks(this.minecraft, layerFilter, false);
		}

		// Exact material counts from MaterialCountingEngine
		Map<String, Long> requiredCounts = thisLayerOnly
			? schematicData.getMaterialCountsForLayer(currentLayer)
			: schematicData.itemCounts();

		String query = (searchBox != null) ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";

		for (Map.Entry<String, Long> entry : requiredCounts.entrySet()) {
			String itemId = entry.getKey();
			long need = entry.getValue();
			if (need <= 0) continue;

			int invHave = inventoryCounts.getOrDefault(itemId, 0);
			int placedHave = placedCounts.getOrDefault(itemId, 0);
			int have = invHave + placedHave; // effectiveHave = inventoryHave + placedInWorld
			boolean isChecked = MaterialProgressManager.isChecked(schematicKey, itemId);
			boolean isDone = isChecked || (autoCheck && have >= need);
			long missing = isDone ? 0 : Math.max(0, need - have);

			totalItemTypes++;
			totalItemsCount += need;
			if (isDone) {
				readyItemTypes++;
				readyItemsCount += need;
			} else {
				readyItemsCount += Math.min(have, need);
			}

			// Format display item & name
			ItemStack stack = resolveItemStack(itemId);
			String displayName = stack.getHoverName().getString();

			if (!query.isEmpty() && !displayName.toLowerCase(Locale.ROOT).contains(query) && !itemId.toLowerCase(Locale.ROOT).contains(query)) {
				continue;
			}

			if (hideDone && isDone) {
				continue;
			}

			boolean isCreative = MaterialCountingEngine.isCreativeOnly(itemId);
			filteredRows.add(new MaterialRow(itemId, stack, displayName, need, have, missing, isChecked, isDone, isCreative));
		}

		// Sorting
		Comparator<MaterialRow> comp = switch (sortMode) {
			case ALPHABETICAL -> Comparator.comparing(r -> r.displayName().toLowerCase(Locale.ROOT));
			case NOT_DONE_FIRST -> (r1, r2) -> {
				if (r1.isDone() != r2.isDone()) {
					return r1.isDone() ? 1 : -1;
				}
				return Long.compare(r2.missing(), r1.missing());
			};
			case MOST_NEEDED -> (r1, r2) -> Long.compare(r2.need(), r1.need());
		};
		filteredRows.sort(comp);

		// Adjust scroll bounds
		int rowH = 22;
		int listTop = (this.height < 140) ? 46 : 56;
		int listBottom = this.height - ((this.height < 140) ? 22 : 28);
		int visibleH = Math.max(rowH, listBottom - listTop);
		int totalContentH = filteredRows.size() * rowH;
		maxScroll = Math.max(0, totalContentH - visibleH);
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
	}

	private ItemStack resolveItemStack(String itemId) {
		return io.github.rafayshhh.shhhbuilder.client.hud.MaterialDisplayHelper.getItemStack(itemId);
	}

	private void copyListToClipboard() {
		if (filteredRows.isEmpty()) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Shhh Builder - Materials Checklist (").append(schematicFileName != null ? schematicFileName : "Build").append(")\n");
		if (thisLayerOnly) {
			sb.append("Filter: Layer ").append(currentLayer + 1).append("\n");
		}
		sb.append("--------------------------------------------------\n");

		for (MaterialRow row : filteredRows) {
			sb.append(row.displayName())
				.append(" x").append(String.format(Locale.ROOT, "%,d", row.need()))
				.append(" (").append(UiStyle.formatStacks(row.need())).append(")");
			if (row.isDone()) {
				sb.append(" [DONE]");
			} else if (row.have() > 0) {
				sb.append(" [Have: ").append(String.format(Locale.ROOT, "%,d", row.have())).append("]");
			}
			sb.append("\n");
		}

		if (this.minecraft != null && this.minecraft.keyboardHandler != null) {
			this.minecraft.keyboardHandler.setClipboard(sb.toString());
			this.copyFeedback = "List copied to clipboard!";
			this.copyFeedbackTime = System.currentTimeMillis();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount != 0 && maxScroll > 0) {
			scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 22)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isFocused) {
		if (event.button() == 0) {
			int listTop = (this.height < 140) ? 46 : 56;
			int listBottom = this.height - ((this.height < 140) ? 22 : 28);
			int rowH = 22;

			double mouseX = event.x();
			double mouseY = event.y();

			if (mouseY >= listTop && mouseY <= listBottom && mouseX >= 8 && mouseX <= this.width - 8) {
				int clickedIndex = (int) ((mouseY - listTop + scrollOffset) / rowH);
				if (clickedIndex >= 0 && clickedIndex < filteredRows.size()) {
					MaterialRow row = filteredRows.get(clickedIndex);
					MaterialProgressManager.toggleItem(schematicKey, row.itemId());
					updateFilteredList();
					return true;
				}
			}
		}
		return super.mouseClicked(event, isFocused);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (this.searchBox != null && this.searchBox.isFocused()) {
			if (event.key() == 256) {
				this.searchBox.setFocused(false);
				return true;
			}
			return this.searchBox.keyPressed(event);
		}
		if (event.key() == 256) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(this.parent);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		int pad = 8;
		int headerH = (this.height < 140) ? 22 : 28;
		int footerH = (this.height < 140) ? 20 : 26;
		int footerY = this.height - footerH;
		int listTop = (this.height < 140) ? 46 : 56;
		int listBottom = this.height - ((this.height < 140) ? 22 : 28);
		int rowH = 22;

		// a) Screen backdrop
		extractor.fill(0, 0, this.width, this.height, UiStyle.BG_SCREEN_OVERLAY);

		// b) Header Bar background fill
		extractor.fill(0, 0, this.width, headerH, UiStyle.BG_PANEL);
		extractor.fill(0, headerH - 1, this.width, headerH, UiStyle.BORDER_NORMAL);

		// Footer Bar background fill
		extractor.fill(0, footerY, this.width, this.height, UiStyle.BG_PANEL);
		extractor.fill(0, footerY, this.width, footerY + 1, UiStyle.BORDER_NORMAL);

		// Header text and Progress Bar
		String title = (schematicFileName != null) ? schematicFileName : "Materials Checklist";
		extractor.text(this.font, title, pad, 6, UiStyle.TEXT_TITLE);

		if (totalItemTypes > 0 && this.width >= 240) {
			float pct = (totalItemsCount > 0) ? (float) readyItemsCount / (float) totalItemsCount : 0.0f;
			int barW = Math.max(60, Math.min(140, this.width / 4));
			int barX = this.width - pad - barW;
			int barY = 6;
			int barH = 10;
			UiStyle.drawProgressBar(extractor, barX, barY, barW, barH, pct, UiStyle.COLOR_ACCENT);

			String progText = readyItemTypes + "/" + totalItemTypes + " ready (" + Math.round(pct * 100) + "%)";
			extractor.text(this.font, progText, barX - 6 - this.font.width(progText), 7, UiStyle.TEXT_HEADER);
		}

		// Temporary copy feedback message above footer
		if (copyFeedback != null) {
			long elapsed = System.currentTimeMillis() - copyFeedbackTime;
			if (elapsed < 2500) {
				extractor.centeredText(this.font, copyFeedback, this.width / 2, footerY - 14, UiStyle.COLOR_SUCCESS);
			} else {
				copyFeedback = null;
			}
		}

		MaterialRow hoveredRow = null;

		// Empty or No Schematic State
		if (schematicData == null) {
			UiStyle.drawPanel(extractor, pad, listTop + 10, this.width - (pad * 2), 70, UiStyle.BG_CARD, UiStyle.BORDER_WARNING);
			extractor.centeredText(this.font, "No schematic selected", this.width / 2, listTop + 22, UiStyle.COLOR_WARNING);
			extractor.centeredText(this.font, "Open the Shhh Builder browser to pick a build.", this.width / 2, listTop + 36, UiStyle.TEXT_BODY);
			if (openMainButton != null) openMainButton.visible = true;
		} else {
			if (openMainButton != null) openMainButton.visible = false;

			// Materials List Container Background
			extractor.fill(pad, listTop, this.width - pad, listBottom, UiStyle.BG_PANEL);
			extractor.fill(pad, listTop, this.width - pad, listTop + 1, UiStyle.BORDER_NORMAL);
			extractor.fill(pad, listBottom - 1, this.width - pad, listBottom, UiStyle.BORDER_NORMAL);

			if (filteredRows.isEmpty()) {
				String emptyMsg = (searchBox != null && !searchBox.getValue().isEmpty())
					? "No materials match your search."
					: (hideDone ? "All required materials are marked done!" : "No materials needed for this selection.");
				extractor.centeredText(this.font, emptyMsg, this.width / 2, listTop + 30, UiStyle.TEXT_BODY);
			} else {
				// c) Custom row text and icons
				int startIdx = Math.max(0, scrollOffset / rowH);
				int maxVisibleRows = ((listBottom - listTop) / rowH) + 2;
				int endIdx = Math.min(filteredRows.size(), startIdx + maxVisibleRows);

				for (int i = startIdx; i < endIdx; i++) {
					MaterialRow row = filteredRows.get(i);
					int rowY = listTop + (i * rowH) - scrollOffset;
					if (rowY + rowH <= listTop || rowY >= listBottom) continue;

					boolean isHovered = (mouseY >= rowY && mouseY < rowY + rowH && mouseX >= pad && mouseX <= this.width - pad);
					if (isHovered) {
						hoveredRow = row;
					}

					// Row surface
					int rowBg = row.isDone() ? 0x40141619 : (isHovered ? UiStyle.BG_CARD_HOVER : UiStyle.BG_CARD);
					extractor.fill(pad + 1, rowY + 1, this.width - pad - 1, rowY + rowH - 1, rowBg);

					// Checkbox
					int checkX = pad + 4;
					int checkY = rowY + 4;
					int checkSize = 14;
					extractor.fill(checkX, checkY, checkX + checkSize, checkY + checkSize, UiStyle.BG_PANEL);
					extractor.fill(checkX, checkY, checkX + checkSize, checkY + 1, row.isDone() ? UiStyle.COLOR_SUCCESS : UiStyle.BORDER_LIGHT);
					extractor.fill(checkX, checkY + checkSize - 1, checkX + checkSize, checkY + checkSize, row.isDone() ? UiStyle.COLOR_SUCCESS : UiStyle.BORDER_LIGHT);
					extractor.fill(checkX, checkY, checkX + 1, checkY + checkSize, row.isDone() ? UiStyle.COLOR_SUCCESS : UiStyle.BORDER_LIGHT);
					extractor.fill(checkX + checkSize - 1, checkY, checkX + checkSize, checkY + checkSize, row.isDone() ? UiStyle.COLOR_SUCCESS : UiStyle.BORDER_LIGHT);

					if (row.isDone()) {
						extractor.centeredText(this.font, "✓", checkX + (checkSize / 2), checkY + 3, UiStyle.COLOR_SUCCESS);
					}

					// Item icon
					extractor.fakeItem(row.stack(), checkX + checkSize + 4, rowY + 3);

					// Item Name & Creative Badge
					int textX = checkX + checkSize + 4 + 18;
					int textY = rowY + 6;
					int textColor = row.isDone() ? UiStyle.TEXT_BODY : UiStyle.TEXT_TITLE;
					extractor.text(this.font, row.displayName(), textX, textY, textColor);

					if (row.isCreativeOnly()) {
						int nameW = this.font.width(row.displayName());
						UiStyle.drawBadge(extractor, this.font, "Creative only", textX + nameW + 6, textY - 1, UiStyle.COLOR_WARNING, UiStyle.BG_CHIP, UiStyle.BORDER_WARNING);
					}

					// Need, Have breakdown
					int rightX = this.width - pad - 8;
					String needStr = "Need: " + String.format(Locale.ROOT, "%,d", row.need());
					String haveStr = "Have: " + String.format(Locale.ROOT, "%,d", row.have());

					if (this.width >= 480) {
						extractor.text(this.font, haveStr, rightX - 90 - this.font.width(haveStr), textY, row.have() >= row.need() ? UiStyle.COLOR_SUCCESS : UiStyle.TEXT_BODY);
					}
					extractor.text(this.font, needStr, rightX - this.font.width(needStr), textY, row.isDone() ? UiStyle.TEXT_BODY : UiStyle.COLOR_ACCENT);
				}

				// Scrollbar
				if (maxScroll > 0) {
					int scrollTrackH = listBottom - listTop;
					int scrollThumbH = Math.max(16, (scrollTrackH * (listBottom - listTop)) / (filteredRows.size() * rowH));
					int scrollThumbY = listTop + ((scrollOffset * (scrollTrackH - scrollThumbH)) / maxScroll);
					extractor.fill(this.width - pad - 3, listTop, this.width - pad - 1, listBottom, UiStyle.BG_PANEL);
					extractor.fill(this.width - pad - 3, scrollThumbY, this.width - pad - 1, scrollThumbY + scrollThumbH, UiStyle.BORDER_ACCENT);
				}
			}
		}

		// d) super.extractRenderState so all StyledButtons ([Check All], [Uncheck All], [Copy List], [Reset], [Close], sort, filter, nav) render ON TOP of background fills
		super.extractRenderState(extractor, mouseX, mouseY, delta);

		// e) Final tooltip pass on top of everything
		if (hoveredRow != null) {
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.literal(hoveredRow.displayName()).withColor(UiStyle.COLOR_ACCENT));
			tooltip.add(Component.literal("Need: " + UiStyle.formatNeedWithShulkers(hoveredRow.need())));
			tooltip.add(Component.literal("Have: " + String.format(Locale.ROOT, "%,d", hoveredRow.have())));
			if (!hoveredRow.isDone()) {
				tooltip.add(Component.literal("Missing: " + UiStyle.formatNeedWithShulkers(hoveredRow.missing())).withColor(UiStyle.COLOR_WARNING));
			} else {
				tooltip.add(Component.literal("Status: Ready / Done").withColor(UiStyle.COLOR_SUCCESS));
			}
			if (hoveredRow.isCreativeOnly()) {
				tooltip.add(Component.literal("⚠ Creative-only item (cannot obtain in Survival)").withColor(UiStyle.COLOR_WARNING));
			}
			extractor.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
		}
	}
}
