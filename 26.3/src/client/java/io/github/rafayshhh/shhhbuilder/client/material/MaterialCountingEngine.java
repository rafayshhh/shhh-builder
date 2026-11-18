package io.github.rafayshhh.shhhbuilder.client.material;

import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exact material counting engine for Shhh Builder.
 * Evaluates the actual carried items a player must have to place schematic blocks,
 * taking into account multi-block structures, stackable-in-one-block items, potted plants,
 * carried item resolutions, creative-only items, and unobtainable fluids/portals.
 */
public class MaterialCountingEngine {

	/**
	 * Represents an item requirement produced by a block state.
	 *
	 * @param itemIdentifier full item identifier, e.g. "minecraft:oak_planks"
	 * @param count quantity of this item required
	 */
	public record ItemRequirement(String itemIdentifier, int count) {}

	/**
	 * Evaluation result for a single block state.
	 */
	public record BlockEvaluation(
		List<ItemRequirement> items,
		boolean isUnobtainable,
		boolean isSkipped,
		boolean isUnknown,
		String ruleExplanation
	) {
		public static final BlockEvaluation AIR = new BlockEvaluation(Collections.emptyList(), false, true, false, "Air");
		public static final BlockEvaluation SKIPPED = new BlockEvaluation(Collections.emptyList(), false, true, false, "Skipped (multi-part duplicate)");
		public static final BlockEvaluation UNOBTAINABLE = new BlockEvaluation(Collections.emptyList(), true, false, false, "Unobtainable (water, lava, fire, portal)");
		public static final BlockEvaluation UNKNOWN = new BlockEvaluation(Collections.emptyList(), false, false, true, "Unknown block in this version");

		public boolean isAir() {
			return this == AIR;
		}

		public static BlockEvaluation of(String itemId, int count, String explanation) {
			return new BlockEvaluation(List.of(new ItemRequirement(itemId, count)), false, false, false, explanation);
		}

		public static BlockEvaluation ofTwo(String itemId1, int count1, String itemId2, int count2, String explanation) {
			return new BlockEvaluation(List.of(new ItemRequirement(itemId1, count1), new ItemRequirement(itemId2, count2)), false, false, false, explanation);
		}
	}

	/**
	 * Complete counting result for a schematic, including overall and per-layer counts.
	 */
	public record MaterialCountResult(
		Map<String, Long> overallItemCounts,
		Map<Integer, Map<String, Long>> layerItemCounts,
		List<Map.Entry<String, Long>> sortedOverallItems,
		long totalVoxels,
		long totalItemsNeeded,
		long unobtainableCount,
		long unknownCount,
		Set<String> creativeOnlyItemIds
	) {
		public Map<String, Long> getCountsForLayer(int layer) {
			if (layer < 0 || layerItemCounts == null) {
				return overallItemCounts;
			}
			return layerItemCounts.getOrDefault(layer, Collections.emptyMap());
		}
	}

	private static final Set<String> CREATIVE_ONLY_ITEMS = new HashSet<>();
	private static final Map<LitematicData, MaterialCountResult> RESULT_CACHE = new ConcurrentHashMap<>();
	private static final Map<BlockState, BlockEvaluation> STATE_EVAL_CACHE = new ConcurrentHashMap<>();

	static {
		loadCreativeOnlyItems();
	}

	private static void loadCreativeOnlyItems() {
		// Hardcoded baseline fallback
		Set<String> defaults = Set.of(
			"minecraft:bedrock",
			"minecraft:barrier",
			"minecraft:spawner",
			"minecraft:command_block",
			"minecraft:chain_command_block",
			"minecraft:repeating_command_block",
			"minecraft:structure_block",
			"minecraft:structure_void",
			"minecraft:jigsaw",
			"minecraft:light",
			"minecraft:budding_amethyst",
			"minecraft:reinforced_deepslate",
			"minecraft:end_portal_frame",
			"minecraft:debug_stick",
			"minecraft:trial_spawner",
			"minecraft:vault",
			"minecraft:test_block",
			"minecraft:test_instance_block"
		);
		CREATIVE_ONLY_ITEMS.addAll(defaults);

		try (InputStream in = MaterialCountingEngine.class.getResourceAsStream("/assets/shhh-builder/data/creative_only_items.txt")) {
			if (in != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						line = line.trim();
						if (!line.isEmpty() && !line.startsWith("#")) {
							CREATIVE_ONLY_ITEMS.add(line.toLowerCase(Locale.ROOT));
						}
					}
				}
			}
		} catch (Throwable t) {
			ShhhBuilder.LOGGER.warn("Failed to load creative_only_items.txt, using defaults: {}", t.getMessage());
		}
	}

	public static boolean isCreativeOnly(String itemId) {
		if (itemId == null) return false;
		return CREATIVE_ONLY_ITEMS.contains(itemId.toLowerCase(Locale.ROOT).trim());
	}

	public static Set<String> getCreativeOnlyItems() {
		return Collections.unmodifiableSet(CREATIVE_ONLY_ITEMS);
	}

	/**
	 * Evaluates a Minecraft BlockState to determine the items required to place it.
	 */
	public static BlockEvaluation evaluate(BlockState state) {
		if (state == null || state.isAir()) {
			return BlockEvaluation.AIR;
		}
		return STATE_EVAL_CACHE.computeIfAbsent(state, MaterialCountingEngine::evaluateInternal);
	}

	private static BlockEvaluation evaluateInternal(BlockState state) {
		Block block = state.getBlock();
		Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
		String path = blockId != null ? blockId.getPath() : "";
		String fullId = blockId != null ? blockId.toString() : "";

		// 1. Technical multi-part blocks to skip (Rule 2)
		if (path.equals("piston_head") || path.equals("moving_piston")) {
			return BlockEvaluation.SKIPPED;
		}

		// 2. Doors: count only half=lower (Rule 2)
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && (path.endsWith("_door") || path.equals("door"))) {
			if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
				return BlockEvaluation.SKIPPED;
			}
			return BlockEvaluation.of(resolveItem(state, fullId), 1, "Door lower half");
		}

		// 3. Beds: count only part=foot (Rule 2)
		if (state.hasProperty(BlockStateProperties.BED_PART)) {
			if (state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
				return BlockEvaluation.SKIPPED;
			}
			return BlockEvaluation.of(resolveItem(state, fullId), 1, "Bed foot");
		}

		// 4. Double-height plants and double flowers: count only half=lower (Rule 2)
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
				return BlockEvaluation.SKIPPED;
			}
			if (path.equals("tall_seagrass")) {
				return BlockEvaluation.of("minecraft:seagrass", 1, "Tall seagrass lower half -> seagrass");
			}
			return BlockEvaluation.of(resolveItem(state, fullId), 1, "Double plant lower half");
		}

		// 5. Slabs: type=double counts as 2 of that slab item (Rule 1)
		if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
			int count = state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
			return BlockEvaluation.of(resolveItem(state, fullId), count, count == 2 ? "Double slab (2 items)" : "Single slab");
		}

		// 6. Stackable-in-one-block items (Rule 3)
		if (state.hasProperty(BlockStateProperties.CANDLES)) {
			int candles = state.getValue(BlockStateProperties.CANDLES);
			return BlockEvaluation.of(resolveItem(state, fullId), candles, "Candles (" + candles + ")");
		}
		if (state.hasProperty(BlockStateProperties.PICKLES)) {
			int pickles = state.getValue(BlockStateProperties.PICKLES);
			return BlockEvaluation.of(resolveItem(state, fullId), pickles, "Sea pickles (" + pickles + ")");
		}
		if (state.hasProperty(BlockStateProperties.EGGS)) {
			int eggs = state.getValue(BlockStateProperties.EGGS);
			return BlockEvaluation.of(resolveItem(state, fullId), eggs, "Turtle eggs (" + eggs + ")");
		}
		if (state.hasProperty(BlockStateProperties.LAYERS)) {
			int layers = state.getValue(BlockStateProperties.LAYERS);
			return BlockEvaluation.of(resolveItem(state, fullId), layers, "Snow layers (" + layers + ")");
		}
		if (state.hasProperty(BlockStateProperties.FLOWER_AMOUNT)) {
			int amount = state.getValue(BlockStateProperties.FLOWER_AMOUNT);
			return BlockEvaluation.of(resolveItem(state, fullId), amount, "Flower amount (" + amount + ")");
		}
		if (state.hasProperty(BlockStateProperties.SEGMENT_AMOUNT)) {
			int amount = state.getValue(BlockStateProperties.SEGMENT_AMOUNT);
			return BlockEvaluation.of(resolveItem(state, fullId), amount, "Leaf litter segment amount (" + amount + ")");
		}

		// 7. Potted plants count TWO items: flower pot + plant (Rule 5)
		if (block instanceof FlowerPotBlock pot) {
			Block potted = pot.getPotted();
			if (potted == null || potted == Blocks.AIR) {
				return BlockEvaluation.of("minecraft:flower_pot", 1, "Empty flower pot");
			}
			String plantItemId = resolveBlockItem(potted);
			return BlockEvaluation.ofTwo("minecraft:flower_pot", 1, plantItemId, 1, "Potted plant (flower pot + " + plantItemId + ")");
		}

		// 8. Fluids, fire, portals, technical blocks: no item (Rule 6)
		if (isUnobtainableBlock(block, path)) {
			return BlockEvaluation.UNOBTAINABLE;
		}

		// 9. Special block-to-item overrides (Rule 4)
		if (path.equals("tall_seagrass")) {
			return BlockEvaluation.of("minecraft:seagrass", 1, "Tall seagrass -> seagrass");
		}
		if (path.equals("attached_pumpkin_stem")) {
			return BlockEvaluation.of("minecraft:pumpkin_seeds", 1, "Attached pumpkin stem -> pumpkin seeds");
		}
		if (path.equals("attached_melon_stem")) {
			return BlockEvaluation.of("minecraft:melon_seeds", 1, "Attached melon stem -> melon seeds");
		}
		if (path.equals("kelp_plant")) {
			return BlockEvaluation.of("minecraft:kelp", 1, "Kelp plant -> kelp");
		}
		if (path.equals("bamboo_sapling")) {
			return BlockEvaluation.of("minecraft:bamboo", 1, "Bamboo sapling -> bamboo");
		}
		if (path.equals("weeping_vines_plant")) {
			return BlockEvaluation.of("minecraft:weeping_vines", 1, "Weeping vines plant -> weeping vines");
		}
		if (path.equals("twisting_vines_plant")) {
			return BlockEvaluation.of("minecraft:twisting_vines", 1, "Twisting vines plant -> twisting vines");
		}
		if (path.equals("cave_vines_plant")) {
			return BlockEvaluation.of("minecraft:glow_berries", 1, "Cave vines plant -> glow berries");
		}

		// 10. Standard block resolution via block.asItem()
		String itemId = resolveItem(state, fullId);
		if (itemId.equals("minecraft:air")) {
			// If asItem is air and not handled, it's unobtainable
			return BlockEvaluation.UNOBTAINABLE;
		}

		return BlockEvaluation.of(itemId, 1, "Standard block item");
	}

	/**
	 * Evaluates a block from raw NBT or string (for tests and unbootstrapped environments).
	 */
	public static BlockEvaluation evaluate(CompoundTag stateTag, String rawName) {
		if (rawName == null || rawName.isBlank()) {
			if (stateTag != null) {
				rawName = stateTag.getStringOr("Name", stateTag.getStringOr("id", "minecraft:air"));
			} else {
				return BlockEvaluation.SKIPPED;
			}
		}

		if (BlockStateHelper.isAirName(rawName)) {
			return BlockEvaluation.AIR;
		}

		// If bootstrapped, parse the real block state and evaluate
		if (BlockStateHelper.isBootstrapped()) {
			try {
				BlockState state = BlockStateHelper.parseBlockState(stateTag != null ? stateTag : BlockStateHelper.stringToBlockStateTag(rawName));
				if (state != null) {
					return evaluate(state);
				}
			} catch (Throwable ignored) {
			}
		}

		// Fallback property parsing from tag or string
		String cleanName = BlockStateHelper.extractBlockName(rawName).toLowerCase(Locale.ROOT);
		if (!cleanName.contains(":")) {
			cleanName = "minecraft:" + cleanName;
		}
		String path = cleanName.substring(cleanName.indexOf(':') + 1);

		CompoundTag props = null;
		if (stateTag != null) {
			if (stateTag.contains("Properties")) props = stateTag.getCompoundOrEmpty("Properties");
			else if (stateTag.contains("properties")) props = stateTag.getCompoundOrEmpty("properties");
		}
		if (props == null) {
			props = extractPropertiesFromString(rawName);
		}

		// Unknown block check
		if (!BlockStateHelper.isKnownBlock(cleanName)) {
			return BlockEvaluation.UNKNOWN;
		}

		// Rule 2: Technical multipart
		if (path.equals("piston_head") || path.equals("moving_piston")) {
			return BlockEvaluation.SKIPPED;
		}

		// Rule 2: Doors
		if (path.endsWith("_door") || path.equals("door")) {
			String half = props.getStringOr("half", "").toLowerCase(Locale.ROOT);
			if (half.equals("upper")) {
				return BlockEvaluation.SKIPPED;
			}
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), 1, "Door lower half");
		}

		// Rule 2: Beds
		if (path.endsWith("_bed") || path.equals("bed")) {
			String part = props.getStringOr("part", "").toLowerCase(Locale.ROOT);
			if (part.equals("head")) {
				return BlockEvaluation.SKIPPED;
			}
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), 1, "Bed foot");
		}

		// Rule 2: Double-height plants
		if (BlockStateHelper.isDoubleHeightPlant(path) || props.contains("half")) {
			String half = props.getStringOr("half", "").toLowerCase(Locale.ROOT);
			if (half.equals("upper")) {
				return BlockEvaluation.SKIPPED;
			}
			if (path.equals("tall_seagrass")) {
				return BlockEvaluation.of("minecraft:seagrass", 1, "Tall seagrass lower half");
			}
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), 1, "Double plant lower half");
		}

		// Rule 1: Slabs
		if (path.endsWith("_slab")) {
			String type = props.getStringOr("type", "").toLowerCase(Locale.ROOT);
			int count = type.equals("double") ? 2 : 1;
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), count, count == 2 ? "Double slab" : "Single slab");
		}

		// Rule 3: Stackable items
		if (props.contains("candles")) {
			int val = parseIntProp(props, "candles", 1);
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), val, "Candles (" + val + ")");
		}
		if (props.contains("pickles")) {
			int val = parseIntProp(props, "pickles", 1);
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), val, "Sea pickles (" + val + ")");
		}
		if (props.contains("eggs")) {
			int val = parseIntProp(props, "eggs", 1);
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), val, "Turtle eggs (" + val + ")");
		}
		if (props.contains("layers")) {
			int val = parseIntProp(props, "layers", 1);
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), val, "Snow layers (" + val + ")");
		}
		if (props.contains("flower_amount")) {
			int val = parseIntProp(props, "flower_amount", 1);
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), val, "Flower amount (" + val + ")");
		}
		if (props.contains("segment_amount")) {
			int val = parseIntProp(props, "segment_amount", 1);
			return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), val, "Leaf litter segment amount (" + val + ")");
		}

		// Rule 5: Potted plants
		if (path.startsWith("potted_")) {
			String plantName = path.substring("potted_".length());
			String plantItem = "minecraft:" + plantName;
			// Special plant item mappings
			if (plantName.equals("azalea_bush")) plantItem = "minecraft:azalea";
			else if (plantName.equals("flowering_azalea_bush")) plantItem = "minecraft:flowering_azalea";
			return BlockEvaluation.ofTwo("minecraft:flower_pot", 1, plantItem, 1, "Potted plant");
		}
		if (path.equals("flower_pot")) {
			return BlockEvaluation.of("minecraft:flower_pot", 1, "Empty flower pot");
		}

		// Rule 6: Unobtainable
		if (BlockStateHelper.isKnownUnobtainableName(path)) {
			return BlockEvaluation.UNOBTAINABLE;
		}

		// Rule 4: Overrides
		if (path.equals("tall_seagrass")) return BlockEvaluation.of("minecraft:seagrass", 1, "Tall seagrass");
		if (path.equals("attached_pumpkin_stem")) return BlockEvaluation.of("minecraft:pumpkin_seeds", 1, "Attached pumpkin stem");
		if (path.equals("attached_melon_stem")) return BlockEvaluation.of("minecraft:melon_seeds", 1, "Attached melon stem");
		if (path.equals("kelp_plant")) return BlockEvaluation.of("minecraft:kelp", 1, "Kelp plant");
		if (path.equals("bamboo_sapling")) return BlockEvaluation.of("minecraft:bamboo", 1, "Bamboo sapling");
		if (path.equals("weeping_vines_plant")) return BlockEvaluation.of("minecraft:weeping_vines", 1, "Weeping vines plant");
		if (path.equals("twisting_vines_plant")) return BlockEvaluation.of("minecraft:twisting_vines", 1, "Twisting vines plant");
		if (path.equals("cave_vines_plant")) return BlockEvaluation.of("minecraft:glow_berries", 1, "Cave vines plant");

		return BlockEvaluation.of(BlockStateHelper.resolveFallbackItem(cleanName), 1, "Standard item");
	}

	private static int parseIntProp(CompoundTag props, String key, int def) {
		try {
			if (props.contains(key)) {
				return Integer.parseInt(props.getStringOr(key, String.valueOf(def)));
			}
		} catch (Throwable ignored) {
		}
		return def;
	}

	private static CompoundTag extractPropertiesFromString(String raw) {
		CompoundTag tag = new CompoundTag();
		int start = raw.indexOf('[');
		int end = raw.indexOf(']');
		if (start >= 0 && end > start) {
			String propsStr = raw.substring(start + 1, end);
			String[] pairs = propsStr.split(",");
			for (String pair : pairs) {
				String[] kv = pair.split("=");
				if (kv.length == 2) {
					tag.putString(kv[0].trim(), kv[1].trim());
				}
			}
		}
		return tag;
	}

	private static boolean isUnobtainableBlock(Block block, String path) {
		if (block == Blocks.WATER || block == Blocks.LAVA
			|| block == Blocks.FIRE || block == Blocks.SOUL_FIRE
			|| block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL
			|| block == Blocks.END_GATEWAY || block == Blocks.BUBBLE_COLUMN
			|| block == Blocks.FROSTED_ICE) {
			return true;
		}
		return BlockStateHelper.isKnownUnobtainableName(path);
	}

	private static String resolveItem(BlockState state, String fallbackFullId) {
		if (state != null) {
			Item item = state.getBlock().asItem();
			if (item != null && item != Items.AIR && BuiltInRegistries.ITEM != null) {
				Identifier key = BuiltInRegistries.ITEM.getKey(item);
				if (key != null) {
					return key.toString();
				}
			}
		}
		return BlockStateHelper.resolveFallbackItem(fallbackFullId);
	}

	private static String resolveBlockItem(Block block) {
		if (block != null) {
			Item item = block.asItem();
			if (item != null && item != Items.AIR && BuiltInRegistries.ITEM != null) {
				Identifier key = BuiltInRegistries.ITEM.getKey(item);
				if (key != null) {
					return key.toString();
				}
			}
		}
		Identifier key = BuiltInRegistries.BLOCK.getKey(block);
		return key != null ? key.toString() : "minecraft:air";
	}

	/**
	 * Single master method (Rule 10): retrieves material counts for a schematic,
	 * either overall (layer < 0) or for a specific Y layer (layer >= 0).
	 */
	public static Map<String, Long> getMaterialCounts(LitematicData data, int layer) {
		if (data == null) {
			return Collections.emptyMap();
		}
		MaterialCountResult result = getCounts(data);
		return result.getCountsForLayer(layer);
	}

	/**
	 * Retrieves or computes the full MaterialCountResult for a schematic (cached, Rule 11).
	 */
	public static MaterialCountResult getCounts(LitematicData data) {
		if (data == null) {
			return emptyResult();
		}
		return RESULT_CACHE.computeIfAbsent(data, MaterialCountingEngine::computeResult);
	}

	public static void invalidateCache() {
		RESULT_CACHE.clear();
	}

	private static MaterialCountResult computeResult(LitematicData data) {
		if (data.materialResult() != null) {
			return data.materialResult();
		}

		// Fallback construction from basic counts
		Map<String, Long> counts = data.itemCounts();
		List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
			.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
			.toList();

		Set<String> creativeOnly = new HashSet<>();
		for (String item : counts.keySet()) {
			if (isCreativeOnly(item)) {
				creativeOnly.add(item);
			}
		}

		return new MaterialCountResult(
			counts,
			Collections.emptyMap(),
			sorted,
			data.totalBlocks(),
			data.totalItems(),
			data.unobtainableBlocks(),
			data.unknownBlocks(),
			creativeOnly
		);
	}

	private static MaterialCountResult emptyResult() {
		return new MaterialCountResult(
			Collections.emptyMap(),
			Collections.emptyMap(),
			Collections.emptyList(),
			0L, 0L, 0L, 0L,
			Collections.emptySet()
		);
	}

	/**
	 * Builder for accumulating exact material counts across 3D coordinates.
	 */
	public static class Collector {
		private final Map<String, Long> overallCounts = new HashMap<>();
		private final Map<Integer, Map<String, Long>> layerCounts = new HashMap<>();
		private long totalVoxels = 0;
		private long totalItemsNeeded = 0;
		private long unobtainableCount = 0;
		private long unknownCount = 0;

		public void addVoxel(int y, BlockEvaluation eval, int multiplier) {
			if (multiplier <= 0 || eval == null || eval.isAir()) return;
			totalVoxels += multiplier;

			if (eval.isUnobtainable()) {
				unobtainableCount += multiplier;
				return;
			}
			if (eval.isUnknown()) {
				unknownCount += multiplier;
				return;
			}
			if (eval.isSkipped()) {
				return;
			}

			for (ItemRequirement req : eval.items()) {
				long needed = (long) req.count() * multiplier;
				if (needed > 0 && req.itemIdentifier() != null) {
					overallCounts.merge(req.itemIdentifier(), needed, Long::sum);
					totalItemsNeeded += needed;

					layerCounts.computeIfAbsent(y, k -> new HashMap<>())
						.merge(req.itemIdentifier(), needed, Long::sum);
				}
			}
		}

		public MaterialCountResult build() {
			List<Map.Entry<String, Long>> sorted = overallCounts.entrySet().stream()
				.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
				.toList();

			Set<String> creative = new HashSet<>();
			for (String item : overallCounts.keySet()) {
				if (isCreativeOnly(item)) {
					creative.add(item);
				}
			}

			// Freeze layer maps
			Map<Integer, Map<String, Long>> frozenLayers = new HashMap<>();
			for (Map.Entry<Integer, Map<String, Long>> entry : layerCounts.entrySet()) {
				frozenLayers.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
			}

			return new MaterialCountResult(
				Collections.unmodifiableMap(overallCounts),
				Collections.unmodifiableMap(frozenLayers),
				sorted,
				totalVoxels,
				totalItemsNeeded,
				unobtainableCount,
				unknownCount,
				Collections.unmodifiableSet(creative)
			);
		}
	}
}
