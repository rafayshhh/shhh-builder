package io.github.rafayshhh.shhhbuilder.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import io.github.rafayshhh.shhhbuilder.client.command.ShhhBuilderCommand;
import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderScreen;
import io.github.rafayshhh.shhhbuilder.client.hud.GhostBlockRaycastHelper;
import io.github.rafayshhh.shhhbuilder.client.hud.MaterialHudElement;
import io.github.rafayshhh.shhhbuilder.client.material.MaterialProgressManager;
import io.github.rafayshhh.shhhbuilder.client.render.BoxOutlineRenderer;
import io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class ShhhBuilderClient implements ClientModInitializer {
	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(ShhhBuilder.MOD_ID, "main")
	);

	public static final KeyMapping OPEN_SCREEN_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.open_screen",
			InputConstants.KEY_V,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping TOGGLE_HUD_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.toggle_hud",
			InputConstants.KEY_H,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping TOGGLE_OUTLINE_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.toggle_outline",
			InputConstants.KEY_B,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping TOGGLE_GHOST_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.toggle_ghost",
			InputConstants.KEY_G,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping MATERIALS_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.materials",
			InputConstants.KEY_K,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping ROTATE_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.rotate",
			InputConstants.KEY_R,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping FLIP_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.flip",
			InputConstants.KEY_Z,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping CYCLE_LAYER_MODE_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.cycle_layer_mode",
			InputConstants.KEY_M,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping LAYER_UP_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.layer_up",
			InputConstants.KEY_RBRACKET,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping LAYER_DOWN_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.layer_down",
			InputConstants.KEY_LBRACKET,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping LAYER_ALL_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.layer_all",
			InputConstants.KEY_BACKSLASH,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping NUDGE_FORWARD_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.nudge_forward",
			InputConstants.KEY_UP,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping NUDGE_BACKWARD_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.nudge_backward",
			InputConstants.KEY_DOWN,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping NUDGE_LEFT_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.nudge_left",
			InputConstants.KEY_LEFT,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping NUDGE_RIGHT_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.nudge_right",
			InputConstants.KEY_RIGHT,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping NUDGE_UP_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.nudge_up",
			InputConstants.KEY_PAGEUP,
			KEY_CATEGORY
		)
	);

	public static final KeyMapping NUDGE_DOWN_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.shhh-builder.nudge_down",
			InputConstants.KEY_PAGEDOWN,
			KEY_CATEGORY
		)
	);

	@Override
	public void onInitializeClient() {
		// Load persistent user configuration
		ShhhBuilderConfig.getInstance().load();

		ensureSchematicsDirectory();
		// Initial schematics scan at startup with full directory logging (Requirement 2)
		SchematicManager.scanSchematics(null);

		registerKeyBindings();
		registerHud();
		BoxOutlineRenderer.register();
		GhostBlockRenderer.register();

		// Register client-side commands (/shhhbuilder)
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			ShhhBuilderCommand.register(dispatcher);
		});

		// Clear in-world placement, cached block transforms, and active schematic session when leaving a world/server
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			cleanupWorldSession();
		});

		// Ensure everything is cleanly flushed and unloaded when client exits
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			cleanupWorldSession();
		});

		ShhhBuilder.LOGGER.info("Shhh Builder client initialized.");
	}

	/**
	 * Thoroughly clears active placements, cached visible block transforms, and unloads
	 * the active schematic session so joining a new multiplayer server or singleplayer world
	 * never carries over ghost blocks, bounding boxes, or state from a previous world.
	 * Also safely flushes pending checklist progress to disk.
	 */
	public static void cleanupWorldSession() {
		try {
			PlacementManager.clear();
			GhostBlockRenderer.resetOnDisconnect();
			SchematicManager.setSelectedFileName(null);
			MaterialProgressManager.flush();
		} catch (Throwable t) {
			ShhhBuilder.LOGGER.error("Error during world session cleanup: {}", t.getMessage(), t);
		}
	}

	private void ensureSchematicsDirectory() {
		Path schematicsDir = SchematicManager.getSchematicsDirectory();
		if (schematicsDir != null && !Files.exists(schematicsDir)) {
			try {
				Files.createDirectories(schematicsDir);
				ShhhBuilder.LOGGER.info("Created schematics directory at: {}", schematicsDir);
			} catch (IOException e) {
				ShhhBuilder.LOGGER.error("Failed to create schematics directory: {}", schematicsDir, e);
			}
		}

		checkAndDeploySampleHouse(schematicsDir);
	}

	public static void checkAndDeploySampleHouse(Path schematicsDir) {
		if (schematicsDir == null || !Files.exists(schematicsDir)) {
			return;
		}

		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
		if (config.isSampleHouseCopied()) {
			return;
		}

		// Check if schematics folder contains any .litematic or .schem files
		boolean hasSchematics = false;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(schematicsDir)) {
			for (Path p : stream) {
				String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
				if (name.endsWith(".litematic") || name.endsWith(".schem")) {
					hasSchematics = true;
					break;
				}
			}
		} catch (Exception e) {
			ShhhBuilder.LOGGER.error("Failed to inspect schematics directory for sample deployment", e);
			return;
		}

		// If no schematics exist and flag is false, copy sample_house.litematic
		if (!hasSchematics) {
			Path sampleDest = schematicsDir.resolve("sample_house.litematic");
			try (InputStream in = ShhhBuilderClient.class.getResourceAsStream("/assets/shhh-builder/sample/sample_house.litematic")) {
				if (in != null) {
					Files.copy(in, sampleDest, StandardCopyOption.REPLACE_EXISTING);
					ShhhBuilder.LOGGER.info("Deployed starter sample house schematic to {}", sampleDest);
				} else {
					ShhhBuilder.LOGGER.warn("Bundled sample_house.litematic resource not found in jar assets.");
				}
			} catch (Exception e) {
				ShhhBuilder.LOGGER.error("Failed to copy sample_house.litematic into schematics folder", e);
			}
		}

		// Always set flag and save so we never re-copy if user deletes it later
		config.setSampleHouseCopied(true);
		config.save();
	}

	public static String getKeyName(net.minecraft.client.KeyMapping keyMapping, String fallback) {
		if (keyMapping != null) {
			try {
				Component msg = keyMapping.getTranslatedKeyMessage();
				if (msg != null) {
					String str = msg.getString();
					if (str != null && !str.isBlank()) {
						return str;
					}
				}
			} catch (Throwable ignored) {
			}
		}
		return fallback;
	}

	private void registerKeyBindings() {
		// Ghost Pick-Block: intercepts pick-block when looking at an unplaced preview block within 5 blocks
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.gui != null && client.gui.screen() != null) {
				return;
			}
			if (client.player == null || client.level == null || client.options == null || client.options.keyPickItem == null) {
				return;
			}
			if (!PlacementManager.hasPlacement() || !PlacementManager.isGhostVisible()) {
				return;
			}

			GhostBlockRaycastHelper.GhostHit targetHit = GhostBlockRaycastHelper.raycast(client);
			if (targetHit != null && (targetHit.status() == WorldBlockValidator.MatchStatus.MISSING
					|| targetHit.status() == WorldBlockValidator.MatchStatus.WRONG_STATE
					|| targetHit.status() == WorldBlockValidator.MatchStatus.WRONG_BLOCK)) {
				while (client.options.keyPickItem.consumeClick()) {
					GhostBlockRaycastHelper.performGhostPickBlock(client, targetHit);
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// First-run help message (shown once)
			if (client.player != null && !ShhhBuilderConfig.getInstance().isFirstRunHelpShown()) {
				ShhhBuilderConfig.getInstance().setFirstRunHelpShown(true);
				ShhhBuilderConfig.getInstance().save();

				String openKey = getKeyName(OPEN_SCREEN_KEY, "V");
				Component openTutorialBtn = Component.literal("[Open tutorial]")
					.withStyle(style -> style
						.withColor(TextColor.fromRgb(0x00E5FF))
						.withUnderlined(true)
						.withClickEvent(new ClickEvent.RunCommand("/shhhbuilder tutorial"))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open the step-by-step tutorial")))
					);

				Component chatMsg = Component.literal("Shhh Builder: press ")
					.withStyle(ChatFormatting.GRAY)
					.append(Component.literal(openKey).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
					.append(Component.literal(" to open, or ").withStyle(ChatFormatting.GRAY))
					.append(openTutorialBtn)
					.append(Component.literal(".").withStyle(ChatFormatting.GRAY));

				client.player.sendSystemMessage(chatMsg);
				client.player.sendOverlayMessage(Component.translatable("message.shhh-builder.first_run", openKey));
			}

			// If any screen (chat, inventory, container, pause menu) is open, do not execute builder shortcuts
			if (client.gui != null && client.gui.screen() != null) {
				drainKeyMappings();
				return;
			}

			String openKey = getKeyName(OPEN_SCREEN_KEY, "V");

			// V key: open main schematic picker screen
			while (OPEN_SCREEN_KEY.consumeClick()) {
				if (client.gui != null && client.gui.screen() == null) {
					client.gui.setScreen(new ShhhBuilderScreen());
				}
			}

			// K key: open materials checklist screen
			while (MATERIALS_KEY.consumeClick()) {
				if (client.gui != null && client.gui.screen() == null) {
					client.gui.setScreen(new io.github.rafayshhh.shhhbuilder.client.gui.MaterialsScreen(null));
				}
			}

			// H key: toggle material list HUD
			while (TOGGLE_HUD_KEY.consumeClick()) {
				ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
				boolean newState = !config.isHudEnabled();
				config.setHudEnabled(newState);
				config.save();

				if (client.player != null) {
					client.player.sendOverlayMessage(
						Component.literal("Blocks needed HUD: " + (newState ? "Shown" : "Hidden"))
							.withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED)
					);
				}
			}

			// B key: toggle outline box
			while (TOGGLE_OUTLINE_KEY.consumeClick()) {
				if (!PlacementManager.hasPlacement()) {
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("No build placed yet. Press " + openKey + " to pick one.")
								.withStyle(ChatFormatting.YELLOW)
						);
					}
				} else {
					boolean newState = PlacementManager.toggleVisible();
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("Outline box: " + (newState ? "Shown" : "Hidden"))
								.withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED)
						);
					}
				}
			}

			// G key: toggle ghost block preview
			while (TOGGLE_GHOST_KEY.consumeClick()) {
				if (!PlacementManager.hasPlacement()) {
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("No build placed yet. Press " + openKey + " to pick one.")
								.withStyle(ChatFormatting.YELLOW)
						);
					}
				} else {
					LitematicData data = SchematicManager.getLoadedData();
					if (data != null && data.blockData() != null && !data.blockData().canPreview()) {
						if (client.player != null) {
							client.player.sendOverlayMessage(
								Component.literal("Preview unavailable: " + data.blockData().previewDisabledReason())
									.withStyle(ChatFormatting.RED)
							);
						}
					} else {
						boolean newState = PlacementManager.toggleGhostVisible();
						if (client.player != null) {
							client.player.sendOverlayMessage(
								Component.literal("Preview blocks: " + (newState ? "Shown" : "Hidden"))
									.withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED)
							);
						}
					}
				}
			}

			// R key: rotate schematic 90 degrees
			while (ROTATE_KEY.consumeClick()) {
				if (!PlacementManager.hasPlacement()) {
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("No build placed yet. Press " + openKey + " to pick one.")
								.withStyle(ChatFormatting.YELLOW)
						);
					}
				} else {
					int rot = PlacementManager.rotateClockwise();
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("Turned 90° clockwise (" + rot + "°)")
								.withStyle(ChatFormatting.AQUA)
						);
					}
				}
			}

			// Z key: flip / mirror schematic left-to-right
			while (FLIP_KEY.consumeClick()) {
				if (!PlacementManager.hasPlacement()) {
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("No build placed yet. Press " + openKey + " to pick one.")
								.withStyle(ChatFormatting.YELLOW)
						);
					}
				} else {
					boolean flipped = PlacementManager.toggleMirrored();
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("Flip left-right: " + (flipped ? "ON" : "OFF"))
								.withStyle(ChatFormatting.AQUA)
						);
					}
				}
			}

			// M key: cycle layer mode (All -> Single -> Up-to)
			while (CYCLE_LAYER_MODE_KEY.consumeClick()) {
				if (!PlacementManager.hasPlacement()) {
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("No build placed yet. Press " + openKey + " to pick one.")
								.withStyle(ChatFormatting.YELLOW)
						);
					}
				} else {
					LayerMode mode = PlacementManager.cycleLayerMode();
					LitematicData data = SchematicManager.getLoadedData();
					int h = data != null ? data.height() : 1;
					if (client.player != null) {
						String msg = switch (mode) {
							case ALL -> "Layer mode: All layers";
							case SINGLE -> "Layer " + PlacementManager.getCurrentLayer() + " / " + h + " (one layer)";
							case UP_TO -> "Layers 1 to " + PlacementManager.getCurrentLayer() + " / " + h;
						};
						client.player.sendOverlayMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
					}
				}
			}

			// ] key: layer up
			while (LAYER_UP_KEY.consumeClick()) {
				if (!PlacementManager.hasPlacement()) {
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("No build placed yet. Press " + openKey + " to pick one.")
								.withStyle(ChatFormatting.YELLOW)
						);
					}
				} else {
					LitematicData data = SchematicManager.getLoadedData();
					int h = data != null ? data.height() : 1;
					if (PlacementManager.getLayerMode() == LayerMode.ALL) {
						PlacementManager.setLayerMode(LayerMode.SINGLE);
					}
					int layer = PlacementManager.layerUp(h);
					String modeName = PlacementManager.getLayerMode() == LayerMode.SINGLE ? "one layer" : "up to layer";
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("Layer " + layer + " / " + h + " (" + modeName + ")")
								.withStyle(ChatFormatting.AQUA)
						);
					}
				}
			}

			// [ key: layer down
			while (LAYER_DOWN_KEY.consumeClick()) {
				if (!PlacementManager.hasPlacement()) {
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("No build placed yet. Press " + openKey + " to pick one.")
								.withStyle(ChatFormatting.YELLOW)
						);
					}
				} else {
					LitematicData data = SchematicManager.getLoadedData();
					int h = data != null ? data.height() : 1;
					if (PlacementManager.getLayerMode() == LayerMode.ALL) {
						PlacementManager.setLayerMode(LayerMode.SINGLE);
					}
					int layer = PlacementManager.layerDown();
					String modeName = PlacementManager.getLayerMode() == LayerMode.SINGLE ? "one layer" : "up to layer";
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("Layer " + layer + " / " + h + " (" + modeName + ")")
								.withStyle(ChatFormatting.AQUA)
						);
					}
				}
			}

			// \ key: show all layers
			while (LAYER_ALL_KEY.consumeClick()) {
				if (!PlacementManager.hasPlacement()) {
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("No build placed yet. Press " + openKey + " to pick one.")
								.withStyle(ChatFormatting.YELLOW)
						);
					}
				} else {
					PlacementManager.resetLayerMode();
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.literal("Layer mode: All layers")
								.withStyle(ChatFormatting.AQUA)
						);
					}
				}
			}

			// Movement nudge keys
			while (NUDGE_FORWARD_KEY.consumeClick()) {
				handleNudge(client, client.player != null ? client.player.getDirection() : Direction.NORTH, "Forward");
			}

			while (NUDGE_BACKWARD_KEY.consumeClick()) {
				handleNudge(client, client.player != null ? client.player.getDirection().getOpposite() : Direction.SOUTH, "Backward");
			}

			while (NUDGE_LEFT_KEY.consumeClick()) {
				handleNudge(client, client.player != null ? client.player.getDirection().getCounterClockWise() : Direction.WEST, "Left");
			}

			while (NUDGE_RIGHT_KEY.consumeClick()) {
				handleNudge(client, client.player != null ? client.player.getDirection().getClockWise() : Direction.EAST, "Right");
			}

			while (NUDGE_UP_KEY.consumeClick()) {
				handleNudge(client, Direction.UP, "Up");
			}

			while (NUDGE_DOWN_KEY.consumeClick()) {
				handleNudge(client, Direction.DOWN, "Down");
			}
		});
	}

	private void handleNudge(net.minecraft.client.Minecraft client, Direction direction, String label) {
		if (client.player == null) {
			return;
		}

		if (!PlacementManager.hasPlacement()) {
			String openKey = getKeyName(OPEN_SCREEN_KEY, "V");
			client.player.sendOverlayMessage(
				Component.literal("No build placed yet. Press " + openKey + " to pick one.")
					.withStyle(ChatFormatting.YELLOW)
			);
			return;
		}

		PlacementManager.nudge(direction);
		BlockPos pos = PlacementManager.getPosition();
		client.player.sendOverlayMessage(
			Component.literal("Moved " + label + " -> " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
				.withStyle(ChatFormatting.WHITE)
		);
	}

	private void drainKeyMappings() {
		while (OPEN_SCREEN_KEY.consumeClick()) {}
		while (MATERIALS_KEY.consumeClick()) {}
		while (TOGGLE_HUD_KEY.consumeClick()) {}
		while (TOGGLE_OUTLINE_KEY.consumeClick()) {}
		while (TOGGLE_GHOST_KEY.consumeClick()) {}
		while (ROTATE_KEY.consumeClick()) {}
		while (FLIP_KEY.consumeClick()) {}
		while (CYCLE_LAYER_MODE_KEY.consumeClick()) {}
		while (LAYER_DOWN_KEY.consumeClick()) {}
		while (LAYER_UP_KEY.consumeClick()) {}
		while (LAYER_ALL_KEY.consumeClick()) {}
		while (NUDGE_FORWARD_KEY.consumeClick()) {}
		while (NUDGE_BACKWARD_KEY.consumeClick()) {}
		while (NUDGE_LEFT_KEY.consumeClick()) {}
		while (NUDGE_RIGHT_KEY.consumeClick()) {}
		while (NUDGE_UP_KEY.consumeClick()) {}
		while (NUDGE_DOWN_KEY.consumeClick()) {}
	}

	private void registerHud() {
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath(ShhhBuilder.MOD_ID, "material_hud"),
			new MaterialHudElement()
		);
	}
}
