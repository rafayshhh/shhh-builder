package io.github.rafayshhh.shhhbuilder.client.schematic;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorldBlockValidatorTest {

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	@DisplayName("Exact Match: identical block and state skips preview")
	void testExactMatch() {
		BlockState cobblestone = Blocks.COBBLESTONE.defaultBlockState();
		assertEquals(WorldBlockValidator.MatchStatus.EXACT_MATCH,
			WorldBlockValidator.check(cobblestone, cobblestone));

		BlockState stairNorth = Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(StairBlock.FACING, Direction.NORTH)
			.setValue(StairBlock.HALF, Half.BOTTOM);
		assertEquals(WorldBlockValidator.MatchStatus.EXACT_MATCH,
			WorldBlockValidator.check(stairNorth, stairNorth));

		// Air or null expected state is treated as exact match (nothing to build)
		assertEquals(WorldBlockValidator.MatchStatus.EXACT_MATCH,
			WorldBlockValidator.check(Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState()));
		assertEquals(WorldBlockValidator.MatchStatus.EXACT_MATCH,
			WorldBlockValidator.check(Blocks.STONE.defaultBlockState(), null));
	}

	@Test
	@DisplayName("Wrong State: same block type placed with differing orientation or properties")
	void testWrongState() {
		BlockState stairNorth = Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(StairBlock.FACING, Direction.NORTH)
			.setValue(StairBlock.HALF, Half.BOTTOM);
		BlockState stairSouth = Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(StairBlock.FACING, Direction.SOUTH)
			.setValue(StairBlock.HALF, Half.BOTTOM);
		BlockState stairTop = Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(StairBlock.FACING, Direction.NORTH)
			.setValue(StairBlock.HALF, Half.TOP);

		assertEquals(WorldBlockValidator.MatchStatus.WRONG_STATE,
			WorldBlockValidator.check(stairSouth, stairNorth),
			"Stair facing South when North is expected must be WRONG_STATE");

		assertEquals(WorldBlockValidator.MatchStatus.WRONG_STATE,
			WorldBlockValidator.check(stairTop, stairNorth),
			"Stair top half when bottom half is expected must be WRONG_STATE");

		// Log axis orientation
		BlockState logY = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState logX = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		assertEquals(WorldBlockValidator.MatchStatus.WRONG_STATE,
			WorldBlockValidator.check(logX, logY),
			"Log with Axis X when Axis Y is expected must be WRONG_STATE");

		// Slab bottom vs top
		BlockState slabBottom = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
		BlockState slabTop = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
		assertEquals(WorldBlockValidator.MatchStatus.WRONG_STATE,
			WorldBlockValidator.check(slabTop, slabBottom),
			"Slab top when bottom is expected must be WRONG_STATE");
	}

	@Test
	@DisplayName("Wrong Block: completely different solid block placed")
	void testWrongBlock() {
		BlockState worldStone = Blocks.STONE.defaultBlockState();
		BlockState expectedCobblestone = Blocks.COBBLESTONE.defaultBlockState();

		assertEquals(WorldBlockValidator.MatchStatus.WRONG_BLOCK,
			WorldBlockValidator.check(worldStone, expectedCobblestone),
			"Stone placed instead of Cobblestone must be WRONG_BLOCK");

		BlockState worldDirt = Blocks.DIRT.defaultBlockState();
		BlockState expectedPlanks = Blocks.OAK_PLANKS.defaultBlockState();

		assertEquals(WorldBlockValidator.MatchStatus.WRONG_BLOCK,
			WorldBlockValidator.check(worldDirt, expectedPlanks),
			"Dirt placed instead of Oak Planks must be WRONG_BLOCK");

		BlockState worldCobbleStair = Blocks.COBBLESTONE_STAIRS.defaultBlockState();
		BlockState expectedOakStair = Blocks.OAK_STAIRS.defaultBlockState();

		assertEquals(WorldBlockValidator.MatchStatus.WRONG_BLOCK,
			WorldBlockValidator.check(worldCobbleStair, expectedOakStair),
			"Cobblestone stairs placed instead of Oak stairs must be WRONG_BLOCK");
	}

	@Test
	@DisplayName("Missing: world block is air or fluid")
	void testMissing() {
		BlockState expectedPlanks = Blocks.OAK_PLANKS.defaultBlockState();

		// Air in world
		assertEquals(WorldBlockValidator.MatchStatus.MISSING,
			WorldBlockValidator.check(Blocks.AIR.defaultBlockState(), expectedPlanks),
			"Air in world must be MISSING");

		assertEquals(WorldBlockValidator.MatchStatus.MISSING,
			WorldBlockValidator.check(null, expectedPlanks),
			"Null world state must be MISSING");

		// Fluids in world (water, lava)
		assertEquals(WorldBlockValidator.MatchStatus.MISSING,
			WorldBlockValidator.check(Blocks.WATER.defaultBlockState(), expectedPlanks),
			"Water in world must be MISSING");

		assertEquals(WorldBlockValidator.MatchStatus.MISSING,
			WorldBlockValidator.check(Blocks.LAVA.defaultBlockState(), expectedPlanks),
			"Lava in world must be MISSING");
	}

	@Test
	@DisplayName("Color constants conform to 32-bit ARGB requirements")
	void testColorConstants() {
		// Alpha must be 0xFF (full opacity)
		assertEquals(0xFFFFAA00, WorldBlockValidator.COLOR_WRONG_STATE,
			"Wrong state color must be bright Amber/Orange 0xFFFFAA00");
		assertEquals(0xFFFF3333, WorldBlockValidator.COLOR_WRONG_BLOCK,
			"Wrong block color must be bright Red 0xFFFF3333");
		assertEquals(0xFFFF5555, WorldBlockValidator.COLOR_HUD_WRONG,
			"HUD warning color must be 0xFFFF5555");
	}

	@Test
	@DisplayName("formatExpectedDetails correctly includes block name and key orientation properties")
	void testFormatExpectedDetails() {
		BlockState stair = Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(StairBlock.FACING, Direction.NORTH)
			.setValue(StairBlock.HALF, Half.BOTTOM);

		String details = WorldBlockValidator.formatExpectedDetails(stair);
		assertNotNull(details);
		assertTrue(details.toLowerCase().contains("oak stairs") || details.toLowerCase().contains("stairs"),
			"Details must contain block name: " + details);
		assertTrue(details.toLowerCase().contains("north"),
			"Details must contain facing property: " + details);
		assertTrue(details.toLowerCase().contains("bottom"),
			"Details must contain half property: " + details);

		// Simple block with no orientation properties
		BlockState stone = Blocks.STONE.defaultBlockState();
		String stoneDetails = WorldBlockValidator.formatExpectedDetails(stone);
		assertEquals("Stone", stoneDetails);
	}
}
