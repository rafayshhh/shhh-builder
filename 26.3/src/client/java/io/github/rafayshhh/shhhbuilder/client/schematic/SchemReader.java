package io.github.rafayshhh.shhhbuilder.client.schematic;

import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Custom reader for Sponge schematic (.schem) files.
 * Parses Sponge Schematic Specification formats (v1, v2, v3),
 * decodes VarInt-packed block arrays, extracts metadata and block counts,
 * and builds a compact 3D block representation with 6-neighbor occlusion culling.
 */
public class SchemReader {
	public static final long MAX_FILE_SIZE = 50 * 1024 * 1024L; // 50 MB safety limit
	public static final long MAX_PREVIEW_VOLUME = 2_000_000L; // 2 million blocks preview cap
	public static final long MAX_SCHEM_VOLUME = 10_000_000L; // 10 million blocks safety limit
	public static final int MAX_PALETTE_SIZE = 65536; // 64k block states limit
	public static final long MAX_NBT_QUOTA = 256 * 1024 * 1024L; // 256 MB safety quota against decompression bombs
	public static final int MAX_NBT_DEPTH = 512; // 512 nested tag depth

	/**
	 * Reads and parses a .schem file into a LitematicData model.
	 */
	public static LitematicData read(Path path) throws IOException {
		if (path == null) {
			throw new IOException("Schematic path cannot be null.");
		}

		if (!Files.exists(path)) {
			throw new IOException("File not found: " + path.getFileName());
		}

		long fileSize = Files.size(path);
		if (fileSize == 0) {
			throw new IOException("The schematic file is empty (0 bytes).");
		}

		if (fileSize > MAX_FILE_SIZE) {
			throw new IOException(String.format(
				Locale.ROOT,
				"File size (%.1f MB) exceeds maximum allowed limit of 50 MB.",
				fileSize / (1024.0 * 1024.0)
			));
		}

		CompoundTag root;
		try {
			root = NbtIo.readCompressed(path, new NbtAccounter(MAX_NBT_QUOTA, MAX_NBT_DEPTH));
		} catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("tried to read nbt tag that was too big")) {
				throw new IOException("Decompressed size exceeds 256 MB safety limit. Schematic is too large or corrupt.", e);
			}
			throw new IOException("Failed to read compressed NBT data. The file may be corrupt or not a valid .schem file.", e);
		}

		if (root == null) {
			throw new IOException("Schematic root tag is null.");
		}

		// Detect version and data container:
		// In v3, the data is nested under a root "Schematic" tag.
		// In v1 and v2, the data sits directly in the root tag.
		CompoundTag schematicTag;
		if (root.contains("Schematic")) {
			schematicTag = root.getCompoundOrEmpty("Schematic");
		} else {
			schematicTag = root;
		}

		int version = schematicTag.getIntOr("Version", root.getIntOr("Version", 0));
		int dataVersion = schematicTag.getIntOr("DataVersion", root.getIntOr("DataVersion", -1));

		// Dimensions: Width (X), Height (Y), Length (Z)
		int width = readDimension(schematicTag, "Width");
		int height = readDimension(schematicTag, "Height");
		int length = readDimension(schematicTag, "Length");

		if (width <= 0 || height <= 0 || length <= 0) {
			throw new IOException("Schematic has invalid dimensions (" + width + " x " + height + " x " + length + ").");
		}
		if (width > 4096 || height > 4096 || length > 4096) {
			throw new IOException("Schematic dimensions (" + width + "x" + height + "x" + length + ") exceed safety limit of 4096 blocks per axis.");
		}

		long totalVolume = (long) width * height * length;
		if (totalVolume <= 0 || totalVolume > MAX_SCHEM_VOLUME) {
			throw new IOException("Schematic volume (" + totalVolume + " blocks) is invalid or exceeds safety limit of " + MAX_SCHEM_VOLUME + " blocks.");
		}

		// Metadata: Name and Author
		CompoundTag metadata = schematicTag.getCompoundOrEmpty("Metadata");
		String name = metadata.getStringOr("Name", "").trim();
		if (name.isEmpty()) {
			name = schematicTag.getStringOr("Name", "").trim();
		}
		if (name.isEmpty()) {
			String fileName = path.getFileName().toString();
			int dot = fileName.lastIndexOf('.');
			name = dot > 0 ? fileName.substring(0, dot) : fileName;
		}

		String author = metadata.getStringOr("Author", "").trim();
		if (author.isEmpty()) {
			author = schematicTag.getStringOr("Author", "").trim();
		}
		if (author.isEmpty()) {
			author = "Unknown";
		}

		// Palette: "BlockPalette" in v3, "Palette" in v1/v2
		CompoundTag paletteTag = null;
		if (schematicTag.contains("BlockPalette")) {
			paletteTag = schematicTag.getCompoundOrEmpty("BlockPalette");
		} else if (schematicTag.contains("Palette")) {
			paletteTag = schematicTag.getCompoundOrEmpty("Palette");
		} else if (root.contains("BlockPalette")) {
			paletteTag = root.getCompoundOrEmpty("BlockPalette");
		} else if (root.contains("Palette")) {
			paletteTag = root.getCompoundOrEmpty("Palette");
		}

		if (paletteTag == null || paletteTag.isEmpty()) {
			throw new IOException("Schematic is missing block palette data (Palette / BlockPalette tag).");
		}
		if (paletteTag.keySet().size() > MAX_PALETTE_SIZE) {
			throw new IOException("Schematic palette entries (" + paletteTag.keySet().size() + ") exceed safety limit of " + MAX_PALETTE_SIZE + ".");
		}

		// Find maximum palette index to allocate index lookup array
		int maxPaletteIndex = -1;
		for (String key : paletteTag.keySet()) {
			int idx = paletteTag.getIntOr(key, -1);
			if (idx > maxPaletteIndex) {
				maxPaletteIndex = idx;
			}
		}

		if (maxPaletteIndex < 0) {
			throw new IOException("Schematic palette is empty or contains no valid indices.");
		}
		if (maxPaletteIndex > MAX_PALETTE_SIZE) {
			throw new IOException("Schematic palette max index (" + maxPaletteIndex + ") exceeds safety limit of " + MAX_PALETTE_SIZE + ".");
		}

		String[] paletteRawNames = new String[maxPaletteIndex + 1];
		BlockState[] paletteStates = new BlockState[maxPaletteIndex + 1];
		Arrays.fill(paletteRawNames, "minecraft:air");

		for (String rawState : paletteTag.keySet()) {
			int idx = paletteTag.getIntOr(rawState, -1);
			if (idx >= 0 && idx <= maxPaletteIndex) {
				paletteRawNames[idx] = rawState;
				paletteStates[idx] = BlockStateHelper.parseBlockStateString(rawState);
			}
		}

		// BlockData: "BlockData" in v2/v3, "Data" in some v1/tools
		byte[] rawBlockData = schematicTag.getByteArray("BlockData").orElse(null);
		if (rawBlockData == null) {
			rawBlockData = schematicTag.getByteArray("Data").orElse(null);
		}
		if (rawBlockData == null && root.contains("BlockData")) {
			rawBlockData = root.getByteArray("BlockData").orElse(null);
		}
		if (rawBlockData == null && root.contains("Data")) {
			rawBlockData = root.getByteArray("Data").orElse(null);
		}

		if (rawBlockData == null || rawBlockData.length == 0) {
			throw new IOException("Schematic is missing BlockData byte array.");
		}

		int volume = (int) totalVolume;
		int[] blockIndices = new int[volume];
		decodeVarIntArray(rawBlockData, blockIndices);

		// Material counts & item evaluation using MaterialCountingEngine
		io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.BlockEvaluation[] evaluations =
			new io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.BlockEvaluation[paletteRawNames.length];
		for (int i = 0; i < paletteRawNames.length; i++) {
			if (paletteStates[i] != null) {
				evaluations[i] = io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.evaluate(paletteStates[i]);
			} else {
				evaluations[i] = io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.evaluate(null, paletteRawNames[i]);
			}
		}

		// Map local palette indices to a unified palette where index 0 is always Air
		List<BlockState> unifiedPalette = new ArrayList<>();
		unifiedPalette.add(BlockStateHelper.isBootstrapped() ? Blocks.AIR.defaultBlockState() : null);

		int[] localToGlobalPalette = new int[paletteRawNames.length];
		for (int i = 0; i < paletteRawNames.length; i++) {
			String rawName = paletteRawNames[i];
			if (BlockStateHelper.isAirName(rawName)) {
				localToGlobalPalette[i] = 0;
			} else {
				int globalIndex = unifiedPalette.size();
				unifiedPalette.add(paletteStates[i]);
				localToGlobalPalette[i] = globalIndex;
			}
		}

		// Count materials overall and per layer using MaterialCountingEngine, and build 3D grid
		io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.Collector matCollector =
			new io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.Collector();

		int[] grid = new int[volume];
		int blockIdx = 0;
		for (int y = 0; y < height; y++) {
			for (int z = 0; z < length; z++) {
				for (int x = 0; x < width; x++) {
					int palIdx = blockIndices[blockIdx];
					if (palIdx >= 0 && palIdx < paletteRawNames.length) {
						String rawName = paletteRawNames[palIdx];
						if (!BlockStateHelper.isAirName(rawName)) {
							matCollector.addVoxel(y, evaluations[palIdx], 1);
							grid[blockIdx] = localToGlobalPalette[palIdx];
						} else {
							grid[blockIdx] = 0;
						}
					} else {
						grid[blockIdx] = 0;
					}
					blockIdx++;
				}
			}
		}

		io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.MaterialCountResult materialResult = matCollector.build();
		Map<String, Long> itemCounts = materialResult.overallItemCounts();
		List<Map.Entry<String, Long>> topItems = materialResult.sortedOverallItems();
		long totalNonAirBlocks = materialResult.totalVoxels();
		long totalItems = materialResult.totalItemsNeeded();
		long unobtainableBlocks = materialResult.unobtainableCount();
		long unknownBlocks = materialResult.unknownCount();

		// Occlusion culling and block data creation
		SchematicBlockData schematicBlockData;
		if (totalVolume > MAX_PREVIEW_VOLUME) {
			schematicBlockData = SchematicBlockData.tooLarge(width, height, length, totalVolume, MAX_PREVIEW_VOLUME);
		} else {
			List<SchematicBlockData.VisibleBlock> visibleBlocks = new ArrayList<>();
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

						visibleBlocks.add(new SchematicBlockData.VisibleBlock(x, y, z, gIndex, occludedY));
					}
				}
			}

			schematicBlockData = new SchematicBlockData(
				width,
				height,
				length,
				totalVolume,
				true,
				null,
				unifiedPalette.toArray(new BlockState[0]),
				visibleBlocks.toArray(new SchematicBlockData.VisibleBlock[0])
			);
		}

		// Log concise summary
		ShhhBuilder.LOGGER.info("Loaded schematic '{}' ({} non-air blocks, {} items needed, {}x{}x{})", name, totalNonAirBlocks, totalItems, width, height, length);

		return new LitematicData(
			name,
			author,
			1, // Sponge schematics represent a single rectangular region
			width,
			height,
			length,
			totalNonAirBlocks,
			totalItems,
			unobtainableBlocks,
			unknownBlocks,
			itemCounts,
			topItems,
			schematicBlockData,
			materialResult
		);
	}

	private static int readDimension(CompoundTag tag, String key) {
		int val = tag.getIntOr(key, -1);
		if (val <= 0) {
			val = tag.getShortOr(key, (short) 0) & 0xFFFF;
		}
		return val;
	}

	/**
	 * Decodes an array of VarInts from a byte array into a target integer array.
	 * VarInt format uses 7 data bits per byte with bit 7 (0x80) as continuation flag.
	 */
	public static void decodeVarIntArray(byte[] bytes, int[] target) throws IOException {
		if (bytes == null || target == null) {
			throw new IOException("Cannot decode VarInts: source or target is null.");
		}

		int byteIndex = 0;
		for (int i = 0; i < target.length; i++) {
			int value = 0;
			int shift = 0;
			while (true) {
				if (byteIndex >= bytes.length) {
					throw new IOException("Unexpected end of BlockData: expected " + target.length + " blocks, but byte array ended at block " + i);
				}
				byte b = bytes[byteIndex++];
				value |= (b & 0x7F) << shift;
				if ((b & 0x80) == 0) {
					break;
				}
				shift += 7;
				if (shift >= 35) {
					throw new IOException("VarInt decoding error: exceeded 35 bits at block " + i);
				}
			}
			target[i] = value;
		}
	}

	/**
	 * Encodes an integer array into a VarInt-packed byte array.
	 */
	public static byte[] encodeVarIntArray(int[] values) {
		if (values == null || values.length == 0) {
			return new byte[0];
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(values.length);
		for (int val : values) {
			encodeVarInt(out, val);
		}
		return out.toByteArray();
	}

	/**
	 * Encodes a single 32-bit integer as a VarInt into the given output stream.
	 */
	public static void encodeVarInt(OutputStream out, int value) {
		try {
			while ((value & ~0x7F) != 0) {
				out.write((value & 0x7F) | 0x80);
				value >>>= 7;
			}
			out.write(value & 0x7F);
		} catch (IOException ignored) {
		}
	}
}
