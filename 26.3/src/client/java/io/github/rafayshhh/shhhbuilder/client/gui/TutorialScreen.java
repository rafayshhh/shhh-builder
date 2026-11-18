package io.github.rafayshhh.shhhbuilder.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class TutorialScreen extends Screen {
	private static final Component TITLE = Component.translatable("screen.shhh-builder.tutorial.title");
	public static final int TOTAL_PAGES = 7;

	// UI Colors in 32-bit ARGB (kept for backward compatibility with unit tests)
	public static final int COLOR_TITLE = UiStyle.TEXT_TITLE;
	public static final int COLOR_SUBTITLE = UiStyle.COLOR_ACCENT;
	public static final int COLOR_CARD_BG = 0x80000000;
	public static final int COLOR_CARD_BORDER = 0x40FFFFFF;
	public static final int COLOR_TEXT_PRIMARY = UiStyle.TEXT_BODY;
	public static final int COLOR_TEXT_SECONDARY = UiStyle.TEXT_MUTED;
	public static final int COLOR_PAGE_COUNTER = UiStyle.TEXT_MUTED;

	private final Screen parent;
	private int page = 0;
	private UiStyle.StyledButton backButton;
	private UiStyle.StyledButton nextButton;
	private UiStyle.StyledButton closeButton;
	private UiStyle.StyledButton skipButton;

	public TutorialScreen(Screen parent) {
		super(
			net.minecraft.client.Minecraft.getInstance(),
			net.minecraft.client.Minecraft.getInstance() != null ? net.minecraft.client.Minecraft.getInstance().font : null,
			TITLE
		);
		this.parent = parent;
	}

	public int getPage() {
		return this.page;
	}

	public void setPage(int page) {
		this.page = Math.max(0, Math.min(TOTAL_PAGES - 1, page));
		updateButtonStates();
	}

	public void previousPage() {
		if (this.page > 0) {
			this.page--;
			updateButtonStates();
		}
	}

	public void nextPage() {
		if (this.page < TOTAL_PAGES - 1) {
			this.page++;
			updateButtonStates();
		} else {
			this.onClose();
		}
	}

	@Override
	protected void init() {
		super.init();

		int buttonY = (this.height < 120) ? (this.height - 20) : (this.height - 28);
		int buttonHeight = (this.height < 120) ? 16 : 20;
		int gap = 6;
		int availableWidth = Math.min(320, this.width - 24);
		int btnWidth = (availableWidth - gap * 3) / 4;
		int totalWidth = (btnWidth * 4) + (gap * 3);
		int startX = (this.width - totalWidth) / 2;

		// Back button
		this.backButton = new UiStyle.StyledButton(
			startX, buttonY, btnWidth, buttonHeight,
			Component.translatable("screen.shhh-builder.tutorial.back"),
			b -> previousPage(),
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.addRenderableWidget(this.backButton);

		// Skip button
		this.skipButton = new UiStyle.StyledButton(
			startX + btnWidth + gap, buttonY, btnWidth, buttonHeight,
			Component.literal("Skip"),
			b -> this.onClose(),
			UiStyle.StyledButton.ButtonStyle.SUBTLE
		);
		this.addRenderableWidget(this.skipButton);

		// Close button (returns to parent screen)
		this.closeButton = new UiStyle.StyledButton(
			startX + (btnWidth + gap) * 2, buttonY, btnWidth, buttonHeight,
			Component.translatable("screen.shhh-builder.tutorial.close"),
			b -> this.onClose(),
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.addRenderableWidget(this.closeButton);

		// Next / Finish button (Primary Cyan accent)
		Component nextText = (this.page == TOTAL_PAGES - 1)
			? Component.literal("Finish")
			: Component.translatable("screen.shhh-builder.tutorial.next");
		this.nextButton = new UiStyle.StyledButton(
			startX + (btnWidth + gap) * 3, buttonY, btnWidth, buttonHeight,
			nextText,
			b -> nextPage(),
			UiStyle.StyledButton.ButtonStyle.PRIMARY
		);
		this.addRenderableWidget(this.nextButton);

		updateButtonStates();
	}

	private void updateButtonStates() {
		if (this.backButton != null) {
			this.backButton.active = (this.page > 0);
		}
		if (this.nextButton != null) {
			Component nextText = (this.page == TOTAL_PAGES - 1)
				? Component.literal("Finish")
				: Component.translatable("screen.shhh-builder.tutorial.next");
			this.nextButton.setMessage(nextText);
		}
	}

	public Component getPageTitle(int pageIndex) {
		return switch (pageIndex) {
			case 0 -> Component.translatable("screen.shhh-builder.tutorial.page1.title");
			case 1 -> Component.translatable("screen.shhh-builder.tutorial.page2.title");
			case 2 -> Component.translatable("screen.shhh-builder.tutorial.page3.title");
			case 3 -> Component.translatable("screen.shhh-builder.tutorial.page4.title");
			case 4 -> Component.translatable("screen.shhh-builder.tutorial.page5.title");
			case 5 -> Component.translatable("screen.shhh-builder.tutorial.page6.title");
			case 6 -> Component.translatable("screen.shhh-builder.tutorial.page7.title");
			default -> Component.literal("");
		};
	}

	public ItemStack getPageIllustration(int pageIndex) {
		return switch (pageIndex) {
			case 0 -> new ItemStack(Items.CHEST);
			case 1 -> new ItemStack(Items.COMPASS);
			case 2 -> new ItemStack(Items.SHULKER_BOX);
			case 3 -> new ItemStack(Items.STRUCTURE_BLOCK);
			case 4 -> new ItemStack(Items.RECOVERY_COMPASS);
			case 5 -> new ItemStack(Items.SCAFFOLDING);
			case 6 -> new ItemStack(Items.EXPERIENCE_BOTTLE);
			default -> new ItemStack(Items.PAPER);
		};
	}

	public List<Component> getPageLines(int pageIndex) {
		List<Component> lines = new ArrayList<>();

		String rotKey = "R";
		String flipKey = "Z";
		String layerModeKey = "M";
		String layerUpKey = "]";
		String layerDownKey = "[";
		String layerAllKey = "\\";
		String hudKey = "H";
		String ghostKey = "G";
		String outlineKey = "B";
		String materialsKey = "K";

		if (net.minecraft.client.Minecraft.getInstance() != null) {
			rotKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.ROTATE_KEY, "R");
			flipKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.FLIP_KEY, "Z");
			layerModeKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.CYCLE_LAYER_MODE_KEY, "M");
			layerUpKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_UP_KEY, "]");
			layerDownKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_DOWN_KEY, "[");
			layerAllKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_ALL_KEY, "\\");
			hudKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.TOGGLE_HUD_KEY, "H");
			ghostKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.TOGGLE_GHOST_KEY, "G");
			outlineKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.TOGGLE_OUTLINE_KEY, "B");
			materialsKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.MATERIALS_KEY, "K");
		}

		switch (pageIndex) {
			case 0 -> {
				// Page 1: Getting started
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page1.line1"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page1.line2"));
				lines.add(Component.literal("Folder: ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal("schematics folder").withStyle(ChatFormatting.AQUA)));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page1.line3"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page1.line4"));
			}
			case 1 -> {
				// Page 2: Pick a build
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page2.line1"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page2.line2"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page2.line3"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page2.line4"));
			}
			case 2 -> {
				// Page 3: Collect your materials
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page3.line1", materialsKey));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page3.line2"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page3.line3"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page3.line4"));
			}
			case 3 -> {
				// Page 4: Place it
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page4.line1"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page4.line2"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page4.line3"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page4.line4"));
			}
			case 4 -> {
				// Page 5: Move and turn it
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page5.line1"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page5.line2"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page5.line3"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page5.line4", rotKey, flipKey));
			}
			case 5 -> {
				// Page 6: Build floor by floor
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page6.line1", layerModeKey));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page6.line2", layerDownKey, layerUpKey));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page6.line3", layerAllKey));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page6.line4", hudKey));
			}
			case 6 -> {
				// Page 7: Good to know
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page7.line1"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page7.line2"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page7.line3", ghostKey, outlineKey));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page7.line4"));
				lines.add(Component.translatable("screen.shhh-builder.tutorial.page7.line5"));
			}
			default -> {}
		}

		return lines;
	}

	public record TutorialKeycap(String key, String label, String tooltip) {}

	public List<TutorialKeycap> getPageKeycaps(int pageIndex) {
		List<TutorialKeycap> caps = new ArrayList<>();
		String rotKey = "R";
		String flipKey = "Z";
		String layerModeKey = "M";
		String layerUpKey = "]";
		String layerDownKey = "[";
		String layerAllKey = "\\";
		String hudKey = "H";
		String ghostKey = "G";
		String outlineKey = "B";
		String materialsKey = "K";

		if (net.minecraft.client.Minecraft.getInstance() != null) {
			rotKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.ROTATE_KEY, "R");
			flipKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.FLIP_KEY, "Z");
			layerModeKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.CYCLE_LAYER_MODE_KEY, "M");
			layerUpKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_UP_KEY, "]");
			layerDownKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_DOWN_KEY, "[");
			layerAllKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_ALL_KEY, "\\");
			hudKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.TOGGLE_HUD_KEY, "H");
			ghostKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.TOGGLE_GHOST_KEY, "G");
			outlineKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.TOGGLE_OUTLINE_KEY, "B");
			materialsKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.MATERIALS_KEY, "K");
		}

		switch (pageIndex) {
			case 0 -> {
				caps.add(new TutorialKeycap("V", "Menu", "Press V to open the Shhh Builder menu"));
				caps.add(new TutorialKeycap("📁", "Folder", "Drop .litematic or .schem files into schematics folder"));
			}
			case 1 -> {
				caps.add(new TutorialKeycap("V", "Browse", "Open Shhh Builder browser to pick and inspect builds"));
				caps.add(new TutorialKeycap("🔍", "Search", "Type in search box to quickly filter by build name"));
			}
			case 2 -> {
				caps.add(new TutorialKeycap(materialsKey, "Materials", "Open the Materials Checklist and tracker with " + materialsKey));
				caps.add(new TutorialKeycap("📋", "Copy", "Copy formatted item list to clipboard for survival prep"));
			}
			case 3 -> {
				caps.add(new TutorialKeycap("Enter", "Place", "Click 'Place Here' to project the build at your position"));
				caps.add(new TutorialKeycap("Esc", "Close", "Close menu and return to world"));
			}
			case 4 -> {
				caps.add(new TutorialKeycap(rotKey, "Rotate 90°", "Rotate build clockwise (" + rotKey + ")"));
				caps.add(new TutorialKeycap(flipKey, "Flip", "Mirror build along the X axis (" + flipKey + ")"));
				caps.add(new TutorialKeycap("Arrows", "Nudge", "Move 1 block forward, back, left, or right"));
				caps.add(new TutorialKeycap("PgUp/Dn", "Height", "Raise or lower placement 1 block vertically"));
			}
			case 5 -> {
				caps.add(new TutorialKeycap(layerModeKey, "Layer Mode", "Cycle layer mode: All, Single layer, or Up to layer (" + layerModeKey + ")"));
				caps.add(new TutorialKeycap(layerDownKey + "/" + layerUpKey, "Slice", "Lower or raise active layer (" + layerDownKey + " / " + layerUpKey + ")"));
				caps.add(new TutorialKeycap(layerAllKey, "All", "Reset to show all layers at once (" + layerAllKey + ")"));
				caps.add(new TutorialKeycap(hudKey, "HUD", "Toggle the live material & layer HUD overlay (" + hudKey + ")"));
			}
			case 6 -> {
				caps.add(new TutorialKeycap(ghostKey, "Ghost", "Toggle translucent ghost block preview (" + ghostKey + ")"));
				caps.add(new TutorialKeycap(outlineKey, "Outline", "Toggle schematic bounding box outline (" + outlineKey + ")"));
				caps.add(new TutorialKeycap("Pick", "Pick-Block", "Look at unplaced ghost block and middle-click to select from hotbar"));
			}
			default -> {}
		}
		return caps;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		// a) Screen backdrop
		extractor.fill(0, 0, this.width, this.height, UiStyle.BG_SCREEN_OVERLAY);

		int centerX = this.width / 2;
		long time = System.currentTimeMillis();

		// Screen Title
		extractor.centeredText(this.font, TITLE, centerX, 6, UiStyle.TEXT_TITLE);

		// b) Panel, card background fills, and step dots
		int dotSize = 7;
		int gap = 8;
		int totalWidth = (TOTAL_PAGES * dotSize) + ((TOTAL_PAGES - 1) * gap);
		int startX = centerX - (totalWidth / 2);
		int dotsY = 17;

		int hoveredDot = -1;
		for (int i = 0; i < TOTAL_PAGES; i++) {
			int dx = startX + i * (dotSize + gap);
			boolean isHovered = mouseX >= dx - 2 && mouseX <= dx + dotSize + 2 && mouseY >= dotsY - 2 && mouseY <= dotsY + dotSize + 2;
			if (isHovered) {
				hoveredDot = i;
			}
			int dotColor;
			if (i == this.page) {
				dotColor = UiStyle.COLOR_ACCENT;
			} else if (isHovered) {
				dotColor = UiStyle.COLOR_ACCENT_HOVER;
			} else {
				dotColor = UiStyle.BORDER_LIGHT;
			}
			extractor.fill(dx, dotsY, dx + dotSize, dotsY + dotSize, dotColor);
		}

		// Card Container
		int buttonY = (this.height < 120) ? (this.height - 20) : (this.height - 28);
		int cardWidth = Math.min(390, this.width - 24);
		int cardX = centerX - (cardWidth / 2);
		int cardY = (this.height < 120) ? 16 : 28;
		int cardHeight = buttonY - cardY - 4;

		// Glowing Accent Border with subtle animated pulse
		double pulse = 0.5 + 0.5 * Math.sin(time / 420.0);
		int pulseAlpha = (int) (150 + 105 * pulse); // 150 to 255
		int glowBorder = (pulseAlpha << 24) | (UiStyle.COLOR_ACCENT & 0x00FFFFFF);
		int haloAlpha = (int) (35 + 35 * pulse);
		int haloBorder = (haloAlpha << 24) | (UiStyle.COLOR_ACCENT & 0x00FFFFFF);

		// Outer soft glow halo
		extractor.fill(cardX - 1, cardY - 1, cardX + cardWidth + 1, cardY, haloBorder);
		extractor.fill(cardX - 1, cardY + cardHeight, cardX + cardWidth + 1, cardY + cardHeight + 1, haloBorder);
		extractor.fill(cardX - 1, cardY - 1, cardX, cardY + cardHeight + 1, haloBorder);
		extractor.fill(cardX + cardWidth, cardY - 1, cardX + cardWidth + 1, cardY + cardHeight + 1, haloBorder);

		// Dark neutral card panel
		UiStyle.drawPanel(extractor, cardX, cardY, cardWidth, cardHeight, UiStyle.BG_PANEL, glowBorder);

		// c) Custom row/card text and icons
		int innerX = cardX + 14;
		int innerY = cardY + 12;

		String badgeText = "STEP " + (this.page + 1) + " OF " + TOTAL_PAGES;
		UiStyle.drawBadge(extractor, this.font, badgeText, innerX, innerY, UiStyle.COLOR_ACCENT, UiStyle.BG_CHIP, UiStyle.BORDER_ACCENT);

		// Floating Item icon illustration at top right of card with subtle shadow
		ItemStack icon = getPageIllustration(this.page);
		boolean iconHovered = false;
		if (!icon.isEmpty()) {
			int iconBaseX = cardX + cardWidth - 32;
			int iconBaseY = innerY - 2;

			float floatDy = (float) (Math.sin(time / 320.0) * 1.8);
			int iconY = Math.round(iconBaseY + floatDy);

			// Shadow beneath floating item
			int shadowW = 16;
			int shadowH = 3;
			int shadowAlpha = Math.max(15, (int) (0x35 - (floatDy * 8)));
			extractor.fill(iconBaseX, iconBaseY + 18, iconBaseX + shadowW, iconBaseY + 18 + shadowH, (shadowAlpha << 24));

			// Border chip with reactive hover
			iconHovered = mouseX >= iconBaseX - 3 && mouseX <= iconBaseX + 21 && mouseY >= iconBaseY - 3 && mouseY <= iconBaseY + 21;
			int chipBorder = iconHovered ? UiStyle.BORDER_ACCENT : UiStyle.BORDER_LIGHT;
			int chipBg = iconHovered ? 0xE6132832 : UiStyle.BG_CHIP;
			UiStyle.drawPanel(extractor, iconBaseX - 3, iconBaseY - 2, 22, 22, chipBg, chipBorder);
			extractor.fakeItem(icon, iconBaseX, iconY);
		}

		// Step Title
		Component titleComp = getPageTitle(this.page);
		extractor.text(this.font, titleComp, innerX, innerY + 16, UiStyle.TEXT_TITLE, false);

		// Divider line
		extractor.fill(innerX, innerY + 28, cardX + cardWidth - 14, innerY + 29, UiStyle.BORDER_LIGHT);

		// Content Lines with high-contrast typography
		List<Component> lines = getPageLines(this.page);
		int textY = innerY + 34;
		int textMaxWidth = cardWidth - 28;

		for (Component line : lines) {
			List<FormattedCharSequence> wrapped = this.font.split(line, textMaxWidth);
			for (FormattedCharSequence piece : wrapped) {
				extractor.text(this.font, piece, innerX, textY, UiStyle.TEXT_BODY, false);
				textY += 11;
			}
			textY += 4;
		}

		// Interactive Keycaps & Badges Strip
		TutorialKeycap hoveredCap = null;
		List<TutorialKeycap> keycaps = getPageKeycaps(this.page);
		if (!keycaps.isEmpty() && cardHeight >= 115) {
			int kcAreaY = cardY + cardHeight - 22;
			int kcX = innerX;
			for (TutorialKeycap cap : keycaps) {
				int keyW = this.font.width(cap.key());
				int labelW = this.font.width(cap.label());
				int pillW = keyW + labelW + 16;
				int pillH = 14;

				if (kcX + pillW > cardX + cardWidth - 14) {
					break; // Avoid overflowing right
				}

				boolean hovered = mouseX >= kcX && mouseX <= kcX + pillW && mouseY >= kcAreaY && mouseY <= kcAreaY + pillH;
				if (hovered) {
					hoveredCap = cap;
				}
				int pillBg = hovered ? 0xE6132832 : UiStyle.BG_CARD;
				int pillBorder = hovered ? UiStyle.COLOR_ACCENT : UiStyle.BORDER_LIGHT;

				UiStyle.drawPanel(extractor, kcX, kcAreaY, pillW, pillH, pillBg, pillBorder);
				UiStyle.drawKeyCap(extractor, this.font, cap.key(), kcX + 2, kcAreaY + 1, hovered);

				int labelCol = hovered ? UiStyle.COLOR_ACCENT_HOVER : UiStyle.TEXT_HEADER;
				extractor.text(this.font, cap.label(), kcX + keyW + 11, kcAreaY + 3, labelCol, false);

				kcX += pillW + 5;
			}
		}

		// d) super.extractRenderState so all StyledButtons ([Back], [Skip], [Close], [Next]) render ON TOP of background fills
		super.extractRenderState(extractor, mouseX, mouseY, delta);

		// e) Final tooltip pass on top of everything
		if (hoveredCap != null) {
			extractor.setComponentTooltipForNextFrame(this.font, List.of(
				Component.literal("[" + hoveredCap.key() + "] " + hoveredCap.label()).withColor(UiStyle.COLOR_ACCENT),
				Component.literal(hoveredCap.tooltip()).withColor(UiStyle.TEXT_BODY)
			), mouseX, mouseY);
		} else if (iconHovered && !icon.isEmpty()) {
			extractor.setComponentTooltipForNextFrame(this.font, List.of(
				Component.literal(icon.getHoverName().getString()).withColor(UiStyle.COLOR_ACCENT)
			), mouseX, mouseY);
		} else if (hoveredDot >= 0) {
			extractor.setComponentTooltipForNextFrame(this.font, List.of(
				Component.literal("Step " + (hoveredDot + 1) + ": ").withColor(UiStyle.COLOR_ACCENT).append(getPageTitle(hoveredDot))
			), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isFocused) {
		if (event.button() == 0) {
			int dotSize = 7;
			int gap = 8;
			int totalWidth = (TOTAL_PAGES * dotSize) + ((TOTAL_PAGES - 1) * gap);
			int startX = (this.width / 2) - (totalWidth / 2);
			int dotsY = 17;

			for (int i = 0; i < TOTAL_PAGES; i++) {
				int dx = startX + i * (dotSize + gap);
				if (event.x() >= dx - 3 && event.x() <= dx + dotSize + 3 && event.y() >= dotsY - 3 && event.y() <= dotsY + dotSize + 3) {
					setPage(i);
					return true;
				}
			}
		}
		return super.mouseClicked(event, isFocused);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_LEFT || event.key() == InputConstants.KEY_PAGEUP) {
			previousPage();
			return true;
		}
		if (event.key() == InputConstants.KEY_RIGHT || event.key() == InputConstants.KEY_PAGEDOWN) {
			nextPage();
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
	public boolean isPauseScreen() {
		return false;
	}
}
