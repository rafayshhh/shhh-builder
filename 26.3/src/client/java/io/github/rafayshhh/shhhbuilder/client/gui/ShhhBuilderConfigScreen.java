package io.github.rafayshhh.shhhbuilder.client.gui;

import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Professional settings screen for Shhh Builder.
 * Grouped into "Preview", "Performance", and "HUD" sections with sliders, cycle buttons,
 * hover descriptions, and "Reset to defaults".
 */
public class ShhhBuilderConfigScreen extends Screen {
	private static final Component TITLE = Component.translatable("screen.shhh-builder.config.title");

	public enum Section {
		PREVIEW("Preview"),
		PERFORMANCE("Performance"),
		HUD("HUD");

		private final String displayName;

		Section(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	private final Screen parent;
	private Section currentSection = Section.PREVIEW;

	// Section Tab Buttons
	private UiStyle.StyledButton tabPreviewButton;
	private UiStyle.StyledButton tabPerformanceButton;
	private UiStyle.StyledButton tabHudButton;

	// Preview Section Widgets
	private UiStyle.StyledButton previewStyleButton;
	private OpacitySlider opacitySlider;
	private UiStyle.StyledButton bordersButton;
	private UiStyle.StyledButton borderColorButton;
	private UiStyle.StyledButton borderOpacityButton;

	// Performance Section Widgets
	private UiStyle.StyledButton qualityPresetButton;
	private UiStyle.StyledButton texturedDistanceButton;
	private UiStyle.StyledButton texturedBlockLimitButton;
	private UiStyle.StyledButton ghostLimitButton;
	private UiStyle.StyledButton ghostDistanceButton;

	// HUD Section Widgets
	private UiStyle.StyledButton positionButton;
	private UiStyle.StyledButton scaleButton;
	private UiStyle.StyledButton hudShowModeButton;
	private UiStyle.StyledButton keyHintsButton;
	private UiStyle.StyledButton autoCheckButton;
	private UiStyle.StyledButton includeShulkersButton;

	// Bottom Action Buttons
	private UiStyle.StyledButton resetDefaultsButton;
	private UiStyle.StyledButton doneButton;

	private final List<net.minecraft.client.gui.components.AbstractWidget> allSettingButtons = new ArrayList<>();

	public ShhhBuilderConfigScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
	}

	public static class OpacitySlider extends AbstractSliderButton {
		private final ShhhBuilderConfig config;

		public OpacitySlider(int x, int y, int width, int height, ShhhBuilderConfig config) {
			super(x, y, width, height, Component.empty(), (config.getPreviewOpacity() - 10) / 90.0);
			this.config = config;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int percent = 10 + (int) Math.round(this.value * 18.0) * 5;
			this.setMessage(Component.literal("Preview Opacity: " + percent + "%"));
		}

		@Override
		protected void applyValue() {
			int percent = 10 + (int) Math.round(this.value * 18.0) * 5;
			config.setPreviewOpacity(percent);
			config.save();
		}

		@Override
		public void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
			boolean hovered = this.isHoveredOrFocused();
			int bg = hovered ? UiStyle.BG_CARD_HOVER : UiStyle.BG_CARD;
			int border = hovered ? UiStyle.BORDER_ACCENT : UiStyle.BORDER_NORMAL;

			// Base track
			UiStyle.drawPanel(extractor, this.getX(), this.getY(), this.width, this.height, bg, border);

			// Progress fill
			int fillW = (int) Math.round((this.width - 2) * this.value);
			if (fillW > 0) {
				extractor.fill(this.getX() + 1, this.getY() + 1, this.getX() + 1 + fillW, this.getY() + this.height - 1, 0xFF007799);
			}

			// Thumb handle
			int thumbX = this.getX() + 1 + Math.max(0, Math.min(this.width - 8, (int) Math.round((this.width - 8) * this.value)));
			UiStyle.drawPanel(extractor, thumbX, this.getY() + 1, 6, this.height - 2, UiStyle.COLOR_ACCENT, 0xFFFFFFFF);

			// Text label
			Font font = Minecraft.getInstance().font;
			if (font != null) {
				int textY = this.getY() + (this.height - font.lineHeight) / 2 + 1;
				extractor.centeredText(font, this.getMessage(), this.getX() + this.width / 2, textY, UiStyle.TEXT_TITLE);
			}
		}
	}

	@Override
	protected void init() {
		super.init();

		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
		int centerX = this.width / 2;
		this.allSettingButtons.clear();

		int buttonHeight = (this.height < 140) ? 14 : ((this.height < 220) ? 15 : 19);
		int stride = (this.height < 140) ? 16 : ((this.height < 220) ? 16 : 22);

		int panelWidth = Math.min(340, Math.max(150, this.width - 24));
		int panelLeft = centerX - (panelWidth / 2);

		// Section Tabs at top
		int tabWidth = (panelWidth - 6) / 3;
		int titleY = (this.height < 140) ? 3 : Math.max(2, (this.height < 220 ? 4 : 8));
		int tabY = (this.height < 140) ? 15 : (titleY + 13);

		this.tabPreviewButton = new UiStyle.StyledButton(
			panelLeft, tabY, tabWidth, buttonHeight,
			Component.literal(Section.PREVIEW.getDisplayName()),
			b -> switchSection(Section.PREVIEW),
			(currentSection == Section.PREVIEW) ? UiStyle.StyledButton.ButtonStyle.PRIMARY : UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.addRenderableWidget(this.tabPreviewButton);

		this.tabPerformanceButton = new UiStyle.StyledButton(
			panelLeft + tabWidth + 3, tabY, tabWidth, buttonHeight,
			Component.literal(Section.PERFORMANCE.getDisplayName()),
			b -> switchSection(Section.PERFORMANCE),
			(currentSection == Section.PERFORMANCE) ? UiStyle.StyledButton.ButtonStyle.PRIMARY : UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.addRenderableWidget(this.tabPerformanceButton);

		this.tabHudButton = new UiStyle.StyledButton(
			panelLeft + (tabWidth + 3) * 2, tabY, panelWidth - (tabWidth + 3) * 2, buttonHeight,
			Component.literal(Section.HUD.getDisplayName()),
			b -> switchSection(Section.HUD),
			(currentSection == Section.HUD) ? UiStyle.StyledButton.ButtonStyle.PRIMARY : UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.addRenderableWidget(this.tabHudButton);

		// Content start Y
		int contentY = tabY + buttonHeight + 3;
		int colGap = 6;
		int colWidth = (panelWidth - colGap) / 2;
		int colLeft = panelLeft;
		int colRight = panelLeft + colWidth + colGap;

		// --- Section 1: Preview ---
		this.previewStyleButton = new UiStyle.StyledButton(colLeft, contentY, colWidth, buttonHeight, getPreviewStyleText(config), b -> {
			config.cyclePreviewStyle();
			GhostBlockRenderer.resetCircuitBreaker();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.previewStyleButton);
		this.allSettingButtons.add(this.previewStyleButton);

		this.opacitySlider = new OpacitySlider(colRight, contentY, colWidth, buttonHeight, config);
		this.addRenderableWidget(this.opacitySlider);
		this.allSettingButtons.add(this.opacitySlider);

		this.bordersButton = new UiStyle.StyledButton(colLeft, contentY + stride, colWidth, buttonHeight, getBordersText(config), b -> {
			config.toggleTexturedBorders();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.bordersButton);
		this.allSettingButtons.add(this.bordersButton);

		this.borderColorButton = new UiStyle.StyledButton(colRight, contentY + stride, colWidth, buttonHeight, getBorderColorText(config), b -> {
			config.cycleBorderColor();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.borderColorButton);
		this.allSettingButtons.add(this.borderColorButton);

		this.borderOpacityButton = new UiStyle.StyledButton(colLeft, contentY + (stride * 2), panelWidth, buttonHeight, getBorderOpacityText(config), b -> {
			config.cycleBorderOpacity();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.borderOpacityButton);
		this.allSettingButtons.add(this.borderOpacityButton);

		// --- Section 2: Performance ---
		this.qualityPresetButton = new UiStyle.StyledButton(colLeft, contentY, colWidth, buttonHeight, getQualityPresetText(config), b -> {
			config.cycleQualityPreset();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.qualityPresetButton);
		this.allSettingButtons.add(this.qualityPresetButton);

		this.texturedDistanceButton = new UiStyle.StyledButton(colRight, contentY, colWidth, buttonHeight, getTexturedDistanceText(config), b -> {
			config.cycleTexturedDistance();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.texturedDistanceButton);
		this.allSettingButtons.add(this.texturedDistanceButton);

		this.texturedBlockLimitButton = new UiStyle.StyledButton(colLeft, contentY + stride, colWidth, buttonHeight, getTexturedBlockLimitText(config), b -> {
			config.cycleTexturedBlockLimit();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.texturedBlockLimitButton);
		this.allSettingButtons.add(this.texturedBlockLimitButton);

		this.ghostLimitButton = new UiStyle.StyledButton(colRight, contentY + stride, colWidth, buttonHeight, getGhostBlockLimitText(config), b -> {
			config.cycleGhostBlockLimit();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.ghostLimitButton);
		this.allSettingButtons.add(this.ghostLimitButton);

		this.ghostDistanceButton = new UiStyle.StyledButton(colLeft, contentY + (stride * 2), panelWidth, buttonHeight, getGhostDistanceText(config), b -> {
			config.cycleGhostRenderDistance();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.ghostDistanceButton);
		this.allSettingButtons.add(this.ghostDistanceButton);

		// --- Section 3: HUD & Checklist ---
		this.positionButton = new UiStyle.StyledButton(colLeft, contentY, colWidth, buttonHeight, getPositionText(config), b -> {
			config.setHudPosition(config.getHudPosition().next());
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.positionButton);
		this.allSettingButtons.add(this.positionButton);

		this.scaleButton = new UiStyle.StyledButton(colRight, contentY, colWidth, buttonHeight, getScaleText(config), b -> {
			config.setHudScale(config.getHudScale().next());
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.scaleButton);
		this.allSettingButtons.add(this.scaleButton);

		this.hudShowModeButton = new UiStyle.StyledButton(colLeft, contentY + stride, colWidth, buttonHeight, getHudShowModeText(config), b -> {
			config.cycleHudShowMode();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.hudShowModeButton);
		this.allSettingButtons.add(this.hudShowModeButton);

		this.keyHintsButton = new UiStyle.StyledButton(colRight, contentY + stride, colWidth, buttonHeight, getKeyHintsText(config), b -> {
			config.toggleShowKeyHints();
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.keyHintsButton);
		this.allSettingButtons.add(this.keyHintsButton);

		this.autoCheckButton = new UiStyle.StyledButton(colLeft, contentY + (stride * 2), colWidth, buttonHeight, getAutoCheckText(config), b -> {
			config.setAutoCheckFromInventory(!config.isAutoCheckFromInventory());
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.autoCheckButton);
		this.allSettingButtons.add(this.autoCheckButton);

		this.includeShulkersButton = new UiStyle.StyledButton(colRight, contentY + (stride * 2), colWidth, buttonHeight, getIncludeShulkersText(config), b -> {
			config.setIncludeShulkerBoxes(!config.isIncludeShulkerBoxes());
			config.save();
			updateButtonLabels();
		});
		this.addRenderableWidget(this.includeShulkersButton);
		this.allSettingButtons.add(this.includeShulkersButton);

		// Bottom Actions: Reset to Defaults & Done
		int bottomY = (this.height < 140) ? (contentY + (stride * 3) + 2) : (this.height - buttonHeight - 8);
		int bottomWidth = panelWidth;
		int btnW = (bottomWidth - 6) / 2;

		this.resetDefaultsButton = new UiStyle.StyledButton(
			panelLeft, bottomY, btnW, buttonHeight,
			Component.literal("Reset Defaults"),
			b -> resetToDefaults(),
			UiStyle.StyledButton.ButtonStyle.DANGER
		);
		this.addRenderableWidget(this.resetDefaultsButton);

		this.doneButton = new UiStyle.StyledButton(
			panelLeft + btnW + 6, bottomY, bottomWidth - btnW - 6, buttonHeight,
			Component.literal("Done"),
			b -> this.onClose(),
			UiStyle.StyledButton.ButtonStyle.PRIMARY
		);
		this.addRenderableWidget(this.doneButton);

		updateVisibility();
	}

	private void switchSection(Section section) {
		this.currentSection = section;
		this.rebuildWidgets();
	}

	private void updateVisibility() {
		boolean isPreview = (currentSection == Section.PREVIEW);
		boolean isPerf = (currentSection == Section.PERFORMANCE);
		boolean isHud = (currentSection == Section.HUD);

		if (previewStyleButton != null) previewStyleButton.visible = isPreview;
		if (opacitySlider != null) opacitySlider.visible = isPreview;
		if (bordersButton != null) bordersButton.visible = isPreview;
		if (borderColorButton != null) borderColorButton.visible = isPreview;
		if (borderOpacityButton != null) borderOpacityButton.visible = isPreview;

		if (qualityPresetButton != null) qualityPresetButton.visible = isPerf;
		if (texturedDistanceButton != null) texturedDistanceButton.visible = isPerf;
		if (texturedBlockLimitButton != null) texturedBlockLimitButton.visible = isPerf;
		if (ghostLimitButton != null) ghostLimitButton.visible = isPerf;
		if (ghostDistanceButton != null) ghostDistanceButton.visible = isPerf;

		if (positionButton != null) positionButton.visible = isHud;
		if (scaleButton != null) scaleButton.visible = isHud;
		if (hudShowModeButton != null) hudShowModeButton.visible = isHud;
		if (keyHintsButton != null) keyHintsButton.visible = isHud;
		if (autoCheckButton != null) autoCheckButton.visible = isHud;
		if (includeShulkersButton != null) includeShulkersButton.visible = isHud;
	}

	private void resetToDefaults() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
		config.setHudEnabled(true);
		config.setHudPosition(ShhhBuilderConfig.HudPosition.TOP_RIGHT);
		config.setHudScale(ShhhBuilderConfig.HudScale.NORMAL);
		config.setShowKeyHints(true);
		config.setHudShowMode(ShhhBuilderConfig.HudShowMode.MISSING_ONLY);
		config.setAutoCheckFromInventory(true);
		config.setIncludeShulkerBoxes(true);
		config.setPreviewStyle(ShhhBuilderConfig.PreviewStyle.TEXTURED);
		config.setPreviewOpacity(45);
		config.setTexturedBorders(true);
		config.setBorderColor(ShhhBuilderConfig.BorderColor.BLOCK_COLOR);
		config.setBorderOpacity(100);
		config.applyQualityPreset(ShhhBuilderConfig.QualityPreset.BALANCED);
		GhostBlockRenderer.resetCircuitBreaker();
		config.save();
		this.rebuildWidgets();
	}

	private void updateButtonLabels() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
		if (this.positionButton != null) this.positionButton.setMessage(getPositionText(config));
		if (this.scaleButton != null) this.scaleButton.setMessage(getScaleText(config));
		if (this.hudShowModeButton != null) this.hudShowModeButton.setMessage(getHudShowModeText(config));
		if (this.keyHintsButton != null) this.keyHintsButton.setMessage(getKeyHintsText(config));
		if (this.autoCheckButton != null) this.autoCheckButton.setMessage(getAutoCheckText(config));
		if (this.includeShulkersButton != null) this.includeShulkersButton.setMessage(getIncludeShulkersText(config));
		if (this.qualityPresetButton != null) this.qualityPresetButton.setMessage(getQualityPresetText(config));
		if (this.texturedDistanceButton != null) this.texturedDistanceButton.setMessage(getTexturedDistanceText(config));
		if (this.texturedBlockLimitButton != null) this.texturedBlockLimitButton.setMessage(getTexturedBlockLimitText(config));
		if (this.ghostLimitButton != null) this.ghostLimitButton.setMessage(getGhostBlockLimitText(config));

		if (this.previewStyleButton != null) this.previewStyleButton.setMessage(getPreviewStyleText(config));
		if (this.bordersButton != null) this.bordersButton.setMessage(getBordersText(config));
		if (this.borderColorButton != null) this.borderColorButton.setMessage(getBorderColorText(config));
		if (this.borderOpacityButton != null) this.borderOpacityButton.setMessage(getBorderOpacityText(config));
		if (this.ghostDistanceButton != null) this.ghostDistanceButton.setMessage(getGhostDistanceText(config));
	}

	private Component getPositionText(ShhhBuilderConfig config) {
		return Component.literal("HUD Position: " + config.getHudPosition().getDisplayName());
	}

	private Component getScaleText(ShhhBuilderConfig config) {
		return Component.literal("HUD Scale: " + config.getHudScale().getDisplayName());
	}

	private Component getHudShowModeText(ShhhBuilderConfig config) {
		return Component.literal("HUD Items: " + config.getHudShowMode().getDisplayName());
	}

	private Component getKeyHintsText(ShhhBuilderConfig config) {
		return Component.literal("Key Hints: " + (config.isShowKeyHints() ? "ON" : "OFF"));
	}

	private Component getAutoCheckText(ShhhBuilderConfig config) {
		return Component.literal("Auto-Check Inventory: " + (config.isAutoCheckFromInventory() ? "ON" : "OFF"));
	}

	private Component getIncludeShulkersText(ShhhBuilderConfig config) {
		return Component.literal("Include Shulkers: " + (config.isIncludeShulkerBoxes() ? "ON" : "OFF"));
	}

	private Component getQualityPresetText(ShhhBuilderConfig config) {
		return Component.literal("Quality Preset: " + config.getQualityPreset().getDisplayName());
	}

	private Component getTexturedDistanceText(ShhhBuilderConfig config) {
		return Component.literal("Textured Distance: " + config.getTexturedDistance() + "m");
	}

	private Component getTexturedBlockLimitText(ShhhBuilderConfig config) {
		return Component.literal("Textured Limit: " + String.format(java.util.Locale.ROOT, "%,d", config.getTexturedBlockLimit()));
	}

	private Component getGhostBlockLimitText(ShhhBuilderConfig config) {
		return Component.literal("Ghost Limit: " + String.format(java.util.Locale.ROOT, "%,d", config.getGhostBlockLimit()));
	}

	private Component getPreviewStyleText(ShhhBuilderConfig config) {
		return Component.literal("Preview Style: " + config.getPreviewStyle().getDisplayName());
	}

	private Component getBordersText(ShhhBuilderConfig config) {
		return Component.literal("Block Borders: " + (config.isTexturedBorders() ? "ON" : "OFF"));
	}

	private Component getBorderColorText(ShhhBuilderConfig config) {
		return Component.literal("Border Color: " + config.getBorderColor().getDisplayName());
	}

	private Component getBorderOpacityText(ShhhBuilderConfig config) {
		return Component.literal("Border Opacity: " + config.getBorderOpacity() + "%");
	}

	private Component getGhostDistanceText(ShhhBuilderConfig config) {
		return Component.literal("Preview Distance: " + config.getGhostRenderDistance() + "m");
	}

	private String getHoverDescription() {
		if (previewStyleButton != null && previewStyleButton.isHoveredOrFocused()) {
			return "Preview Style: Choose between see-through textured blocks or wireframe outlines.";
		}
		if (opacitySlider != null && opacitySlider.isHoveredOrFocused()) {
			return "Preview Opacity: Adjust how see-through the textured blocks are in the world.";
		}
		if (bordersButton != null && bordersButton.isHoveredOrFocused()) {
			return "Block Borders: Draw thin outlines around textured blocks to tell them apart.";
		}
		if (borderColorButton != null && borderColorButton.isHoveredOrFocused()) {
			return "Border Color: Choose matching block color, white, black, or cyan borders.";
		}
		if (borderOpacityButton != null && borderOpacityButton.isHoveredOrFocused()) {
			return "Border Opacity: Adjust the transparency of the 3D block borders.";
		}
		if (qualityPresetButton != null && qualityPresetButton.isHoveredOrFocused()) {
			return "Quality Preset: 1-click settings for Low, Balanced, or High performance.";
		}
		if (texturedDistanceButton != null && texturedDistanceButton.isHoveredOrFocused()) {
			return "Textured Distance: Distance around you where textured models are drawn.";
		}
		if (texturedBlockLimitButton != null && texturedBlockLimitButton.isHoveredOrFocused()) {
			return "Textured Block Limit: Maximum textured blocks drawn at once to keep FPS smooth.";
		}
		if (ghostLimitButton != null && ghostLimitButton.isHoveredOrFocused()) {
			return "Total Ghost Limit: Maximum total preview blocks (textured + outlines) drawn at once.";
		}
		if (ghostDistanceButton != null && ghostDistanceButton.isHoveredOrFocused()) {
			return "Preview Distance: Total render distance for build preview wireframes.";
		}
		if (positionButton != null && positionButton.isHoveredOrFocused()) {
			return "HUD Position: Choose screen corner for the blocks needed list.";
		}
		if (scaleButton != null && scaleButton.isHoveredOrFocused()) {
			return "HUD Scale: Set display size for the on-screen materials list.";
		}
		if (hudShowModeButton != null && hudShowModeButton.isHoveredOrFocused()) {
			return "HUD Items: Show missing items only or all items in the on-screen overlay.";
		}
		if (keyHintsButton != null && keyHintsButton.isHoveredOrFocused()) {
			return "Key Hints: Show keyboard shortcut reminders at the bottom of the HUD.";
		}
		if (autoCheckButton != null && autoCheckButton.isHoveredOrFocused()) {
			return "Auto-Check: Automatically mark checklist rows as done when you have enough items.";
		}
		if (includeShulkersButton != null && includeShulkersButton.isHoveredOrFocused()) {
			return "Include Shulkers: Count items stored inside shulker boxes in your inventory.";
		}
		if (resetDefaultsButton != null && resetDefaultsButton.isHoveredOrFocused()) {
			return "Reset Defaults: Restore all settings to their original recommended values.";
		}
		if (doneButton != null && doneButton.isHoveredOrFocused()) {
			return "Done: Save all changes and return to the previous screen.";
		}
		return "Hover over any setting to see what it does.";
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount != 0) {
			int currentIdx = currentSection.ordinal();
			if (verticalAmount > 0) {
				currentIdx = (currentIdx + Section.values().length - 1) % Section.values().length;
			} else {
				currentIdx = (currentIdx + 1) % Section.values().length;
			}
			switchSection(Section.values()[currentIdx]);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		// a) Screen backdrop
		extractor.fill(0, 0, this.width, this.height, UiStyle.BG_SCREEN_OVERLAY);

		int centerX = this.width / 2;
		int titleY = (this.height < 140) ? 4 : Math.max(2, (this.height < 220 ? 4 : 8));

		// b) Screen Title
		extractor.centeredText(this.font, this.title, centerX, titleY, UiStyle.TEXT_TITLE);

		// Description bar above bottom buttons if space permits (height >= 140)
		if (this.height >= 140) {
			int buttonHeight = (this.height < 220) ? 15 : 19;
			int descY = this.height - buttonHeight - 26;
			int panelWidth = Math.min(340, Math.max(160, this.width - 24));
			int panelLeft = centerX - (panelWidth / 2);

			UiStyle.drawPanel(extractor, panelLeft, descY, panelWidth, 14, UiStyle.BG_CHIP, UiStyle.BORDER_LIGHT);
			String desc = getHoverDescription();
			extractor.centeredText(this.font, Component.literal(desc), centerX, descY + 3, UiStyle.TEXT_MUTED);
		}

		// d) super.extractRenderState so all widgets (section tabs, setting buttons, sliders, bottom actions) render ON TOP of background fills
		super.extractRenderState(extractor, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		ShhhBuilderConfig.getInstance().save();
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(this.parent);
		}
	}
}
