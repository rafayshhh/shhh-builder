package io.github.rafayshhh.shhhbuilder.client.schematic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BlockCategoryHelperTest {

	@Test
	@DisplayName("BlockCategoryHelper correctly categorizes normal 3D model blocks")
	void testNormalCategory() {
		String[] normalBlocks = {
			"minecraft:oak_planks",
			"minecraft:cobblestone",
			"minecraft:stone_stairs",
			"minecraft:smooth_stone_slab",
			"minecraft:oak_fence",
			"minecraft:cobblestone_wall",
			"minecraft:glass_pane",
			"minecraft:iron_bars",
			"minecraft:oak_door",
			"minecraft:oak_trapdoor",
			"minecraft:poppy",
			"minecraft:rail",
			"minecraft:torch",
			"minecraft:lantern",
			"minecraft:redstone_wire",
			"minecraft:repeater",
			"minecraft:comparator"
		};

		for (String id : normalBlocks) {
			assertEquals(BlockCategoryHelper.Category.NORMAL, BlockCategoryHelper.getCategory(id, null),
				"Block '" + id + "' must be classified as NORMAL");
		}
	}

	@Test
	@DisplayName("BlockCategoryHelper correctly categorizes fluids")
	void testFluidCategory() {
		String[] fluids = {
			"minecraft:water",
			"minecraft:flowing_water",
			"minecraft:lava",
			"minecraft:flowing_lava"
		};

		for (String id : fluids) {
			assertEquals(BlockCategoryHelper.Category.FLUID, BlockCategoryHelper.getCategory(id, null),
				"Fluid '" + id + "' must be classified as FLUID");
		}
	}

	@Test
	@DisplayName("BlockCategoryHelper correctly categorizes block entities with no usual model")
	void testBlockEntityCategory() {
		String[] blockEntities = {
			"minecraft:chest",
			"minecraft:trapped_chest",
			"minecraft:ender_chest",
			"minecraft:red_bed",
			"minecraft:white_bed",
			"minecraft:oak_sign",
			"minecraft:oak_wall_sign",
			"minecraft:oak_hanging_sign",
			"minecraft:oak_wall_hanging_sign",
			"minecraft:red_banner",
			"minecraft:white_wall_banner",
			"minecraft:skeleton_skull",
			"minecraft:zombie_head",
			"minecraft:creeper_head",
			"minecraft:shulker_box",
			"minecraft:black_shulker_box",
			"minecraft:bell",
			"minecraft:enchanting_table",
			"minecraft:conduit",
			"minecraft:decorated_pot",
			"minecraft:lectern",
			"minecraft:trial_spawner",
			"minecraft:vault",
			"minecraft:spawner",
			"minecraft:piston_head",
			"minecraft:moving_piston"
		};

		for (String id : blockEntities) {
			assertEquals(BlockCategoryHelper.Category.BLOCK_ENTITY, BlockCategoryHelper.getCategory(id, null),
				"Block entity '" + id + "' must be classified as BLOCK_ENTITY");
		}
	}

	@Test
	@DisplayName("BlockCategoryHelper correctly categorizes technical and invisible blocks")
	void testTechnicalCategory() {
		String[] technicalBlocks = {
			"minecraft:barrier",
			"minecraft:light",
			"minecraft:structure_void",
			"minecraft:structure_block",
			"minecraft:jigsaw"
		};

		for (String id : technicalBlocks) {
			assertEquals(BlockCategoryHelper.Category.TECHNICAL, BlockCategoryHelper.getCategory(id, null),
				"Technical block '" + id + "' must be classified as TECHNICAL");
		}
	}

	@Test
	@DisplayName("BlockCategoryHelper correctly categorizes air variants and null inputs")
	void testAirCategory() {
		assertEquals(BlockCategoryHelper.Category.AIR, BlockCategoryHelper.getCategory("minecraft:air", null));
		assertEquals(BlockCategoryHelper.Category.AIR, BlockCategoryHelper.getCategory("minecraft:cave_air", null));
		assertEquals(BlockCategoryHelper.Category.AIR, BlockCategoryHelper.getCategory("minecraft:void_air", null));
		assertEquals(BlockCategoryHelper.Category.AIR, BlockCategoryHelper.getCategory("", null));
		assertEquals(BlockCategoryHelper.Category.AIR, BlockCategoryHelper.getCategory((String) null, null));
	}
}
