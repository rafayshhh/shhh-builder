package io.github.rafayshhh.shhhbuilder.client.schematic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * Helper utilities for parsing NBT into Minecraft BlockStates, determining
 * display colors for ghost block wireframes, and mapping block voxels to required player items.
 */
public class BlockStateHelper {

	/**
	 * Represents the evaluated item requirement for a block state.
	 *
	 * @param itemIdentifier the carried item identifier (e.g. "minecraft:torch"), or null if skipped/unobtainable
	 * @param countMultiplier the multiplier for this voxel (0 for skipped/unobtainable, 1 for normal, 2 for double slabs)
	 * @param isUnobtainable whether the block has no obtainable item form (water, lava, fire, portal, etc.)
	 * @param isSkipped whether the block is a technical or duplicate part to skip (piston_head, door upper, bed head, etc.)
	 */
	public record ItemEvaluation(
		String itemIdentifier,
		int countMultiplier,
		boolean isUnobtainable,
		boolean isSkipped
	) {}

	/**
	 * Checks whether the Minecraft game environment has been bootstrapped.
	 * In standalone JUnit tests, this returns false so we avoid touching Minecraft block registries.
	 */
	public static boolean isBootstrapped() {
		try {
			Bootstrap.checkBootstrapCalled(() -> "");
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	/**
	 * Evaluates a block state CompoundTag (containing "id" or "Name" and optional "properties" or "Properties")
	 * to determine the actual carried item and count multiplier required to place it.
	 */
	public static ItemEvaluation evaluateItem(CompoundTag stateTag) {
		if (stateTag == null) {
			return new ItemEvaluation(null, 0, false, true);
		}

		String rawName = null;
		if (stateTag.contains("id")) {
			rawName = stateTag.getStringOr("id", "").trim();
		}
		if ((rawName == null || rawName.isEmpty()) && stateTag.contains("Name")) {
			rawName = stateTag.getStringOr("Name", "").trim();
		}
		if (rawName == null || rawName.isEmpty()) {
			rawName = "minecraft:air";
		}

		if (isAirName(rawName)) {
			return new ItemEvaluation(null, 0, false, true);
		}

		String cleanName = extractBlockName(rawName).toLowerCase(Locale.ROOT);
		CompoundTag properties = null;
		if (stateTag.contains("properties")) {
			properties = stateTag.getCompoundOrEmpty("properties");
		} else if (stateTag.contains("Properties")) {
			properties = stateTag.getCompoundOrEmpty("Properties");
		}
		if (properties == null) {
			properties = new CompoundTag();
		}
		String path = cleanName.contains(":") ? cleanName.substring(cleanName.indexOf(':') + 1) : cleanName;

		// 1. Technical multi-part blocks to skip: piston_head and moving_piston (Rule 5)
		if (path.equals("piston_head") || path.equals("moving_piston")) {
			return new ItemEvaluation(null, 0, false, true);
		}

		// 2. Doors: count only half=lower (1 door item per door; skip half=upper) (Rule 2)
		if (path.endsWith("_door") || path.equals("door")) {
			String half = properties.getStringOr("half", "").toLowerCase(Locale.ROOT);
			if (half.equals("upper")) {
				return new ItemEvaluation(null, 0, false, true);
			}
			return new ItemEvaluation(resolveItemIdentifier(stateTag, cleanName), 1, false, false);
		}

		// 3. Beds: count only part=foot (1 bed item per bed; skip part=head) (Rule 3)
		if (path.endsWith("_bed") || path.equals("bed")) {
			String part = properties.getStringOr("part", "").toLowerCase(Locale.ROOT);
			if (part.equals("head")) {
				return new ItemEvaluation(null, 0, false, true);
			}
			return new ItemEvaluation(resolveItemIdentifier(stateTag, cleanName), 1, false, false);
		}

		// 4. Tall plants and double-height blocks: count only half=lower (Rule 4)
		if (isDoubleHeightPlant(path)) {
			String half = properties.getStringOr("half", "").toLowerCase(Locale.ROOT);
			if (half.equals("upper")) {
				return new ItemEvaluation(null, 0, false, true);
			}
			return new ItemEvaluation(resolveItemIdentifier(stateTag, cleanName), 1, false, false);
		}

		// 5. Slabs: type=double counts as 2 of that slab item (Rule 1)
		if (path.endsWith("_slab")) {
			String type = properties.getStringOr("type", "").toLowerCase(Locale.ROOT);
			int count = type.equals("double") ? 2 : 1;
			return new ItemEvaluation(resolveItemIdentifier(stateTag, cleanName), count, false, false);
		}

		// 6. Blocks with no obtainable item (water, lava, fire, nether_portal, end_portal, bubble_column, etc.) (Rule 7)
		if (isUnobtainableBlock(stateTag, path, cleanName)) {
			return new ItemEvaluation(null, 0, true, false);
		}

		// 7. Standard block -> map to carried item (Rule 8)
		String itemId = resolveItemIdentifier(stateTag, cleanName);
		return new ItemEvaluation(itemId, 1, false, false);
	}

	/**
	 * Evaluates a raw block state string (e.g. "minecraft:oak_slab[type=double]")
	 * to determine the actual carried item and count multiplier required to place it.
	 */
	public static ItemEvaluation evaluateItem(String rawState) {
		if (rawState == null || rawState.isBlank()) {
			return new ItemEvaluation(null, 0, false, true);
		}
		CompoundTag tag = stringToBlockStateTag(rawState);
		return evaluateItem(tag);
	}

	/**
	 * Checks if a block name belongs to the double-height plant list.
	 */
	public static boolean isDoubleHeightPlant(String path) {
		return path.equals("tall_grass")
			|| path.equals("large_fern")
			|| path.equals("sunflower")
			|| path.equals("lilac")
			|| path.equals("rose_bush")
			|| path.equals("peony")
			|| path.equals("tall_seagrass")
			|| path.equals("pitcher_plant");
	}

	/**
	 * Checks if a block has no obtainable item form.
	 */
	public static boolean isUnobtainableBlock(CompoundTag stateTag, String path, String cleanName) {
		if (isBootstrapped()) {
			try {
				BlockState state = parseBlockState(stateTag);
				if (state != null && !state.isAir()) {
					net.minecraft.world.item.Item item = state.getBlock().asItem();
					if (item == net.minecraft.world.item.Items.AIR) {
						return true;
					}
				}
			} catch (Throwable ignored) {
			}
		}

		return isKnownUnobtainableName(path);
	}

	/**
	 * Known unobtainable block paths for unbootstrapped unit tests and fallback.
	 */
	public static boolean isKnownUnobtainableName(String path) {
		return path.equals("water")
			|| path.equals("flowing_water")
			|| path.equals("lava")
			|| path.equals("flowing_lava")
			|| path.equals("fire")
			|| path.equals("soul_fire")
			|| path.equals("nether_portal")
			|| path.equals("end_portal")
			|| path.equals("end_gateway")
			|| path.equals("bubble_column")
			|| path.equals("light");
	}

	/**
	 * Maps a block state to the item the player carries in their inventory.
	 * Uses vanilla block.asItem() when bootstrapped, and falls back to name resolution rules.
	 */
	public static String resolveItemIdentifier(CompoundTag stateTag, String cleanName) {
		if (isBootstrapped()) {
			try {
				BlockState state = parseBlockState(stateTag);
				if (state != null && !state.isAir()) {
					net.minecraft.world.item.Item item = state.getBlock().asItem();
					if (item != null && item != net.minecraft.world.item.Items.AIR && BuiltInRegistries.ITEM != null) {
						Identifier id = BuiltInRegistries.ITEM.getKey(item);
						if (id != null) {
							return id.toString();
						}
					}
				}
			} catch (Throwable ignored) {
			}
		}

		return resolveFallbackItem(cleanName);
	}

	/**
	 * Deterministic fallback item mapping when registries are unbootstrapped or unresolvable.
	 */
	public static String resolveFallbackItem(String cleanName) {
		if (cleanName == null || cleanName.isBlank()) {
			return "minecraft:air";
		}
		String name = cleanName.toLowerCase(Locale.ROOT).trim();
		if (!name.contains(":")) {
			name = "minecraft:" + name;
		}

		// Common carried item mappings for blocks whose block ID != item ID
		if (name.endsWith("_wall_hanging_sign")) {
			return name.replace("_wall_hanging_sign", "_hanging_sign");
		}
		if (name.endsWith("_wall_sign")) {
			return name.replace("_wall_sign", "_sign");
		}
		if (name.contains("wall_torch")) {
			return name.replace("wall_torch", "torch");
		}
		if (name.endsWith("_wall_fan")) {
			return name.replace("_wall_fan", "_fan");
		}
		if (name.endsWith("_wall_banner")) {
			return name.replace("_wall_banner", "_banner");
		}
		if (name.equals("minecraft:redstone_wire")) {
			return "minecraft:redstone";
		}
		if (name.equals("minecraft:tripwire")) {
			return "minecraft:string";
		}
		if (name.equals("minecraft:wheat")) {
			return "minecraft:wheat_seeds";
		}
		if (name.equals("minecraft:beetroots")) {
			return "minecraft:beetroot_seeds";
		}
		if (name.equals("minecraft:carrots")) {
			return "minecraft:carrot";
		}
		if (name.equals("minecraft:potatoes")) {
			return "minecraft:potato";
		}
		if (name.equals("minecraft:bamboo_sapling")) {
			return "minecraft:bamboo";
		}

		return name;
	}

	/**
	 * Checks if a block or carried item name is recognized in the current Minecraft version.
	 */
	public static boolean isKnownBlock(String blockName) {
		if (blockName == null || blockName.isBlank()) {
			return false;
		}
		if (isAirName(blockName)) {
			return true;
		}
		String clean = extractBlockName(blockName);
		if (!isBootstrapped()) {
			// In standalone unit tests without bootstrapped registries, standard minecraft namespace is known
			return clean.startsWith("minecraft:");
		}
		try {
			Identifier id = Identifier.tryParse(clean);
			if (id == null) {
				return false;
			}
			boolean inBlock = BuiltInRegistries.BLOCK != null && BuiltInRegistries.BLOCK.containsKey(id);
			boolean inItem = BuiltInRegistries.ITEM != null && BuiltInRegistries.ITEM.containsKey(id);
			return inBlock || inItem;
		} catch (Throwable ignored) {
			return false;
		}
	}

	/**
	 * Parses a block state CompoundTag (supporting both 26.3 "id"/"properties" and legacy "Name"/"Properties")
	 * into a Minecraft BlockState. If unknown or invalid, returns null.
	 */
	public static BlockState parseBlockState(CompoundTag stateTag) {
		if (!isBootstrapped() || stateTag == null) {
			return null;
		}

		String name = null;
		if (stateTag.contains("id")) {
			name = stateTag.getStringOr("id", "").trim();
		}
		if ((name == null || name.isEmpty()) && stateTag.contains("Name")) {
			name = stateTag.getStringOr("Name", "").trim();
		}
		if (name == null || name.isEmpty()) {
			name = "minecraft:air";
		}

		if (!isBootstrapped()) {
			return null;
		}

		if (isAirName(name)) {
			try {
				return Blocks.AIR.defaultBlockState();
			} catch (Throwable ignored) {
				return null;
			}
		}

		try {
			if (BuiltInRegistries.BLOCK != null) {
				CompoundTag normalized = new CompoundTag();
				normalized.putString("id", name);
				normalized.putString("Name", name);

				CompoundTag props = null;
				if (stateTag.contains("properties")) {
					props = stateTag.getCompoundOrEmpty("properties");
				} else if (stateTag.contains("Properties")) {
					props = stateTag.getCompoundOrEmpty("Properties");
				}
				if (props != null && !props.isEmpty()) {
					normalized.put("properties", props.copy());
					normalized.put("Properties", props.copy());
				}

				BlockState parsed = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, normalized);
				if (parsed != null && !parsed.isAir()) {
					return parsed;
				}

				// If NbtUtils returned air but the block exists in registry, return defaultBlockState
				Identifier id = Identifier.tryParse(name);
				if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
					return BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
				}
			}
		} catch (Throwable ignored) {
		}

		return null;
	}

	/**
	 * Parses a block state string (e.g. "minecraft:oak_stairs[facing=north,half=bottom]")
	 * into a Minecraft BlockState.
	 */
	public static BlockState parseBlockStateString(String rawState) {
		if (rawState == null || rawState.isBlank()) {
			return null;
		}
		CompoundTag tag = stringToBlockStateTag(rawState);
		return parseBlockState(tag);
	}

	/**
	 * Converts a block state string with optional properties into an NBT CompoundTag
	 * suitable for Minecraft's NbtUtils.readBlockState (both 26.3 and legacy formats).
	 */
	public static CompoundTag stringToBlockStateTag(String rawState) {
		CompoundTag tag = new CompoundTag();
		if (rawState == null || rawState.isBlank()) {
			tag.putString("id", "minecraft:air");
			tag.putString("Name", "minecraft:air");
			return tag;
		}

		String trimmed = rawState.trim();
		String blockName;
		CompoundTag properties = null;

		int bracketOpen = trimmed.indexOf('[');
		if (bracketOpen >= 0 && trimmed.endsWith("]")) {
			blockName = trimmed.substring(0, bracketOpen).trim();
			String propPart = trimmed.substring(bracketOpen + 1, trimmed.length() - 1).trim();
			if (!propPart.isEmpty()) {
				properties = new CompoundTag();
				String[] entries = propPart.split(",");
				for (String entry : entries) {
					int eq = entry.indexOf('=');
					if (eq > 0) {
						String key = entry.substring(0, eq).trim();
						String val = entry.substring(eq + 1).trim();
						properties.putString(key, val);
					}
				}
			}
		} else {
			blockName = trimmed;
		}

		if (!blockName.contains(":")) {
			blockName = "minecraft:" + blockName;
		}

		tag.putString("id", blockName);
		tag.putString("Name", blockName);
		if (properties != null && !properties.isEmpty()) {
			tag.put("properties", properties);
			tag.put("Properties", properties);
		}
		return tag;
	}

	/**
	 * Extracts the base block name from a block state string, stripping properties.
	 * E.g. "minecraft:oak_stairs[facing=north]" -> "minecraft:oak_stairs"
	 */
	public static String extractBlockName(String rawState) {
		if (rawState == null || rawState.isBlank()) {
			return "minecraft:air";
		}
		String trimmed = rawState.trim();
		int bracketOpen = trimmed.indexOf('[');
		String name = bracketOpen >= 0 ? trimmed.substring(0, bracketOpen).trim() : trimmed;
		if (!name.contains(":")) {
			name = "minecraft:" + name;
		}
		return name;
	}

	/**
	 * Checks whether a block name is an air variant.
	 */
	public static boolean isAirName(String name) {
		if (name == null || name.isBlank()) {
			return true;
		}
		String clean = name.toLowerCase(Locale.ROOT).trim();
		return clean.equals("minecraft:air") || clean.equals("air")
			|| clean.equals("minecraft:cave_air") || clean.equals("cave_air")
			|| clean.equals("minecraft:void_air") || clean.equals("void_air");
	}

	/**
	 * Checks whether a BlockState is air.
	 */
	public static boolean isAir(BlockState state) {
		if (state == null) {
			return true;
		}
		try {
			return state.isAir();
		} catch (Throwable ignored) {
			return false;
		}
	}

	// Category wireframe line colors in 32-bit ARGB (alpha = 0xFF)
	public static final int COLOR_DEFAULT = 0xFF00E5FF;  // Default crisp cyan
	public static final int COLOR_WOOD = 0xFFD7A15C;     // Warm light wood amber
	public static final int COLOR_GLASS = 0xFFE0F7FA;    // Ice cyan/white
	public static final int COLOR_FOLIAGE = 0xFFA5D6A7;  // Soft foliage green
	public static final int COLOR_REDSTONE = 0xFFFF8A80; // Soft red/orange
	public static final int COLOR_WATER = 0xFF4FC3F7;    // Soft water blue
	public static final int COLOR_WOOL = 0xFFFFE082;     // Warm pastel yellow
	public static final int COLOR_STONE = 0xFF90CAF9;    // Soft stone cyan-blue

	/**
	 * Defensive helper: Ensures any 24-bit RGB color has its alpha byte forced to 0xFF (100% opaque).
	 * In Minecraft 26.3, passing colors with alpha 0 causes rendering pipelines to silently drop them.
	 */
	public static int ensureAlpha(int color) {
		if ((color & 0xFF000000) == 0) {
			return color | 0xFF000000;
		}
		return color;
	}

	/**
	 * Returns an ARGB color for drawing ghost wireframe lines based on the block ID string.
	 * Can be tested in non-bootstrapped environments without Minecraft registries.
	 */
	public static int getColorForBlockId(String blockId) {
		if (blockId == null || blockId.isBlank()) {
			return COLOR_DEFAULT;
		}
		String clean = blockId.toLowerCase(Locale.ROOT).trim();
		if (clean.contains(":")) {
			clean = clean.substring(clean.indexOf(':') + 1);
		}

		if (clean.contains("planks") || clean.contains("log") || clean.contains("wood")
			|| clean.contains("fence") || clean.contains("door") || clean.contains("trapdoor")
			|| clean.contains("slab") && (clean.contains("oak") || clean.contains("spruce") || clean.contains("birch"))) {
			return COLOR_WOOD;
		}
		if (clean.contains("glass") || clean.contains("ice")) {
			return COLOR_GLASS;
		}
		if (clean.contains("leaves") || clean.contains("grass") || clean.contains("plant")
			|| clean.contains("stem") || clean.contains("vine") || clean.contains("sapling")) {
			return COLOR_FOLIAGE;
		}
		if (clean.contains("redstone") || clean.contains("torch") || clean.contains("lava")
			|| clean.contains("tnt") || clean.contains("fire")) {
			return COLOR_REDSTONE;
		}
		if (clean.contains("water")) {
			return COLOR_WATER;
		}
		if (clean.contains("wool") || clean.contains("carpet") || clean.contains("terracotta")
			|| clean.contains("concrete")) {
			return COLOR_WOOL;
		}
		if (clean.contains("stone") || clean.contains("brick") || clean.contains("cobble")
			|| clean.contains("deepslate") || clean.contains("granite") || clean.contains("diorite")
			|| clean.contains("andesite")) {
			return COLOR_STONE;
		}

		return COLOR_DEFAULT;
	}

	/**
	 * Returns an ARGB color for drawing ghost wireframe lines based on the block type.
	 * Guarantees non-zero alpha byte (0xFF) under Minecraft 26.3.
	 */
	public static int getBlockColor(BlockState state) {
		if (state == null) {
			return COLOR_DEFAULT;
		}

		try {
			if (isBootstrapped()) {
				net.minecraft.world.level.material.MapColor mapColor = state.getMapColor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
				if (mapColor != null && mapColor != net.minecraft.world.level.material.MapColor.NONE && mapColor.col != 0) {
					return ensureAlpha(mapColor.col);
				}
				String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
				return ensureAlpha(getColorForBlockId(blockId));
			}
		} catch (Throwable ignored) {
		}

		return COLOR_DEFAULT;
	}
}
