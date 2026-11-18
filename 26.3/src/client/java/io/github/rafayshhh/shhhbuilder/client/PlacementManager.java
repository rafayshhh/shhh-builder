package io.github.rafayshhh.shhhbuilder.client;

import io.github.rafayshhh.shhhbuilder.client.material.MaterialProgressManager;
import io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer;
import io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import io.github.rafayshhh.shhhbuilder.client.schematic.SchematicBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the active in-world schematic placement:
 * anchor position (X, Y, Z), rotation (0, 90, 180, 270), mirroring (left-to-right),
 * layer mode filtering, dimension, and visibility toggles.
 */
public class PlacementManager {
	private static volatile BlockPos position = null;
	private static volatile int rotation = 0; // 0, 90, 180, 270
	private static volatile boolean mirrored = false; // Left-to-right mirror
	private static volatile ResourceKey<Level> dimension = null;
	private static volatile boolean outlineVisible = true;
	private static volatile boolean ghostVisible = true;

	private static volatile LayerMode layerMode = LayerMode.ALL;
	private static volatile int currentLayer = 1; // 1-based (1 to height)

	/**
	 * Represents a block with its position and BlockState transformed (mirrored and rotated).
	 */
	public record TransformedBlock(int relX, int relY, int relZ, BlockState state) {
		public int x() { return relX; }
		public int y() { return relY; }
		public int z() { return relZ; }
	}

	/**
	 * Represents a 16x16x16 section of transformed blocks for distance-prioritized rendering.
	 */
	public record BlockSection(int secX, int secY, int secZ, double centerX, double centerY, double centerZ, TransformedBlock[] blocks) {}

	private static TransformedBlock[] cachedBlocks = null;
	private static BlockSection[] cachedSections = null;
	private static Map<Long, TransformedBlock> cachedBlockMap = null;
	private static java.util.Set<Long> cachedSolidBlockCoords = null;
	private static int cachedRotation = -1;
	private static boolean cachedMirrored = false;
	private static LayerMode cachedLayerMode = null;
	private static int cachedLayer = -1;
	private static SchematicBlockData cachedSourceBlockData = null;

	public static boolean hasPlacement() {
		return position != null;
	}

	public static BlockPos getPosition() {
		return position;
	}

	public static void setPosition(BlockPos pos) {
		position = pos;
	}

	public static void move(int dx, int dy, int dz) {
		if (position != null) {
			position = position.offset(dx, dy, dz);
		}
	}

	public static int getRotation() {
		return rotation;
	}

	public static void setRotation(int rot) {
		int normalized = normalizeRotation(rot);
		if (rotation != normalized) {
			rotation = normalized;
			invalidateBlockCache();
		}
	}

	public static int rotateClockwise() {
		rotation = (rotation + 90) % 360;
		invalidateBlockCache();
		return rotation;
	}

	public static int rotateCounterClockwise() {
		rotation = (rotation + 270) % 360;
		invalidateBlockCache();
		return rotation;
	}

	public static boolean isMirrored() {
		return mirrored;
	}

	public static void setMirrored(boolean mirror) {
		if (mirrored != mirror) {
			mirrored = mirror;
			invalidateBlockCache();
		}
	}

	public static boolean toggleMirrored() {
		mirrored = !mirrored;
		invalidateBlockCache();
		return mirrored;
	}

	public static LayerMode getLayerMode() {
		return layerMode != null ? layerMode : LayerMode.ALL;
	}

	public static void setLayerMode(LayerMode mode) {
		LayerMode target = mode != null ? mode : LayerMode.ALL;
		if (layerMode != target) {
			layerMode = target;
			invalidateBlockCache();
		}
	}

	public static LayerMode cycleLayerMode() {
		layerMode = getLayerMode().next();
		invalidateBlockCache();
		return layerMode;
	}

	public static void resetLayerMode() {
		layerMode = LayerMode.ALL;
		currentLayer = 1;
		invalidateBlockCache();
	}

	public static int getCurrentLayer() {
		return currentLayer;
	}

	public static void setCurrentLayer(int layer, int maxHeight) {
		int clamped = Math.max(1, Math.min(layer, Math.max(1, maxHeight)));
		if (currentLayer != clamped) {
			currentLayer = clamped;
			invalidateBlockCache();
		}
	}

	public static int layerUp(int maxHeight) {
		if (currentLayer < maxHeight) {
			currentLayer++;
			invalidateBlockCache();
		}
		return currentLayer;
	}

	public static int layerDown() {
		if (currentLayer > 1) {
			currentLayer--;
			invalidateBlockCache();
		}
		return currentLayer;
	}

	public static ResourceKey<Level> getDimension() {
		return dimension;
	}

	public static void setDimension(ResourceKey<Level> dim) {
		dimension = dim;
	}

	public static boolean isVisible() {
		return outlineVisible;
	}

	public static void setVisible(boolean vis) {
		outlineVisible = vis;
	}

	public static boolean toggleVisible() {
		outlineVisible = !outlineVisible;
		return outlineVisible;
	}

	public static boolean isGhostVisible() {
		return ghostVisible;
	}

	public static void setGhostVisible(boolean vis) {
		ghostVisible = vis;
	}

	public static boolean toggleGhostVisible() {
		ghostVisible = !ghostVisible;
		return ghostVisible;
	}

	public static void setPlacement(BlockPos pos, ResourceKey<Level> dim) {
		position = pos;
		dimension = dim;
		outlineVisible = true;
		ghostVisible = true;
		invalidateBlockCache();
	}

	public static void clear() {
		position = null;
		dimension = null;
		rotation = 0;
		mirrored = false;
		layerMode = LayerMode.ALL;
		currentLayer = 1;
		invalidateBlockCache();
		io.github.rafayshhh.shhhbuilder.client.material.PlayerInventoryScanner.clearPlacedCache();
	}

	/**
	 * Thoroughly clears active placements, cached visible block transforms, and unloads
	 * the active schematic session so joining a new multiplayer server or singleplayer world
	 * never carries over ghost blocks, bounding boxes, or state from a previous world.
	 * Also safely flushes pending checklist progress to disk.
	 */
	public static void cleanupWorldSession() {
		clear();
		GhostBlockRenderer.resetOnDisconnect();
		SchematicManager.setSelectedFileName(null);
		MaterialProgressManager.flush();
	}

	public static void nudge(Direction direction) {
		if (position != null && direction != null) {
			position = position.relative(direction);
		}
	}

	public static void nudge(int dx, int dy, int dz) {
		if (position != null) {
			position = position.offset(dx, dy, dz);
		}
	}

	public static void invalidateBlockCache() {
		cachedBlocks = null;
		cachedSections = null;
		cachedBlockMap = null;
		cachedSolidBlockCoords = null;
		cachedRotation = -1;
		cachedMirrored = false;
		cachedLayerMode = null;
		cachedLayer = -1;
		cachedSourceBlockData = null;
	}

	public static boolean isSolidAt(int relX, int relY, int relZ) {
		if (cachedSolidBlockCoords != null) {
			return cachedSolidBlockCoords.contains(BlockPos.asLong(relX, relY, relZ));
		}
		return false;
	}

	public static boolean hasBlockAt(int relX, int relY, int relZ) {
		if (cachedBlockMap != null) {
			return cachedBlockMap.containsKey(BlockPos.asLong(relX, relY, relZ));
		}
		return false;
	}

	public static TransformedBlock getBlockAt(int relX, int relY, int relZ) {
		if (cachedBlockMap != null) {
			return cachedBlockMap.get(BlockPos.asLong(relX, relY, relZ));
		}
		return null;
	}

	/**
	 * Retrieves the cached array of transformed blocks.
	 * Recomputes only when the schematic, rotation, mirror state, or layer mode/level changes.
	 */
	public static TransformedBlock[] getCachedBlocks(LitematicData data) {
		if (data == null || data.blockData() == null || !data.blockData().canPreview()) {
			return new TransformedBlock[0];
		}

		SchematicBlockData blockData = data.blockData();
		if (cachedBlocks != null
			&& cachedRotation == rotation
			&& cachedMirrored == mirrored
			&& cachedLayerMode == layerMode
			&& cachedLayer == currentLayer
			&& cachedSourceBlockData == blockData) {
			return cachedBlocks;
		}

		Rotation rotEnum = switch (rotation) {
			case 90 -> Rotation.CLOCKWISE_90;
			case 180 -> Rotation.CLOCKWISE_180;
			case 270 -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};

		BlockState[] origPalette = blockData.palette();
		BlockState[] transformedPalette = new BlockState[origPalette.length];
		for (int i = 0; i < origPalette.length; i++) {
			BlockState s = origPalette[i];
			if (s != null) {
				try {
					if (mirrored) {
						s = s.mirror(Mirror.FRONT_BACK);
					}
					s = s.rotate(rotEnum);
					transformedPalette[i] = s;
				} catch (Throwable ignored) {
					transformedPalette[i] = s;
				}
			}
		}

		int W = blockData.width();
		int L = blockData.length();

		SchematicBlockData.VisibleBlock[] visibleBlocks = blockData.visibleBlocks();
		List<TransformedBlock> list = new ArrayList<>(visibleBlocks.length);
		Map<Long, TransformedBlock> blockMap = new java.util.HashMap<>(visibleBlocks.length);
		java.util.Set<Long> solidCoords = new java.util.HashSet<>(visibleBlocks.length);

		LayerMode mode = getLayerMode();

		for (SchematicBlockData.VisibleBlock vb : visibleBlocks) {
			int blockLayer = vb.y() + 1; // 1-based layer

			// Layer mode filtering
			if (mode == LayerMode.SINGLE && blockLayer != currentLayer) {
				continue;
			}
			if (mode == LayerMode.UP_TO && blockLayer > currentLayer) {
				continue;
			}

			// Occlusion culling:
			// When all 6 neighbors are rendered, blocks with occludedY are completely hidden inside the volume.
			// When sliced horizontally:
			// - In SINGLE mode: neither Y-1 nor Y+1 is rendered, so top/bottom faces are exposed -> kept visible.
			// - In UP_TO mode: at currentLayer, Y+1 is not rendered, so top face is exposed -> kept visible.
			//                  below currentLayer, both Y-1 and Y+1 are rendered -> culled.
			if (vb.occludedY()) {
				if (mode == LayerMode.ALL) {
					continue;
				}
				if (mode == LayerMode.UP_TO && blockLayer < currentLayer) {
					continue;
				}
			}

			// Mirror left-to-right (flips X axis within [0, W - 1])
			int mx = mirrored ? (W - 1 - vb.x()) : vb.x();
			int mz = vb.z();

			// Rotate around Y axis
			int rx, rz;
			switch (rotation) {
				case 90 -> {
					rx = L - 1 - mz;
					rz = mx;
				}
				case 180 -> {
					rx = W - 1 - mx;
					rz = L - 1 - mz;
				}
				case 270 -> {
					rx = mz;
					rz = W - 1 - mx;
				}
				default -> {
					rx = mx;
					rz = mz;
				}
			}

			BlockState state = (vb.paletteIndex() >= 0 && vb.paletteIndex() < transformedPalette.length && transformedPalette[vb.paletteIndex()] != null)
				? transformedPalette[vb.paletteIndex()]
				: (BlockStateHelper.isBootstrapped() ? Blocks.STONE.defaultBlockState() : null);

			TransformedBlock tb = new TransformedBlock(rx, vb.y(), rz, state);
			list.add(tb);

			long posKey = BlockPos.asLong(rx, vb.y(), rz);
			blockMap.put(posKey, tb);
			boolean solid = false;
			if (state != null) {
				try {
					solid = state.isSolidRender();
				} catch (Throwable ignored) {
					solid = true;
				}
			} else {
				solid = true; // In non-bootstrapped unit test environments
			}
			if (solid) {
				solidCoords.add(posKey);
			}
		}

		cachedBlocks = list.toArray(TransformedBlock[]::new);
		cachedBlockMap = blockMap;
		cachedSolidBlockCoords = solidCoords;

		// Group into 16x16x16 sections for distance-prioritized culling and rendering
		Map<Long, List<TransformedBlock>> sectionMap = new LinkedHashMap<>();
		for (TransformedBlock b : list) {
			int sx = b.relX() >> 4;
			int sy = b.relY() >> 4;
			int sz = b.relZ() >> 4;
			long key = ((long) (sx & 0x3FFFFF) << 42) | ((long) (sy & 0xFFFFF) << 20) | (long) (sz & 0xFFFFF);
			sectionMap.computeIfAbsent(key, k -> new ArrayList<>()).add(b);
		}

		List<BlockSection> secList = new ArrayList<>(sectionMap.size());
		for (List<TransformedBlock> secBlocks : sectionMap.values()) {
			if (secBlocks.isEmpty()) continue;
			TransformedBlock first = secBlocks.get(0);
			int sx = first.relX() >> 4;
			int sy = first.relY() >> 4;
			int sz = first.relZ() >> 4;
			double cx = (sx << 4) + 8.0;
			double cy = (sy << 4) + 8.0;
			double cz = (sz << 4) + 8.0;
			secList.add(new BlockSection(sx, sy, sz, cx, cy, cz, secBlocks.toArray(TransformedBlock[]::new)));
		}
		cachedSections = secList.toArray(BlockSection[]::new);

		cachedRotation = rotation;
		cachedMirrored = mirrored;
		cachedLayerMode = layerMode;
		cachedLayer = currentLayer;
		cachedSourceBlockData = blockData;
		return cachedBlocks;
	}

	public static BlockSection[] getCachedSections(LitematicData data) {
		getCachedBlocks(data);
		return cachedSections != null ? cachedSections : new BlockSection[0];
	}

	/**
	 * Returns all transformed blocks for the placement, including interior blocks,
	 * optionally filtered to a single layer (0-based) or all layers (-1).
	 */
	public static List<TransformedBlock> getTransformedBlocks(LitematicData data, int layerIndex0Based) {
		if (data == null || data.blockData() == null || !data.blockData().canPreview()) {
			return java.util.Collections.emptyList();
		}
		SchematicBlockData blockData = data.blockData();
		Rotation rotEnum = switch (rotation) {
			case 90 -> Rotation.CLOCKWISE_90;
			case 180 -> Rotation.CLOCKWISE_180;
			case 270 -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};

		BlockState[] origPalette = blockData.palette();
		BlockState[] transformedPalette = new BlockState[origPalette.length];
		for (int i = 0; i < origPalette.length; i++) {
			BlockState s = origPalette[i];
			if (s != null) {
				try {
					if (mirrored) {
						s = s.mirror(Mirror.FRONT_BACK);
					}
					s = s.rotate(rotEnum);
					transformedPalette[i] = s;
				} catch (Throwable ignored) {
					transformedPalette[i] = s;
				}
			}
		}

		int W = blockData.width();
		int L = blockData.length();
		SchematicBlockData.VisibleBlock[] visibleBlocks = blockData.visibleBlocks();
		List<TransformedBlock> list = new ArrayList<>(visibleBlocks.length);

		for (SchematicBlockData.VisibleBlock vb : visibleBlocks) {
			if (layerIndex0Based >= 0 && vb.y() != layerIndex0Based) {
				continue;
			}
			int mx = mirrored ? (W - 1 - vb.x()) : vb.x();
			int mz = vb.z();
			int rx, rz;
			switch (rotation) {
				case 90 -> {
					rx = L - 1 - mz;
					rz = mx;
				}
				case 180 -> {
					rx = W - 1 - mx;
					rz = L - 1 - mz;
				}
				case 270 -> {
					rx = mz;
					rz = W - 1 - mx;
				}
				default -> {
					rx = mx;
					rz = mz;
				}
			}

			BlockState state = (vb.paletteIndex() >= 0 && vb.paletteIndex() < transformedPalette.length && transformedPalette[vb.paletteIndex()] != null)
				? transformedPalette[vb.paletteIndex()]
				: (BlockStateHelper.isBootstrapped() ? Blocks.STONE.defaultBlockState() : null);
			if (state != null && !state.isAir()) {
				list.add(new TransformedBlock(rx, vb.y(), rz, state));
			}
		}
		return list;
	}

	public static int normalizeRotation(int rot) {
		int normalized = rot % 360;
		if (normalized < 0) {
			normalized += 360;
		}
		// Snap to nearest 90-degree step
		return (Math.round(normalized / 90.0f) * 90) % 360;
	}
}
