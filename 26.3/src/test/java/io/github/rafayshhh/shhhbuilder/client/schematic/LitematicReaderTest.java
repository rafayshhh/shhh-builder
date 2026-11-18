package io.github.rafayshhh.shhhbuilder.client.schematic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class LitematicReaderTest {

	@Test
	public void testBitsPerBlock() {
		assertEquals(2, LitematicReader.getBitsPerBlock(0));
		assertEquals(2, LitematicReader.getBitsPerBlock(1));
		assertEquals(2, LitematicReader.getBitsPerBlock(2));
		assertEquals(2, LitematicReader.getBitsPerBlock(3));
		assertEquals(2, LitematicReader.getBitsPerBlock(4));
		assertEquals(3, LitematicReader.getBitsPerBlock(5));
		assertEquals(3, LitematicReader.getBitsPerBlock(8));
		assertEquals(4, LitematicReader.getBitsPerBlock(9));
		assertEquals(4, LitematicReader.getBitsPerBlock(16));
		assertEquals(5, LitematicReader.getBitsPerBlock(17));
		assertEquals(6, LitematicReader.getBitsPerBlock(64));
		assertEquals(7, LitematicReader.getBitsPerBlock(65));
	}

	@Test
	public void testIsAir() {
		assertTrue(LitematicReader.isAir("minecraft:air"));
		assertTrue(LitematicReader.isAir("air"));
		assertTrue(LitematicReader.isAir("minecraft:cave_air"));
		assertTrue(LitematicReader.isAir("cave_air"));
		assertTrue(LitematicReader.isAir("minecraft:void_air"));
		assertTrue(LitematicReader.isAir("void_air"));
		assertTrue(LitematicReader.isAir(null));
		assertTrue(LitematicReader.isAir(""));

		assertFalse(LitematicReader.isAir("minecraft:stone"));
		assertFalse(LitematicReader.isAir("minecraft:oak_planks"));
		assertFalse(LitematicReader.isAir("stone"));
	}

	@Test
	public void testBitPackingAcross64BitBoundaries() {
		// Test multiple bit widths (from 2 bits up to 10 bits)
		int[] testBits = {2, 3, 5, 7, 9};

		for (int b : testBits) {
			int totalBlocks = 150;
			long totalBits = (long) totalBlocks * b;
			int numLongs = (int) ((totalBits + 63) / 64);
			long[] array = new long[numLongs];

			int maxVal = (1 << b) - 1;
			int[] expected = new int[totalBlocks];
			Random rnd = new Random(42 + b);

			for (int i = 0; i < totalBlocks; i++) {
				expected[i] = rnd.nextInt(maxVal + 1);
				LitematicReader.packBlockStateIndex(array, i, b, expected[i]);
			}

			for (int i = 0; i < totalBlocks; i++) {
				int unpacked = LitematicReader.unpackBlockStateIndex(array, i, b);
				assertEquals(expected[i], unpacked, "Mismatch at block index " + i + " for " + b + " bits per block");
			}
		}
	}

	@Test
	public void testSyntheticLitematicReading(@TempDir Path tempDir) throws IOException {
		// Create a synthetic .litematic NBT file
		CompoundTag root = new CompoundTag();

		// Metadata
		CompoundTag metadata = new CompoundTag();
		metadata.putString("Name", "Test Structure");
		metadata.putString("Author", "Test Builder");
		CompoundTag enclosing = new CompoundTag();
		enclosing.putInt("x", 2);
		enclosing.putInt("y", 2);
		enclosing.putInt("z", 2);
		metadata.put("EnclosingSize", enclosing);
		root.put("Metadata", metadata);

		// Regions
		CompoundTag regions = new CompoundTag();
		CompoundTag mainRegion = new CompoundTag();

		CompoundTag pos = new CompoundTag();
		pos.putInt("x", 0);
		pos.putInt("y", 0);
		pos.putInt("z", 0);
		mainRegion.put("Position", pos);

		CompoundTag size = new CompoundTag();
		size.putInt("x", 2);
		size.putInt("y", 2);
		size.putInt("z", 2);
		mainRegion.put("Size", size);

		// Palette: 3 items (air, stone, oak_planks)
		ListTag palette = new ListTag();
		CompoundTag state0 = new CompoundTag();
		state0.putString("Name", "minecraft:air");
		palette.add(state0);

		CompoundTag state1 = new CompoundTag();
		state1.putString("Name", "minecraft:stone");
		palette.add(state1);

		CompoundTag state2 = new CompoundTag();
		state2.putString("Name", "minecraft:oak_planks");
		palette.add(state2);

		mainRegion.put("BlockStatePalette", palette);

		// 8 blocks total, palette size 3 -> 2 bits per block -> 16 bits total (fits in 1 long)
		int b = LitematicReader.getBitsPerBlock(3);
		assertEquals(2, b);
		long[] blockStates = new long[1];

		// Set blocks:
		// block 0: air (0)
		// block 1: stone (1)
		// block 2: stone (1)
		// block 3: oak_planks (2)
		// block 4: oak_planks (2)
		// block 5: oak_planks (2)
		// block 6: air (0)
		// block 7: stone (1)
		int[] blocks = {0, 1, 1, 2, 2, 2, 0, 1};
		for (int i = 0; i < blocks.length; i++) {
			LitematicReader.packBlockStateIndex(blockStates, i, b, blocks[i]);
		}
		mainRegion.putLongArray("BlockStates", blockStates);

		regions.put("Main", mainRegion);
		root.put("Regions", regions);

		Path file = tempDir.resolve("test.litematic");
		NbtIo.writeCompressed(root, file);

		// Read using LitematicReader
		LitematicData data = LitematicReader.read(file);

		assertEquals("Test Structure", data.name());
		assertEquals("Test Builder", data.author());
		assertEquals(1, data.regionCount());
		assertEquals(2, data.width());
		assertEquals(2, data.height());
		assertEquals(2, data.length());

		// Total non-air: 3 stone + 3 oak_planks = 6 blocks
		assertEquals(6, data.totalBlocks());
		assertEquals(3L, data.blockCounts().get("minecraft:stone"));
		assertEquals(3L, data.blockCounts().get("minecraft:oak_planks"));
		assertFalse(data.blockCounts().containsKey("minecraft:air"));
	}

	@Test
	public void testWriteAndReadSampleStarterHouse() throws IOException {
		CompoundTag root = new CompoundTag();

		CompoundTag metadata = new CompoundTag();
		metadata.putString("Name", "Starter House");
		metadata.putString("Author", "RafayShhh");
		CompoundTag enclosing = new CompoundTag();
		enclosing.putInt("x", 5);
		enclosing.putInt("y", 4);
		enclosing.putInt("z", 5);
		metadata.put("EnclosingSize", enclosing);
		root.put("Metadata", metadata);

		CompoundTag regions = new CompoundTag();
		CompoundTag region = new CompoundTag();

		CompoundTag pos = new CompoundTag();
		pos.putInt("x", 0);
		pos.putInt("y", 0);
		pos.putInt("z", 0);
		region.put("Position", pos);

		CompoundTag size = new CompoundTag();
		size.putInt("x", 5);
		size.putInt("y", 4);
		size.putInt("z", 5);
		region.put("Size", size);

		ListTag palette = new ListTag();
		String[] blockNames = {
			"minecraft:air",
			"minecraft:stone_bricks",
			"minecraft:oak_planks",
			"minecraft:glass",
			"minecraft:torch"
		};
		for (String bName : blockNames) {
			CompoundTag s = new CompoundTag();
			s.putString("Name", bName);
			palette.add(s);
		}
		region.put("BlockStatePalette", palette);

		int b = LitematicReader.getBitsPerBlock(blockNames.length);
		int totalBlocks = 5 * 4 * 5; // 100 blocks
		int numLongs = (int) (((long) totalBlocks * b + 63) / 64);
		long[] blockStates = new long[numLongs];

		int idx = 0;
		for (int y = 0; y < 4; y++) {
			for (int z = 0; z < 5; z++) {
				for (int x = 0; x < 5; x++) {
					int block = 0; // air
					if (y == 0) {
						block = 1; // stone_bricks floor (25 blocks)
					} else if (y == 3) {
						block = 2; // oak_planks roof (25 blocks)
					} else {
						boolean isWall = (x == 0 || x == 4 || z == 0 || z == 4);
						if (isWall) {
							if ((x == 2 && z == 0) || (x == 2 && z == 4)) {
								block = 3; // glass window (4 blocks)
							} else {
								block = 2; // oak_planks wall (28 blocks)
							}
						} else if (y == 1 && x == 2 && z == 2) {
							block = 4; // torch (1 block)
						}
					}
					LitematicReader.packBlockStateIndex(blockStates, idx++, b, block);
				}
			}
		}
		region.putLongArray("BlockStates", blockStates);
		regions.put("House", region);
		root.put("Regions", regions);

		Path runSchematics = Path.of("run/schematics");
		if (java.nio.file.Files.exists(runSchematics)) {
			Path sampleFile = runSchematics.resolve("starter_house.litematic");
			NbtIo.writeCompressed(root, sampleFile);

			LitematicData data = LitematicReader.read(sampleFile);
			assertEquals("Starter House", data.name());
			assertEquals("RafayShhh", data.author());
			assertEquals(5, data.width());
			assertEquals(4, data.height());
			assertEquals(5, data.length());
			assertEquals(83, data.totalBlocks());
			assertEquals(25L, data.blockCounts().get("minecraft:stone_bricks"));
			assertEquals(53L, data.blockCounts().get("minecraft:oak_planks"));
			assertEquals(4L, data.blockCounts().get("minecraft:glass"));
			assertEquals(1L, data.blockCounts().get("minecraft:torch"));
		}
	}
}
