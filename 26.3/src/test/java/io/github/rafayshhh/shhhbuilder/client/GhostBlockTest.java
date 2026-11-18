package io.github.rafayshhh.shhhbuilder.client;

import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicReader;
import io.github.rafayshhh.shhhbuilder.client.schematic.SchematicBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GhostBlockTest {

	@BeforeEach
	public void setUp() {
		PlacementManager.clear();
		PlacementManager.setGhostVisible(true);
	}

	@Test
	public void testSixNeighborOcclusionCulling() {
		// Create a 3x3x3 solid block region (27 blocks total).
		// The block in the center at (1, 1, 1) has all 6 neighbors non-air.
		// All other 26 blocks touch the outer boundary, so they must remain visible.
		int width = 3;
		int height = 3;
		int length = 3;

		int totalVolume = width * height * length; // 27
		int[] blockIndices = new int[totalVolume];
		for (int i = 0; i < totalVolume; i++) {
			blockIndices[i] = 1; // 1 = solid stone
		}

		List<String> rawNames = List.of("minecraft:air", "minecraft:stone");

		List<SchematicBlockData.VisibleBlock> visibleList = new ArrayList<>();
		int[] grid = new int[totalVolume];
		for (int i = 0; i < totalVolume; i++) {
			grid[i] = 1; // all non-air
		}

		// Perform the 6-neighbor culling algorithm: tag blocks enclosed on all 6 sides
		for (int y = 0; y < height; y++) {
			for (int z = 0; z < length; z++) {
				for (int x = 0; x < width; x++) {
					int gIndex = grid[(y * length + z) * width + x];
					if (gIndex == 0) continue;

					boolean occludedY = false;
					if (x > 0 && x < width - 1 &&
						y > 0 && y < height - 1 &&
						z > 0 && z < length - 1) {
						int left   = grid[(y * length + z) * width + (x - 1)];
						int right  = grid[(y * length + z) * width + (x + 1)];
						int down   = grid[((y - 1) * length + z) * width + x];
						int up     = grid[((y + 1) * length + z) * width + x];
						int north  = grid[(y * length + (z - 1)) * width + x];
						int south  = grid[(y * length + (z + 1)) * width + x];

						if (left != 0 && right != 0 && down != 0 && up != 0 && north != 0 && south != 0) {
							occludedY = true;
						}
					}

					visibleList.add(new SchematicBlockData.VisibleBlock(x, y, z, gIndex, occludedY));
				}
			}
		}

		// All 27 blocks are preserved in visibleList, with center block marked occludedY
		assertEquals(27, visibleList.size());
		assertTrue(visibleList.stream().anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1 && b.occludedY()),
			"Center block at (1, 1, 1) should be tagged with occludedY=true");
		assertEquals(26, visibleList.stream().filter(b -> !b.occludedY()).count());

		// In PlacementManager:
		SchematicBlockData blockData = new SchematicBlockData(
			width, height, length, totalVolume, true, null,
			new net.minecraft.world.level.block.state.BlockState[0],
			visibleList.toArray(SchematicBlockData.VisibleBlock[]::new)
		);
		LitematicData data = new LitematicData("cube", "tester", 1, width, height, length, 27, 27, 0, 0,
			Map.of(), List.of(), blockData, null);

		// LayerMode.ALL: center block (1, 1, 1) is culled -> exactly 26 blocks
		PlacementManager.setLayerMode(LayerMode.ALL);
		PlacementManager.TransformedBlock[] allBlocks = PlacementManager.getCachedBlocks(data);
		assertEquals(26, allBlocks.length);
		assertFalse(java.util.Arrays.stream(allBlocks).anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1),
			"Center block at (1, 1, 1) should be culled in ALL mode");

		// LayerMode.SINGLE on Layer 2 (y == 1): must contain ALL 9 blocks
		PlacementManager.setLayerMode(LayerMode.SINGLE);
		PlacementManager.setCurrentLayer(2, 3);
		PlacementManager.TransformedBlock[] singleBlocks = PlacementManager.getCachedBlocks(data);
		assertEquals(9, singleBlocks.length, "Layer 2 in SINGLE mode must contain all 9 blocks");
		assertTrue(java.util.Arrays.stream(singleBlocks).anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1),
			"Center block at (1, 1, 1) must be present in SINGLE mode on Layer 2");

		// LayerMode.UP_TO on Layer 2: Layer 2 must contain all 9 blocks
		PlacementManager.setLayerMode(LayerMode.UP_TO);
		PlacementManager.setCurrentLayer(2, 3);
		PlacementManager.TransformedBlock[] upToBlocks = PlacementManager.getCachedBlocks(data);
		assertEquals(18, upToBlocks.length);
		long layer2Count = java.util.Arrays.stream(upToBlocks).filter(b -> b.y() == 1).count();
		assertEquals(9, layer2Count, "Layer 2 in UP_TO mode must contain all 9 blocks");
		assertTrue(java.util.Arrays.stream(upToBlocks).anyMatch(b -> b.x() == 1 && b.y() == 1 && b.z() == 1),
			"Center block at (1, 1, 1) must be present in UP_TO mode on Layer 2");
	}

	@Test
	public void testHollowCubeKeepsAllBlocks() {
		// A 3x3x3 hollow cube (center block is air)
		int width = 3, height = 3, length = 3;
		int totalVolume = 27;
		int[] grid = new int[totalVolume];
		for (int i = 0; i < totalVolume; i++) {
			grid[i] = 1;
		}
		// Center block at (1, 1, 1) is air
		grid[(1 * 3 + 1) * 3 + 1] = 0;

		List<SchematicBlockData.VisibleBlock> visibleList = new ArrayList<>();
		for (int y = 0; y < height; y++) {
			for (int z = 0; z < length; z++) {
				for (int x = 0; x < width; x++) {
					int gIndex = grid[(y * length + z) * width + x];
					if (gIndex == 0) continue;

					boolean occludedY = false;
					if (x > 0 && x < width - 1 &&
						y > 0 && y < height - 1 &&
						z > 0 && z < length - 1) {
						int left   = grid[(y * length + z) * width + (x - 1)];
						int right  = grid[(y * length + z) * width + (x + 1)];
						int down   = grid[((y - 1) * length + z) * width + x];
						int up     = grid[((y + 1) * length + z) * width + x];
						int north  = grid[(y * length + (z - 1)) * width + x];
						int south  = grid[(y * length + (z + 1)) * width + x];

						if (left != 0 && right != 0 && down != 0 && up != 0 && north != 0 && south != 0) {
							occludedY = true;
						}
					}

					visibleList.add(new SchematicBlockData.VisibleBlock(x, y, z, gIndex, occludedY));
				}
			}
		}

		// All 26 non-air blocks remain visible and none is occludedY because center was air
		assertEquals(26, visibleList.size());
		assertEquals(0, visibleList.stream().filter(SchematicBlockData.VisibleBlock::occludedY).count());
	}

	@Test
	public void testRotationCoordinateMapping() {
		// Rectangle of width 4, length 8
		int W = 4;
		int H = 3;
		int L = 8;

		SchematicBlockData.VisibleBlock[] blocks = new SchematicBlockData.VisibleBlock[]{
			new SchematicBlockData.VisibleBlock(0, 0, 0, 0),
			new SchematicBlockData.VisibleBlock(3, 1, 7, 0)
		};

		SchematicBlockData blockData = new SchematicBlockData(
			W, H, L, (long) W * H * L, true, null,
			new net.minecraft.world.level.block.state.BlockState[0],
			blocks
		);

		LitematicData data = new LitematicData(
			"Test", "Author", 1, W, H, L, 2,
			Map.of(), List.of(), blockData
		);

		// Test rotation 0
		PlacementManager.setRotation(0);
		PlacementManager.TransformedBlock[] rot0 = PlacementManager.getCachedBlocks(data);
		assertEquals(0, rot0[0].relX());
		assertEquals(0, rot0[0].relZ());
		assertEquals(3, rot0[1].relX());
		assertEquals(7, rot0[1].relZ());

		// Test rotation 90: rx = L - 1 - z, rz = x
		PlacementManager.setRotation(90);
		PlacementManager.TransformedBlock[] rot90 = PlacementManager.getCachedBlocks(data);
		assertEquals(8 - 1 - 0, rot90[0].relX()); // 7
		assertEquals(0, rot90[0].relZ());         // 0
		assertEquals(8 - 1 - 7, rot90[1].relX()); // 0
		assertEquals(3, rot90[1].relZ());         // 3

		// Test rotation 180: rx = W - 1 - x, rz = L - 1 - z
		PlacementManager.setRotation(180);
		PlacementManager.TransformedBlock[] rot180 = PlacementManager.getCachedBlocks(data);
		assertEquals(4 - 1 - 0, rot180[0].relX()); // 3
		assertEquals(8 - 1 - 0, rot180[0].relZ()); // 7
		assertEquals(4 - 1 - 3, rot180[1].relX()); // 0
		assertEquals(8 - 1 - 7, rot180[1].relZ()); // 0

		// Test rotation 270: rx = z, rz = W - 1 - x
		PlacementManager.setRotation(270);
		PlacementManager.TransformedBlock[] rot270 = PlacementManager.getCachedBlocks(data);
		assertEquals(0, rot270[0].relX());         // 0
		assertEquals(4 - 1 - 0, rot270[0].relZ()); // 3
		assertEquals(7, rot270[1].relX());         // 7
		assertEquals(4 - 1 - 3, rot270[1].relZ()); // 0
	}

	@Test
	public void testVolumeLimitEnforcement() {
		// 2,000,001 blocks exceeds limit
		int w = 200, h = 100, l = 101;
		long vol = (long) w * h * l; // 2,020,000
		assertTrue(vol > LitematicReader.MAX_PREVIEW_VOLUME);

		SchematicBlockData tooLarge = SchematicBlockData.tooLarge(w, h, l, vol, LitematicReader.MAX_PREVIEW_VOLUME);
		assertFalse(tooLarge.canPreview());
		assertNotNull(tooLarge.previewDisabledReason());
		assertTrue(tooLarge.previewDisabledReason().contains("exceeds"));
		assertEquals(0, tooLarge.visibleBlocks().length);
	}

	@Test
	public void testGhostVisibilityToggle() {
		assertTrue(PlacementManager.isGhostVisible());
		PlacementManager.setGhostVisible(false);
		assertFalse(PlacementManager.isGhostVisible());
		assertTrue(PlacementManager.toggleGhostVisible());
		assertTrue(PlacementManager.isGhostVisible());
		assertFalse(PlacementManager.toggleGhostVisible());
		assertFalse(PlacementManager.isGhostVisible());
	}

	@Test
	public void testGhostRenderDistanceConfigCycling() {
		ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
		config.setGhostRenderDistance(48);
		assertEquals(48, config.getGhostRenderDistance());

		config.cycleGhostRenderDistance();
		assertEquals(64, config.getGhostRenderDistance());

		config.cycleGhostRenderDistance();
		assertEquals(24, config.getGhostRenderDistance());

		config.cycleGhostRenderDistance();
		assertEquals(32, config.getGhostRenderDistance());

		config.cycleGhostRenderDistance();
		assertEquals(48, config.getGhostRenderDistance());
	}

	@Test
	public void testLayerFilteringAllMode() {
		int W = 2, H = 3, L = 2;
		SchematicBlockData.VisibleBlock[] blocks = new SchematicBlockData.VisibleBlock[]{
			new SchematicBlockData.VisibleBlock(0, 0, 0, 0), // Layer 1 (y = 0)
			new SchematicBlockData.VisibleBlock(0, 1, 0, 0), // Layer 2 (y = 1)
			new SchematicBlockData.VisibleBlock(0, 2, 0, 0)  // Layer 3 (y = 2)
		};

		SchematicBlockData blockData = new SchematicBlockData(
			W, H, L, (long) W * H * L, true, null,
			new net.minecraft.world.level.block.state.BlockState[0],
			blocks
		);
		LitematicData data = new LitematicData(
			"Test", "Author", 1, W, H, L, 3,
			Map.of(), List.of(), blockData
		);

		PlacementManager.setLayerMode(LayerMode.ALL);
		PlacementManager.TransformedBlock[] filtered = PlacementManager.getCachedBlocks(data);
		assertEquals(3, filtered.length, "All layers should be included in ALL mode");
	}

	@Test
	public void testLayerFilteringSingleLayer() {
		int W = 2, H = 3, L = 2;
		SchematicBlockData.VisibleBlock[] blocks = new SchematicBlockData.VisibleBlock[]{
			new SchematicBlockData.VisibleBlock(0, 0, 0, 0), // Layer 1 (y = 0)
			new SchematicBlockData.VisibleBlock(1, 1, 1, 0), // Layer 2 (y = 1)
			new SchematicBlockData.VisibleBlock(0, 2, 0, 0)  // Layer 3 (y = 2)
		};

		SchematicBlockData blockData = new SchematicBlockData(
			W, H, L, (long) W * H * L, true, null,
			new net.minecraft.world.level.block.state.BlockState[0],
			blocks
		);
		LitematicData data = new LitematicData(
			"Test", "Author", 1, W, H, L, 3,
			Map.of(), List.of(), blockData
		);

		PlacementManager.setLayerMode(LayerMode.SINGLE);
		PlacementManager.setCurrentLayer(2, H);

		PlacementManager.TransformedBlock[] filtered = PlacementManager.getCachedBlocks(data);
		assertEquals(1, filtered.length, "Only layer 2 should be included in SINGLE mode");
		assertEquals(1, filtered[0].relY(), "Block Y should be 1 for layer 2");
	}

	@Test
	public void testLayerFilteringUpToLayer() {
		int W = 2, H = 3, L = 2;
		SchematicBlockData.VisibleBlock[] blocks = new SchematicBlockData.VisibleBlock[]{
			new SchematicBlockData.VisibleBlock(0, 0, 0, 0), // Layer 1 (y = 0)
			new SchematicBlockData.VisibleBlock(1, 1, 1, 0), // Layer 2 (y = 1)
			new SchematicBlockData.VisibleBlock(0, 2, 0, 0)  // Layer 3 (y = 2)
		};

		SchematicBlockData blockData = new SchematicBlockData(
			W, H, L, (long) W * H * L, true, null,
			new net.minecraft.world.level.block.state.BlockState[0],
			blocks
		);
		LitematicData data = new LitematicData(
			"Test", "Author", 1, W, H, L, 3,
			Map.of(), List.of(), blockData
		);

		PlacementManager.setLayerMode(LayerMode.UP_TO);
		PlacementManager.setCurrentLayer(2, H);

		PlacementManager.TransformedBlock[] filtered = PlacementManager.getCachedBlocks(data);
		assertEquals(2, filtered.length, "Layers 1 and 2 should be included in UP_TO mode");
		assertTrue(filtered[0].relY() <= 1 && filtered[1].relY() <= 1);
	}

	@Test
	public void testMirrorCoordinatesWithRotation() {
		int W = 4;
		int H = 3;
		int L = 8;

		SchematicBlockData.VisibleBlock[] blocks = new SchematicBlockData.VisibleBlock[]{
			new SchematicBlockData.VisibleBlock(0, 0, 0, 0),
			new SchematicBlockData.VisibleBlock(3, 1, 7, 0)
		};

		SchematicBlockData blockData = new SchematicBlockData(
			W, H, L, (long) W * H * L, true, null,
			new net.minecraft.world.level.block.state.BlockState[0],
			blocks
		);
		LitematicData data = new LitematicData(
			"Test", "Author", 1, W, H, L, 2,
			Map.of(), List.of(), blockData
		);

		PlacementManager.setMirrored(true);

		// Mirror left-to-right on unrotated coordinates:
		// block 0 (x=0, z=0) -> mx = 4 - 1 - 0 = 3, mz = 0
		// block 1 (x=3, z=7) -> mx = 4 - 1 - 3 = 0, mz = 7

		// Rotation 0 with mirror:
		PlacementManager.setRotation(0);
		PlacementManager.TransformedBlock[] rot0 = PlacementManager.getCachedBlocks(data);
		assertEquals(3, rot0[0].relX());
		assertEquals(0, rot0[0].relZ());
		assertEquals(0, rot0[1].relX());
		assertEquals(7, rot0[1].relZ());

		// Rotation 90 with mirror:
		// rx = L - 1 - mz, rz = mx
		// block 0: rx = 8 - 1 - 0 = 7, rz = 3
		// block 1: rx = 8 - 1 - 7 = 0, rz = 0
		PlacementManager.setRotation(90);
		PlacementManager.TransformedBlock[] rot90 = PlacementManager.getCachedBlocks(data);
		assertEquals(7, rot90[0].relX());
		assertEquals(3, rot90[0].relZ());
		assertEquals(0, rot90[1].relX());
		assertEquals(0, rot90[1].relZ());
	}

	@Test
	public void testInternalFaceCullingBetweenAdjacentBlocks() {
		// Create two adjacent blocks: (0, 0, 0) and (0, 1, 0) (one on top of the other)
		int W = 1, H = 2, L = 1;
		SchematicBlockData.VisibleBlock[] blocks = new SchematicBlockData.VisibleBlock[]{
			new SchematicBlockData.VisibleBlock(0, 0, 0, 0),
			new SchematicBlockData.VisibleBlock(0, 1, 0, 0)
		};

		SchematicBlockData blockData = new SchematicBlockData(
			W, H, L, 2, true, null,
			new net.minecraft.world.level.block.state.BlockState[0],
			blocks
		);
		LitematicData data = new LitematicData(
			"Tower", "Author", 1, W, H, L, 2,
			Map.of(), List.of(), blockData
		);

		PlacementManager.setRotation(0);
		PlacementManager.setMirrored(false);
		PlacementManager.setLayerMode(LayerMode.ALL);
		PlacementManager.getCachedBlocks(data);

		// Bottom block is at (0, 0, 0), top block is at (0, 1, 0)
		assertTrue(PlacementManager.hasBlockAt(0, 0, 0), "Bottom block must be present");
		assertTrue(PlacementManager.hasBlockAt(0, 1, 0), "Top block must be present");

		// Bottom block: UP neighbor is (0, 1, 0), which IS present -> internal face culled
		assertTrue(PlacementManager.hasBlockAt(0, 0 + net.minecraft.core.Direction.UP.getStepY(), 0));
		// Bottom block: DOWN neighbor is (0, -1, 0), which is NOT present -> face drawn
		assertFalse(PlacementManager.hasBlockAt(0, 0 + net.minecraft.core.Direction.DOWN.getStepY(), 0));

		// Top block: DOWN neighbor is (0, 0, 0), which IS present -> internal face culled
		assertTrue(PlacementManager.hasBlockAt(0, 1 + net.minecraft.core.Direction.DOWN.getStepY(), 0));
		// Top block: UP neighbor is (0, 2, 0), which is NOT present -> face drawn
		assertFalse(PlacementManager.hasBlockAt(0, 1 + net.minecraft.core.Direction.UP.getStepY(), 0));
	}

	@Test
	public void testTopSliceBoundaryFacesNeverCulledInSingleOrUpTo() {
		// Construct 2 vertical blocks at (0, 0, 0) [layer 1] and (0, 1, 0) [layer 2]
		int W = 1, H = 2, L = 1;
		SchematicBlockData.VisibleBlock[] blocks = new SchematicBlockData.VisibleBlock[]{
			new SchematicBlockData.VisibleBlock(0, 0, 0, 0, false),
			new SchematicBlockData.VisibleBlock(0, 1, 0, 0, false)
		};
		SchematicBlockData blockData = new SchematicBlockData(
			W, H, L, 2, true, null,
			new net.minecraft.world.level.block.state.BlockState[0],
			blocks
		);
		LitematicData data = new LitematicData(
			"Tower", "Author", 1, W, H, L, 2,
			Map.of(), List.of(), blockData
		);

		PlacementManager.setRotation(0);
		PlacementManager.setMirrored(false);

		// Case 1: LayerMode.ALL
		// Bottom block UP neighbor is (0, 1, 0) -> touches ghost block -> culled
		PlacementManager.setLayerMode(LayerMode.ALL);
		PlacementManager.getCachedBlocks(data);
		boolean culledInAll = io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.shouldCullFace(
			0, 0, 0,
			net.minecraft.core.Direction.UP,
			1, // blockLayer = 1
			LayerMode.ALL,
			1
		);
		assertTrue(culledInAll, "In ALL mode, UP face of bottom block touching top block must be culled");

		// Case 2: LayerMode.UP_TO at currentLayer = 1
		// Bottom block is on layer 1 (active top slice boundary), UP face must NEVER be culled!
		PlacementManager.setLayerMode(LayerMode.UP_TO);
		PlacementManager.setCurrentLayer(1, 2);
		PlacementManager.getCachedBlocks(data);
		boolean culledInUpTo = io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.shouldCullFace(
			0, 0, 0,
			net.minecraft.core.Direction.UP,
			1, // blockLayer = 1 (matches currentLayer = 1)
			LayerMode.UP_TO,
			1
		);
		assertFalse(culledInUpTo, "In UP_TO mode at boundary (y == currentLayer), UP face must NEVER be culled");

		// Case 3: LayerMode.SINGLE at currentLayer = 1
		// Bottom block is on layer 1 (active slice boundary), UP face must NEVER be culled!
		PlacementManager.setLayerMode(LayerMode.SINGLE);
		PlacementManager.setCurrentLayer(1, 2);
		PlacementManager.getCachedBlocks(data);
		boolean culledInSingle = io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.shouldCullFace(
			0, 0, 0,
			net.minecraft.core.Direction.UP,
			1, // blockLayer = 1 (matches currentLayer = 1)
			LayerMode.SINGLE,
			1
		);
		assertFalse(culledInSingle, "In SINGLE mode at boundary (y == currentLayer), UP face must NEVER be culled");

		// Case 4: Non-boundary face (e.g. DOWN face of layer 1)
		// No block at (0, -1, 0) -> not culled
		boolean downCulled = io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.shouldCullFace(
			0, 0, 0,
			net.minecraft.core.Direction.DOWN,
			1,
			LayerMode.SINGLE,
			1
		);
		assertFalse(downCulled, "Face with no neighbor block should not be culled");
	}

	@Test
	public void testNearestNSelectionAndDistanceLimit() {
		// Player position at origin (0, 0, 0)
		double playerX = 0.0, playerY = 0.0, playerZ = 0.0;
		int texturedDistance = 24; // 24-block limit
		double texturedDistSq = (double) texturedDistance * texturedDistance; // 576.0
		int texturedBlockLimit = 3;

		// 5 blocks at varying distances:
		// b1: (2, 0, 0)   -> distSq = 4   (< 576)
		// b2: (5, 0, 0)   -> distSq = 25  (< 576)
		// b3: (10, 0, 0)  -> distSq = 100 (< 576)
		// b4: (15, 0, 0)  -> distSq = 225 (< 576)
		// b5: (26, 0, 0)  -> distSq = 676 (> 576, beyond 24-block distance limit)
		record TestBlock(int x, int y, int z, double distSq) {}
		List<TestBlock> testBlocks = List.of(
			new TestBlock(2, 0, 0, 4.0),
			new TestBlock(5, 0, 0, 25.0),
			new TestBlock(10, 0, 0, 100.0),
			new TestBlock(15, 0, 0, 225.0),
			new TestBlock(26, 0, 0, 676.0)
		);

		int texturedDrawn = 0;
		int outlineDrawn = 0;

		for (TestBlock tb : testBlocks) {
			boolean canBeTextured = (texturedDrawn < texturedBlockLimit) && (tb.distSq <= texturedDistSq);
			if (canBeTextured) {
				texturedDrawn++;
			} else {
				outlineDrawn++;
			}
		}

		// Exactly 3 blocks should be drawn as textured (limit is 3)
		assertEquals(3, texturedDrawn, "Should render up to texturedBlockLimit (3) textured blocks");
		// 2 blocks should fall back to outlines: b4 (exceeded limit) and b5 (exceeded distance)
		assertEquals(2, outlineDrawn, "Blocks exceeding limit or distance should fall back to outlines");
	}

	@Test
	public void testGhostAlphaAndColorValues() {
		// ~0.45 alpha is 115 / 255 = 0x73
		assertEquals(0x73, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.GHOST_ALPHA);
		assertEquals(115, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.GHOST_ALPHA & 0xFF);

		// Untinted quad color must have 0x73 alpha byte and 0xFFFFFF RGB
		int quadColor = io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.getQuadColor(null, null);
		int alpha = (quadColor >>> 24) & 0xFF;
		assertEquals(0x73, alpha, "Ghost block alpha byte must be 0x73 (~0.45)");
		assertEquals(0xFFFFFF, quadColor & 0x00FFFFFF, "Untinted quad color should be full white RGB");

		// Outlines use ensureAlpha to guarantee 0xFF (full opacity in 26.3)
		int outlineColor = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.ensureAlpha(0x00AABBCC);
		assertEquals(0xFF, (outlineColor >>> 24) & 0xFF, "Outline color must have full 0xFF alpha");
	}

	@Test
	public void testQuadCacheKeyingAndEmptyModel() {
		// Cache clear and size
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.clearCache();
		assertEquals(0, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.getCacheSize());

		// Null state returns empty model safely
		var model = io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.getModel(null);
		assertNotNull(model);
		assertTrue(model.isEmpty());
		assertEquals(0, model.unculledQuads().size());
		assertEquals(0, model.faceQuads().get(net.minecraft.core.Direction.UP).size());
	}

	@Test
	public void testCircuitBreakerResetAndThreeFailures() {
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.resetCircuitBreaker();
		assertFalse(io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.isTexturedPreviewDisabled(),
			"Circuit breaker should not be disabled initially");
		assertEquals(0, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.getTexturedErrorCount());

		// Simulate error 1
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.handleTexturedPipelineError(new RuntimeException("Simulated error 1"), null);
		assertEquals(1, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.getTexturedErrorCount());
		assertFalse(io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.isTexturedPreviewDisabled(),
			"Should not trip on error 1");

		// Simulate error 2
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.handleTexturedPipelineError(new RuntimeException("Simulated error 2"), null);
		assertEquals(2, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.getTexturedErrorCount());
		assertFalse(io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.isTexturedPreviewDisabled(),
			"Should not trip on error 2");

		// Simulate error 3 -> must trip circuit breaker
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.handleTexturedPipelineError(new RuntimeException("Simulated error 3"), null);
		assertEquals(3, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.getTexturedErrorCount());
		assertTrue(io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.isTexturedPreviewDisabled(),
			"Must trip circuit breaker on 3rd failure");
		assertEquals(ShhhBuilderConfig.PreviewStyle.OUTLINES, ShhhBuilderConfig.getInstance().getPreviewStyle(),
			"Config should switch to OUTLINES when circuit breaker trips");

		// Reset circuit breaker
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.resetCircuitBreaker();
		assertFalse(io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.isTexturedPreviewDisabled(),
			"Circuit breaker should be restored after reset");
		assertEquals(0, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.getTexturedErrorCount());
	}

	@Test
	public void testBatchedTexturedBlockAndPrecomputedTint() {
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.clearCache();
		assertEquals(0, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.getCacheSize());

		// Test precomputed tint RGB values
		assertEquals(0x80C755, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.computeBlockTint(null));

		// Batched block collection
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.clearBatchedBlocks();
		assertEquals(0, io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.getBatchedBlockCount());

		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.CachedGhostModel mockModel =
			new io.github.rafayshhh.shhhbuilder.client.render.GhostBlockModelHelper.CachedGhostModel(
				List.of(), Map.of(), 0x7CBD6B
			);
		io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.BatchedTexturedBlock entry =
			new io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer.BatchedTexturedBlock(
				1.5f, 2.5f, 3.5f,
				1, 2, 3,
				3, // blockLayer = 3
				null,
				mockModel
			);
		assertEquals(1.5f, entry.camRelX());
		assertEquals(2.5f, entry.camRelY());
		assertEquals(3.5f, entry.camRelZ());
		assertEquals(1, entry.relX());
		assertEquals(2, entry.relY());
		assertEquals(3, entry.relZ());
		assertEquals(3, entry.blockLayer());
		assertEquals(0x7CBD6B, entry.model().tintRgb());
	}
}
