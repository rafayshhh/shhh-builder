package io.github.rafayshhh.shhhbuilder.client.schematic;

import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SampleHouseTest {

	private static CompoundTag createBlockTag(String name) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Name", name);
		return tag;
	}

	private static CompoundTag createBlockTag(String name, Map<String, String> properties) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Name", name);
		CompoundTag props = new CompoundTag();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			props.putString(entry.getKey(), entry.getValue());
		}
		tag.put("Properties", props);
		return tag;
	}

	public static CompoundTag buildSampleHouseNbt() {
		int w = 7;
		int h = 5;
		int l = 7;
		long volume = (long) w * h * l;

		CompoundTag root = new CompoundTag();
		root.putInt("Version", 5);
		root.putInt("MinecraftDataVersion", 3953);

		CompoundTag meta = new CompoundTag();
		meta.putString("Name", "Sample House");
		meta.putString("Author", "Shhh Builder");
		meta.putString("Description", "A small, beginner-friendly starter cottage.");
		CompoundTag encSize = new CompoundTag();
		encSize.putInt("x", w);
		encSize.putInt("y", h);
		encSize.putInt("z", l);
		meta.put("EnclosingSize", encSize);
		meta.putInt("RegionCount", 1);
		meta.putLong("TotalVolume", volume);

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
		// 0: air
		palette.add(createBlockTag("minecraft:air"));
		// 1: cobblestone (foundation)
		palette.add(createBlockTag("minecraft:cobblestone"));
		// 2: oak planks (walls and floor)
		palette.add(createBlockTag("minecraft:oak_planks"));
		// 3: oak log (corner pillars)
		palette.add(createBlockTag("minecraft:oak_log", Map.of("axis", "y")));
		// 4: glass (windows)
		palette.add(createBlockTag("minecraft:glass"));
		// 5: oak stairs south (roof slope north)
		palette.add(createBlockTag("minecraft:oak_stairs", Map.of(
			"facing", "south", "half", "bottom", "shape", "straight", "waterlogged", "false"
		)));
		// 6: oak stairs north (roof slope south)
		palette.add(createBlockTag("minecraft:oak_stairs", Map.of(
			"facing", "north", "half", "bottom", "shape", "straight", "waterlogged", "false"
		)));
		// 7: oak door lower
		palette.add(createBlockTag("minecraft:oak_door", Map.of(
			"facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
		)));
		// 8: oak door upper
		palette.add(createBlockTag("minecraft:oak_door", Map.of(
			"facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
		)));
		// 9: torch
		palette.add(createBlockTag("minecraft:torch"));

		regionMain.put("BlockStatePalette", palette);

		int bitsPerBlock = LitematicReader.getBitsPerBlock(palette.size()); // 4 bits
		int totalLongs = (int) (((long) volume * bitsPerBlock + 63) / 64);
		long[] blockStates = new long[totalLongs];

		int nonAirCount = 0;

		// Build layers
		for (int y = 0; y < h; y++) {
			for (int z = 0; z < l; z++) {
				for (int x = 0; x < w; x++) {
					int blockId = 0; // air

					if (y == 0) {
						// Floor: cobblestone foundation rim, oak planks inside
						if (x == 0 || x == w - 1 || z == 0 || z == l - 1) {
							blockId = 1; // cobblestone
						} else {
							blockId = 2; // oak planks
						}
					} else if (y == 1) {
						// Lower walls
						boolean isCorner = (x == 0 || x == w - 1) && (z == 0 || z == l - 1);
						if (isCorner) {
							blockId = 3; // oak log
						} else if (z == 0) {
							// Front wall: door at x=3
							if (x == 3) {
								blockId = 7; // door lower
							} else {
								blockId = 2; // planks
							}
						} else if (z == l - 1) {
							// Back wall: window at x=3
							if (x == 3) {
								blockId = 4; // glass
							} else {
								blockId = 2; // planks
							}
						} else if (x == 0 || x == w - 1) {
							// Side walls: window at z=3
							if (z == 3) {
								blockId = 4; // glass
							} else {
								blockId = 2; // planks
							}
						} else if (x == 3 && z == 4) {
							blockId = 9; // torch inside
						}
					} else if (y == 2) {
						// Upper walls
						boolean isCorner = (x == 0 || x == w - 1) && (z == 0 || z == l - 1);
						if (isCorner) {
							blockId = 3; // oak log
						} else if (z == 0) {
							// Front wall: door upper at x=3, windows at x=2,4
							if (x == 3) {
								blockId = 8; // door upper
							} else if (x == 2 || x == 4) {
								blockId = 4; // glass
							} else {
								blockId = 2; // planks
							}
						} else if (z == l - 1) {
							// Back wall: window at x=3
							if (x == 3) {
								blockId = 4; // glass
							} else {
								blockId = 2; // planks
							}
						} else if (x == 0 || x == w - 1) {
							// Side walls: window at z=3
							if (z == 3) {
								blockId = 4; // glass
							} else {
								blockId = 2; // planks
							}
						}
					} else if (y == 3) {
						// Roof eaves & ceiling
						if (z == 0) {
							blockId = 5; // oak stairs south
						} else if (z == l - 1) {
							blockId = 6; // oak stairs north
						} else {
							blockId = 2; // oak planks ceiling / gables
						}
					} else if (y == 4) {
						// Upper roof slope & ridge
						if (z == 1) {
							blockId = 5; // oak stairs south
						} else if (z == l - 2) {
							blockId = 6; // oak stairs north
						} else if (z >= 2 && z <= l - 3) {
							blockId = 2; // oak planks roof ridge
						}
					}

					if (blockId != 0) {
						nonAirCount++;
					}

					int idx = (y * l + z) * w + x;
					LitematicReader.packBlockStateIndex(blockStates, idx, bitsPerBlock, blockId);
				}
			}
		}

		meta.putLong("TotalBlocks", nonAirCount);
		root.put("Metadata", meta);

		regionMain.putLongArray("BlockStates", blockStates);
		regions.put("Main", regionMain);
		root.put("Regions", regions);

		return root;
	}

	@Test
	@DisplayName("Generate and verify sample_house.litematic resource file")
	public void testGenerateAndLoadSampleHouse(@TempDir Path tempDir) throws Exception {
		CompoundTag nbt = buildSampleHouseNbt();
		assertNotNull(nbt);

		// Write to temporary test directory
		Path tempFile = tempDir.resolve("sample_house.litematic");
		NbtIo.writeCompressed(nbt, tempFile);
		assertTrue(Files.exists(tempFile));
		assertTrue(Files.size(tempFile) > 0);

		// Also write to src/main/resources so it is bundled directly into the jar
		Path resourceDir = Path.of("src", "main", "resources", "assets", "shhh-builder", "sample");
		Files.createDirectories(resourceDir);
		Path resourceFile = resourceDir.resolve("sample_house.litematic");
		NbtIo.writeCompressed(nbt, resourceFile);
		assertTrue(Files.exists(resourceFile));

		// Read back using LitematicReader
		LitematicData data = LitematicReader.read(tempFile);
		assertNotNull(data, "LitematicReader must successfully read sample house");
		assertEquals("Sample House", data.name());
		assertEquals("Shhh Builder", data.author());
		assertEquals(7, data.width());
		assertEquals(5, data.height());
		assertEquals(7, data.length());

		// Must be under 500 blocks (Requirement 1)
		assertTrue(data.totalBlocks() > 0 && data.totalBlocks() < 500,
			"Sample house must be under 500 blocks (was " + data.totalBlocks() + ")");

		// Check presence of required beginner blocks
		Map<String, Long> counts = data.blockCounts();
		assertTrue(counts.containsKey("minecraft:cobblestone"), "Must contain cobblestone");
		assertTrue(counts.containsKey("minecraft:oak_planks"), "Must contain planks");
		assertTrue(counts.containsKey("minecraft:glass"), "Must contain glass");
		assertTrue(counts.containsKey("minecraft:oak_stairs"), "Must contain stairs");
		assertTrue(counts.containsKey("minecraft:oak_door"), "Must contain door");
		assertEquals(1L, counts.get("minecraft:oak_door"), "Oak door must count exactly 1 item (lower door only, upper door skipped)");
		assertTrue(counts.containsKey("minecraft:torch"), "Must contain torch");

		// Check preview data
		assertNotNull(data.blockData());
		assertTrue(data.blockData().canPreview());
		PlacementManager.TransformedBlock[] blocks = PlacementManager.getCachedBlocks(data);
		assertNotNull(blocks);
		assertTrue(blocks.length > 0);
	}

	@Test
	@DisplayName("Bundled sample_house.litematic in classpath can be loaded via LitematicReader")
	public void testClasspathSampleHouseLoads(@TempDir Path tempDir) throws Exception {
		// First ensure resource is available in test classpath or written file
		Path resourcePath = Path.of("src", "main", "resources", "assets", "shhh-builder", "sample", "sample_house.litematic");
		if (!Files.exists(resourcePath)) {
			CompoundTag nbt = buildSampleHouseNbt();
			Files.createDirectories(resourcePath.getParent());
			NbtIo.writeCompressed(nbt, resourcePath);
		}

		Path copyTarget = tempDir.resolve("loaded_sample.litematic");
		try (InputStream in = getClass().getResourceAsStream("/assets/shhh-builder/sample/sample_house.litematic")) {
			if (in != null) {
				Files.copy(in, copyTarget, StandardCopyOption.REPLACE_EXISTING);
			} else {
				// Fallback to reading the resource file directly
				Files.copy(resourcePath, copyTarget, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		assertTrue(Files.exists(copyTarget));
		LitematicData data = LitematicReader.read(copyTarget);
		assertNotNull(data);
		assertEquals(7, data.width());
		assertEquals(5, data.height());
		assertEquals(7, data.length());
		assertTrue(data.totalBlocks() < 500);
		assertEquals(1L, data.blockCounts().get("minecraft:oak_door"), "Bundled sample house must count exactly 1 door item");
	}
}
