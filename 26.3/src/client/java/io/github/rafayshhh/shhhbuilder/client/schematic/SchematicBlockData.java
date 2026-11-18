package io.github.rafayshhh.shhhbuilder.client.schematic;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Compact block structure data for a schematic:
 * a compact palette of Minecraft BlockStates and a pre-culled list of visible blocks
 * (excluding blocks hidden on all six sides).
 */
public record SchematicBlockData(
	int width,
	int height,
	int length,
	long totalVolume,
	boolean canPreview,
	String previewDisabledReason,
	BlockState[] palette,
	VisibleBlock[] visibleBlocks
) {
	/**
	 * Represents a non-hidden block inside the schematic.
	 *
	 * @param x local X coordinate (0 <= x < width)
	 * @param y local Y coordinate (0 <= y < height)
	 * @param z local Z coordinate (0 <= z < length)
	 * @param paletteIndex index into the schematic palette
	 * @param occludedY true if this block is enclosed by solid non-air neighbors on all 6 sides
	 *                  (including Y-1 and Y+1), meaning it can be culled when both Y
	 *                  neighbors are rendered (LayerMode.ALL, or below currentLayer in UP_TO),
	 *                  but should remain visible when sliced horizontally (LayerMode.SINGLE,
	 *                  or at currentLayer in UP_TO).
	 */
	public record VisibleBlock(int x, int y, int z, int paletteIndex, boolean occludedY) {
		public VisibleBlock(int x, int y, int z, int paletteIndex) {
			this(x, y, z, paletteIndex, false);
		}
	}

	public static SchematicBlockData tooLarge(int width, int height, int length, long totalVolume, long maxVolume) {
		return new SchematicBlockData(
			width,
			height,
			length,
			totalVolume,
			false,
			String.format("Schematic volume (%,d blocks) exceeds %,d preview limit.", totalVolume, maxVolume),
			new BlockState[0],
			new VisibleBlock[0]
		);
	}

	public static SchematicBlockData empty() {
		return new SchematicBlockData(
			0, 0, 0, 0,
			false,
			"Schematic contains no blocks.",
			new BlockState[0],
			new VisibleBlock[0]
		);
	}
}
