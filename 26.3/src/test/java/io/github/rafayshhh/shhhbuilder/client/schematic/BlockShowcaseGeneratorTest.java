package io.github.rafayshhh.shhhbuilder.client.schematic;

import io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generates and verifies comprehensive showcase schematics covering EVERY registered block in Minecraft 26.3,
 * plus all tricky multi-part, stackable, potted, and technical block states.
 */
public class BlockShowcaseGeneratorTest {

	private static final Path OUT_DIR = Paths.get("build", "test-files");
	private static final Path LITEMATIC_FILE = OUT_DIR.resolve("block_showcase.litematic");
	private static final Path SCHEM_FILE = OUT_DIR.resolve("block_showcase.schem");
	private static final Path EXPECTED_TXT_FILE = OUT_DIR.resolve("block_showcase_expected.txt");

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	public record ShowcasePlacement(
		int x,
		int y,
		int z,
		BlockState state,
		String rawName,
		String description
	) {}

	private static Block getBlock(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	/**
	 * Builds a list of placements covering all registered blocks in their default states,
	 * plus special states for all tricky multi-part, stackable, and variant blocks.
	 */
	public static List<ShowcasePlacement> buildShowcasePlacements() {
		List<ShowcasePlacement> placements = new ArrayList<>();

		// Grid layout dimensions
		int gridWidth = 40;
		int gridLength = 40;

		// 1. Layer 0: ALL registered blocks in their default states
		int curX = 0;
		int curZ = 0;
		int curY = 0;

		for (Block block : BuiltInRegistries.BLOCK) {
			Identifier id = BuiltInRegistries.BLOCK.getKey(block);
			String idStr = (id != null) ? id.toString() : "minecraft:air";
			BlockState defState = block.defaultBlockState();

			placements.add(new ShowcasePlacement(curX, curY, curZ, defState, idStr, "Default state of " + idStr));

			curX++;
			if (curX >= gridWidth) {
				curX = 0;
				curZ++;
				if (curZ >= gridLength) {
					curZ = 0;
					curY++;
				}
			}
		}

		// Advance to next layer for tricky and special blocks
		curY++;
		curX = 0;
		curZ = 0;

		// 2. Layer curY+: Tricky test blocks specifically requested
		List<BlockState> trickyStates = new ArrayList<>();

		// a) Slabs: double slab
		if (Blocks.OAK_SLAB.defaultBlockState().hasProperty(BlockStateProperties.SLAB_TYPE)) {
			trickyStates.add(Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE));
			trickyStates.add(Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE));
			trickyStates.add(Blocks.DARK_PRISMARINE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE));
		}

		// b) Doors: lower and upper
		if (Blocks.OAK_DOOR.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			trickyStates.add(Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
			trickyStates.add(Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
			trickyStates.add(Blocks.IRON_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
			trickyStates.add(Blocks.IRON_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		}

		// c) Beds: foot and head
		Block redBed = getBlock("minecraft:red_bed");
		if (redBed.defaultBlockState().hasProperty(BlockStateProperties.BED_PART)) {
			trickyStates.add(redBed.defaultBlockState().setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
			trickyStates.add(redBed.defaultBlockState().setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
		}
		Block blueBed = getBlock("minecraft:blue_bed");
		if (blueBed.defaultBlockState().hasProperty(BlockStateProperties.BED_PART)) {
			trickyStates.add(blueBed.defaultBlockState().setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
			trickyStates.add(blueBed.defaultBlockState().setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
		}

		// d) Double-height plants: lower and upper
		for (Block plantBlock : List.of(Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.TALL_SEAGRASS)) {
			if (plantBlock.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
				trickyStates.add(plantBlock.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
				trickyStates.add(plantBlock.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
			}
		}

		// e) Stackables: Candles 1..4
		for (int c = 1; c <= 4; c++) {
			trickyStates.add(Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, c));
		}
		Block redCandle = getBlock("minecraft:red_candle");
		if (redCandle.defaultBlockState().hasProperty(BlockStateProperties.CANDLES)) {
			for (int c = 1; c <= 4; c++) {
				trickyStates.add(redCandle.defaultBlockState().setValue(BlockStateProperties.CANDLES, c));
			}
		}

		// f) Stackables: Sea Pickles 1..4
		for (int p = 1; p <= 4; p++) {
			trickyStates.add(Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES, p));
		}

		// g) Stackables: Turtle Eggs 1..4
		for (int e = 1; e <= 4; e++) {
			trickyStates.add(Blocks.TURTLE_EGG.defaultBlockState().setValue(BlockStateProperties.EGGS, e));
		}

		// h) Stackables: Snow layers 1..8
		for (int l = 1; l <= 8; l++) {
			trickyStates.add(Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, l));
		}

		// i) Stackables: Pink Petals 1..4, Wildflowers
		if (Blocks.PINK_PETALS.defaultBlockState().hasProperty(BlockStateProperties.FLOWER_AMOUNT)) {
			for (int f = 1; f <= 4; f++) {
				trickyStates.add(Blocks.PINK_PETALS.defaultBlockState().setValue(BlockStateProperties.FLOWER_AMOUNT, f));
			}
		}
		if (Blocks.WILDFLOWERS.defaultBlockState().hasProperty(BlockStateProperties.FLOWER_AMOUNT)) {
			for (int f = 1; f <= 4; f++) {
				trickyStates.add(Blocks.WILDFLOWERS.defaultBlockState().setValue(BlockStateProperties.FLOWER_AMOUNT, f));
			}
		}
		if (Blocks.LEAF_LITTER.defaultBlockState().hasProperty(BlockStateProperties.SEGMENT_AMOUNT)) {
			for (int s = 1; s <= 4; s++) {
				trickyStates.add(Blocks.LEAF_LITTER.defaultBlockState().setValue(BlockStateProperties.SEGMENT_AMOUNT, s));
			}
		}

		// j) Potted plants (flower pot + potted plant item)
		trickyStates.add(Blocks.FLOWER_POT.defaultBlockState());
		trickyStates.add(Blocks.POTTED_POPPY.defaultBlockState());
		trickyStates.add(Blocks.POTTED_DANDELION.defaultBlockState());
		trickyStates.add(Blocks.POTTED_OAK_SAPLING.defaultBlockState());
		trickyStates.add(Blocks.POTTED_FERN.defaultBlockState());
		trickyStates.add(Blocks.POTTED_AZALEA.defaultBlockState());

		// k) Cauldrons
		trickyStates.add(Blocks.CAULDRON.defaultBlockState());
		trickyStates.add(Blocks.WATER_CAULDRON.defaultBlockState());
		trickyStates.add(Blocks.LAVA_CAULDRON.defaultBlockState());
		trickyStates.add(Blocks.POWDER_SNOW_CAULDRON.defaultBlockState());

		// l) Pistons
		trickyStates.add(Blocks.PISTON.defaultBlockState());
		trickyStates.add(Blocks.STICKY_PISTON.defaultBlockState());
		trickyStates.add(Blocks.PISTON_HEAD.defaultBlockState());
		trickyStates.add(Blocks.MOVING_PISTON.defaultBlockState());

		// m) Wall versions of torches, signs, banners, heads, corals
		trickyStates.add(Blocks.TORCH.defaultBlockState());
		trickyStates.add(Blocks.WALL_TORCH.defaultBlockState());
		trickyStates.add(Blocks.SOUL_TORCH.defaultBlockState());
		trickyStates.add(Blocks.SOUL_WALL_TORCH.defaultBlockState());
		trickyStates.add(Blocks.REDSTONE_TORCH.defaultBlockState());
		trickyStates.add(Blocks.REDSTONE_WALL_TORCH.defaultBlockState());
		trickyStates.add(Blocks.OAK_SIGN.defaultBlockState());
		trickyStates.add(Blocks.OAK_WALL_SIGN.defaultBlockState());
		trickyStates.add(Blocks.OAK_HANGING_SIGN.defaultBlockState());
		trickyStates.add(Blocks.OAK_WALL_HANGING_SIGN.defaultBlockState());
		Block whiteBanner = getBlock("minecraft:white_banner");
		trickyStates.add(whiteBanner.defaultBlockState());
		Block whiteWallBanner = getBlock("minecraft:white_wall_banner");
		trickyStates.add(whiteWallBanner.defaultBlockState());
		trickyStates.add(Blocks.SKELETON_SKULL.defaultBlockState());
		trickyStates.add(Blocks.SKELETON_WALL_SKULL.defaultBlockState());

		// n) Wire, tripwire, crops, vines
		trickyStates.add(Blocks.REDSTONE_WIRE.defaultBlockState());
		trickyStates.add(Blocks.TRIPWIRE.defaultBlockState());
		trickyStates.add(Blocks.WHEAT.defaultBlockState());
		trickyStates.add(Blocks.CARROTS.defaultBlockState());
		trickyStates.add(Blocks.POTATOES.defaultBlockState());
		trickyStates.add(Blocks.BEETROOTS.defaultBlockState());
		trickyStates.add(Blocks.NETHER_WART.defaultBlockState());
		trickyStates.add(Blocks.KELP.defaultBlockState());
		trickyStates.add(Blocks.KELP_PLANT.defaultBlockState());
		trickyStates.add(Blocks.BAMBOO.defaultBlockState());
		trickyStates.add(Blocks.BAMBOO_SAPLING.defaultBlockState());
		trickyStates.add(Blocks.SWEET_BERRY_BUSH.defaultBlockState());
		trickyStates.add(Blocks.CAVE_VINES.defaultBlockState());
		trickyStates.add(Blocks.CAVE_VINES_PLANT.defaultBlockState());
		trickyStates.add(Blocks.BIG_DRIPLEAF_STEM.defaultBlockState());

		// o) Waterlogged blocks
		if (Blocks.OAK_SLAB.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) {
			trickyStates.add(Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
			trickyStates.add(Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
			trickyStates.add(Blocks.OAK_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
		}

		// p) Fluids & unobtainables
		trickyStates.add(Blocks.WATER.defaultBlockState());
		trickyStates.add(Blocks.LAVA.defaultBlockState());
		trickyStates.add(Blocks.FIRE.defaultBlockState());
		trickyStates.add(Blocks.SOUL_FIRE.defaultBlockState());
		trickyStates.add(Blocks.NETHER_PORTAL.defaultBlockState());
		trickyStates.add(Blocks.END_PORTAL.defaultBlockState());
		trickyStates.add(Blocks.BUBBLE_COLUMN.defaultBlockState());

		for (BlockState ts : trickyStates) {
			Identifier id = BuiltInRegistries.BLOCK.getKey(ts.getBlock());
			String idStr = (id != null) ? id.toString() : "minecraft:air";
			placements.add(new ShowcasePlacement(curX, curY, curZ, ts, idStr, "Tricky state: " + ts));

			curX++;
			if (curX >= gridWidth) {
				curX = 0;
				curZ++;
				if (curZ >= gridLength) {
					curZ = 0;
					curY++;
				}
			}
		}

		return placements;
	}

	@Test
	public void generateAndVerifyBlockShowcase() throws Exception {
		Files.createDirectories(OUT_DIR);

		List<ShowcasePlacement> placements = buildShowcasePlacements();
		assertTrue(placements.size() > 1000, "Should have over 1000 placements covering all blocks and tricky states: " + placements.size());

		// Calculate enclosing bounds
		int maxX = 0, maxY = 0, maxZ = 0;
		for (ShowcasePlacement p : placements) {
			maxX = Math.max(maxX, p.x);
			maxY = Math.max(maxY, p.y);
			maxZ = Math.max(maxZ, p.z);
		}

		int width = maxX + 1;
		int height = maxY + 1;
		int length = maxZ + 1;

		// 1. Build Expected Materials Breakdown
		Map<String, Long> expectedOverallItems = new TreeMap<>();
		Map<Integer, Map<String, Long>> expectedLayerItems = new HashMap<>();
		long expectedTotalVoxels = 0;
		long expectedTotalItems = 0;
		long expectedUnobtainable = 0;

		for (ShowcasePlacement p : placements) {
			if (p.state.isAir()) continue;

			expectedTotalVoxels++;
			MaterialCountingEngine.BlockEvaluation eval = MaterialCountingEngine.evaluate(p.state);

			if (eval.isUnobtainable()) {
				expectedUnobtainable++;
			}

			for (MaterialCountingEngine.ItemRequirement req : eval.items()) {
				String itemId = req.itemIdentifier();
				long count = req.count();
				expectedTotalItems += count;

				expectedOverallItems.merge(itemId, count, Long::sum);
				expectedLayerItems.computeIfAbsent(p.y, k -> new TreeMap<>()).merge(itemId, count, Long::sum);
			}
		}

		// 2. Write block_showcase_expected.txt
		StringBuilder expectedTxt = new StringBuilder();
		expectedTxt.append("# Shhh Builder - Block Showcase Expected Materials\n");
		expectedTxt.append("# Total blocks (voxels): ").append(expectedTotalVoxels).append("\n");
		expectedTxt.append("# Total items needed: ").append(expectedTotalItems).append("\n");
		expectedTxt.append("# Unobtainable blocks (water, fire, portals): ").append(expectedUnobtainable).append("\n");
		expectedTxt.append("# Format: <item_identifier>: <count>\n\n");

		for (Map.Entry<String, Long> entry : expectedOverallItems.entrySet()) {
			expectedTxt.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
		}

		Files.writeString(EXPECTED_TXT_FILE, expectedTxt.toString());
		System.out.println("Generated expected materials file at: " + EXPECTED_TXT_FILE.toAbsolutePath());

		// 3. Generate block_showcase.litematic
		generateLitematicFile(LITEMATIC_FILE, placements, width, height, length);
		System.out.println("Generated litematic showcase at: " + LITEMATIC_FILE.toAbsolutePath());

		// 4. Generate block_showcase.schem (Sponge v2)
		generateSpongeSchemFile(SCHEM_FILE, placements, width, height, length);
		System.out.println("Generated sponge schem showcase at: " + SCHEM_FILE.toAbsolutePath());

		// 5. Read both files with mod readers and assert:
		LitematicData litematicData = LitematicReader.read(LITEMATIC_FILE);
		assertNotNull(litematicData, "Litematic reader must successfully parse showcase");

		LitematicData schemData = SchemReader.read(SCHEM_FILE);
		assertNotNull(schemData, "Schem reader must successfully parse showcase");

		// Assertion 1: Total blocks (voxels) match expected
		assertEquals(expectedTotalVoxels, litematicData.totalBlocks(), "Litematic total blocks must match expected voxels");
		assertEquals(expectedTotalVoxels, schemData.totalBlocks(), "Schem total blocks must match expected voxels");

		// Assertion 2: Total items needed match expected
		assertEquals(expectedTotalItems, litematicData.totalItems(), "Litematic total items must match expected items");
		assertEquals(expectedTotalItems, schemData.totalItems(), "Schem total items must match expected items");

		// Assertion 3: Unobtainables match expected
		assertEquals(expectedUnobtainable, litematicData.unobtainableBlocks(), "Litematic unobtainables must match");
		assertEquals(expectedUnobtainable, schemData.unobtainableBlocks(), "Schem unobtainables must match");

		// Assertion 4: Material item breakdown matches expected exactly
		assertEquals(expectedOverallItems, litematicData.itemCounts(), "Litematic item breakdown must equal expected txt");
		assertEquals(expectedOverallItems, schemData.itemCounts(), "Schem item breakdown must equal expected txt");

		// Assertion 5: Litematic and Schem produce the EXACT SAME material counts
		assertEquals(litematicData.itemCounts(), schemData.itemCounts(), "Litematic and Schem must produce the exact same item counts");

		// Assertion 6: Per-layer material counts sum to overall material counts
		Map<String, Long> litematicLayerSum = new TreeMap<>();
		for (int y = 0; y < height; y++) {
			Map<String, Long> layerCounts = litematicData.getMaterialCountsForLayer(y);
			layerCounts.forEach((id, count) -> litematicLayerSum.merge(id, count, Long::sum));
		}
		assertEquals(litematicData.itemCounts(), litematicLayerSum, "Per-layer material sums must equal overall counts in Litematic");

		Map<String, Long> schemLayerSum = new TreeMap<>();
		for (int y = 0; y < height; y++) {
			Map<String, Long> layerCounts = schemData.getMaterialCountsForLayer(y);
			layerCounts.forEach((id, count) -> schemLayerSum.merge(id, count, Long::sum));
		}
		assertEquals(schemData.itemCounts(), schemLayerSum, "Per-layer material sums must equal overall counts in Schem");

		// Assertion 7: Creative-only items are correctly flagged
		int creativeCount = 0;
		for (String itemId : litematicData.itemCounts().keySet()) {
			if (MaterialCountingEngine.isCreativeOnly(itemId)) {
				creativeCount++;
			}
		}
		assertTrue(creativeCount > 0, "Should have flagged several creative-only items: " + creativeCount);

		// Assertion 8: Tricky specific assertions
		// Potted poppy should require flower_pot and poppy
		assertTrue(litematicData.itemCounts().containsKey("minecraft:flower_pot"), "Must need flower_pot");
		assertTrue(litematicData.itemCounts().containsKey("minecraft:poppy"), "Must need poppy");

		// Double slabs require 2 slab items
		assertTrue(litematicData.itemCounts().containsKey("minecraft:oak_slab"), "Must need oak_slab");

		// Piston heads must not be in item requirements
		assertFalse(litematicData.itemCounts().containsKey("minecraft:piston_head"), "piston_head must NEVER be an item requirement");
		assertFalse(litematicData.itemCounts().containsKey("minecraft:moving_piston"), "moving_piston must NEVER be an item requirement");

		// Water must not be an item requirement
		assertFalse(litematicData.itemCounts().containsKey("minecraft:water"), "water must NEVER be an item requirement");

		System.out.println("ALL SHOWCASE ASSERTIONS PASSED! Verified " + placements.size() + " blocks and states.");
	}

	private static String serializeStateToString(BlockState state) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String name = (id != null) ? id.toString() : "minecraft:air";
		if (state.getProperties().isEmpty()) {
			return name;
		}
		StringBuilder sb = new StringBuilder(name).append("[");
		boolean first = true;
		for (Property<?> prop : state.getProperties()) {
			if (!first) sb.append(",");
			first = false;
			sb.append(prop.getName()).append("=").append(state.getValue(prop).toString().toLowerCase(Locale.ROOT));
		}
		sb.append("]");
		return sb.toString();
	}

	private static void generateLitematicFile(Path path, List<ShowcasePlacement> placements, int width, int height, int length) throws IOException {
		CompoundTag root = new CompoundTag();
		root.putInt("Version", 5);
		root.putInt("MinecraftDataVersion", 4444);

		CompoundTag meta = new CompoundTag();
		meta.putString("Name", "block_showcase");
		meta.putString("Author", "ShhhBuilder");
		meta.putLong("TotalBlocks", placements.size());

		CompoundTag encSize = new CompoundTag();
		encSize.putInt("x", width);
		encSize.putInt("y", height);
		encSize.putInt("z", length);
		meta.put("EnclosingSize", encSize);
		root.put("Metadata", meta);

		CompoundTag regions = new CompoundTag();
		CompoundTag region = new CompoundTag();

		CompoundTag pos = new CompoundTag();
		pos.putInt("x", 0);
		pos.putInt("y", 0);
		pos.putInt("z", 0);
		region.put("Position", pos);

		CompoundTag size = new CompoundTag();
		size.putInt("x", width);
		size.putInt("y", height);
		size.putInt("z", length);
		region.put("Size", size);

		// Palette mapping
		List<BlockState> uniqueStates = new ArrayList<>();
		uniqueStates.add(Blocks.AIR.defaultBlockState()); // 0 is air
		Map<BlockState, Integer> stateToIdx = new HashMap<>();
		stateToIdx.put(Blocks.AIR.defaultBlockState(), 0);

		for (ShowcasePlacement p : placements) {
			if (!stateToIdx.containsKey(p.state)) {
				stateToIdx.put(p.state, uniqueStates.size());
				uniqueStates.add(p.state);
			}
		}

		ListTag paletteTag = new ListTag();
		for (BlockState state : uniqueStates) {
			CompoundTag stateTag = new CompoundTag();
			Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			stateTag.putString("Name", (id != null) ? id.toString() : "minecraft:air");

			if (!state.getProperties().isEmpty()) {
				CompoundTag props = new CompoundTag();
				for (Property<?> prop : state.getProperties()) {
					props.putString(prop.getName(), state.getValue(prop).toString().toLowerCase(Locale.ROOT));
				}
				stateTag.put("Properties", props);
			}
			paletteTag.add(stateTag);
		}
		region.put("BlockStatePalette", paletteTag);

		long volume = (long) width * height * length;
		int bitsPerBlock = LitematicReader.getBitsPerBlock(uniqueStates.size());
		int totalLongs = (int) (((volume * bitsPerBlock) + 63) / 64);
		long[] blockStates = new long[totalLongs];

		for (ShowcasePlacement p : placements) {
			int idx = (p.y * length + p.z) * width + p.x;
			int palIdx = stateToIdx.getOrDefault(p.state, 0);
			LitematicReader.packBlockStateIndex(blockStates, idx, bitsPerBlock, palIdx);
		}

		region.putLongArray("BlockStates", blockStates);
		regions.put("Main", region);
		root.put("Regions", regions);

		NbtIo.writeCompressed(root, path);
	}

	private static void generateSpongeSchemFile(Path path, List<ShowcasePlacement> placements, int width, int height, int length) throws IOException {
		CompoundTag root = new CompoundTag();
		CompoundTag schem = new CompoundTag();
		schem.putInt("Version", 2);
		schem.putInt("DataVersion", 4444);
		schem.putShort("Width", (short) width);
		schem.putShort("Height", (short) height);
		schem.putShort("Length", (short) length);

		CompoundTag meta = new CompoundTag();
		meta.putString("Name", "block_showcase");
		schem.put("Metadata", meta);

		// Palette mapping
		Map<String, Integer> paletteMap = new LinkedHashMap<>();
		paletteMap.put("minecraft:air", 0);

		for (ShowcasePlacement p : placements) {
			String serialized = serializeStateToString(p.state);
			paletteMap.putIfAbsent(serialized, paletteMap.size());
		}

		CompoundTag paletteTag = new CompoundTag();
		paletteMap.forEach(paletteTag::putInt);
		schem.put("Palette", paletteTag);

		int volume = width * height * length;
		// Sponge v2 uses VarInt encoding for BlockData
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();

		int[] grid = new int[volume];
		for (ShowcasePlacement p : placements) {
			int idx = (p.y * length + p.z) * width + p.x;
			String serialized = serializeStateToString(p.state);
			grid[idx] = paletteMap.getOrDefault(serialized, 0);
		}

		for (int i = 0; i < volume; i++) {
			int val = grid[i];
			while ((val & -128) != 0) {
				bos.write(val & 127 | 128);
				val >>>= 7;
			}
			bos.write(val);
		}

		schem.putByteArray("BlockData", bos.toByteArray());
		root.put("Schematic", schem);

		NbtIo.writeCompressed(root, path);
	}
}
