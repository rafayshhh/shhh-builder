package io.github.rafayshhh.shhhbuilder.client.schematic;

import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SchemReaderTest {

	@TempDir
	Path tempDir;

	@BeforeEach
	public void setUp() {
		PlacementManager.clear();
	}

	@Test
	public void testVarIntEncodingAndDecoding() throws IOException {
		int[] original = new int[]{0, 1, 127, 128, 255, 300, 16384, 65535, 2097151, 10000000, Integer.MAX_VALUE};
		byte[] encoded = SchemReader.encodeVarIntArray(original);

		int[] decoded = new int[original.length];
		SchemReader.decodeVarIntArray(encoded, decoded);

		assertArrayEquals(original, decoded, "Decoded VarInts must exactly match original values");
	}

	@Test
	public void testVarIntDecodingUnexpectedEndThrows() {
		byte[] incomplete = new byte[]{(byte) 0x80}; // Continuation bit set, but stream ends
		int[] target = new int[1];

		assertThrows(IOException.class, () -> {
			SchemReader.decodeVarIntArray(incomplete, target);
		}, "Incomplete VarInt stream should throw IOException");
	}

	@Test
	public void testReadSpongeSchematicV2() throws IOException {
		// Create a synthetic Sponge v2 schematic: root-level compound
		CompoundTag root = new CompoundTag();
		root.putInt("Version", 2);
		root.putInt("DataVersion", 3955);
		root.putShort("Width", (short) 3);
		root.putShort("Height", (short) 2);
		root.putShort("Length", (short) 2);
		root.putIntArray("Offset", new int[]{0, 0, 0});

		CompoundTag metadata = new CompoundTag();
		metadata.putString("Name", "SpongeV2House");
		metadata.putString("Author", "TestBuilder");
		root.put("Metadata", metadata);

		// Palette mapping block state strings to integer indices
		CompoundTag palette = new CompoundTag();
		palette.putInt("minecraft:air", 0);
		palette.putInt("minecraft:stone", 1);
		palette.putInt("minecraft:oak_stairs[facing=north,half=bottom]", 2);
		root.put("Palette", palette);

		// 3 x 2 x 2 = 12 blocks total
		// Layer 0 (y=0, 6 blocks): 3 stone (index 1), 3 oak stairs (index 2)
		// Layer 1 (y=1, 6 blocks): 6 air (index 0)
		int[] blockIndices = new int[]{
			1, 1, 1, 2, 2, 2, // y=0 (z=0: x=0,1,2; z=1: x=0,1,2)
			0, 0, 0, 0, 0, 0  // y=1
		};
		byte[] blockDataBytes = SchemReader.encodeVarIntArray(blockIndices);
		root.putByteArray("BlockData", blockDataBytes);

		Path file = tempDir.resolve("test_v2.schem");
		NbtIo.writeCompressed(root, file);

		LitematicData data = SchemReader.read(file);

		assertNotNull(data);
		assertEquals("SpongeV2House", data.name());
		assertEquals("TestBuilder", data.author());
		assertEquals(3, data.width());
		assertEquals(2, data.height());
		assertEquals(2, data.length());
		assertEquals(6, data.totalBlocks(), "Total non-air blocks should be 6 (3 stone + 3 stairs)");

		assertEquals(3L, data.blockCounts().get("minecraft:stone"));
		assertEquals(3L, data.blockCounts().get("minecraft:oak_stairs"));
		assertFalse(data.blockCounts().containsKey("minecraft:air"), "Air must not be in material counts");

		assertNotNull(data.blockData());
		assertTrue(data.blockData().canPreview());
		assertEquals(6, data.blockData().visibleBlocks().length);
	}

	@Test
	public void testReadSpongeSchematicV3Nested() throws IOException {
		// Create a synthetic Sponge v3 schematic: nested under "Schematic" tag
		CompoundTag root = new CompoundTag();

		CompoundTag schem = new CompoundTag();
		schem.putInt("Version", 3);
		schem.putInt("DataVersion", 3955);
		schem.putShort("Width", (short) 2);
		schem.putShort("Height", (short) 2);
		schem.putShort("Length", (short) 2);

		CompoundTag metadata = new CompoundTag();
		metadata.putString("Name", "DiamondCubeV3");
		metadata.putString("Author", "Alex");
		schem.put("Metadata", metadata);

		// V3 uses "BlockPalette"
		CompoundTag blockPalette = new CompoundTag();
		blockPalette.putInt("minecraft:air", 0);
		blockPalette.putInt("minecraft:diamond_block", 1);
		schem.put("BlockPalette", blockPalette);

		// 2 x 2 x 2 = 8 blocks: all diamond blocks (index 1)
		int[] blockIndices = new int[]{1, 1, 1, 1, 1, 1, 1, 1};
		schem.putByteArray("BlockData", SchemReader.encodeVarIntArray(blockIndices));

		root.put("Schematic", schem);

		Path file = tempDir.resolve("test_v3.schem");
		NbtIo.writeCompressed(root, file);

		LitematicData data = SchemReader.read(file);

		assertNotNull(data);
		assertEquals("DiamondCubeV3", data.name());
		assertEquals("Alex", data.author());
		assertEquals(2, data.width());
		assertEquals(2, data.height());
		assertEquals(2, data.length());
		assertEquals(8, data.totalBlocks());
		assertEquals(8L, data.blockCounts().get("minecraft:diamond_block"));
	}

	@Test
	public void testEmptyFileThrows() throws IOException {
		Path emptyFile = tempDir.resolve("empty.schem");
		Files.createFile(emptyFile);

		IOException ex = assertThrows(IOException.class, () -> SchemReader.read(emptyFile));
		assertTrue(ex.getMessage().contains("empty"));
	}

	@Test
	public void testCorruptFileThrows() throws IOException {
		Path corrupt = tempDir.resolve("corrupt.schem");
		Files.write(corrupt, new byte[]{1, 2, 3, 4, 5, 6, 7, 8});

		IOException ex = assertThrows(IOException.class, () -> SchemReader.read(corrupt));
		assertTrue(ex.getMessage().contains("corrupt") || ex.getMessage().contains("NBT"));
	}

	@Test
	public void testMissingBlockDataThrows() throws IOException {
		CompoundTag root = new CompoundTag();
		root.putShort("Width", (short) 2);
		root.putShort("Height", (short) 2);
		root.putShort("Length", (short) 2);

		CompoundTag palette = new CompoundTag();
		palette.putInt("minecraft:stone", 0);
		root.put("Palette", palette);

		Path file = tempDir.resolve("no_data.schem");
		NbtIo.writeCompressed(root, file);

		IOException ex = assertThrows(IOException.class, () -> SchemReader.read(file));
		assertTrue(ex.getMessage().contains("missing BlockData"));
	}

	@Test
	public void testVolumeLimitEnforcementInSchem() throws IOException {
		CompoundTag root = new CompoundTag();
		// 200 x 100 x 101 = 2,020,000 blocks (> 2,000,000)
		root.putShort("Width", (short) 200);
		root.putShort("Height", (short) 100);
		root.putShort("Length", (short) 101);

		CompoundTag palette = new CompoundTag();
		palette.putInt("minecraft:air", 0);
		palette.putInt("minecraft:stone", 1);
		root.put("Palette", palette);

		// Single block index 0 repeated 2,020,000 times (all air)
		// For varint 0, each byte is 0x00, so a byte array of 2,020,000 zeros
		byte[] zeros = new byte[200 * 100 * 101];
		root.putByteArray("BlockData", zeros);

		Path file = tempDir.resolve("oversized.schem");
		NbtIo.writeCompressed(root, file);

		LitematicData data = SchemReader.read(file);
		assertNotNull(data);
		assertNotNull(data.blockData());
		assertFalse(data.blockData().canPreview(), "Volume over 2M should disable ghost preview");
		assertTrue(data.blockData().previewDisabledReason().contains("exceeds"));
	}
}
