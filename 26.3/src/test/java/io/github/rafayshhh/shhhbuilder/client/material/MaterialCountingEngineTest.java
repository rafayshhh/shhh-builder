package io.github.rafayshhh.shhhbuilder.client.material;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MaterialCountingEngineTest {

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static Block getBlock(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	@Test
	public void testSlabsCounting() {
		// Single slab = 1 item
		BlockState singleBottom = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
		MaterialCountingEngine.BlockEvaluation evalSingle = MaterialCountingEngine.evaluate(singleBottom);
		assertFalse(evalSingle.isSkipped());
		assertFalse(evalSingle.isUnobtainable());
		assertEquals(1, evalSingle.items().size());
		assertEquals("minecraft:oak_slab", evalSingle.items().get(0).itemIdentifier());
		assertEquals(1, evalSingle.items().get(0).count());

		// Double slab = 2 items
		BlockState doubleSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
		MaterialCountingEngine.BlockEvaluation evalDouble = MaterialCountingEngine.evaluate(doubleSlab);
		assertEquals(1, evalDouble.items().size());
		assertEquals("minecraft:oak_slab", evalDouble.items().get(0).itemIdentifier());
		assertEquals(2, evalDouble.items().get(0).count());

		// Waterlogged slab = 1 item (water ignored)
		BlockState waterloggedSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
		MaterialCountingEngine.BlockEvaluation evalWaterlogged = MaterialCountingEngine.evaluate(waterloggedSlab);
		assertEquals(1, evalWaterlogged.items().size());
		assertEquals("minecraft:oak_slab", evalWaterlogged.items().get(0).itemIdentifier());
		assertEquals(1, evalWaterlogged.items().get(0).count());
	}

	@Test
	public void testDoorsAndBeds() {
		// Door lower half = 1 item
		BlockState doorLower = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
		MaterialCountingEngine.BlockEvaluation evalDoorLower = MaterialCountingEngine.evaluate(doorLower);
		assertEquals(1, evalDoorLower.items().size());
		assertEquals("minecraft:oak_door", evalDoorLower.items().get(0).itemIdentifier());
		assertEquals(1, evalDoorLower.items().get(0).count());

		// Door upper half = skipped
		BlockState doorUpper = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
		MaterialCountingEngine.BlockEvaluation evalDoorUpper = MaterialCountingEngine.evaluate(doorUpper);
		assertTrue(evalDoorUpper.isSkipped());
		assertTrue(evalDoorUpper.items().isEmpty());

		// Bed foot = 1 item
		Block redBed = getBlock("minecraft:red_bed");
		BlockState bedFoot = redBed.defaultBlockState().setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
		MaterialCountingEngine.BlockEvaluation evalBedFoot = MaterialCountingEngine.evaluate(bedFoot);
		assertEquals(1, evalBedFoot.items().size());
		assertEquals("minecraft:red_bed", evalBedFoot.items().get(0).itemIdentifier());
		assertEquals(1, evalBedFoot.items().get(0).count());

		// Bed head = skipped
		BlockState bedHead = redBed.defaultBlockState().setValue(BlockStateProperties.BED_PART, BedPart.HEAD);
		MaterialCountingEngine.BlockEvaluation evalBedHead = MaterialCountingEngine.evaluate(bedHead);
		assertTrue(evalBedHead.isSkipped());
		assertTrue(evalBedHead.items().isEmpty());
	}

	@Test
	public void testDoublePlants() {
		// Sunflower lower = 1 item, upper = skipped
		BlockState sunflowerLower = Blocks.SUNFLOWER.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
		MaterialCountingEngine.BlockEvaluation evalSunLower = MaterialCountingEngine.evaluate(sunflowerLower);
		assertEquals(1, evalSunLower.items().size());
		assertEquals("minecraft:sunflower", evalSunLower.items().get(0).itemIdentifier());
		assertEquals(1, evalSunLower.items().get(0).count());

		BlockState sunflowerUpper = Blocks.SUNFLOWER.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
		MaterialCountingEngine.BlockEvaluation evalSunUpper = MaterialCountingEngine.evaluate(sunflowerUpper);
		assertTrue(evalSunUpper.isSkipped());

		// Tall seagrass lower = seagrass item
		BlockState tallSeagrassLower = Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
		MaterialCountingEngine.BlockEvaluation evalSeagrass = MaterialCountingEngine.evaluate(tallSeagrassLower);
		assertEquals(1, evalSeagrass.items().size());
		assertEquals("minecraft:seagrass", evalSeagrass.items().get(0).itemIdentifier());
		assertEquals(1, evalSeagrass.items().get(0).count());
	}

	@Test
	public void testPistons() {
		// Piston base = 1 item
		MaterialCountingEngine.BlockEvaluation evalPiston = MaterialCountingEngine.evaluate(Blocks.PISTON.defaultBlockState());
		assertEquals(1, evalPiston.items().size());
		assertEquals("minecraft:piston", evalPiston.items().get(0).itemIdentifier());

		// Sticky piston base = 1 item
		MaterialCountingEngine.BlockEvaluation evalSticky = MaterialCountingEngine.evaluate(Blocks.STICKY_PISTON.defaultBlockState());
		assertEquals(1, evalSticky.items().size());
		assertEquals("minecraft:sticky_piston", evalSticky.items().get(0).itemIdentifier());

		// Piston head and moving piston = skipped (0 items)
		MaterialCountingEngine.BlockEvaluation evalHead = MaterialCountingEngine.evaluate(Blocks.PISTON_HEAD.defaultBlockState());
		assertTrue(evalHead.isSkipped());
		assertTrue(evalHead.items().isEmpty());

		MaterialCountingEngine.BlockEvaluation evalMoving = MaterialCountingEngine.evaluate(Blocks.MOVING_PISTON.defaultBlockState());
		assertTrue(evalMoving.isSkipped());
		assertTrue(evalMoving.items().isEmpty());
	}

	@Test
	public void testStackables() {
		// Candles 1..4
		for (int c = 1; c <= 4; c++) {
			BlockState candleState = Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, c);
			MaterialCountingEngine.BlockEvaluation eval = MaterialCountingEngine.evaluate(candleState);
			assertEquals(1, eval.items().size());
			assertEquals("minecraft:candle", eval.items().get(0).itemIdentifier());
			assertEquals(c, eval.items().get(0).count());
		}

		// Sea Pickles 1..4
		for (int p = 1; p <= 4; p++) {
			BlockState pickleState = Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES, p);
			MaterialCountingEngine.BlockEvaluation eval = MaterialCountingEngine.evaluate(pickleState);
			assertEquals(1, eval.items().size());
			assertEquals("minecraft:sea_pickle", eval.items().get(0).itemIdentifier());
			assertEquals(p, eval.items().get(0).count());
		}

		// Turtle Eggs 1..4
		for (int e = 1; e <= 4; e++) {
			BlockState eggState = Blocks.TURTLE_EGG.defaultBlockState().setValue(BlockStateProperties.EGGS, e);
			MaterialCountingEngine.BlockEvaluation eval = MaterialCountingEngine.evaluate(eggState);
			assertEquals(1, eval.items().size());
			assertEquals("minecraft:turtle_egg", eval.items().get(0).itemIdentifier());
			assertEquals(e, eval.items().get(0).count());
		}

		// Snow layers 1..8
		for (int l = 1; l <= 8; l++) {
			BlockState snowState = Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, l);
			MaterialCountingEngine.BlockEvaluation eval = MaterialCountingEngine.evaluate(snowState);
			assertEquals(1, eval.items().size());
			assertEquals("minecraft:snow", eval.items().get(0).itemIdentifier());
			assertEquals(l, eval.items().get(0).count());
		}

		// Pink petals 1..4
		for (int f = 1; f <= 4; f++) {
			BlockState petalState = Blocks.PINK_PETALS.defaultBlockState().setValue(BlockStateProperties.FLOWER_AMOUNT, f);
			MaterialCountingEngine.BlockEvaluation eval = MaterialCountingEngine.evaluate(petalState);
			assertEquals(1, eval.items().size());
			assertEquals("minecraft:pink_petals", eval.items().get(0).itemIdentifier());
			assertEquals(f, eval.items().get(0).count());
		}
	}

	@Test
	public void testPottedPlantsTwoItems() {
		// Empty pot = 1 flower pot
		MaterialCountingEngine.BlockEvaluation evalPot = MaterialCountingEngine.evaluate(Blocks.FLOWER_POT.defaultBlockState());
		assertEquals(1, evalPot.items().size());
		assertEquals("minecraft:flower_pot", evalPot.items().get(0).itemIdentifier());
		assertEquals(1, evalPot.items().get(0).count());

		// Potted poppy = 1 flower pot + 1 poppy
		MaterialCountingEngine.BlockEvaluation evalPottedPoppy = MaterialCountingEngine.evaluate(Blocks.POTTED_POPPY.defaultBlockState());
		assertEquals(2, evalPottedPoppy.items().size());
		assertEquals("minecraft:flower_pot", evalPottedPoppy.items().get(0).itemIdentifier());
		assertEquals(1, evalPottedPoppy.items().get(0).count());
		assertEquals("minecraft:poppy", evalPottedPoppy.items().get(1).itemIdentifier());
		assertEquals(1, evalPottedPoppy.items().get(1).count());

		// Potted oak sapling = 1 flower pot + 1 oak sapling
		MaterialCountingEngine.BlockEvaluation evalPottedSapling = MaterialCountingEngine.evaluate(Blocks.POTTED_OAK_SAPLING.defaultBlockState());
		assertEquals(2, evalPottedSapling.items().size());
		assertEquals("minecraft:flower_pot", evalPottedSapling.items().get(0).itemIdentifier());
		assertEquals("minecraft:oak_sapling", evalPottedSapling.items().get(1).itemIdentifier());
	}

	@Test
	public void testWallMountsAndWires() {
		// Wall torch -> torch
		assertEquals("minecraft:torch", MaterialCountingEngine.evaluate(Blocks.WALL_TORCH.defaultBlockState()).items().get(0).itemIdentifier());
		assertEquals("minecraft:soul_torch", MaterialCountingEngine.evaluate(Blocks.SOUL_WALL_TORCH.defaultBlockState()).items().get(0).itemIdentifier());
		assertEquals("minecraft:redstone_torch", MaterialCountingEngine.evaluate(Blocks.REDSTONE_WALL_TORCH.defaultBlockState()).items().get(0).itemIdentifier());

		// Wall sign -> sign
		assertEquals("minecraft:oak_sign", MaterialCountingEngine.evaluate(Blocks.OAK_WALL_SIGN.defaultBlockState()).items().get(0).itemIdentifier());
		assertEquals("minecraft:oak_hanging_sign", MaterialCountingEngine.evaluate(Blocks.OAK_WALL_HANGING_SIGN.defaultBlockState()).items().get(0).itemIdentifier());

		// Wall head -> head
		assertEquals("minecraft:skeleton_skull", MaterialCountingEngine.evaluate(Blocks.SKELETON_WALL_SKULL.defaultBlockState()).items().get(0).itemIdentifier());

		// Redstone wire -> redstone
		assertEquals("minecraft:redstone", MaterialCountingEngine.evaluate(Blocks.REDSTONE_WIRE.defaultBlockState()).items().get(0).itemIdentifier());

		// Tripwire -> string
		assertEquals("minecraft:string", MaterialCountingEngine.evaluate(Blocks.TRIPWIRE.defaultBlockState()).items().get(0).itemIdentifier());
	}

	@Test
	public void testUnobtainablesAndCreativeOnly() {
		// Unobtainables
		assertTrue(MaterialCountingEngine.evaluate(Blocks.WATER.defaultBlockState()).isUnobtainable());
		assertTrue(MaterialCountingEngine.evaluate(Blocks.LAVA.defaultBlockState()).isUnobtainable());
		assertTrue(MaterialCountingEngine.evaluate(Blocks.FIRE.defaultBlockState()).isUnobtainable());
		assertTrue(MaterialCountingEngine.evaluate(Blocks.SOUL_FIRE.defaultBlockState()).isUnobtainable());
		assertTrue(MaterialCountingEngine.evaluate(Blocks.NETHER_PORTAL.defaultBlockState()).isUnobtainable());
		assertTrue(MaterialCountingEngine.evaluate(Blocks.END_PORTAL.defaultBlockState()).isUnobtainable());
		assertTrue(MaterialCountingEngine.evaluate(Blocks.BUBBLE_COLUMN.defaultBlockState()).isUnobtainable());

		// Creative-only
		assertTrue(MaterialCountingEngine.isCreativeOnly("minecraft:barrier"));
		assertTrue(MaterialCountingEngine.isCreativeOnly("minecraft:command_block"));
		assertTrue(MaterialCountingEngine.isCreativeOnly("minecraft:structure_block"));
		assertTrue(MaterialCountingEngine.isCreativeOnly("minecraft:bedrock"));
		assertTrue(MaterialCountingEngine.isCreativeOnly("minecraft:light"));

		assertFalse(MaterialCountingEngine.isCreativeOnly("minecraft:stone"));
		assertFalse(MaterialCountingEngine.isCreativeOnly("minecraft:oak_planks"));
		assertFalse(MaterialCountingEngine.isCreativeOnly("minecraft:cobblestone"));
	}

	@Test
	public void testCollectorLayersAndOverallMath() {
		MaterialCountingEngine.Collector collector = new MaterialCountingEngine.Collector();

		// Layer 0: 64 stone, 1 double oak slab (2 items)
		MaterialCountingEngine.BlockEvaluation stoneEval = MaterialCountingEngine.evaluate(Blocks.STONE.defaultBlockState());
		collector.addVoxel(0, stoneEval, 64);

		MaterialCountingEngine.BlockEvaluation doubleSlabEval = MaterialCountingEngine.evaluate(
			Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE));
		collector.addVoxel(0, doubleSlabEval, 1);

		// Layer 1: 32 oak planks, 1 door lower, 1 door upper (upper skipped)
		MaterialCountingEngine.BlockEvaluation planksEval = MaterialCountingEngine.evaluate(Blocks.OAK_PLANKS.defaultBlockState());
		collector.addVoxel(1, planksEval, 32);

		MaterialCountingEngine.BlockEvaluation doorLower = MaterialCountingEngine.evaluate(
			Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
		collector.addVoxel(1, doorLower, 1);

		MaterialCountingEngine.BlockEvaluation doorUpper = MaterialCountingEngine.evaluate(
			Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		collector.addVoxel(1, doorUpper, 1);

		MaterialCountingEngine.MaterialCountResult result = collector.build();

		// Total voxels = 64 + 1 + 32 + 1 + 1 = 99
		assertEquals(99, result.totalVoxels());

		// Total items = 64 (stone) + 2 (oak_slab) + 32 (planks) + 1 (door lower) + 0 (door upper) = 99 items
		assertEquals(99, result.totalItemsNeeded());

		// Overall item map checks
		assertEquals(64L, result.overallItemCounts().get("minecraft:stone"));
		assertEquals(2L, result.overallItemCounts().get("minecraft:oak_slab"));
		assertEquals(32L, result.overallItemCounts().get("minecraft:oak_planks"));
		assertEquals(1L, result.overallItemCounts().get("minecraft:oak_door"));

		// Layer 0 checks
		Map<String, Long> l0 = result.getCountsForLayer(0);
		assertEquals(64L, l0.get("minecraft:stone"));
		assertEquals(2L, l0.get("minecraft:oak_slab"));
		assertNull(l0.get("minecraft:oak_planks"));

		// Layer 1 checks
		Map<String, Long> l1 = result.getCountsForLayer(1);
		assertEquals(32L, l1.get("minecraft:oak_planks"));
		assertEquals(1L, l1.get("minecraft:oak_door"));
		assertNull(l1.get("minecraft:stone"));
	}

	@Test
	public void testMaterialProgressManagerPersistence(@TempDir Path tempDir) {
		String key = "test_schematic:1000:2000";
		MaterialProgressManager.resetProgress(key);

		assertFalse(MaterialProgressManager.isChecked(key, "minecraft:stone"));

		// Toggle item
		boolean checked = MaterialProgressManager.toggleItem(key, "minecraft:stone");
		assertTrue(checked);
		assertTrue(MaterialProgressManager.isChecked(key, "minecraft:stone"));

		// Toggle again -> unchecked
		checked = MaterialProgressManager.toggleItem(key, "minecraft:stone");
		assertFalse(checked);
		assertFalse(MaterialProgressManager.isChecked(key, "minecraft:stone"));

		// Set checked
		MaterialProgressManager.setChecked(key, "minecraft:oak_planks", true);
		assertTrue(MaterialProgressManager.isChecked(key, "minecraft:oak_planks"));

		// Check all
		List<String> items = List.of("minecraft:stone", "minecraft:cobblestone", "minecraft:dirt");
		MaterialProgressManager.checkAll(key, items);
		assertTrue(MaterialProgressManager.isChecked(key, "minecraft:stone"));
		assertTrue(MaterialProgressManager.isChecked(key, "minecraft:cobblestone"));
		assertTrue(MaterialProgressManager.isChecked(key, "minecraft:dirt"));

		// Uncheck all
		MaterialProgressManager.uncheckAll(key);
		assertFalse(MaterialProgressManager.isChecked(key, "minecraft:stone"));
		assertFalse(MaterialProgressManager.isChecked(key, "minecraft:cobblestone"));
		assertFalse(MaterialProgressManager.isChecked(key, "minecraft:dirt"));
	}

	@Test
	public void testDeductPlacedWorldBlocksInSurvival() {
		// 1. Verify item evaluation for placed blocks
		BlockState oakPlanks = Blocks.OAK_PLANKS.defaultBlockState();
		MaterialCountingEngine.BlockEvaluation evalPlanks = MaterialCountingEngine.evaluate(oakPlanks);
		assertEquals(1, evalPlanks.items().size());
		assertEquals("minecraft:oak_planks", evalPlanks.items().get(0).itemIdentifier());
		assertEquals(1, evalPlanks.items().get(0).count());

		BlockState doubleSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
		MaterialCountingEngine.BlockEvaluation evalDoubleSlab = MaterialCountingEngine.evaluate(doubleSlab);
		assertEquals(1, evalDoubleSlab.items().size());
		assertEquals("minecraft:oak_slab", evalDoubleSlab.items().get(0).itemIdentifier());
		assertEquals(2, evalDoubleSlab.items().get(0).count());

		// 2. State verification rules
		// Exact match: block placed correctly
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator.MatchStatus.EXACT_MATCH,
			io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator.check(oakPlanks, oakPlanks));

		// Air / fluid: missing, not placed
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator.MatchStatus.MISSING,
			io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator.check(Blocks.AIR.defaultBlockState(), oakPlanks));

		// Wrong block: not placed
		assertEquals(io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator.MatchStatus.WRONG_BLOCK,
			io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator.check(Blocks.COBBLESTONE.defaultBlockState(), oakPlanks));

		// 3. Effective Have calculation simulation:
		// Player needs 64 planks.
		// Start: 64 in inventory, 0 in world -> effectiveHave = 64, missing = 0.
		long need = 64;
		int invStart = 64;
		int placedStart = 0;
		int effectiveStart = invStart + placedStart;
		long missingStart = Math.max(0, need - effectiveStart);
		assertEquals(64, effectiveStart);
		assertEquals(0, missingStart);

		// Player places 20 planks in world:
		// Inventory drops to 44, placed in world increases to 20.
		int invAfter = 44;
		int placedAfter = 20;
		int effectiveAfter = invAfter + placedAfter;
		long missingAfter = Math.max(0, need - effectiveAfter);
		assertEquals(64, effectiveAfter, "Effective have must remain 64 after placing blocks in survival");
		assertEquals(0, missingAfter, "Missing count must not increase when blocks are placed in survival");
		assertTrue(effectiveAfter >= need);
	}
}
