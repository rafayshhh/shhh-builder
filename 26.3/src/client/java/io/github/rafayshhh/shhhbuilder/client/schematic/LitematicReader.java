package io.github.rafayshhh.shhhbuilder.client.schematic;

import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Custom reader for .litematic files.
 * Parses gzip-compressed NBT data, extracts metadata, regions, and materials breakdown,
 * and builds a compact 3D block representation with 6-neighbor occlusion culling.
 */
public class LitematicReader {
	public static final long MAX_FILE_SIZE = 50 * 1024 * 1024L; // 50 MB safety limit
	public static final long MAX_PREVIEW_VOLUME = 2_000_000L; // 2 million blocks preview cap
	public static final long MAX_REGION_VOLUME = 10_000_000L; // 10 million blocks safety limit per region
	public static final long MAX_NBT_QUOTA = 256 * 1024 * 1024L; // 256 MB safety quota against decompression bombs
	public static final int MAX_NBT_DEPTH = 512; // 512 nested tag depth

	/**
	 * Calculates the minimum number of bits required to store indices for a palette of the given size.
	 * In .litematic format, bits per block is at least 2 bits.
	 */
	public static int getBitsPerBlock(int paletteSize) {
		if (paletteSize <= 4) {
			return 2;
		}
		int bits = 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
		return Math.max(2, bits);
	}

	/**
	 * Checks if a block name represents an air block.
	 */
	public static boolean isAir(String blockName) {
		return BlockStateHelper.isAirName(blockName);
	}

	/**
	 * Unpacks a single palette index from a packed long array at a specific block offset.
	 */
	public static int unpackBlockStateIndex(long[] longs, long blockIndex, int bitsPerBlock) {
		if (longs == null || longs.length == 0 || bitsPerBlock <= 0) {
			return 0;
		}

		long bitIndex = blockIndex * bitsPerBlock;
		int startLongIndex = (int) (bitIndex / 64);
		int bitOffset = (int) (bitIndex % 64);
		int endLongIndex = (int) ((bitIndex + bitsPerBlock - 1) / 64);

		if (startLongIndex < 0 || startLongIndex >= longs.length) {
			return 0;
		}

		long mask = (1L << bitsPerBlock) - 1L;

		if (startLongIndex == endLongIndex) {
			return (int) ((longs[startLongIndex] >>> bitOffset) & mask);
		} else {
			int bitsInFirstLong = 64 - bitOffset;
			long lowBits = (longs[startLongIndex] >>> bitOffset);
			long highBits = (endLongIndex < longs.length) ? (longs[endLongIndex] << bitsInFirstLong) : 0L;
			return (int) ((lowBits | highBits) & mask);
		}
	}

	/**
	 * Packs a palette index into a long array at a specific block offset (used in synthetic test generation).
	 */
	public static void packBlockStateIndex(long[] longs, long blockIndex, int bitsPerBlock, int value) {
		if (longs == null || longs.length == 0 || bitsPerBlock <= 0) {
			return;
		}

		long bitIndex = blockIndex * bitsPerBlock;
		int startLongIndex = (int) (bitIndex / 64);
		int bitOffset = (int) (bitIndex % 64);
		int endLongIndex = (int) ((bitIndex + bitsPerBlock - 1) / 64);

		long mask = (1L << bitsPerBlock) - 1L;
		long val = value & mask;

		if (startLongIndex >= longs.length) {
			return;
		}

		if (startLongIndex == endLongIndex) {
			longs[startLongIndex] &= ~(mask << bitOffset);
			longs[startLongIndex] |= (val << bitOffset);
		} else {
			int bitsInFirstLong = 64 - bitOffset;
			longs[startLongIndex] &= ~(mask << bitOffset);
			longs[startLongIndex] |= (val << bitOffset);

			if (endLongIndex < longs.length) {
				longs[endLongIndex] &= ~(mask >>> bitsInFirstLong);
				longs[endLongIndex] |= (val >>> bitsInFirstLong);
			}
		}
	}

	/**
	 * Temporary raw representation of a region during parsing.
	 */
	private static class RawRegion {
		final int posX, posY, posZ;
		final int sizeX, sizeY, sizeZ;
		final int absX, absY, absZ;
		final List<BlockState> palette;
		final List<String> rawPaletteNames;
		final int[] blockIndices;

		RawRegion(int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ,
				  List<BlockState> palette, List<String> rawPaletteNames, int[] blockIndices) {
			this.posX = posX;
			this.posY = posY;
			this.posZ = posZ;
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = sizeZ;
			this.absX = Math.abs(sizeX);
			this.absY = Math.abs(sizeY);
			this.absZ = Math.abs(sizeZ);
			this.palette = palette;
			this.rawPaletteNames = rawPaletteNames;
			this.blockIndices = blockIndices;
		}
	}

	/**
	 * Reads and parses a .litematic file from disk.
	 */
	public static LitematicData read(Path path) throws IOException {
		if (!Files.exists(path)) {
			throw new IOException("File does not exist: " + path.getFileName());
		}

		long fileSize = Files.size(path);
		if (fileSize == 0) {
			throw new IOException("File is empty (0 bytes).");
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
			throw new IOException("Failed to read compressed NBT data. The file may be corrupt or not a valid .litematic schematic.", e);
		}

		if (root == null) {
			throw new IOException("Schematic root tag is null.");
		}

		// Metadata (optional)
		CompoundTag metadata = root.getCompoundOrEmpty("Metadata");
		String rawName = metadata.getStringOr("Name", "").trim();
		if (rawName.isEmpty()) {
			String fileName = path.getFileName().toString();
			int dot = fileName.lastIndexOf('.');
			rawName = dot > 0 ? fileName.substring(0, dot) : fileName;
		}

		String author = metadata.getStringOr("Author", "").trim();
		if (author.isEmpty()) {
			author = "Unknown";
		}

		// Regions
		CompoundTag regionsTag = root.getCompoundOrEmpty("Regions");
		if (regionsTag.isEmpty()) {
			throw new IOException("Schematic is invalid: missing 'Regions' compound data.");
		}

		int regionCount = regionsTag.keySet().size();
		Map<String, Long> itemCounts = new LinkedHashMap<>();
		long totalNonAirBlocks = 0;
		long totalItems = 0;
		long unobtainableBlocks = 0;

		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		boolean hasBounds = false;

		List<RawRegion> rawRegions = new ArrayList<>();

		for (String regionName : regionsTag.keySet()) {
			CompoundTag region = regionsTag.getCompoundOrEmpty(regionName);

			// Region position and size
			CompoundTag posTag = region.getCompoundOrEmpty("Position");
			int posX = posTag.getIntOr("x", 0);
			int posY = posTag.getIntOr("y", 0);
			int posZ = posTag.getIntOr("z", 0);

			CompoundTag sizeTag = region.getCompoundOrEmpty("Size");
			int sizeX = sizeTag.getIntOr("x", 0);
			int sizeY = sizeTag.getIntOr("y", 0);
			int sizeZ = sizeTag.getIntOr("z", 0);

			long absX = Math.abs((long) sizeX);
			long absY = Math.abs((long) sizeY);
			long absZ = Math.abs((long) sizeZ);
			if (absX > 4096 || absY > 4096 || absZ > 4096) {
				throw new IOException("Region '" + regionName + "' dimensions (" + absX + "x" + absY + "x" + absZ + ") exceed safety limit of 4096 blocks per axis.");
			}
			long regionVolume = absX * absY * absZ;
			if (regionVolume > MAX_REGION_VOLUME) {
				throw new IOException("Region '" + regionName + "' volume (" + regionVolume + " blocks) exceeds safety limit of " + MAX_REGION_VOLUME + " blocks.");
			}

			// Update bounding box taking signed coordinates into account
			if (absX > 0 && absY > 0 && absZ > 0) {
				int regMinX = sizeX < 0 ? (int) (posX + sizeX + 1) : posX;
				int regMaxX = sizeX < 0 ? posX : (int) (posX + sizeX - 1);
				int regMinY = sizeY < 0 ? (int) (posY + sizeY + 1) : posY;
				int regMaxY = sizeY < 0 ? posY : (int) (posY + sizeY - 1);
				int regMinZ = sizeZ < 0 ? (int) (posZ + sizeZ + 1) : posZ;
				int regMaxZ = sizeZ < 0 ? posZ : (int) (posZ + sizeZ - 1);

				minX = Math.min(minX, regMinX);
				maxX = Math.max(maxX, regMaxX);
				minY = Math.min(minY, regMinY);
				maxY = Math.max(maxY, regMaxY);
				minZ = Math.min(minZ, regMinZ);
				maxZ = Math.max(maxZ, regMaxZ);
				hasBounds = true;
			}

			// BlockStatePalette
			ListTag paletteTag = region.getListOrEmpty("BlockStatePalette");
			List<String> rawNames = new ArrayList<>(paletteTag.size());
			List<BlockState> parsedPalette = new ArrayList<>(paletteTag.size());

			for (int i = 0; i < paletteTag.size(); i++) {
				CompoundTag stateTag = paletteTag.getCompoundOrEmpty(i);
				String blockName = stateTag.getStringOr("id", stateTag.getStringOr("Name", "minecraft:air"));
				rawNames.add(blockName);
				BlockState state = BlockStateHelper.parseBlockState(stateTag);
				parsedPalette.add(state);
			}

			if (parsedPalette.isEmpty()) {
				rawNames.add("minecraft:air");
				parsedPalette.add(BlockStateHelper.isBootstrapped() ? Blocks.AIR.defaultBlockState() : null);
			}

			// BlockStates array
			Optional<long[]> blockStatesOpt = region.getLongArray("BlockStates");
			long[] blockStates = blockStatesOpt.orElse(null);
			int[] blockIndices = (regionVolume > 0 && regionVolume <= Integer.MAX_VALUE) ? new int[(int) regionVolume] : null;

			if (blockStates != null && blockStates.length > 0 && regionVolume > 0 && blockIndices != null) {
				int bitsPerBlock = getBitsPerBlock(parsedPalette.size());
				for (int i = 0; i < (int) regionVolume; i++) {
					blockIndices[i] = unpackBlockStateIndex(blockStates, i, bitsPerBlock);
				}
			}

			if (blockIndices != null) {
				rawRegions.add(new RawRegion(posX, posY, posZ, sizeX, sizeY, sizeZ, parsedPalette, rawNames, blockIndices));
			}
		}

		// Enclosing dimensions
		int overallWidth = 0;
		int overallHeight = 0;
		int overallLength = 0;

		CompoundTag enclosingSize = metadata.getCompoundOrEmpty("EnclosingSize");
		int encX = Math.abs(enclosingSize.getIntOr("x", 0));
		int encY = Math.abs(enclosingSize.getIntOr("y", 0));
		int encZ = Math.abs(enclosingSize.getIntOr("z", 0));

		if (hasBounds) {
			int boundW = maxX - minX + 1;
			int boundH = maxY - minY + 1;
			int boundL = maxZ - minZ + 1;
			if (encX > 0 && encY > 0 && encZ > 0) {
				overallWidth = Math.max(encX, boundW);
				overallHeight = Math.max(encY, boundH);
				overallLength = Math.max(encZ, boundL);
			} else {
				overallWidth = boundW;
				overallHeight = boundH;
				overallLength = boundL;
			}
		} else {
			overallWidth = encX;
			overallHeight = encY;
			overallLength = encZ;
		}

		// Calculate exact material requirements overall and per layer using MaterialCountingEngine
		io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.Collector matCollector =
			new io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.Collector();

		for (RawRegion reg : rawRegions) {
			List<io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.BlockEvaluation> evaluations = new ArrayList<>(reg.palette.size());
			for (int p = 0; p < reg.palette.size(); p++) {
				BlockState state = reg.palette.get(p);
				String paletteRawName = p < reg.rawPaletteNames.size() ? reg.rawPaletteNames.get(p) : "";
				if (state != null) {
					evaluations.add(io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.evaluate(state));
				} else {
					evaluations.add(io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.evaluate(null, paletteRawName));
				}
			}

			if (reg.blockIndices != null && reg.blockIndices.length > 0) {
				for (int ly = 0; ly < reg.absY; ly++) {
					int relY = reg.posY + (reg.sizeY < 0 ? -ly : ly);
					int by = hasBounds ? relY - minY : ly;
					for (int lz = 0; lz < reg.absZ; lz++) {
						for (int lx = 0; lx < reg.absX; lx++) {
							int regIdx = (ly * reg.absZ + lz) * reg.absX + lx;
							if (regIdx < reg.blockIndices.length) {
								int pIndex = reg.blockIndices[regIdx];
								if (pIndex >= 0 && pIndex < evaluations.size()) {
									matCollector.addVoxel(by, evaluations.get(pIndex), 1);
								}
							}
						}
					}
				}
			} else if (reg.palette.size() == 1 && reg.absX * reg.absY * reg.absZ > 0) {
				int by = hasBounds ? reg.posY - minY : 0;
				matCollector.addVoxel(by, evaluations.get(0), (int) (reg.absX * reg.absY * reg.absZ));
			}
		}

		io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine.MaterialCountResult materialResult = matCollector.build();
		itemCounts = materialResult.overallItemCounts();
		List<Map.Entry<String, Long>> topItems = materialResult.sortedOverallItems();
		totalNonAirBlocks = materialResult.totalVoxels();
		totalItems = materialResult.totalItemsNeeded();
		unobtainableBlocks = materialResult.unobtainableCount();
		long unknownBlocks = materialResult.unknownCount();

		// Build compact block data & perform 6-neighbor occlusion culling
		SchematicBlockData blockData = buildSchematicBlockData(
			rawRegions,
			overallWidth,
			overallHeight,
			overallLength,
			minX,
			minY,
			minZ
		);

		// Log concise summary
		ShhhBuilder.LOGGER.info("Loaded schematic '{}' ({} non-air blocks, {} items needed, {}x{}x{})", rawName, totalNonAirBlocks, totalItems, overallWidth, overallHeight, overallLength);

		return new LitematicData(
			rawName,
			author,
			regionCount,
			overallWidth,
			overallHeight,
			overallLength,
			totalNonAirBlocks,
			totalItems,
			unobtainableBlocks,
			unknownBlocks,
			itemCounts,
			topItems,
			blockData,
			materialResult
		);
	}

	/**
	 * Builds unified compact block layout and applies 6-neighbor occlusion culling.
	 */
	public static SchematicBlockData buildSchematicBlockData(
		List<RawRegion> regions,
		int width,
		int height,
		int length,
		int minX,
		int minY,
		int minZ
	) {
		if (width <= 0 || height <= 0 || length <= 0 || regions.isEmpty()) {
			return SchematicBlockData.empty();
		}

		long totalVolume = (long) width * height * length;
		if (totalVolume > MAX_PREVIEW_VOLUME) {
			return SchematicBlockData.tooLarge(width, height, length, totalVolume, MAX_PREVIEW_VOLUME);
		}

		int volume = (int) totalVolume;

		// Unified palette: index 0 is always Air
		List<BlockState> unifiedPalette = new ArrayList<>();
		unifiedPalette.add(BlockStateHelper.isBootstrapped() ? Blocks.AIR.defaultBlockState() : null);
		Map<Object, Integer> keyToGlobal = new HashMap<>();

		// 3D grid: index = (y * length + z) * width + x
		int[] grid = new int[volume];

		for (RawRegion reg : regions) {
			int[] localToGlobal = new int[reg.palette.size()];
			for (int p = 0; p < reg.palette.size(); p++) {
				BlockState state = reg.palette.get(p);
				String rawName = p < reg.rawPaletteNames.size() ? reg.rawPaletteNames.get(p) : "";
				if (isAir(rawName) || (state != null && state.isAir())) {
					localToGlobal[p] = 0;
				} else {
					Object key = state != null ? state : rawName;
					int gIndex = keyToGlobal.computeIfAbsent(key, k -> {
						unifiedPalette.add(state);
						return unifiedPalette.size() - 1;
					});
					localToGlobal[p] = gIndex;
				}
			}

			// Map blocks into 3D grid
			for (int ly = 0; ly < reg.absY; ly++) {
				int relY = reg.posY + (reg.sizeY < 0 ? -ly : ly);
				int by = relY - minY;
				if (by < 0 || by >= height) continue;

				for (int lz = 0; lz < reg.absZ; lz++) {
					int relZ = reg.posZ + (reg.sizeZ < 0 ? -lz : lz);
					int bz = relZ - minZ;
					if (bz < 0 || bz >= length) continue;

					for (int lx = 0; lx < reg.absX; lx++) {
						int relX = reg.posX + (reg.sizeX < 0 ? -lx : lx);
						int bx = relX - minX;
						if (bx < 0 || bx >= width) continue;

						int regIdx = (ly * reg.absZ + lz) * reg.absX + lx;
						if (reg.blockIndices != null && regIdx < reg.blockIndices.length) {
							int pIndex = reg.blockIndices[regIdx];
							if (pIndex >= 0 && pIndex < localToGlobal.length) {
								int gIndex = localToGlobal[pIndex];
								if (gIndex != 0) {
									grid[(by * length + bz) * width + bx] = gIndex;
								}
							}
						}
					}
				}
			}
		}

		// 6-Neighbor Occlusion Culling:
		// Mark blocks that are completely enclosed by non-air blocks on all six sides.
		List<SchematicBlockData.VisibleBlock> visibleList = new ArrayList<>();

		for (int y = 0; y < height; y++) {
			for (int z = 0; z < length; z++) {
				for (int x = 0; x < width; x++) {
					int gIndex = grid[(y * length + z) * width + x];
					if (gIndex == 0) {
						continue; // Air block
					}

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

		return new SchematicBlockData(
			width,
			height,
			length,
			totalVolume,
			true,
			null,
			unifiedPalette.toArray(BlockState[]::new),
			visibleList.toArray(SchematicBlockData.VisibleBlock[]::new)
		);
	}
}
