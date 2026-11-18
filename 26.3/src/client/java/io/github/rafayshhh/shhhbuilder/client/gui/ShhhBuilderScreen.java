package io.github.rafayshhh.shhhbuilder.client.gui;

import com.mojang.blaze3d.Blaze3D;
import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import io.github.rafayshhh.shhhbuilder.client.LayerMode;
import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import io.github.rafayshhh.shhhbuilder.client.SchematicFileEntry;
import io.github.rafayshhh.shhhbuilder.client.SchematicManager;
import io.github.rafayshhh.shhhbuilder.client.ShhhBuilderClient;
import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.hud.MaterialDisplayHelper;
import io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main schematic management and placement screen for Shhh Builder.
 * Redesigned with dark neutral panels, 1px borders, cyan #00E5FF accents,
 * search filtering, interactive file cards, stack-aware material counts,
 * and keycaps for all placement controls.
 */
public class ShhhBuilderScreen extends Screen {
	private static final Component TITLE = Component.translatable("screen.shhh-builder.title");
	private static final Component NO_SCHEMATICS = Component.translatable("screen.shhh-builder.no_schematics");
	private static final Component NO_SCHEMATICS_HINT = Component.translatable("screen.shhh-builder.no_schematics_hint");
	private static final Component REFRESH_TEXT = Component.translatable("screen.shhh-builder.refresh");
	private static final Component OPEN_FOLDER_TEXT = Component.translatable("screen.shhh-builder.open_folder");
	private static final Component CLOSE_TEXT = Component.translatable("screen.shhh-builder.close");
	private static final Component SHOW_MATERIALS_TEXT = Component.literal("Show on HUD");
	public static final Component PLACE_HERE_TEXT = Component.translatable("screen.shhh-builder.place_here");
	public static final Component CLEAR_PLACEMENT_TEXT = Component.translatable("screen.shhh-builder.clear_placement");

	public record LayoutMetrics(
		int padding,
		int panelY,
		int panelHeight,
		int leftX,
		int leftWidth,
		int rightX,
		int rightWidth,
		int tabY,
		int tabDetailsX,
		int tabDetailsW,
		int tabControlsX,
		int tabControlsW,
		int bottomBarY,
		int bottomBarH,
		int[] bottomButtonX,
		int[] bottomButtonW
	) {}

	public static LayoutMetrics calculateLayout(int screenWidth, int screenHeight) {
		int margin = 12;
		int headerHeight = (screenHeight < 120) ? 16 : 32;
		int actionbarHeight = (screenHeight < 120) ? 20 : 26;
		int panelY = (screenHeight < 120) ? 18 : 36;

		// Bottom Action Bar: 5 buttons evenly distributed across screenWidth - 24, pinned at y = screenHeight - 36
		int bottomBarY = (screenHeight < 120) ? (screenHeight - 24) : (screenHeight - 36);
		int bottomBarH = (screenHeight < 120) ? 18 : 22;

		// Calculate usable height: usableHeight = screenHeight - headerHeight - actionbarHeight - 24
		int maxPanelH = Math.max(10, bottomBarY - panelY - 4);
		int usableHeight;
		if (screenHeight < 120) {
			usableHeight = Math.max(24, maxPanelH);
		} else {
			usableHeight = Math.min(maxPanelH, screenHeight - headerHeight - actionbarHeight - 24);
			usableHeight = Math.max(10, usableHeight);
		}
		int panelHeight = usableHeight;

		// Total width available for the two panels with margins and column gap
		int colGap = 12;
		int availableWidth = Math.max(20, screenWidth - (margin * 2) - colGap);

		// Left Panel: responsive width, min 200px, max 240px when room permits
		int targetLeft = availableWidth / 3;
		int leftWidth;
		if (availableWidth >= 500) {
			leftWidth = Math.max(200, Math.min(240, targetLeft));
		} else if (availableWidth >= 280) {
			leftWidth = Math.max(100, Math.min(availableWidth - 100, targetLeft));
		} else if (availableWidth >= 80) {
			leftWidth = Math.max(40, Math.min(availableWidth - 40, targetLeft));
		} else {
			leftWidth = Math.max(10, availableWidth / 2);
		}

		int leftX = margin;
		int rightX = leftX + leftWidth + colGap;
		int rightWidth = Math.max(10, screenWidth - rightX - margin);

		// Tab Buttons in Right Panel
		int tabY = (screenHeight < 120) ? (panelY + 2) : (panelY + 6);
		int tabGap = 4;
		int tabAreaW = Math.max(10, rightWidth - 16);
		int tabDetailsW = Math.max(5, (tabAreaW - tabGap) / 2);
		int tabControlsW = Math.max(5, (tabAreaW - tabGap) - tabDetailsW);
		int tabDetailsX = rightX + 8;
		int tabControlsX = tabDetailsX + tabDetailsW + tabGap;

		int btnCount = 5;
		int spacing = 6;
		int totalWidth = Math.max(btnCount * 10, screenWidth - (margin * 2));
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

		return new LayoutMetrics(
			margin, panelY, panelHeight,
			leftX, leftWidth, rightX, rightWidth,
			tabY, tabDetailsX, tabDetailsW, tabControlsX, tabControlsW,
			bottomBarY, bottomBarH, bottomButtonX, bottomButtonW
		);
	}

	// Backward-compatible color constants for existing unit tests
	public static final int COLOR_TITLE = UiStyle.TEXT_TITLE;
	public static final int COLOR_FOLDER_PATH = UiStyle.TEXT_MUTED;
	public static final int COLOR_HINT = UiStyle.TEXT_MUTED;
	public static final int COLOR_SELECTED_GREEN = UiStyle.COLOR_SUCCESS;
	public static final int COLOR_ERROR_TITLE = UiStyle.COLOR_ERROR;
	public static final int COLOR_ERROR_TEXT = 0xFFFFAAAA;
	public static final int COLOR_LOADING = UiStyle.COLOR_WARNING;
	public static final int COLOR_TEXT_WHITE = UiStyle.TEXT_TITLE;
	public static final int COLOR_TEXT_GRAY = UiStyle.TEXT_BODY;
	public static final int COLOR_TEXT_MUTED = UiStyle.TEXT_MUTED;
	public static final int COLOR_MATERIALS_HEADER = UiStyle.TEXT_HEADER;
	public static final int COLOR_MATERIALS_ITEM = UiStyle.TEXT_BODY;

	public enum Tab {
		DETAILS,
		CONTROLS
	}

	private static Tab activeTab = Tab.DETAILS;

	// Schematic scanning & filtering state
	private final List<SchematicFileEntry> scannedFiles = new ArrayList<>();
	private final List<SchematicFileEntry> filteredFiles = new ArrayList<>();
	private final List<Button> fileButtons = new ArrayList<>();
	private EditBox searchBox;
	private String searchQuery = "";
	private Button prevPageButton;
	private Button nextPageButton;
	private int currentPage = 0;
	private int itemsPerPage = 6;
	private boolean scannedOnOpen = false;
	private String scanError = null;

	// Right Tab & Action Buttons
	private UiStyle.StyledButton tabDetailsButton;
	private UiStyle.StyledButton tabControlsButton;
	private UiStyle.StyledButton showMaterialsButton;
	private UiStyle.StyledButton openChecklistButton;
	private UiStyle.StyledButton placeHereButton;
	private UiStyle.StyledButton clearPlacementButton;
	private UiStyle.StyledButton refreshButton;
	private UiStyle.StyledButton openFolderButton;
	private UiStyle.StyledButton closeButton;
	private UiStyle.StyledButton materialsButton;
	private UiStyle.StyledButton howToUseButton;
	private UiStyle.StyledButton settingsButton;

	// Materials list scrolling
	private int materialScrollOffset = 0;

	// Placement control panel widgets
	private final List<AbstractWidget> controlWidgets = new ArrayList<>();
	private UiStyle.StyledButton fwdButton;
	private UiStyle.StyledButton backButton;
	private UiStyle.StyledButton leftButton;
	private UiStyle.StyledButton rightButton;
	private UiStyle.StyledButton upButton;
	private UiStyle.StyledButton downButton;
	private UiStyle.StyledButton rotLeftButton;
	private UiStyle.StyledButton rotRightButton;
	private UiStyle.StyledButton flipButton;
	private UiStyle.StyledButton layerDownButton;
	private UiStyle.StyledButton layerModeButton;
	private UiStyle.StyledButton layerUpButton;
	private UiStyle.StyledButton layerAllButton;
	private UiStyle.StyledButton opacityMinusButton;
	private UiStyle.StyledButton opacityValueButton;
	private UiStyle.StyledButton opacityPlusButton;

	// Selected schematic details state
	private boolean loading = false;
	private String loadError = null;
	private LitematicData loadedData = null;

	public ShhhBuilderScreen() {
		super(TITLE);
	}

	@Override
	protected void init() {
		super.init();

		LayoutMetrics layout = calculateLayout(this.width, this.height);
		int panelY = layout.panelY();
		int panelHeight = layout.panelHeight();
		int leftWidth = layout.leftWidth();
		int leftX = layout.leftX();
		int rightX = layout.rightX();
		int rightWidth = layout.rightWidth();

		// Calculate items per page based on card height (24px stride on normal screens, 20px on small)
		int cardStride = (this.height < 220) ? 20 : 25;
		int searchAndNavHeight = 44;
		int availableListH = panelHeight - searchAndNavHeight;
		this.itemsPerPage = Math.max(2, Math.min(8, availableListH / cardStride));

		// Scan schematics once on initial screen open
		if (!this.scannedOnOpen) {
			this.scannedOnOpen = true;
			this.doScan(false);
		} else {
			this.updateFilteredList();
		}

		// Top Header Buttons: "Materials", "How to use", and "Settings"
		int topBtnH = 16;
		int settingsW = 56;
		int howToUseW = 68;
		int materialsW = 62;
		int topGap = 4;
		int availableTop = this.width - 100;
		if (availableTop < 200 && availableTop > 60) {
			settingsW = Math.max(38, (availableTop - topGap * 2) * 28 / 100);
			materialsW = Math.max(38, (availableTop - topGap * 2) * 32 / 100);
			howToUseW = Math.max(38, availableTop - topGap * 2 - settingsW - materialsW);
		}
		int settingsX = this.width - settingsW - 12;
		int howToUseX = settingsX - howToUseW - topGap;
		int materialsX = howToUseX - materialsW - topGap;

		this.materialsButton = new UiStyle.StyledButton(
			materialsX, 6, materialsW, topBtnH,
			Component.literal("Materials"),
			b -> {
				if (this.minecraft != null) {
					this.minecraft.gui.setScreen(new MaterialsScreen(this));
				}
			},
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.materialsButton.setTooltip(Tooltip.create(Component.literal("Open materials checklist and inventory tracker (Key: K)")));
		this.addRenderableWidget(this.materialsButton);

		this.howToUseButton = new UiStyle.StyledButton(
			howToUseX, 6, howToUseW, topBtnH,
			Component.translatable("screen.shhh-builder.how_to_use"),
			b -> {
				if (this.minecraft != null) {
					this.minecraft.gui.setScreen(new TutorialScreen(this));
				}
			},
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.howToUseButton.setTooltip(Tooltip.create(Component.literal("Open the step-by-step tutorial guide")));
		this.addRenderableWidget(this.howToUseButton);

		this.settingsButton = new UiStyle.StyledButton(
			settingsX, 6, settingsW, topBtnH,
			Component.literal("Settings"),
			b -> {
				if (this.minecraft != null) {
					this.minecraft.gui.setScreen(new ShhhBuilderConfigScreen(this));
				}
			},
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.settingsButton.setTooltip(Tooltip.create(Component.literal("Open preview, performance, and HUD settings")));
		this.addRenderableWidget(this.settingsButton);

		// Search Box in Left Panel
		int searchBoxY = panelY + 16;
		int searchBoxW = leftWidth - 12;
		this.searchBox = new EditBox(this.font, leftX + 6, searchBoxY, searchBoxW, 15, Component.literal("Search"));
		this.searchBox.setHint(Component.literal("Search builds...").withStyle(ChatFormatting.DARK_GRAY));
		this.searchBox.setValue(this.searchQuery);
		this.searchBox.setResponder(query -> {
			this.searchQuery = query;
			this.currentPage = 0;
			this.updateFilteredList();
			this.populatePageWidgets();
		});
		this.addRenderableWidget(this.searchBox);

		// Populate paged file buttons
		this.populatePageWidgets();

		// Right Panel: Tab Switcher (Blocks needed vs Move & Turn)
		this.tabDetailsButton = new UiStyle.StyledButton(
			layout.tabDetailsX(), layout.tabY(), layout.tabDetailsW(), 17,
			Component.literal("Blocks needed"),
			b -> setTab(Tab.DETAILS),
			(activeTab == Tab.DETAILS) ? UiStyle.StyledButton.ButtonStyle.PRIMARY : UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.tabDetailsButton.setTooltip(Tooltip.create(Component.literal("View build dimensions and required materials")));
		this.addRenderableWidget(this.tabDetailsButton);

		this.tabControlsButton = new UiStyle.StyledButton(
			layout.tabControlsX(), layout.tabY(), layout.tabControlsW(), 17,
			Component.literal("Position"),
			b -> setTab(Tab.CONTROLS),
			(activeTab == Tab.CONTROLS) ? UiStyle.StyledButton.ButtonStyle.PRIMARY : UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.tabControlsButton.setTooltip(Tooltip.create(Component.literal("Move, turn, flip, or adjust layers and opacity")));
		this.addRenderableWidget(this.tabControlsButton);

		// Keybind names for tooltips and keycaps
		String rotKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.ROTATE_KEY, "R");
		String flipKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.FLIP_KEY, "Z");
		String layerDownKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_DOWN_KEY, "[");
		String layerUpKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_UP_KEY, "]");
		String layerModeKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.CYCLE_LAYER_MODE_KEY, "M");
		String layerAllKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.LAYER_ALL_KEY, "\\");
		String hudKey = ShhhBuilderClient.getKeyName(ShhhBuilderClient.TOGGLE_HUD_KEY, "H");

		// Placement Controls Widgets
		this.controlWidgets.clear();
		int colGap = 3;
		int actionAreaWidth = rightWidth - 16;
		int actionLeft = rightX + 8;
		int colWidth = (actionAreaWidth - colGap * 2) / 3;
		int col0 = actionLeft;
		int col1 = actionLeft + colWidth + colGap;
		int col2 = actionLeft + (colWidth + colGap) * 2;
		int col2Width = actionAreaWidth - (colWidth + colGap) * 2;

		int ctrlStartY = panelY + 68;
		int ctrlStride = 19;

		// Move Horizontal: Left, Fwd, Right
		this.leftButton = new UiStyle.StyledButton(col0, ctrlStartY, colWidth, 16, Component.literal("◄ Left"), b -> movePlacement(0, -1, 0)).withKeyCap("←");
		this.leftButton.setTooltip(Tooltip.create(Component.literal("Move build 1 block left (Key: Left Arrow)")));
		this.addRenderableWidget(this.leftButton);
		this.controlWidgets.add(this.leftButton);

		this.fwdButton = new UiStyle.StyledButton(col1, ctrlStartY, colWidth, 16, Component.literal("▲ Fwd"), b -> movePlacement(1, 0, 0)).withKeyCap("↑");
		this.fwdButton.setTooltip(Tooltip.create(Component.literal("Move build 1 block forward (Key: Up Arrow)")));
		this.addRenderableWidget(this.fwdButton);
		this.controlWidgets.add(this.fwdButton);

		this.rightButton = new UiStyle.StyledButton(col2, ctrlStartY, col2Width, 16, Component.literal("Right ►"), b -> movePlacement(0, 1, 0)).withKeyCap("→");
		this.rightButton.setTooltip(Tooltip.create(Component.literal("Move build 1 block right (Key: Right Arrow)")));
		this.addRenderableWidget(this.rightButton);
		this.controlWidgets.add(this.rightButton);

		// Move Vertical & Back: Down, Back, Up
		this.downButton = new UiStyle.StyledButton(col0, ctrlStartY + ctrlStride, colWidth, 16, Component.literal("Down ▼"), b -> movePlacement(0, 0, -1)).withKeyCap("PgDn");
		this.downButton.setTooltip(Tooltip.create(Component.literal("Move build 1 block down (Key: PageDown)")));
		this.addRenderableWidget(this.downButton);
		this.controlWidgets.add(this.downButton);

		this.backButton = new UiStyle.StyledButton(col1, ctrlStartY + ctrlStride, colWidth, 16, Component.literal("▼ Back"), b -> movePlacement(-1, 0, 0)).withKeyCap("↓");
		this.backButton.setTooltip(Tooltip.create(Component.literal("Move build 1 block backward (Key: Down Arrow)")));
		this.addRenderableWidget(this.backButton);
		this.controlWidgets.add(this.backButton);

		this.upButton = new UiStyle.StyledButton(col2, ctrlStartY + ctrlStride, col2Width, 16, Component.literal("Up ▲"), b -> movePlacement(0, 0, 1)).withKeyCap("PgUp");
		this.upButton.setTooltip(Tooltip.create(Component.literal("Move build 1 block up (Key: PageUp)")));
		this.addRenderableWidget(this.upButton);
		this.controlWidgets.add(this.upButton);

		// Rotate & Flip: ↶ -90°, Flip, +90° ↷
		this.rotLeftButton = new UiStyle.StyledButton(col0, ctrlStartY + (ctrlStride * 2), colWidth, 16, Component.literal("↶ -90°"), b -> {
			PlacementManager.rotateCounterClockwise();
			updateButtonStates();
		});
		this.rotLeftButton.setTooltip(Tooltip.create(Component.literal("Turn build 90° counter-clockwise")));
		this.addRenderableWidget(this.rotLeftButton);
		this.controlWidgets.add(this.rotLeftButton);

		this.flipButton = new UiStyle.StyledButton(col1, ctrlStartY + (ctrlStride * 2), colWidth, 16, Component.literal("Flip"), b -> {
			PlacementManager.toggleMirrored();
			updateButtonStates();
		}).withKeyCap(flipKey);
		this.flipButton.setTooltip(Tooltip.create(Component.literal("Flip build left-right (Key: " + flipKey + ")")));
		this.addRenderableWidget(this.flipButton);
		this.controlWidgets.add(this.flipButton);

		this.rotRightButton = new UiStyle.StyledButton(col2, ctrlStartY + (ctrlStride * 2), col2Width, 16, Component.literal("+90° ↷"), b -> {
			PlacementManager.rotateClockwise();
			updateButtonStates();
		}).withKeyCap(rotKey);
		this.rotRightButton.setTooltip(Tooltip.create(Component.literal("Turn build 90° clockwise (Key: " + rotKey + ")")));
		this.addRenderableWidget(this.rotRightButton);
		this.controlWidgets.add(this.rotRightButton);

		// Layers: ▼ Layer, Mode, Layer ▲
		this.layerDownButton = new UiStyle.StyledButton(col0, ctrlStartY + (ctrlStride * 3), colWidth, 16, Component.literal("▼ Layer"), b -> {
			PlacementManager.layerDown();
			updateButtonStates();
		}).withKeyCap(layerDownKey);
		this.layerDownButton.setTooltip(Tooltip.create(Component.literal("Show lower layer (Key: " + layerDownKey + ")")));
		this.addRenderableWidget(this.layerDownButton);
		this.controlWidgets.add(this.layerDownButton);

		this.layerModeButton = new UiStyle.StyledButton(col1, ctrlStartY + (ctrlStride * 3), colWidth, 16, Component.literal("Mode"), b -> {
			PlacementManager.cycleLayerMode();
			updateButtonStates();
		}).withKeyCap(layerModeKey);
		this.layerModeButton.setTooltip(Tooltip.create(Component.literal("Cycle layer mode: All, Single, or Up to (Key: " + layerModeKey + ")")));
		this.addRenderableWidget(this.layerModeButton);
		this.controlWidgets.add(this.layerModeButton);

		this.layerUpButton = new UiStyle.StyledButton(col2, ctrlStartY + (ctrlStride * 3), col2Width, 16, Component.literal("Layer ▲"), b -> {
			int maxH = this.loadedData != null ? this.loadedData.height() : 256;
			PlacementManager.layerUp(maxH);
			updateButtonStates();
		}).withKeyCap(layerUpKey);
		this.layerUpButton.setTooltip(Tooltip.create(Component.literal("Show higher layer (Key: " + layerUpKey + ")")));
		this.addRenderableWidget(this.layerUpButton);
		this.controlWidgets.add(this.layerUpButton);

		// Reset All Layers
		this.layerAllButton = new UiStyle.StyledButton(actionLeft, ctrlStartY + (ctrlStride * 4), actionAreaWidth, 16, Component.literal("Show All Layers"), b -> {
			PlacementManager.resetLayerMode();
			updateButtonStates();
		}).withKeyCap(layerAllKey);
		this.layerAllButton.setTooltip(Tooltip.create(Component.literal("Show all layers at once (Key: " + layerAllKey + ")")));
		this.addRenderableWidget(this.layerAllButton);
		this.controlWidgets.add(this.layerAllButton);

		// Opacity row: [-] [Opacity: 45%] [+]
		int stepBtnW = 22;
		int opacityLabelW = actionAreaWidth - (stepBtnW * 2) - 4;
		this.opacityMinusButton = new UiStyle.StyledButton(actionLeft, ctrlStartY + (ctrlStride * 5), stepBtnW, 16, Component.literal("[-]"), b -> {
			ShhhBuilderConfig.getInstance().stepOpacity(-5);
			ShhhBuilderConfig.getInstance().save();
			updateButtonStates();
		});
		this.opacityMinusButton.setTooltip(Tooltip.create(Component.literal("Decrease preview opacity by 5% (min 10%)")));
		this.addRenderableWidget(this.opacityMinusButton);
		this.controlWidgets.add(this.opacityMinusButton);

		this.opacityValueButton = new UiStyle.StyledButton(actionLeft + stepBtnW + 2, ctrlStartY + (ctrlStride * 5), opacityLabelW, 16, Component.literal("Opacity: " + ShhhBuilderConfig.getInstance().getPreviewOpacity() + "%"), b -> {
			int cur = ShhhBuilderConfig.getInstance().getPreviewOpacity();
			int next = (cur == 45) ? 75 : ((cur == 75) ? 100 : ((cur == 100) ? 25 : 45));
			ShhhBuilderConfig.getInstance().setPreviewOpacity(next);
			ShhhBuilderConfig.getInstance().save();
			updateButtonStates();
		});
		this.opacityValueButton.setTooltip(Tooltip.create(Component.literal("Live preview opacity. Click [-] or [+] to adjust.")));
		this.addRenderableWidget(this.opacityValueButton);
		this.controlWidgets.add(this.opacityValueButton);

		this.opacityPlusButton = new UiStyle.StyledButton(actionLeft + stepBtnW + 2 + opacityLabelW + 2, ctrlStartY + (ctrlStride * 5), stepBtnW, 16, Component.literal("[+]"), b -> {
			ShhhBuilderConfig.getInstance().stepOpacity(5);
			ShhhBuilderConfig.getInstance().save();
			updateButtonStates();
		});
		this.opacityPlusButton.setTooltip(Tooltip.create(Component.literal("Increase preview opacity by 5% (max 100%)")));
		this.addRenderableWidget(this.opacityPlusButton);
		this.controlWidgets.add(this.opacityPlusButton);

		// Details Tab only: Checklist button and Show on HUD button
		int btnGap = 4;
		int halfBtnW = (actionAreaWidth - btnGap) / 2;
		this.openChecklistButton = new UiStyle.StyledButton(
			actionLeft, panelY + panelHeight - 24, halfBtnW, 18,
			Component.literal("Checklist"),
			b -> {
				if (this.minecraft != null) {
					this.minecraft.gui.setScreen(new MaterialsScreen(this));
				}
			},
			UiStyle.StyledButton.ButtonStyle.PRIMARY
		);
		this.openChecklistButton.setTooltip(Tooltip.create(Component.literal("Open interactive materials checklist and inventory tracker (Key: K)")));
		this.addRenderableWidget(this.openChecklistButton);

		this.showMaterialsButton = new UiStyle.StyledButton(
			actionLeft + halfBtnW + btnGap, panelY + panelHeight - 24, actionAreaWidth - halfBtnW - btnGap, 18,
			SHOW_MATERIALS_TEXT,
			b -> {
				ShhhBuilderConfig.getInstance().setHudEnabled(true);
				ShhhBuilderConfig.getInstance().save();
				this.onClose();
			},
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.showMaterialsButton.setTooltip(Tooltip.create(Component.literal("Show blocks needed on screen while building (Key: " + hudKey + ")")));
		this.addRenderableWidget(this.showMaterialsButton);

		// Bottom Action Bar: 5 buttons distributed evenly across this.width - 20, pinned at y = this.height - 30
		int placeW = layout.bottomButtonW()[0];
		int clearW = layout.bottomButtonW()[1];
		int refreshW = layout.bottomButtonW()[2];
		int folderW = layout.bottomButtonW()[3];
		int closeW = layout.bottomButtonW()[4];

		int placeX = layout.bottomButtonX()[0];
		int clearX = layout.bottomButtonX()[1];
		int refreshX = layout.bottomButtonX()[2];
		int folderX = layout.bottomButtonX()[3];
		int closeX = layout.bottomButtonX()[4];
		int bottomY = layout.bottomBarY();
		int btnH = layout.bottomBarH();

		this.placeHereButton = new UiStyle.StyledButton(
			placeX, bottomY, placeW, btnH,
			PLACE_HERE_TEXT,
			b -> {
				if (this.minecraft != null && this.minecraft.player != null) {
					BlockPos playerPos = this.minecraft.player.blockPosition();
					PlacementManager.setPlacement(playerPos, this.minecraft.player.level().dimension());
					PlacementManager.setVisible(true);
					this.onClose();
				}
			},
			UiStyle.StyledButton.ButtonStyle.PRIMARY
		);
		this.placeHereButton.setTooltip(Tooltip.create(Component.literal("Place build right where you are standing")));
		this.addRenderableWidget(this.placeHereButton);

		this.clearPlacementButton = new UiStyle.StyledButton(
			clearX, bottomY, clearW, btnH,
			CLEAR_PLACEMENT_TEXT,
			b -> {
				PlacementManager.clear();
				updateButtonStates();
			},
			UiStyle.StyledButton.ButtonStyle.DANGER
		);
		this.clearPlacementButton.setTooltip(Tooltip.create(Component.literal("Clear build preview from the world")));
		this.addRenderableWidget(this.clearPlacementButton);

		this.refreshButton = new UiStyle.StyledButton(
			refreshX, bottomY, refreshW, btnH,
			REFRESH_TEXT,
			b -> {
				this.doScan(true);
				this.populatePageWidgets();
			},
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.refreshButton.setTooltip(Tooltip.create(Component.literal("Rescan schematics folder for new files")));
		this.addRenderableWidget(this.refreshButton);

		this.openFolderButton = new UiStyle.StyledButton(
			folderX, bottomY, folderW, btnH,
			OPEN_FOLDER_TEXT,
			b -> openSchematicsFolder(),
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.openFolderButton.setTooltip(Tooltip.create(Component.literal("Open the schematics folder on your computer")));
		this.addRenderableWidget(this.openFolderButton);

		this.closeButton = new UiStyle.StyledButton(
			closeX, bottomY, closeW, btnH,
			CLOSE_TEXT,
			b -> this.onClose(),
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.addRenderableWidget(this.closeButton);

		setTab(activeTab);
	}

	private void updateFilteredList() {
		this.filteredFiles.clear();
		String q = (this.searchQuery != null) ? this.searchQuery.trim().toLowerCase(Locale.ROOT) : "";
		if (q.isEmpty()) {
			this.filteredFiles.addAll(this.scannedFiles);
		} else {
			for (SchematicFileEntry entry : this.scannedFiles) {
				if (entry.fileName().toLowerCase(Locale.ROOT).contains(q) || entry.baseName().toLowerCase(Locale.ROOT).contains(q)) {
					this.filteredFiles.add(entry);
				}
			}
		}
	}

	private void doScan(boolean isRefresh) {
		Path dir = SchematicManager.getSchematicsDirectory();
		if (isRefresh) {
			ShhhBuilder.LOGGER.info("[Schematics Scan] User clicked Refresh: scanning directory: {}", dir);
		}
		this.scanError = null;
		this.scannedFiles.clear();
		this.scannedFiles.addAll(SchematicManager.scanSchematics(error -> {
			this.scanError = error;
		}));

		this.updateFilteredList();

		String currentSelected = SchematicManager.getSelectedFileName();
		SchematicFileEntry activeEntry = null;
		if (currentSelected != null) {
			for (int i = 0; i < this.filteredFiles.size(); i++) {
				SchematicFileEntry file = this.filteredFiles.get(i);
				if (file.fileName().equalsIgnoreCase(currentSelected)) {
					activeEntry = file;
					this.currentPage = i / Math.max(1, this.itemsPerPage);
					break;
				}
			}
		}

		if (activeEntry != null) {
			this.onSchematicSelected(activeEntry);
		} else {
			if (currentSelected != null) {
				SchematicManager.setSelectedFileName(null);
			}
			this.loadedData = null;
			this.loadError = null;
			this.loading = false;
			updateButtonStates();
		}
	}

	private void populatePageWidgets() {
		for (Button b : this.fileButtons) {
			this.removeWidget(b);
		}
		this.fileButtons.clear();

		LayoutMetrics layout = calculateLayout(this.width, this.height);
		int panelY = layout.panelY();
		int leftWidth = layout.leftWidth();
		int leftX = layout.leftX();
		int listTop = panelY + 36;
		int stride = (this.height < 220) ? 20 : 25;

		int totalFiles = this.filteredFiles.size();
		int maxPages = Math.max(1, (totalFiles + this.itemsPerPage - 1) / this.itemsPerPage);
		if (this.currentPage >= maxPages) {
			this.currentPage = maxPages - 1;
		}
		if (this.currentPage < 0) {
			this.currentPage = 0;
		}

		int startIndex = this.currentPage * this.itemsPerPage;
		int endIndex = Math.min(totalFiles, startIndex + this.itemsPerPage);
		String currentSelected = SchematicManager.getSelectedFileName();

		for (int i = startIndex; i < endIndex; i++) {
			SchematicFileEntry file = this.filteredFiles.get(i);
			int slot = i - startIndex;
			int itemY = listTop + (slot * stride);

			boolean isSelected = (currentSelected != null && currentSelected.equalsIgnoreCase(file.fileName()));
			SchematicManager.CachedSchematic cached = SchematicManager.getCached(file.path());
			LitematicData cData = (cached != null) ? cached.data : null;

			FileCardButton fileBtn = new FileCardButton(
				leftX + 6, itemY, leftWidth - 12, stride - 2,
				file, isSelected, cData,
				b -> {
					this.onSchematicSelected(file);
					this.populatePageWidgets();
				}
			);

			this.addRenderableWidget(fileBtn);
			this.fileButtons.add(fileBtn);
		}

		// Page Navigation Row in Panel Footer
		int navY = listTop + (this.itemsPerPage * stride) + 2;
		int navBtnWidth = 42;

		if (this.prevPageButton != null) {
			this.removeWidget(this.prevPageButton);
		}
		this.prevPageButton = new UiStyle.StyledButton(
			leftX + 6, navY, navBtnWidth, 16,
			Component.literal("◀ Prev"),
			b -> previousPage(),
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.prevPageButton.active = (this.currentPage > 0);
		this.prevPageButton.visible = (totalFiles > 0);
		this.addRenderableWidget(this.prevPageButton);

		if (this.nextPageButton != null) {
			this.removeWidget(this.nextPageButton);
		}
		this.nextPageButton = new UiStyle.StyledButton(
			leftX + leftWidth - navBtnWidth - 6, navY, navBtnWidth, 16,
			Component.literal("Next ▶"),
			b -> nextPage(),
			UiStyle.StyledButton.ButtonStyle.NORMAL
		);
		this.nextPageButton.active = (this.currentPage < maxPages - 1);
		this.nextPageButton.visible = (totalFiles > 0);
		this.addRenderableWidget(this.nextPageButton);
	}

	private static class FileCardButton extends Button {
		private final SchematicFileEntry file;
		private final boolean isSelected;
		private final LitematicData cachedData;

		public FileCardButton(int x, int y, int width, int height, SchematicFileEntry file, boolean isSelected, LitematicData cachedData, OnPress onPress) {
			super(x, y, width, height, Component.literal(file.baseName()), onPress, DEFAULT_NARRATION);
			this.file = file;
			this.isSelected = isSelected;
			this.cachedData = cachedData;
			this.setTooltip(Tooltip.create(Component.literal(file.fileName() + " (" + file.extension() + ")")));
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
			boolean hovered = this.isHoveredOrFocused();
			int bg = isSelected ? UiStyle.BG_CARD_SELECTED : (hovered ? UiStyle.BG_CARD_HOVER : UiStyle.BG_CARD);
			int border = isSelected ? UiStyle.BORDER_ACCENT : (hovered ? UiStyle.BORDER_HOVER : UiStyle.BORDER_NORMAL);

			UiStyle.drawPanel(extractor, this.getX(), this.getY(), this.width, this.height, bg, border);

			Font font = Minecraft.getInstance().font;
			if (font == null) return;

			int innerX = this.getX() + 6;
			int innerY = this.getY() + 3;

			// Base file name
			int nameColor = isSelected ? UiStyle.COLOR_ACCENT : (hovered ? UiStyle.TEXT_TITLE : UiStyle.TEXT_BODY);
			String name = file.baseName();
			int badgeW = font.width(file.extension()) + 8;
			int maxNameW = this.width - badgeW - 14;
			if (font.width(name) > maxNameW && maxNameW > 10) {
				name = font.plainSubstrByWidth(name, maxNameW - font.width("..")) + "..";
			}
			extractor.text(font, name, innerX, innerY, nameColor, false);

			// Format badge at right of card
			int badgeX = this.getX() + this.width - badgeW - 4;
			int badgeBg = file.extension().equalsIgnoreCase(".litematic") ? 0xFF0D2538 : 0xFF2A1C38;
			int badgeBorder = file.extension().equalsIgnoreCase(".litematic") ? 0xFF1D4E75 : 0xFF583675;
			int badgeText = file.extension().equalsIgnoreCase(".litematic") ? 0xFF58A6FF : 0xFFBC8CFF;
			UiStyle.drawBadge(extractor, font, file.extension(), badgeX, innerY, badgeText, badgeBg, badgeBorder);

			// Metadata line if card height allows
			if (this.height >= 22) {
				String meta;
				if (cachedData != null) {
					meta = cachedData.width() + "x" + cachedData.height() + "x" + cachedData.length() + " • " + String.format(Locale.ROOT, "%,d", cachedData.totalBlocks()) + " blocks";
				} else {
					meta = "Schematic file • " + file.extension();
				}
				if (font.width(meta) > this.width - 12) {
					meta = font.plainSubstrByWidth(meta, this.width - 16) + "..";
				}
				extractor.text(font, meta, innerX, innerY + 10, hovered ? UiStyle.TEXT_HEADER : UiStyle.TEXT_BODY, false);
			}
		}
	}

	private void previousPage() {
		if (this.currentPage > 0) {
			this.currentPage--;
			populatePageWidgets();
		}
	}

	private void nextPage() {
		int maxPages = Math.max(1, (this.filteredFiles.size() + this.itemsPerPage - 1) / this.itemsPerPage);
		if (this.currentPage < maxPages - 1) {
			this.currentPage++;
			populatePageWidgets();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		LayoutMetrics layout = calculateLayout(this.width, this.height);
		int leftWidth = layout.leftWidth();
		int leftX = layout.leftX();
		int rightX = layout.rightX();
		int rightWidth = layout.rightWidth();
		int panelY = layout.panelY();
		int panelHeight = layout.panelHeight();

		// Mouse scroll on Left Schematics list
		if (mouseX >= leftX && mouseX <= leftX + leftWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
			if (verticalAmount > 0) {
				previousPage();
				return true;
			} else if (verticalAmount < 0) {
				nextPage();
				return true;
			}
		}

		// Mouse scroll on Right Materials list (when on Details tab)
		if (activeTab == Tab.DETAILS && mouseX >= rightX && mouseX <= rightX + rightWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
			if (verticalAmount != 0 && this.loadedData != null) {
				int totalMat = this.loadedData.topBlocks().size();
				int maxVisible = Math.max(1, (this.height - 140) / 16);
				int maxOffset = Math.max(0, totalMat - maxVisible);
				if (maxOffset > 0) {
					this.materialScrollOffset = Math.max(0, Math.min(maxOffset, this.materialScrollOffset - (int) verticalAmount));
					return true;
				}
			}
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private void setTab(Tab tab) {
		activeTab = tab;
		boolean isControls = (activeTab == Tab.CONTROLS);
		for (AbstractWidget widget : this.controlWidgets) {
			widget.visible = isControls;
		}
		if (this.showMaterialsButton != null) {
			this.showMaterialsButton.visible = !isControls;
		}
		if (this.openChecklistButton != null) {
			this.openChecklistButton.visible = !isControls;
		}
		if (this.tabDetailsButton != null) {
			this.tabDetailsButton.active = isControls;
		}
		if (this.tabControlsButton != null) {
			this.tabControlsButton.active = !isControls;
		}
		updateButtonStates();
	}

	private void movePlacement(int forward, int right, int up) {
		if (!PlacementManager.hasPlacement()) {
			return;
		}
		Direction facing = (this.minecraft != null && this.minecraft.player != null)
			? this.minecraft.player.getDirection()
			: Direction.NORTH;

		int dx = 0;
		int dz = 0;
		if (forward != 0) {
			Direction dir = (forward > 0) ? facing : facing.getOpposite();
			dx += dir.getStepX() * Math.abs(forward);
			dz += dir.getStepZ() * Math.abs(forward);
		}
		if (right != 0) {
			Direction dir = (right > 0) ? facing.getClockWise() : facing.getCounterClockWise();
			dx += dir.getStepX() * Math.abs(right);
			dz += dir.getStepZ() * Math.abs(right);
		}
		PlacementManager.nudge(dx, up, dz);
		updateButtonStates();
	}

	private void updateButtonStates() {
		boolean hasData = (this.loadedData != null);
		boolean hasPlacement = PlacementManager.hasPlacement();

		if (this.openChecklistButton != null) this.openChecklistButton.active = hasData;
		if (this.showMaterialsButton != null) this.showMaterialsButton.active = hasData;
		if (this.placeHereButton != null) this.placeHereButton.active = hasData;
		if (this.clearPlacementButton != null) this.clearPlacementButton.active = hasPlacement;

		if (this.fwdButton != null) this.fwdButton.active = hasPlacement;
		if (this.backButton != null) this.backButton.active = hasPlacement;
		if (this.leftButton != null) this.leftButton.active = hasPlacement;
		if (this.rightButton != null) this.rightButton.active = hasPlacement;
		if (this.upButton != null) this.upButton.active = hasPlacement;
		if (this.downButton != null) this.downButton.active = hasPlacement;

		if (this.rotLeftButton != null) this.rotLeftButton.active = hasData;
		if (this.rotRightButton != null) this.rotRightButton.active = hasData;
		if (this.flipButton != null) {
			this.flipButton.active = hasData;
			this.flipButton.setMessage(Component.literal(PlacementManager.isMirrored() ? "Flip: ON" : "Flip"));
		}

		LayerMode mode = PlacementManager.getLayerMode();
		boolean notAll = (mode != LayerMode.ALL);
		if (this.layerModeButton != null) {
			this.layerModeButton.active = hasData;
			String modeShort = mode == LayerMode.ALL ? "Mode: All" : (mode == LayerMode.SINGLE ? "Mode: 1" : "Mode: <=");
			this.layerModeButton.setMessage(Component.literal(modeShort));
		}
		if (this.layerUpButton != null) this.layerUpButton.active = hasData && notAll;
		if (this.layerDownButton != null) this.layerDownButton.active = hasData && notAll;
		if (this.layerAllButton != null) this.layerAllButton.active = hasData && notAll;

		if (this.opacityValueButton != null) {
			this.opacityValueButton.setMessage(Component.literal("Opacity: " + ShhhBuilderConfig.getInstance().getPreviewOpacity() + "%"));
		}
	}

	private void openSchematicsFolder() {
		SchematicManager.openSchematicsFolder();
	}

	public void onSchematicSelected(SchematicFileEntry entry) {
		if (entry == null) {
			this.loadedData = null;
			this.loadError = null;
			this.loading = false;
			this.materialScrollOffset = 0;
			updateButtonStates();
			return;
		}

		SchematicManager.setSelectedFileName(entry.fileName());
		ShhhBuilderConfig.getInstance().setHudEnabled(true);
		this.materialScrollOffset = 0;

		SchematicManager.CachedSchematic cached = SchematicManager.getCached(entry.path());
		if (cached != null) {
			this.loading = false;
			this.loadedData = cached.data;
			this.loadError = cached.error;
			updateButtonStates();
			return;
		}

		this.loading = true;
		this.loadError = null;
		this.loadedData = null;
		updateButtonStates();

		Path path = entry.path();
		String selectedName = entry.fileName();

		SchematicManager.loadSchematicAsync(path).thenAccept(result -> {
			if (this.minecraft != null) {
				this.minecraft.execute(() -> {
					if (selectedName.equals(SchematicManager.getSelectedFileName())) {
						this.loading = false;
						this.loadedData = result.data;
						this.loadError = result.error;
						updateButtonStates();
					}
				});
			}
		});
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		// 1. Full-screen dark translucent backdrop to dim the world and hide/mask hotbar/HUD
		extractor.fill(0, 0, this.width, this.height, UiStyle.BG_SCREEN_OVERLAY);

		// Header: Icon and Mod Title
		try {
			extractor.blit(Identifier.fromNamespaceAndPath("shhh-builder", "icon.png"), 12, 8, 16, 16, 0.0f, 0.0f, 16.0f, 16.0f);
		} catch (Throwable ignored) {
			extractor.fakeItem(new ItemStack(Items.STRUCTURE_BLOCK), 12, 8);
		}

		extractor.text(this.font, TITLE, 34, 12, UiStyle.TEXT_TITLE, false);

		LayoutMetrics layout = calculateLayout(this.width, this.height);
		int panelY = layout.panelY();
		int panelHeight = layout.panelHeight();
		int leftWidth = layout.leftWidth();
		int leftX = layout.leftX();
		int rightX = layout.rightX();
		int rightWidth = layout.rightWidth();

		// Draw Left Panel Container
		UiStyle.drawPanel(extractor, leftX, panelY, leftWidth, panelHeight, UiStyle.BG_PANEL, UiStyle.BORDER_NORMAL);

		// Draw Right Panel Container
		UiStyle.drawPanel(extractor, rightX, panelY, rightWidth, panelHeight, UiStyle.BG_PANEL, UiStyle.BORDER_NORMAL);

		// Left Panel Header Text
		extractor.text(this.font, Component.literal("Schematics"), leftX + 8, panelY + 6, UiStyle.TEXT_HEADER, false);

		// Page indicator in left panel footer
		if (!this.filteredFiles.isEmpty()) {
			int maxPages = Math.max(1, (this.filteredFiles.size() + this.itemsPerPage - 1) / this.itemsPerPage);
			int stride = (this.height < 220) ? 20 : 25;
			int listTop = panelY + 36;
			int navY = Math.min(panelY + panelHeight - 14, listTop + (this.itemsPerPage * stride) + 6);
			String pageStr = (this.currentPage + 1) + " / " + maxPages;
			extractor.centeredText(this.font, Component.literal(pageStr), leftX + (leftWidth / 2), navY, UiStyle.TEXT_BODY);
		}

		// Left Panel Empty / Error states
		int listCenterY = panelY + (panelHeight / 2);
		if (this.scanError != null) {
			FriendlyError err = formatFriendlyScanError(this.scanError);
			extractor.centeredText(this.font, Component.literal(err.title), leftX + leftWidth / 2, listCenterY - 14, UiStyle.COLOR_ERROR);
			extractor.centeredText(this.font, Component.literal(err.whatHappened), leftX + leftWidth / 2, listCenterY, UiStyle.TEXT_BODY);
			extractor.centeredText(this.font, Component.literal(err.whatToDo), leftX + leftWidth / 2, listCenterY + 12, UiStyle.TEXT_BODY);
		} else if (this.scannedFiles.isEmpty()) {
			extractor.centeredText(this.font, NO_SCHEMATICS, leftX + leftWidth / 2, listCenterY - 18, UiStyle.TEXT_TITLE);
			extractor.centeredText(this.font, NO_SCHEMATICS_HINT, leftX + leftWidth / 2, listCenterY - 4, UiStyle.TEXT_BODY);
			extractor.centeredText(this.font, Component.literal("Put .litematic or .schem files here"), leftX + leftWidth / 2, listCenterY + 10, UiStyle.TEXT_BODY);
			extractor.centeredText(this.font, Component.translatable("screen.shhh-builder.empty_tutorial_hint"), leftX + leftWidth / 2, listCenterY + 24, UiStyle.COLOR_ACCENT);
		} else if (this.filteredFiles.isEmpty()) {
			extractor.centeredText(this.font, Component.literal("No matches found"), leftX + leftWidth / 2, listCenterY - 8, UiStyle.TEXT_TITLE);
			extractor.centeredText(this.font, Component.literal("No files match '" + this.searchQuery + "'"), leftX + leftWidth / 2, listCenterY + 6, UiStyle.TEXT_BODY);
		}

		// Right Panel Content: Details vs Controls
		if (activeTab == Tab.CONTROLS) {
			renderControlsStatus(extractor, rightX + 8, rightWidth - 16, panelY + 28);
		} else {
			renderDetailsPanel(extractor, rightX + 8, rightWidth - 16, panelY + 28, panelY + panelHeight);
		}

		// Render all widgets (buttons, search box, tabs, action bar) ON TOP of background containers
		super.extractRenderState(extractor, mouseX, mouseY, delta);
	}

	private void renderControlsStatus(GuiGraphicsExtractor extractor, int rightX, int rightWidth, int startY) {
		int y = startY;

		// Details Card Header (Position, Orientation, Layer, Preview Style)
		UiStyle.drawPanel(extractor, rightX, y, rightWidth, 36, UiStyle.BG_CARD, UiStyle.BORDER_LIGHT);

		if (PlacementManager.hasPlacement()) {
			BlockPos p = PlacementManager.getPosition();
			Component posText = Component.literal("Pos: ").append(Component.literal(p.getX() + ", " + p.getY() + ", " + p.getZ()).withStyle(ChatFormatting.AQUA));
			extractor.text(this.font, posText, rightX + 6, y + 4, UiStyle.TEXT_TITLE, false);
		} else {
			extractor.text(this.font, Component.literal("Not placed in world yet.").withStyle(ChatFormatting.GOLD), rightX + 6, y + 4, UiStyle.COLOR_WARNING, false);
		}

		int rot = PlacementManager.getRotation();
		boolean mir = PlacementManager.isMirrored();
		Component rotText = Component.literal("Rot: ").append(Component.literal(rot + "°").withStyle(ChatFormatting.YELLOW))
			.append(Component.literal(" | Mirror: ")).append(Component.literal(mir ? "Yes" : "No").withStyle(mir ? ChatFormatting.GREEN : ChatFormatting.WHITE));
		extractor.text(this.font, rotText, rightX + 6, y + 14, UiStyle.TEXT_BODY, false);

		LayerMode mode = PlacementManager.getLayerMode();
		int layer = PlacementManager.getCurrentLayer();
		int maxLayer = (this.loadedData != null) ? this.loadedData.height() : 1;
		String modeName = (mode == LayerMode.ALL) ? "All layers" : ((mode == LayerMode.SINGLE) ? "Single" : "Up to");
		Component layerComp = Component.literal("Layer: " + layer + "/" + maxLayer + " (" + modeName + ")").withStyle(ChatFormatting.AQUA);
		extractor.text(this.font, layerComp, rightX + 6, y + 24, UiStyle.TEXT_BODY, false);

		// Section divider
		int divY = y + 40;
		extractor.fill(rightX, divY, rightX + rightWidth, divY + 1, UiStyle.BORDER_LIGHT);
	}

	private void renderDetailsPanel(GuiGraphicsExtractor extractor, int rightX, int rightWidth, int startY, int panelBottom) {
		int y = startY;

		String selected = SchematicManager.getSelectedFileName();
		if (selected == null || selected.isEmpty()) {
			UiStyle.drawPanel(extractor, rightX, y, rightWidth, 40, UiStyle.BG_CARD, UiStyle.BORDER_LIGHT);
			extractor.centeredText(this.font, Component.literal("Pick a file from the list."), rightX + rightWidth / 2, y + 15, UiStyle.TEXT_BODY);
			return;
		}

		if (this.loading) {
			UiStyle.drawPanel(extractor, rightX, y, rightWidth, 40, UiStyle.BG_CARD, UiStyle.BORDER_LIGHT);
			extractor.centeredText(this.font, Component.literal("Opening build..."), rightX + rightWidth / 2, y + 15, UiStyle.COLOR_WARNING);
			return;
		}

		if (this.loadError != null) {
			FriendlyError err = formatFriendlyLoadError(this.loadError);
			UiStyle.drawPanel(extractor, rightX, y, rightWidth, 54, UiStyle.BG_CARD, UiStyle.BORDER_ERROR);
			extractor.text(this.font, Component.literal(err.title), rightX + 6, y + 5, UiStyle.COLOR_ERROR, false);
			extractor.text(this.font, Component.literal(err.whatHappened), rightX + 6, y + 18, UiStyle.TEXT_BODY, false);
			extractor.text(this.font, Component.literal(err.whatToDo), rightX + 6, y + 32, UiStyle.TEXT_BODY, false);
			return;
		}

		if (this.loadedData != null) {
			// Details Top Card
			int cardH = 44;
			UiStyle.drawPanel(extractor, rightX, y, rightWidth, cardH, UiStyle.BG_CARD, UiStyle.BORDER_LIGHT);

			String buildName = this.loadedData.name();
			if (buildName == null || buildName.isBlank()) buildName = selected;
			extractor.text(this.font, Component.literal(buildName), rightX + 6, y + 4, UiStyle.TEXT_TITLE, false);

			String author = this.loadedData.author();
			if (author != null && !author.isBlank() && !author.equalsIgnoreCase("unknown")) {
				extractor.text(this.font, Component.literal("by " + author), rightX + 6, y + 15, UiStyle.TEXT_BODY, false);
			}

			// Size and Block count Chips
			String sizeStr = this.loadedData.width() + " x " + this.loadedData.height() + " x " + this.loadedData.length();
			UiStyle.drawChip(extractor, this.font, sizeStr, rightX + 6, y + 26, UiStyle.TEXT_HEADER, UiStyle.BG_CHIP, UiStyle.BORDER_LIGHT);
			int sizeW = this.font.width(sizeStr) + 14;

			String blocksStr = String.format(Locale.ROOT, "%,d", this.loadedData.totalBlocks()) + " blocks";
			UiStyle.drawChip(extractor, this.font, blocksStr, rightX + 6 + sizeW, y + 26, UiStyle.COLOR_SUCCESS, UiStyle.BG_CHIP, UiStyle.BORDER_LIGHT);

			y += cardH + 6;

			// Unobtainable or unknown blocks alert chip
			if (this.loadedData.unobtainableBlocks() > 0) {
				String unobs = String.format(Locale.ROOT, "%,d", this.loadedData.unobtainableBlocks()) + " blocks have no item (water, fire...)";
				extractor.text(this.font, Component.literal(unobs), rightX + 2, y, UiStyle.COLOR_WARNING, false);
				y += 11;
			}
			if (this.loadedData.unknownBlocks() > 0) {
				String unk = String.format(Locale.ROOT, "%,d", this.loadedData.unknownBlocks()) + " blocks are not in this game version";
				extractor.text(this.font, Component.literal(unk), rightX + 2, y, UiStyle.COLOR_ERROR, false);
				y += 11;
			}

			// "Blocks needed:" Section Header
			extractor.text(this.font, Component.literal("Blocks needed:"), rightX + 2, y, UiStyle.TEXT_HEADER, false);
			y += 12;

			// Materials List
			List<Map.Entry<String, Long>> top = this.loadedData.topBlocks();
			int itemRowH = 16;
			int maxAvailableY = panelBottom - 28;
			int visibleCount = Math.max(1, (maxAvailableY - y) / itemRowH);

			int startIdx = Math.min(top.size(), this.materialScrollOffset);
			int endIdx = Math.min(top.size(), startIdx + visibleCount);

			for (int i = startIdx; i < endIdx; i++) {
				Map.Entry<String, Long> entry = top.get(i);
				String blockId = entry.getKey();
				long count = entry.getValue();

				// Item icon
				ItemStack stack = MaterialDisplayHelper.getItemStack(blockId);
				if (!stack.isEmpty()) {
					extractor.fakeItem(stack, rightX + 2, y - 2);
				}

				String friendlyName = MaterialDisplayHelper.formatBlockName(blockId);
				String countWithStacks = UiStyle.formatStacks(count);

				String label = friendlyName + ": ";
				extractor.text(this.font, label, rightX + 22, y + 2, UiStyle.TEXT_HEADER, false);
				int labelW = this.font.width(label);
				extractor.text(this.font, countWithStacks, rightX + 22 + labelW, y + 2, UiStyle.COLOR_ACCENT, false);

				y += itemRowH;
			}

			if (top.size() > endIdx) {
				int rem = top.size() - endIdx;
				extractor.text(this.font, "+ " + rem + " more (scroll to view)", rightX + 22, y + 2, UiStyle.TEXT_BODY, false);
			}
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.searchBox != null && this.searchBox.isFocused()) {
			if (event.key() == 256) { // Escape clears search focus
				this.searchBox.setFocused(false);
				return true;
			}
			return this.searchBox.keyPressed(event);
		}

		if (ShhhBuilderClient.OPEN_SCREEN_KEY.matches(event)) {
			this.onClose();
			return true;
		}
		if (ShhhBuilderClient.MATERIALS_KEY.matches(event)) {
			if (this.minecraft != null) {
				this.minecraft.gui.setScreen(new MaterialsScreen(this));
			}
			return true;
		}
		if (ShhhBuilderClient.FLIP_KEY.matches(event)) {
			PlacementManager.toggleMirrored();
			updateButtonStates();
			return true;
		}
		if (ShhhBuilderClient.CYCLE_LAYER_MODE_KEY.matches(event)) {
			PlacementManager.cycleLayerMode();
			updateButtonStates();
			return true;
		}
		if (ShhhBuilderClient.LAYER_UP_KEY.matches(event)) {
			int maxH = (this.loadedData != null) ? this.loadedData.height() : 256;
			PlacementManager.layerUp(maxH);
			updateButtonStates();
			return true;
		}
		if (ShhhBuilderClient.LAYER_DOWN_KEY.matches(event)) {
			PlacementManager.layerDown();
			updateButtonStates();
			return true;
		}
		if (ShhhBuilderClient.LAYER_ALL_KEY.matches(event)) {
			PlacementManager.resetLayerMode();
			updateButtonStates();
			return true;
		}
		if (ShhhBuilderClient.ROTATE_KEY.matches(event)) {
			PlacementManager.rotateClockwise();
			updateButtonStates();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public static class FriendlyError {
		public final String title;
		public final String whatHappened;
		public final String whatToDo;

		public FriendlyError(String title, String whatHappened, String whatToDo) {
			this.title = title;
			this.whatHappened = whatHappened;
			this.whatToDo = whatToDo;
		}
	}

	public static FriendlyError formatFriendlyLoadError(String rawError) {
		if (rawError == null || rawError.isBlank()) {
			return new FriendlyError("Could not open build", "Something went wrong while opening this file.", "Try refreshing or picking another file.");
		}
		String lower = rawError.toLowerCase(Locale.ROOT);
		if (lower.contains("empty") || lower.contains("0 bytes")) {
			return new FriendlyError("File is empty", "This build file contains no data (0 bytes).", "Try downloading or saving the build again.");
		}
		if (lower.contains("exceeds") || lower.contains("50 mb") || lower.contains("too large")) {
			return new FriendlyError("File is too large", "This file is over 50 MB, which is too big to load safely.", "Try splitting the build into smaller parts.");
		}
		if (lower.contains("not exist") || lower.contains("not found")) {
			return new FriendlyError("File not found", "This file seems to have been moved or deleted.", "Click 'Refresh' below to update your file list.");
		}
		if (lower.contains("unsupported") || lower.contains("missing 'regions'") || lower.contains("blockpalette") || lower.contains("palette tag")) {
			return new FriendlyError("Unsupported version", "This schematic format version could not be read.", "Try saving it with a newer version of Litematica or WorldEdit.");
		}
		if (lower.contains("corrupt") || lower.contains("nbt") || lower.contains("varint") || lower.contains("eof") || lower.contains("unexpected end")) {
			return new FriendlyError("Damaged file", "The file data appears corrupted or incomplete.", "Try downloading it again or re-exporting it.");
		}
		return new FriendlyError("Could not open build", "Something unexpected happened while opening this build.", "Check your game log for technical details.");
	}

	public static FriendlyError formatFriendlyScanError(String rawError) {
		return new FriendlyError("Folder unavailable", "Could not read your schematics folder.", "Click 'Open Folder' below to check it, then press Refresh.");
	}
}
