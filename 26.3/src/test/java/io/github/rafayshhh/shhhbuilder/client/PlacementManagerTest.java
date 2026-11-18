package io.github.rafayshhh.shhhbuilder.client;

import io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import io.github.rafayshhh.shhhbuilder.client.schematic.SchematicBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlacementManagerTest {

	@BeforeAll
	public static void initRegistries() {
		try {
			net.minecraft.SharedConstants.tryDetectVersion();
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	@BeforeEach
	public void setUp() {
		PlacementManager.clear();
	}

	@Test
	public void testPlacementLifecycle() {
		assertFalse(PlacementManager.hasPlacement());
		assertNull(PlacementManager.getPosition());

		BlockPos pos = new BlockPos(100, 64, -200);
		PlacementManager.setPlacement(pos, null);

		assertTrue(PlacementManager.hasPlacement());
		assertEquals(pos, PlacementManager.getPosition());
		assertEquals(0, PlacementManager.getRotation());
		assertTrue(PlacementManager.isVisible());

		PlacementManager.clear();
		assertFalse(PlacementManager.hasPlacement());
		assertNull(PlacementManager.getPosition());
	}

	@Test
	public void testRotation() {
		PlacementManager.setPlacement(new BlockPos(0, 0, 0), null);

		assertEquals(0, PlacementManager.getRotation());
		assertEquals(90, PlacementManager.rotateClockwise());
		assertEquals(180, PlacementManager.rotateClockwise());
		assertEquals(270, PlacementManager.rotateClockwise());
		assertEquals(0, PlacementManager.rotateClockwise());

		PlacementManager.setRotation(180);
		assertEquals(180, PlacementManager.getRotation());

		// Dimension swapping check
		int rawW = 10;
		int rawL = 20;

		// 0 deg: W=10, L=20
		PlacementManager.setRotation(0);
		int boxW0 = (PlacementManager.getRotation() == 90 || PlacementManager.getRotation() == 270) ? rawL : rawW;
		int boxL0 = (PlacementManager.getRotation() == 90 || PlacementManager.getRotation() == 270) ? rawW : rawL;
		assertEquals(10, boxW0);
		assertEquals(20, boxL0);

		// 90 deg: swapped -> W=20, L=10
		PlacementManager.setRotation(90);
		int boxW90 = (PlacementManager.getRotation() == 90 || PlacementManager.getRotation() == 270) ? rawL : rawW;
		int boxL90 = (PlacementManager.getRotation() == 90 || PlacementManager.getRotation() == 270) ? rawW : rawL;
		assertEquals(20, boxW90);
		assertEquals(10, boxL90);
	}

	@Test
	public void testNudge() {
		BlockPos start = new BlockPos(10, 60, 10);
		PlacementManager.setPlacement(start, null);

		PlacementManager.nudge(Direction.UP);
		assertEquals(new BlockPos(10, 61, 10), PlacementManager.getPosition());

		PlacementManager.nudge(Direction.DOWN);
		assertEquals(new BlockPos(10, 60, 10), PlacementManager.getPosition());

		PlacementManager.nudge(Direction.NORTH);
		assertEquals(new BlockPos(10, 60, 9), PlacementManager.getPosition());

		PlacementManager.nudge(Direction.EAST);
		assertEquals(new BlockPos(11, 60, 9), PlacementManager.getPosition());

		PlacementManager.nudge(1, -2, 3);
		assertEquals(new BlockPos(12, 58, 12), PlacementManager.getPosition());
	}

	@Test
	public void testVisibilityToggle() {
		PlacementManager.setPlacement(new BlockPos(0, 0, 0), null);
		assertTrue(PlacementManager.isVisible());

		assertFalse(PlacementManager.toggleVisible());
		assertFalse(PlacementManager.isVisible());

		assertTrue(PlacementManager.toggleVisible());
		assertTrue(PlacementManager.isVisible());
	}

	@Test
	public void testLayerModeCyclingAndStepping() {
		assertEquals(LayerMode.ALL, PlacementManager.getLayerMode());
		assertEquals(1, PlacementManager.getCurrentLayer());

		// Cycle: ALL -> SINGLE -> UP_TO -> ALL
		assertEquals(LayerMode.SINGLE, PlacementManager.cycleLayerMode());
		assertEquals(LayerMode.UP_TO, PlacementManager.cycleLayerMode());
		assertEquals(LayerMode.ALL, PlacementManager.cycleLayerMode());

		PlacementManager.setLayerMode(LayerMode.SINGLE);
		assertEquals(LayerMode.SINGLE, PlacementManager.getLayerMode());

		// Layer stepping within bounds [1, 5]
		assertEquals(2, PlacementManager.layerUp(5));
		assertEquals(3, PlacementManager.layerUp(5));
		assertEquals(4, PlacementManager.layerUp(5));
		assertEquals(5, PlacementManager.layerUp(5));
		assertEquals(5, PlacementManager.layerUp(5)); // Clamped at max

		assertEquals(4, PlacementManager.layerDown());
		assertEquals(3, PlacementManager.layerDown());
		assertEquals(2, PlacementManager.layerDown());
		assertEquals(1, PlacementManager.layerDown());
		assertEquals(1, PlacementManager.layerDown()); // Clamped at min 1

		// Reset layer mode
		PlacementManager.layerUp(5);
		PlacementManager.resetLayerMode();
		assertEquals(LayerMode.ALL, PlacementManager.getLayerMode());
		assertEquals(1, PlacementManager.getCurrentLayer());
	}

	@Test
	public void testMirroring() {
		assertFalse(PlacementManager.isMirrored());

		assertTrue(PlacementManager.toggleMirrored());
		assertTrue(PlacementManager.isMirrored());

		assertFalse(PlacementManager.toggleMirrored());
		assertFalse(PlacementManager.isMirrored());

		PlacementManager.setMirrored(true);
		assertTrue(PlacementManager.isMirrored());

		PlacementManager.clear();
		assertFalse(PlacementManager.isMirrored());
	}

	@Test
	public void testXMirrorDirectionalFacing() {
		if (BlockStateHelper.isBootstrapped() && BuiltInRegistries.BLOCK != null) {
			BlockState stairEast = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
			BlockState stairWest = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
			BlockState stairNorth = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
			BlockState stairSouth = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);

			// Mirror.FRONT_BACK reflects along the X axis (East <-> West)
			assertEquals(Direction.WEST, stairEast.mirror(Mirror.FRONT_BACK).getValue(StairBlock.FACING));
			assertEquals(Direction.EAST, stairWest.mirror(Mirror.FRONT_BACK).getValue(StairBlock.FACING));

			// Mirror.FRONT_BACK preserves North and South (Z axis)
			assertEquals(Direction.NORTH, stairNorth.mirror(Mirror.FRONT_BACK).getValue(StairBlock.FACING));
			assertEquals(Direction.SOUTH, stairSouth.mirror(Mirror.FRONT_BACK).getValue(StairBlock.FACING));

			// Verify composition with Rotation:
			// Mirror (East -> West) + Rotate Clockwise 90 (West -> North)
			BlockState comp90 = stairEast.mirror(Mirror.FRONT_BACK).rotate(Rotation.CLOCKWISE_90);
			assertEquals(Direction.NORTH, comp90.getValue(StairBlock.FACING));

			// Mirror (East -> West) + Rotate Clockwise 180 (West -> East)
			BlockState comp180 = stairEast.mirror(Mirror.FRONT_BACK).rotate(Rotation.CLOCKWISE_180);
			assertEquals(Direction.EAST, comp180.getValue(StairBlock.FACING));

			// Mirror (East -> West) + Rotate Counter-Clockwise 90 (West -> South)
			BlockState comp270 = stairEast.mirror(Mirror.FRONT_BACK).rotate(Rotation.COUNTERCLOCKWISE_90);
			assertEquals(Direction.SOUTH, comp270.getValue(StairBlock.FACING));

			// End-to-end test with PlacementManager.getCachedBlocks
			SchematicBlockData.VisibleBlock[] vb = new SchematicBlockData.VisibleBlock[] {
				new SchematicBlockData.VisibleBlock(0, 0, 0, 1) // x=0, y=0, z=0
			};
			SchematicBlockData blockData = new SchematicBlockData(
				5, 1, 1, 5, true, null,
				new BlockState[] { Blocks.AIR.defaultBlockState(), stairEast },
				vb
			);
			LitematicData data = new LitematicData("stair_test", "tester", 1, 5, 1, 1, 1, 1, 0, 0,
				Map.of(), List.of(), blockData, null);

			// Not mirrored, rot 0: x=0, faces EAST
			PlacementManager.setMirrored(false);
			PlacementManager.setRotation(0);
			PlacementManager.TransformedBlock[] blocks = PlacementManager.getCachedBlocks(data);
			assertEquals(1, blocks.length);
			assertEquals(0, blocks[0].x());
			assertEquals(Direction.EAST, blocks[0].state().getValue(StairBlock.FACING));

			// Mirrored, rot 0: x = 5 - 1 - 0 = 4, faces WEST
			PlacementManager.setMirrored(true);
			PlacementManager.setRotation(0);
			blocks = PlacementManager.getCachedBlocks(data);
			assertEquals(1, blocks.length);
			assertEquals(4, blocks[0].x());
			assertEquals(Direction.WEST, blocks[0].state().getValue(StairBlock.FACING));

			// Mirrored, rot 90: faces NORTH
			PlacementManager.setRotation(90);
			blocks = PlacementManager.getCachedBlocks(data);
			assertEquals(1, blocks.length);
			assertEquals(Direction.NORTH, blocks[0].state().getValue(StairBlock.FACING));
		}
	}

	@Test
	public void testSolidCubeLayerSlicing() {
		// Construct a 3x3x3 solid cube (27 blocks total).
		// Center block at (1, 1, 1) has occludedY = true.
		// All 26 other boundary blocks have occludedY = false.
		List<SchematicBlockData.VisibleBlock> vbs = new ArrayList<>();
		for (int y = 0; y < 3; y++) {
			for (int z = 0; z < 3; z++) {
				for (int x = 0; x < 3; x++) {
					boolean occludedY = (x == 1 && y == 1 && z == 1);
					vbs.add(new SchematicBlockData.VisibleBlock(x, y, z, 1, occludedY));
				}
			}
		}

		BlockState stoneState = BlockStateHelper.isBootstrapped() && BuiltInRegistries.BLOCK != null
			? Blocks.STONE.defaultBlockState()
			: null;

		SchematicBlockData blockData = new SchematicBlockData(
			3, 3, 3, 27, true, null,
			new BlockState[] { Blocks.AIR.defaultBlockState(), stoneState },
			vbs.toArray(SchematicBlockData.VisibleBlock[]::new)
		);
		LitematicData data = new LitematicData("cube", "tester", 1, 3, 3, 3, 27, 27, 0, 0,
			Map.of(), List.of(), blockData, null);

		// 1. LayerMode.ALL: center block (1, 1, 1) is culled -> exactly 26 blocks
		PlacementManager.setLayerMode(LayerMode.ALL);
		PlacementManager.TransformedBlock[] allBlocks = PlacementManager.getCachedBlocks(data);
		assertEquals(26, allBlocks.length, "LayerMode.ALL must cull the center block (1, 1, 1)");
		assertFalse(Arrays.stream(allBlocks).anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1),
			"Center block (1, 1, 1) must be culled in ALL mode");

		// 2. LayerMode.SINGLE on Layer 2 (y == 1):
		// Neither y=0 nor y=2 is rendered, so all 9 blocks of Layer 2 are exposed and must be present!
		PlacementManager.setLayerMode(LayerMode.SINGLE);
		PlacementManager.setCurrentLayer(2, 3);
		PlacementManager.TransformedBlock[] singleLayer2 = PlacementManager.getCachedBlocks(data);
		assertEquals(9, singleLayer2.length, "Layer 2 in SINGLE mode must contain all 9 blocks");
		assertTrue(Arrays.stream(singleLayer2).allMatch(b -> b.y() == 1),
			"All blocks in single layer 2 must have y == 1");
		assertTrue(Arrays.stream(singleLayer2).anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1),
			"Layer 2 in SINGLE mode must include the center floor block (1, 1, 1)");

		// 3. LayerMode.UP_TO on Layer 2:
		// Slices at Layer 2 (y=1). Layer 1 has 9 blocks, Layer 2 has all 9 blocks (including (1, 1, 1) whose top face is exposed)
		PlacementManager.setLayerMode(LayerMode.UP_TO);
		PlacementManager.setCurrentLayer(2, 3);
		PlacementManager.TransformedBlock[] upToLayer2 = PlacementManager.getCachedBlocks(data);
		assertEquals(18, upToLayer2.length, "LayerMode.UP_TO on Layer 2 must contain 18 blocks (9 + 9)");
		long layer2Count = Arrays.stream(upToLayer2).filter(b -> b.y() == 1).count();
		assertEquals(9, layer2Count, "Layer 2 in UP_TO mode must contain all 9 blocks");
		assertTrue(Arrays.stream(upToLayer2).anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1),
			"Layer 2 in UP_TO mode must include the center floor block (1, 1, 1)");

		// 4. LayerMode.UP_TO on Layer 3 (all layers):
		// Layer 3 covers Layer 2, so (1, 1, 1) is now below currentLayer and must be culled -> 26 blocks
		PlacementManager.setLayerMode(LayerMode.UP_TO);
		PlacementManager.setCurrentLayer(3, 3);
		PlacementManager.TransformedBlock[] upToLayer3 = PlacementManager.getCachedBlocks(data);
		assertEquals(26, upToLayer3.length, "LayerMode.UP_TO on Layer 3 must cull (1, 1, 1) and contain 26 blocks");
		assertFalse(Arrays.stream(upToLayer3).anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1),
			"(1, 1, 1) must be culled when Layer 3 is included in UP_TO mode");
	}
}
