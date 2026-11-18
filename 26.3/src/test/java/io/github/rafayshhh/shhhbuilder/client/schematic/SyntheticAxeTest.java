package io.github.rafayshhh.shhhbuilder.client.schematic;

import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class SyntheticAxeTest {

	@Test
	public void testSyntheticAxeLitematicStructure(@TempDir Path tempDir) throws Exception {
		// Create a synthetic .litematic matching axe.litematic:
		// Stored size 160x160x32, 23,578 cobblestone blocks
		int w = 160;
		int h = 160;
		int l = 32;
		long volume = (long) w * h * l;

		CompoundTag root = new CompoundTag();
		root.putInt("Version", 5);
		root.putInt("MinecraftDataVersion", 4444);

		CompoundTag meta = new CompoundTag();
		meta.putString("Name", "synthetic_axe");
		meta.putString("Author", "TestBuilder");
		meta.putLong("TotalBlocks", 23578);
		CompoundTag encSize = new CompoundTag();
		encSize.putInt("x", w);
		encSize.putInt("y", h);
		encSize.putInt("z", l);
		meta.put("EnclosingSize", encSize);
		root.put("Metadata", meta);

		CompoundTag regions = new CompoundTag();
		CompoundTag regionMain = new CompoundTag();
		CompoundTag pos = new CompoundTag();
		pos.putInt("x", 0);
		pos.putInt("y", 0);
		pos.putInt("z", 0);
		regionMain.put("Position", pos);

		CompoundTag size = new CompoundTag();
		size.putInt("x", w);
		size.putInt("y", h);
		size.putInt("z", l);
		regionMain.put("Size", size);

		ListTag palette = new ListTag();
		CompoundTag air = new CompoundTag();
		air.putString("Name", "minecraft:air");
		palette.add(air);

		CompoundTag cobble = new CompoundTag();
		cobble.putString("Name", "minecraft:cobblestone");
		palette.add(cobble);
		regionMain.put("BlockStatePalette", palette);

		int bitsPerBlock = LitematicReader.getBitsPerBlock(2); // 2 bits
		int totalLongs = (int) (((long) volume * bitsPerBlock + 63) / 64);
		long[] blockStates = new long[totalLongs];

		// Place exactly 23,578 blocks of cobblestone (palette index 1)
		// To match 23,576 visible blocks after culling, make 23,576 on surface and 2 completely enclosed
		// Two separate 3x3x3 cubes (27 blocks each) have 26 on surface and 1 enclosed each = 52 surface + 2 enclosed = 54 blocks total.
		// The remaining 23,578 - 54 = 23,524 blocks are placed on spaced y levels (y >= 10, step 2) with air above/below.
		int placed = 0;
		for (int cy = 1; cy <= 3; cy++) {
			for (int cz = 1; cz <= 3; cz++) {
				for (int cx = 1; cx <= 3; cx++) {
					int idx = (cy * l + cz) * w + cx;
					LitematicReader.packBlockStateIndex(blockStates, idx, bitsPerBlock, 1);
					placed++;
				}
			}
		}
		for (int cy = 1; cy <= 3; cy++) {
			for (int cz = 1; cz <= 3; cz++) {
				for (int cx = 5; cx <= 7; cx++) {
					int idx = (cy * l + cz) * w + cx;
					LitematicReader.packBlockStateIndex(blockStates, idx, bitsPerBlock, 1);
					placed++;
				}
			}
		}
		int needed = 23578 - placed;
		outer:
		for (int cy = 10; cy < h; cy += 2) {
			for (int cz = 0; cz < l; cz++) {
				for (int cx = 0; cx < w; cx++) {
					int idx = (cy * l + cz) * w + cx;
					LitematicReader.packBlockStateIndex(blockStates, idx, bitsPerBlock, 1);
					placed++;
					if (--needed == 0) break outer;
				}
			}
		}
		assertEquals(23578, placed);

		regionMain.putLongArray("BlockStates", blockStates);
		regions.put("Main", regionMain);
		root.put("Regions", regions);

		Path file = tempDir.resolve("synthetic_axe.litematic");
		NbtIo.writeCompressed(root, file);

		LitematicData data = LitematicReader.read(file);
		assertNotNull(data);
		assertEquals("synthetic_axe", data.name());
		assertEquals(w, data.width());
		assertEquals(h, data.height());
		assertEquals(l, data.length());
		assertEquals(23578, data.totalBlocks());
		assertEquals(23578L, data.blockCounts().get("minecraft:cobblestone"));
		assertFalse(data.blockCounts().containsKey("minecraft:stone"), "Must NOT contain stone");

		assertNotNull(data.blockData());
		assertTrue(data.blockData().canPreview());
		// Verify visible blocks are extracted
		PlacementManager.TransformedBlock[] blocks = PlacementManager.getCachedBlocks(data);
		assertNotNull(blocks);
		assertEquals(23576, blocks.length, "Should have exactly 23,576 visible blocks: " + blocks.length);
		if (BlockStateHelper.isBootstrapped() && blocks.length > 0) {
			assertEquals(net.minecraft.world.level.block.Blocks.COBBLESTONE, blocks[0].state().getBlock(),
				"Synthetic axe block state must be Cobblestone, never Stone");
		}

		// Verify section bucketing
		PlacementManager.BlockSection[] sections = PlacementManager.getCachedSections(data);
		assertNotNull(sections);
		assertTrue(sections.length > 0);
	}

	@Test
	public void testSyntheticAxeSpongeV2Structure(@TempDir Path tempDir) throws Exception {
		// Create a synthetic Sponge v2 schematic: 153x160x29, 23,578 cobblestone blocks
		int w = 153;
		int h = 160;
		int l = 29;
		int volume = w * h * l;

		CompoundTag root = new CompoundTag();
		CompoundTag schem = new CompoundTag();
		schem.putInt("Version", 2);
		schem.putInt("DataVersion", 4444);
		schem.putShort("Width", (short) w);
		schem.putShort("Height", (short) h);
		schem.putShort("Length", (short) l);

		CompoundTag meta = new CompoundTag();
		meta.putString("Name", "synthetic_sponge_axe");
		schem.put("Metadata", meta);

		CompoundTag palette = new CompoundTag();
		palette.putInt("minecraft:air", 0);
		palette.putInt("minecraft:cobblestone", 1);
		schem.put("Palette", palette);

		byte[] blockData = new byte[volume]; // initialized to 0 (air)
		int placedSponge = 0;
		for (int cy = 1; cy <= 3; cy++) {
			for (int cz = 1; cz <= 3; cz++) {
				for (int cx = 1; cx <= 3; cx++) {
					int idx = (cy * l + cz) * w + cx;
					blockData[idx] = 1;
					placedSponge++;
				}
			}
		}
		for (int cy = 1; cy <= 3; cy++) {
			for (int cz = 1; cz <= 3; cz++) {
				for (int cx = 5; cx <= 7; cx++) {
					int idx = (cy * l + cz) * w + cx;
					blockData[idx] = 1;
					placedSponge++;
				}
			}
		}
		int neededSponge = 23578 - placedSponge;
		outerSponge:
		for (int cy = 10; cy < h; cy += 2) {
			for (int cz = 0; cz < l; cz++) {
				for (int cx = 0; cx < w; cx++) {
					int idx = (cy * l + cz) * w + cx;
					blockData[idx] = 1;
					placedSponge++;
					if (--neededSponge == 0) break outerSponge;
				}
			}
		}
		assertEquals(23578, placedSponge);
		schem.putByteArray("BlockData", blockData);
		root.put("Schematic", schem);

		Path file = tempDir.resolve("synthetic_axe.schem");
		NbtIo.writeCompressed(root, file);

		LitematicData data = SchemReader.read(file);
		assertNotNull(data);
		assertEquals(w, data.width());
		assertEquals(h, data.height());
		assertEquals(l, data.length());
		assertEquals(23578, data.totalBlocks());
		assertEquals(23578L, data.blockCounts().get("minecraft:cobblestone"));
		assertFalse(data.blockCounts().containsKey("minecraft:stone"), "Must NOT contain stone");
		assertTrue(data.blockData().canPreview());

		PlacementManager.TransformedBlock[] blocks = PlacementManager.getCachedBlocks(data);
		assertNotNull(blocks);
		assertEquals(23576, blocks.length, "Should have exactly 23,576 visible blocks: " + blocks.length);
		if (BlockStateHelper.isBootstrapped() && blocks.length > 0) {
			assertEquals(net.minecraft.world.level.block.Blocks.COBBLESTONE, blocks[0].state().getBlock(),
				"Synthetic axe.schem block state must be Cobblestone, never Stone");
		}
	}
}
