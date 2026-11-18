package io.github.rafayshhh.shhhbuilder.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import io.github.rafayshhh.shhhbuilder.client.LayerMode;
import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import io.github.rafayshhh.shhhbuilder.client.SchematicFileEntry;
import io.github.rafayshhh.shhhbuilder.client.SchematicManager;
import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.schematic.BlockCategoryHelper;
import io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper;
import io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Client-side command set for Shhh Builder.
 * Registered via Fabric API's ClientCommandRegistrationCallback into the client-side command dispatcher.
 * Purely client-side: commands are handled locally and never transmitted to multiplayer servers.
 */
public class ShhhBuilderCommand {

	public record FileResolveResult(SchematicFileEntry entry, String error) {
		public boolean isSuccess() {
			return entry != null;
		}
	}

	public record CoordValidationResult(boolean valid, String error) {}

	public record LayerValidationResult(boolean valid, boolean isAll, int layerNumber, String error) {}

	/**
	 * Strips known schematic extensions (.litematic or .schem) from a file name.
	 */
	public static String stripExtension(String name) {
		if (name == null) return "";
		String lower = name.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".litematic")) {
			return name.substring(0, name.length() - ".litematic".length());
		} else if (lower.endsWith(".schem")) {
			return name.substring(0, name.length() - ".schem".length());
		}
		return name;
	}

	/**
	 * Sanitizes user input argument (trims whitespace and strips surrounding quotes).
	 */
	public static String cleanArg(String arg) {
		if (arg == null) return "";
		arg = arg.trim();
		if (arg.length() >= 2 && arg.startsWith("\"") && arg.endsWith("\"")) {
			arg = arg.substring(1, arg.length() - 1).trim();
		}
		return arg;
	}

	/**
	 * Computes tab completion suggestions for schematic files:
	 * Suggests base names without extensions where unambiguous, and full names when ambiguous
	 * or when the user is explicitly typing an extension.
	 */
	public static List<String> computeSuggestions(List<SchematicFileEntry> files, String remaining) {
		if (files == null || files.isEmpty()) {
			return Collections.emptyList();
		}
		String remLower = remaining != null ? remaining.toLowerCase(Locale.ROOT) : "";
		boolean typingExtension = remLower.contains(".");

		// Count occurrences of each base name (case-insensitive)
		Map<String, Integer> baseCounts = new HashMap<>();
		for (SchematicFileEntry f : files) {
			String base = stripExtension(f.fileName()).toLowerCase(Locale.ROOT);
			baseCounts.put(base, baseCounts.getOrDefault(base, 0) + 1);
		}

		Set<String> result = new LinkedHashSet<>();
		for (SchematicFileEntry f : files) {
			String base = stripExtension(f.fileName());
			String full = f.fileName();
			int count = baseCounts.getOrDefault(base.toLowerCase(Locale.ROOT), 0);

			if (count == 1 && !typingExtension) {
				// Unambiguous: suggest base name without extension
				if (base.toLowerCase(Locale.ROOT).startsWith(remLower)) {
					result.add(base);
				}
			} else {
				// Ambiguous or user is typing an extension: suggest full name
				if (full.toLowerCase(Locale.ROOT).startsWith(remLower)) {
					result.add(full);
				}
			}
		}

		List<String> list = new ArrayList<>(result);
		list.sort(String.CASE_INSENSITIVE_ORDER);
		return list;
	}

	/**
	 * Resolves a user-provided file name string to an actual schematic file.
	 * Supports exact full name matches, unambiguous base name matches, and detects ambiguous matches.
	 */
	public static FileResolveResult resolveFile(String input, List<SchematicFileEntry> files) {
		if (input == null || input.isBlank()) {
			return new FileResolveResult(null, "Please specify a file name: /shhhbuilder select <file>");
		}
		String cleaned = cleanArg(input);
		if (cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\")) {
			return new FileResolveResult(null, "Invalid file name: path traversal characters ('..', '/', '\\') are not allowed.");
		}
		if (files == null || files.isEmpty()) {
			return new FileResolveResult(null, "No schematics found in the schematics folder. Put .litematic or .schem files there first.");
		}

		// 1. Exact match on full file name (case-insensitive)
		for (SchematicFileEntry f : files) {
			if (f.fileName().equalsIgnoreCase(cleaned)) {
				return new FileResolveResult(f, null);
			}
		}

		// 2. Exact match on base name (case-insensitive)
		List<SchematicFileEntry> baseMatches = new ArrayList<>();
		for (SchematicFileEntry f : files) {
			String base = stripExtension(f.fileName());
			if (base.equalsIgnoreCase(cleaned)) {
				baseMatches.add(f);
			}
		}

		if (baseMatches.size() == 1) {
			return new FileResolveResult(baseMatches.get(0), null);
		} else if (baseMatches.size() > 1) {
			StringBuilder sb = new StringBuilder("Multiple files match '").append(cleaned).append("': ");
			for (int i = 0; i < baseMatches.size(); i++) {
				if (i > 0) sb.append(" or ");
				sb.append("'").append(baseMatches.get(i).fileName()).append("'");
			}
			sb.append(". Specify the extension to select.");
			return new FileResolveResult(null, sb.toString());
		}

		return new FileResolveResult(null, "Schematic not found: '" + cleaned + "'. Type '/shhhbuilder list' to see available files.");
	}

	/**
	 * Validates coordinates against world height limits and world border.
	 */
	public static CoordValidationResult validateCoordinates(int x, int y, int z, int minY, int maxY) {
		if (Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000) {
			return new CoordValidationResult(false, "Coordinates (" + x + ", " + z + ") are outside the world limits (±30,000,000). Try coordinates closer to the world center.");
		}
		if (y < minY || y > maxY) {
			return new CoordValidationResult(false, "Y coordinate " + y + " is outside world height limits (" + minY + " to " + maxY + "). Try a Y value between " + minY + " and " + maxY + ".");
		}
		return new CoordValidationResult(true, null);
	}

	/**
	 * Validates layer argument ("all" or a 1-based integer within build height).
	 */
	public static LayerValidationResult validateLayer(String arg, int maxHeight) {
		if (arg == null || arg.isBlank()) {
			return new LayerValidationResult(false, false, 0, "Please specify a layer mode or number: /shhhbuilder layer <all|number>");
		}
		String cleaned = arg.trim();
		if (cleaned.equalsIgnoreCase("all")) {
			return new LayerValidationResult(true, true, 0, null);
		}
		int layerNum;
		try {
			layerNum = Integer.parseInt(cleaned);
		} catch (NumberFormatException e) {
			return new LayerValidationResult(false, false, 0, "Invalid layer: '" + cleaned + "'. Expected 'all' or a layer number (e.g. '/shhhbuilder layer 1' or '/shhhbuilder layer all').");
		}
		int safeMax = Math.max(1, maxHeight);
		if (layerNum < 1 || layerNum > safeMax) {
			return new LayerValidationResult(false, false, 0, "Layer " + layerNum + " is out of range. The build height is " + safeMax + " (valid layers: 1 to " + safeMax + "). Try a number between 1 and " + safeMax + ".");
		}
		return new LayerValidationResult(true, false, layerNum, null);
	}

	/**
	 * Registers the root /shhhbuilder command and all subcommands into the dispatcher.
	 */
	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		var root = ClientCommands.literal("shhhbuilder")
			.executes(ctx -> executeHelp(ctx.getSource()));

		// /shhhbuilder help
		root.then(ClientCommands.literal("help")
			.executes(ctx -> executeHelp(ctx.getSource())));

		// /shhhbuilder list
		root.then(ClientCommands.literal("list")
			.executes(ctx -> executeList(ctx.getSource())));

		// /shhhbuilder select <file>
		root.then(ClientCommands.literal("select")
			.executes(ctx -> {
				ctx.getSource().sendError(Component.literal("Usage: /shhhbuilder select <file> (e.g. '/shhhbuilder select sample_house').").withStyle(ChatFormatting.RED));
				return 0;
			})
			.then(ClientCommands.argument("file", StringArgumentType.greedyString())
				.suggests((ctx, builder) -> getFileSuggestions(builder))
				.executes(ctx -> executeSelect(ctx.getSource(), StringArgumentType.getString(ctx, "file")))));

		// /shhhbuilder place and /shhhbuilder place <x> <y> <z>
		var placeZ = ClientCommands.argument("z", IntegerArgumentType.integer())
			.executes(ctx -> executePlace(ctx.getSource(), new BlockPos(
				IntegerArgumentType.getInteger(ctx, "x"),
				IntegerArgumentType.getInteger(ctx, "y"),
				IntegerArgumentType.getInteger(ctx, "z")
			)));
		var placeY = ClientCommands.argument("y", IntegerArgumentType.integer())
			.executes(ctx -> {
				ctx.getSource().sendError(Component.literal("Usage: /shhhbuilder place <x> <y> <z> (e.g. '/shhhbuilder place 100 64 -200').").withStyle(ChatFormatting.RED));
				return 0;
			})
			.then(placeZ);
		var placeX = ClientCommands.argument("x", IntegerArgumentType.integer())
			.executes(ctx -> {
				ctx.getSource().sendError(Component.literal("Usage: /shhhbuilder place <x> <y> <z> (e.g. '/shhhbuilder place 100 64 -200').").withStyle(ChatFormatting.RED));
				return 0;
			})
			.then(placeY);
		var placeCmd = ClientCommands.literal("place")
			.executes(ctx -> executePlace(ctx.getSource(), null))
			.then(placeX);
		root.then(placeCmd);

		// /shhhbuilder move <x> <y> <z>
		var moveDz = ClientCommands.argument("dz", IntegerArgumentType.integer())
			.executes(ctx -> executeMove(
				ctx.getSource(),
				IntegerArgumentType.getInteger(ctx, "dx"),
				IntegerArgumentType.getInteger(ctx, "dy"),
				IntegerArgumentType.getInteger(ctx, "dz")
			));
		var moveDy = ClientCommands.argument("dy", IntegerArgumentType.integer())
			.executes(ctx -> {
				ctx.getSource().sendError(Component.literal("Usage: /shhhbuilder move <x> <y> <z> (e.g. '/shhhbuilder move 0 1 0' to move up 1 block).").withStyle(ChatFormatting.RED));
				return 0;
			})
			.then(moveDz);
		var moveDx = ClientCommands.argument("dx", IntegerArgumentType.integer())
			.executes(ctx -> {
				ctx.getSource().sendError(Component.literal("Usage: /shhhbuilder move <x> <y> <z> (e.g. '/shhhbuilder move 0 1 0' to move up 1 block).").withStyle(ChatFormatting.RED));
				return 0;
			})
			.then(moveDy);
		var moveCmd = ClientCommands.literal("move")
			.executes(ctx -> {
				ctx.getSource().sendError(Component.literal("Usage: /shhhbuilder move <x> <y> <z> (e.g. '/shhhbuilder move 0 1 0' to move up 1 block).").withStyle(ChatFormatting.RED));
				return 0;
			})
			.then(moveDx);
		root.then(moveCmd);

		// /shhhbuilder rotate
		root.then(ClientCommands.literal("rotate")
			.executes(ctx -> executeRotate(ctx.getSource())));

		// /shhhbuilder flip
		root.then(ClientCommands.literal("flip")
			.executes(ctx -> executeFlip(ctx.getSource())));

		// /shhhbuilder layer <all|number>
		root.then(ClientCommands.literal("layer")
			.executes(ctx -> {
				ctx.getSource().sendError(Component.literal("Usage: /shhhbuilder layer <all|number> (e.g. '/shhhbuilder layer 1' or '/shhhbuilder layer all').").withStyle(ChatFormatting.RED));
				return 0;
			})
			.then(ClientCommands.argument("layer", StringArgumentType.word())
				.suggests((ctx, builder) -> getLayerSuggestions(builder))
				.executes(ctx -> executeLayer(ctx.getSource(), StringArgumentType.getString(ctx, "layer")))));

		// /shhhbuilder clear
		root.then(ClientCommands.literal("clear")
			.executes(ctx -> executeClear(ctx.getSource())));

		// /shhhbuilder folder
		root.then(ClientCommands.literal("folder")
			.executes(ctx -> executeFolder(ctx.getSource())));

		// /shhhbuilder tutorial
		root.then(ClientCommands.literal("tutorial")
			.executes(ctx -> executeTutorial(ctx.getSource())));

		// /shhhbuilder materials and /shhhbuilder checklist
		root.then(ClientCommands.literal("materials")
			.executes(ctx -> executeMaterials(ctx.getSource())));
		root.then(ClientCommands.literal("checklist")
			.executes(ctx -> executeMaterials(ctx.getSource())));

		// /shhhbuilder selftest
		root.then(ClientCommands.literal("selftest")
			.executes(ctx -> executeSelfTest(ctx.getSource())));

		dispatcher.register(root);
	}

	private static CompletableFuture<Suggestions> getFileSuggestions(SuggestionsBuilder builder) {
		List<SchematicFileEntry> files = SchematicManager.scanSchematics(null);
		List<String> suggestions = computeSuggestions(files, builder.getRemainingLowerCase());
		for (String s : suggestions) {
			builder.suggest(s);
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> getLayerSuggestions(SuggestionsBuilder builder) {
		builder.suggest("all");
		LitematicData data = SchematicManager.getLoadedData();
		int height = data != null ? data.height() : 5;
		int limit = Math.min(height, 20);
		for (int i = 1; i <= limit; i++) {
			builder.suggest(String.valueOf(i));
		}
		return builder.buildFuture();
	}

	public static int executeHelp(FabricClientCommandSource source) {
		source.sendFeedback(Component.literal("Shhh Builder Commands:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		source.sendFeedback(Component.literal("/shhhbuilder help").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Show this list of commands").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder list").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - List all schematics in your folder").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder select <file>").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Select and load a schematic").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder place").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Place build where you are standing").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder place <x> <y> <z>").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Place build at coordinates (x, y, z)").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder move <x> <y> <z>").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Move placement by offset (dx, dy, dz)").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder rotate").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Turn placement 90° clockwise").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder flip").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Flip placement left-to-right").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder layer <all|number>").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Show all layers or a specific layer").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder clear").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Clear build preview from the world").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder folder").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Open schematics folder in file explorer").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder tutorial").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Open the step-by-step tutorial").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder materials").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Open the materials checklist screen").withStyle(ChatFormatting.WHITE)));
		source.sendFeedback(Component.literal("/shhhbuilder selftest").withStyle(ChatFormatting.YELLOW)
			.append(Component.literal(" - Test all registered blocks for preview & material support").withStyle(ChatFormatting.WHITE)));
		return 1;
	}

	public static int executeMaterials(FabricClientCommandSource source) {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null) {
			mc.execute(() -> {
				mc.gui.setScreen(new io.github.rafayshhh.shhhbuilder.client.gui.MaterialsScreen(mc.gui.screen()));
			});
			source.sendFeedback(Component.literal("Opening materials checklist...").withStyle(ChatFormatting.AQUA));
			return 1;
		}
		return 0;
	}

	public static int executeTutorial(FabricClientCommandSource source) {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null) {
			mc.execute(() -> {
				mc.gui.setScreen(new io.github.rafayshhh.shhhbuilder.client.gui.TutorialScreen(mc.gui.screen()));
			});
			source.sendFeedback(Component.literal("Opening tutorial...").withStyle(ChatFormatting.AQUA));
			return 1;
		}
		return 0;
	}

	public static int executeList(FabricClientCommandSource source) {
		List<SchematicFileEntry> files = SchematicManager.scanSchematics(null);
		if (files.isEmpty()) {
			source.sendFeedback(Component.literal("No schematics found in the schematics folder. Put .litematic or .schem files in your schematics folder and try again (use '/shhhbuilder folder' to open it).").withStyle(ChatFormatting.YELLOW));
			return 0;
		}

		String current = SchematicManager.getSelectedFileName();
		source.sendFeedback(Component.literal("Schematics in folder (" + files.size() + "):").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		for (SchematicFileEntry f : files) {
			boolean isSelected = f.fileName().equalsIgnoreCase(current);
			String prefix = isSelected ? " ▶ " : "   ";
			ChatFormatting color = isSelected ? ChatFormatting.GREEN : ChatFormatting.WHITE;
			source.sendFeedback(Component.literal(prefix + f.fileName()).withStyle(color));
		}
		source.sendFeedback(Component.literal("Type '/shhhbuilder select <file>' to pick one.").withStyle(ChatFormatting.GRAY));
		return files.size();
	}

	public static int executeSelect(FabricClientCommandSource source, String fileArg) {
		List<SchematicFileEntry> files = SchematicManager.scanSchematics(null);
		FileResolveResult resolved = resolveFile(fileArg, files);
		if (!resolved.isSuccess()) {
			source.sendError(Component.literal(resolved.error()).withStyle(ChatFormatting.RED));
			return 0;
		}

		SchematicFileEntry entry = resolved.entry();
		SchematicManager.setSelectedFileName(entry.fileName());
		ShhhBuilderConfig.getInstance().setHudEnabled(true);

		// Check if already cached
		SchematicManager.CachedSchematic cached = SchematicManager.getCached(entry.path());
		if (cached != null) {
			if (cached.data != null) {
				LitematicData data = cached.data;
				String blockCountStr = String.format(Locale.ROOT, "%,d", data.totalBlocks());
				String itemCountStr = String.format(Locale.ROOT, "%,d", data.totalItems());
				String summaryStr = data.totalBlocks() == data.totalItems()
					? blockCountStr + " blocks"
					: blockCountStr + " blocks, " + itemCountStr + " items";
				source.sendFeedback(Component.literal("Selected " + entry.fileName() + " (" + data.width() + "x" + data.height() + "x" + data.length() + ", " + summaryStr + "). Type '/shhhbuilder place' to place it.").withStyle(ChatFormatting.GREEN));
				if (data.unobtainableBlocks() > 0) {
					String unobsStr = String.format(Locale.ROOT, "%,d", data.unobtainableBlocks());
					source.sendFeedback(Component.literal("Notice: " + unobsStr + " blocks have no item (water, fire...).").withStyle(ChatFormatting.GOLD));
				}
				if (data.unknownBlocks() > 0) {
					String unkStr = String.format(Locale.ROOT, "%,d", data.unknownBlocks());
					source.sendFeedback(Component.literal("Notice: " + unkStr + " blocks are not in this game version and will appear as stone wireframes.").withStyle(ChatFormatting.GOLD));
				}
				return 1;
			} else if (cached.error != null) {
				source.sendError(Component.literal("Could not load " + entry.fileName() + ": " + cached.error + ". Try another schematic.").withStyle(ChatFormatting.RED));
				return 0;
			}
		}

		// Asynchronously load on background thread
		source.sendFeedback(Component.literal("Loading " + entry.fileName() + "...").withStyle(ChatFormatting.YELLOW));
		SchematicManager.loadSchematicAsync(entry.path()).thenAccept(result -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null) {
				mc.execute(() -> {
					LocalPlayer player = mc.player;
					if (player != null && entry.fileName().equals(SchematicManager.getSelectedFileName())) {
						if (result.data != null) {
							LitematicData data = result.data;
							String blockCountStr = String.format(Locale.ROOT, "%,d", data.totalBlocks());
							String itemCountStr = String.format(Locale.ROOT, "%,d", data.totalItems());
							String summaryStr = data.totalBlocks() == data.totalItems()
								? blockCountStr + " blocks"
								: blockCountStr + " blocks, " + itemCountStr + " items";
							player.sendSystemMessage(Component.literal("Loaded " + entry.fileName() + " (" + data.width() + "x" + data.height() + "x" + data.length() + ", " + summaryStr + "). Type '/shhhbuilder place' to place it.").withStyle(ChatFormatting.GREEN));
							if (data.unobtainableBlocks() > 0) {
								String unobsStr = String.format(Locale.ROOT, "%,d", data.unobtainableBlocks());
								player.sendSystemMessage(Component.literal("Notice: " + unobsStr + " blocks have no item (water, fire...).").withStyle(ChatFormatting.GOLD));
							}
							if (data.unknownBlocks() > 0) {
								String unkStr = String.format(Locale.ROOT, "%,d", data.unknownBlocks());
								player.sendSystemMessage(Component.literal("Notice: " + unkStr + " blocks are not in this game version and will appear as stone wireframes.").withStyle(ChatFormatting.GOLD));
							}
						} else if (result.error != null) {
							player.sendSystemMessage(Component.literal("Could not load " + entry.fileName() + ": " + result.error + ". Try another schematic.").withStyle(ChatFormatting.RED));
						}
					}
				});
			}
		});

		return 1;
	}

	public static int executePlace(FabricClientCommandSource source, BlockPos explicitPos) {
		String selectedName = SchematicManager.getSelectedFileName();
		if (selectedName == null || selectedName.isBlank()) {
			source.sendError(Component.literal("No schematic selected. Type '/shhhbuilder list' and '/shhhbuilder select <file>' first.").withStyle(ChatFormatting.RED));
			return 0;
		}

		LocalPlayer player = source.getPlayer();
		if (player == null) {
			source.sendError(Component.literal("Player not found.").withStyle(ChatFormatting.RED));
			return 0;
		}

		ClientLevel level = source.getLevel();
		int minY = level != null ? level.getMinY() : -64;
		int maxY = level != null ? level.getMaxY() : 319;

		BlockPos targetPos;
		if (explicitPos != null) {
			CoordValidationResult val = validateCoordinates(explicitPos.getX(), explicitPos.getY(), explicitPos.getZ(), minY, maxY);
			if (!val.valid()) {
				source.sendError(Component.literal(val.error()).withStyle(ChatFormatting.RED));
				return 0;
			}
			targetPos = explicitPos;
		} else {
			targetPos = player.blockPosition();
		}

		PlacementManager.setPlacement(targetPos, player.level().dimension());
		PlacementManager.setVisible(true);

		source.sendFeedback(Component.literal("Placed " + selectedName + " at " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + ".").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	public static int executeMove(FabricClientCommandSource source, int dx, int dy, int dz) {
		if (!PlacementManager.hasPlacement()) {
			source.sendError(Component.literal("No build placed yet. Type '/shhhbuilder place' to place the build first.").withStyle(ChatFormatting.RED));
			return 0;
		}

		BlockPos current = PlacementManager.getPosition();
		BlockPos newPos = current.offset(dx, dy, dz);

		ClientLevel level = source.getLevel();
		int minY = level != null ? level.getMinY() : -64;
		int maxY = level != null ? level.getMaxY() : 319;

		CoordValidationResult val = validateCoordinates(newPos.getX(), newPos.getY(), newPos.getZ(), minY, maxY);
		if (!val.valid()) {
			source.sendError(Component.literal("Cannot move build to " + newPos.getX() + ", " + newPos.getY() + ", " + newPos.getZ() + ": " + val.error()).withStyle(ChatFormatting.RED));
			return 0;
		}

		PlacementManager.move(dx, dy, dz);
		source.sendFeedback(Component.literal("Moved build by (" + dx + ", " + dy + ", " + dz + ") to " + newPos.getX() + ", " + newPos.getY() + ", " + newPos.getZ() + ".").withStyle(ChatFormatting.AQUA));
		return 1;
	}

	public static int executeRotate(FabricClientCommandSource source) {
		if (!PlacementManager.hasPlacement()) {
			source.sendError(Component.literal("No build placed yet. Type '/shhhbuilder place' to place the build first.").withStyle(ChatFormatting.RED));
			return 0;
		}

		int rot = PlacementManager.rotateClockwise();
		source.sendFeedback(Component.literal("Turned build 90° clockwise (" + rot + "°).").withStyle(ChatFormatting.AQUA));
		return 1;
	}

	public static int executeFlip(FabricClientCommandSource source) {
		if (!PlacementManager.hasPlacement()) {
			source.sendError(Component.literal("No build placed yet. Type '/shhhbuilder place' to place the build first.").withStyle(ChatFormatting.RED));
			return 0;
		}

		boolean flipped = PlacementManager.toggleMirrored();
		source.sendFeedback(Component.literal("Flip left-right: " + (flipped ? "ON" : "OFF") + ".").withStyle(ChatFormatting.AQUA));
		return 1;
	}

	public static int executeLayer(FabricClientCommandSource source, String layerArg) {
		if (!PlacementManager.hasPlacement()) {
			source.sendError(Component.literal("No build placed yet. Type '/shhhbuilder place' to place the build first.").withStyle(ChatFormatting.RED));
			return 0;
		}

		LitematicData data = SchematicManager.getLoadedData();
		int maxHeight = data != null ? data.height() : 1;

		LayerValidationResult val = validateLayer(layerArg, maxHeight);
		if (!val.valid()) {
			source.sendError(Component.literal(val.error()).withStyle(ChatFormatting.RED));
			return 0;
		}

		if (val.isAll()) {
			PlacementManager.resetLayerMode();
			source.sendFeedback(Component.literal("Layer mode set to: All layers.").withStyle(ChatFormatting.AQUA));
		} else {
			PlacementManager.setLayerMode(LayerMode.SINGLE);
			PlacementManager.setCurrentLayer(val.layerNumber(), maxHeight);
			source.sendFeedback(Component.literal("Showing layer " + val.layerNumber() + " / " + maxHeight + " (one layer).").withStyle(ChatFormatting.AQUA));
		}
		return 1;
	}

	public static int executeClear(FabricClientCommandSource source) {
		if (!PlacementManager.hasPlacement()) {
			source.sendFeedback(Component.literal("No build placed to clear.").withStyle(ChatFormatting.YELLOW));
			return 0;
		}

		PlacementManager.clear();
		source.sendFeedback(Component.literal("Cleared build preview from the world.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	public static int executeFolder(FabricClientCommandSource source) {
		Path dir = SchematicManager.getSchematicsDirectory();
		ShhhBuilder.LOGGER.info("Opening schematics folder: {}", dir);
		boolean success = SchematicManager.openSchematicsFolder();
		if (success) {
			source.sendFeedback(Component.literal("Opening schematics folder...").withStyle(ChatFormatting.GREEN));
			return 1;
		} else {
			source.sendError(Component.literal("Could not open schematics folder. Check game log for details.").withStyle(ChatFormatting.RED));
			return 0;
		}
	}

	public static int executeSelfTest(FabricClientCommandSource source) {
		if (!BlockStateHelper.isBootstrapped() || BuiltInRegistries.BLOCK == null) {
			source.sendFeedback(Component.literal("Shhh Builder: Self-test requires an active game world.").withStyle(ChatFormatting.RED));
			return 0;
		}

		int total = 0;
		int worked = 0;
		int fluidCount = 0;
		int blockEntityCount = 0;
		int technicalCount = 0;
		int airCount = 0;
		int failed = 0;

		// Material counting metrics
		int matNormal = 0;
		int matSpecial = 0;
		int matTwoItems = 0;
		int matNoItem = 0;
		int matCreative = 0;
		int matFailed = 0;

		List<String> blockEntityList = new ArrayList<>();
		List<String> fluidList = new ArrayList<>();
		List<String> technicalList = new ArrayList<>();
		List<String> failedList = new ArrayList<>();

		try {
			for (Block b : BuiltInRegistries.BLOCK) {
				total++;
				Identifier id = BuiltInRegistries.BLOCK.getKey(b);
				String idStr = (id != null) ? id.toString() : "unknown";
				BlockState state = b.defaultBlockState();

				// 1. Preview Model Check
				try {
					BlockCategoryHelper.Category category = BlockCategoryHelper.getCategory(idStr, state);
					switch (category) {
						case AIR -> airCount++;
						case FLUID -> {
							fluidCount++;
							fluidList.add(idStr);
						}
						case TECHNICAL -> {
							technicalCount++;
							technicalList.add(idStr);
						}
						case BLOCK_ENTITY -> {
							blockEntityCount++;
							blockEntityList.add(idStr);
						}
						case NORMAL -> {
							GhostBlockModelHelper.CachedGhostModel model = GhostBlockModelHelper.getModel(state);
							if (model != null && !model.isEmpty()) {
								worked++;
							} else {
								blockEntityCount++;
								blockEntityList.add(idStr);
							}
						}
					}
				} catch (Throwable t) {
					failed++;
					failedList.add(idStr + " (" + t.getMessage() + ")");
					ShhhBuilder.LOGGER.warn("Self-test preview error on block {}: {}", idStr, t.getMessage());
				}

				// 2. Material Counting Check
				try {
					io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.BlockEvaluation eval =
						io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.evaluate(state);

					if (eval.isUnobtainable() || eval.isSkipped()) {
						matNoItem++;
					} else if (eval.items().size() >= 2) {
						matTwoItems++;
					} else if (eval.items().size() == 1) {
						String itemId = eval.items().get(0).itemIdentifier();
						if (io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.isCreativeOnly(itemId)) {
							matCreative++;
						} else if (!itemId.equals(idStr) || eval.items().get(0).count() != 1) {
							matSpecial++;
						} else {
							matNormal++;
						}
					} else {
						matNoItem++;
					}
				} catch (Throwable t) {
					matFailed++;
					ShhhBuilder.LOGGER.warn("Self-test material counting error on block {}: {}", idStr, t.getMessage());
				}
			}
		} catch (Throwable t) {
			ShhhBuilder.LOGGER.error("Self-test encountered an unexpected error: {}", t.getMessage(), t);
			source.sendFeedback(Component.literal("Self-test error: " + t.getMessage()).withStyle(ChatFormatting.RED));
			return 0;
		}

		// Log complete breakdown
		ShhhBuilder.LOGGER.info("=== Shhh Builder Block & Material Self-Test Results ===");
		ShhhBuilder.LOGGER.info("Total blocks tested: {}, Worked: {}, Failed: {}", total, worked, failed);
		ShhhBuilder.LOGGER.info("Material counting breakdown: Normal: {}, Special rule: {}, Two items: {}, No item: {}, Creative-only: {}, Failed: {}",
			matNormal, matSpecial, matTwoItems, matNoItem, matCreative, matFailed);
		ShhhBuilder.LOGGER.info("Fluids ({}): {}", fluidCount, fluidList);
		ShhhBuilder.LOGGER.info("Technical markers ({}): {}", technicalCount, technicalList);
		ShhhBuilder.LOGGER.info("Block entities / fallbacks ({}): {}", blockEntityCount, blockEntityList);
		if (!failedList.isEmpty()) {
			ShhhBuilder.LOGGER.error("Failed blocks ({}): {}", failed, failedList);
		}
		ShhhBuilder.LOGGER.info("=======================================================");

		// Chat feedback in plain words
		source.sendFeedback(Component.literal("--- Shhh Builder Self-Test ---").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		source.sendFeedback(Component.literal(String.format(Locale.ROOT, "Preview: %,d blocks tested, %,d textured models ready, %,d fallbacks.", total, worked, blockEntityCount + fluidCount + technicalCount)).withStyle(ChatFormatting.GREEN));
		source.sendFeedback(Component.literal(String.format(Locale.ROOT, "Material engine: tested %,d blocks, %,d item mappings, %,d errors.", total, (matNormal + matSpecial + matTwoItems), matFailed)).withStyle(matFailed == 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
		source.sendFeedback(Component.literal(String.format(Locale.ROOT, "Materials breakdown: %,d normal, %,d special rule, %,d two items, %,d creative-only, %,d no item.",
			matNormal, matSpecial, matTwoItems, matCreative, matNoItem)).withStyle(ChatFormatting.YELLOW));
		if (failed == 0 && matFailed == 0) {
			source.sendFeedback(Component.literal("Failed: 0 blocks. All blocks and materials verified!").withStyle(ChatFormatting.GREEN));
		} else {
			source.sendFeedback(Component.literal(String.format(Locale.ROOT, "Failed: %,d preview, %,d material(s). See game log for details.", failed, matFailed)).withStyle(ChatFormatting.RED));
		}

		return 1;
	}
}
