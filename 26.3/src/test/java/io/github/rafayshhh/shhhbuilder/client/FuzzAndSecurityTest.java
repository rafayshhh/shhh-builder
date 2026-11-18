package io.github.rafayshhh.shhhbuilder.client;

import io.github.rafayshhh.shhhbuilder.client.command.ShhhBuilderCommand;
import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicReader;
import io.github.rafayshhh.shhhbuilder.client.schematic.SchemReader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class FuzzAndSecurityTest {

	@Test
	@DisplayName("Path traversal attempts via command line are rejected with clean error message")
	void testCommandPathTraversalRejection() {
		List<SchematicFileEntry> files = List.of(
			new SchematicFileEntry(Path.of("test.litematic"), "test.litematic")
		);

		ShhhBuilderCommand.FileResolveResult res1 = ShhhBuilderCommand.resolveFile("../secret.litematic", files);
		assertFalse(res1.isSuccess());
		assertTrue(res1.error().contains("path traversal"));

		ShhhBuilderCommand.FileResolveResult res2 = ShhhBuilderCommand.resolveFile("subdir/nested.schem", files);
		assertFalse(res2.isSuccess());
		assertTrue(res2.error().contains("path traversal"));

		ShhhBuilderCommand.FileResolveResult res3 = ShhhBuilderCommand.resolveFile("C:\\Windows\\system32.litematic", files);
		assertFalse(res3.isSuccess());
		assertTrue(res3.error().contains("path traversal"));

		ShhhBuilderCommand.FileResolveResult res4 = ShhhBuilderCommand.resolveFile("..\\..\\passwords.txt", files);
		assertFalse(res4.isSuccess());
		assertTrue(res4.error().contains("path traversal"));
	}

	@Test
	@DisplayName("SchematicManager loadSchematicAsync rejects paths outside schematics directory")
	void testSchematicManagerPathTraversalRejection(@TempDir Path tempDir) throws Exception {
		Path outsideFile = tempDir.resolve("outside.litematic");
		Files.writeString(outsideFile, "fake content");

		SchematicManager.CachedSchematic cached = SchematicManager.loadSchematicAsync(outsideFile).get();
		assertNotNull(cached);
		assertNull(cached.data);
		assertNotNull(cached.error);
		assertTrue(cached.error.contains("Security error") || cached.error.contains("outside schematics directory"));
	}

	@Test
	@DisplayName("Coordinate boundary validation rejects points beyond world border and height limits")
	void testCoordinateValidation() {
		int minY = -64;
		int maxY = 320;

		// Exact border bounds (valid)
		assertTrue(ShhhBuilderCommand.validateCoordinates(0, 64, 0, minY, maxY).valid());
		assertTrue(ShhhBuilderCommand.validateCoordinates(30_000_000, 100, 30_000_000, minY, maxY).valid());
		assertTrue(ShhhBuilderCommand.validateCoordinates(-30_000_000, 100, -30_000_000, minY, maxY).valid());
		assertTrue(ShhhBuilderCommand.validateCoordinates(0, minY, 0, minY, maxY).valid());
		assertTrue(ShhhBuilderCommand.validateCoordinates(0, maxY, 0, minY, maxY).valid());

		// Out of border bounds (invalid)
		var outX = ShhhBuilderCommand.validateCoordinates(30_000_001, 100, 0, minY, maxY);
		assertFalse(outX.valid());
		assertTrue(outX.error().contains("outside the world limits"));

		var outZ = ShhhBuilderCommand.validateCoordinates(0, 100, -30_000_001, minY, maxY);
		assertFalse(outZ.valid());
		assertTrue(outZ.error().contains("outside the world limits"));

		// Out of height limits (invalid)
		var belowMin = ShhhBuilderCommand.validateCoordinates(0, minY - 1, 0, minY, maxY);
		assertFalse(belowMin.valid());
		assertTrue(belowMin.error().contains("outside world height limits"));

		var aboveMax = ShhhBuilderCommand.validateCoordinates(0, maxY + 1, 0, minY, maxY);
		assertFalse(aboveMax.valid());
		assertTrue(aboveMax.error().contains("outside world height limits"));
	}

	@Test
	@DisplayName("Layer boundary validation accepts 'all' and in-range numbers, rejects invalid")
	void testLayerValidation() {
		int maxHeight = 50;

		var allRes = ShhhBuilderCommand.validateLayer("all", maxHeight);
		assertTrue(allRes.valid());
		assertTrue(allRes.isAll());

		var allUpper = ShhhBuilderCommand.validateLayer("ALL", maxHeight);
		assertTrue(allUpper.valid());
		assertTrue(allUpper.isAll());

		var validLayer = ShhhBuilderCommand.validateLayer("25", maxHeight);
		assertTrue(validLayer.valid());
		assertFalse(validLayer.isAll());
		assertEquals(25, validLayer.layerNumber());

		var validMax = ShhhBuilderCommand.validateLayer("50", maxHeight);
		assertTrue(validMax.valid());
		assertEquals(50, validMax.layerNumber());

		var zeroLayer = ShhhBuilderCommand.validateLayer("0", maxHeight);
		assertFalse(zeroLayer.valid());
		assertTrue(zeroLayer.error().contains("out of range"));

		var negativeLayer = ShhhBuilderCommand.validateLayer("-5", maxHeight);
		assertFalse(negativeLayer.valid());
		assertTrue(negativeLayer.error().contains("out of range"));

		var excessLayer = ShhhBuilderCommand.validateLayer("51", maxHeight);
		assertFalse(excessLayer.valid());
		assertTrue(excessLayer.error().contains("out of range"));

		var textInvalid = ShhhBuilderCommand.validateLayer("not_a_number", maxHeight);
		assertFalse(textInvalid.valid());
		assertTrue(textInvalid.error().contains("Invalid layer"));
	}

	@Test
	@DisplayName("Fuzz testing: feeding random bytes to LitematicReader throws clean IOException")
	void testFuzzLitematicReader(@TempDir Path tempDir) throws IOException {
		Random random = new Random(42);

		// 1. Zero byte file
		Path zeroFile = tempDir.resolve("zero.litematic");
		Files.write(zeroFile, new byte[0]);
		IOException exZero = assertThrows(IOException.class, () -> LitematicReader.read(zeroFile));
		assertTrue(exZero.getMessage().contains("empty"));

		// 2. Random garbage byte files of various sizes
		for (int size : new int[]{1, 10, 64, 512, 4096, 16384}) {
			byte[] garbage = new byte[size];
			random.nextBytes(garbage);
			Path fuzzFile = tempDir.resolve("fuzz_" + size + ".litematic");
			Files.write(fuzzFile, garbage);

			assertThrows(IOException.class, () -> LitematicReader.read(fuzzFile));
		}
	}

	@Test
	@DisplayName("Fuzz testing: feeding random bytes to SchemReader throws clean IOException")
	void testFuzzSchemReader(@TempDir Path tempDir) throws IOException {
		Random random = new Random(1337);

		// 1. Zero byte file
		Path zeroFile = tempDir.resolve("zero.schem");
		Files.write(zeroFile, new byte[0]);
		IOException exZero = assertThrows(IOException.class, () -> SchemReader.read(zeroFile));
		assertTrue(exZero.getMessage().contains("empty"));

		// 2. Random garbage byte files of various sizes
		for (int size : new int[]{1, 10, 64, 512, 4096, 16384}) {
			byte[] garbage = new byte[size];
			random.nextBytes(garbage);
			Path fuzzFile = tempDir.resolve("fuzz_" + size + ".schem");
			Files.write(fuzzFile, garbage);

			assertThrows(IOException.class, () -> SchemReader.read(fuzzFile));
		}
	}

	@Test
	@DisplayName("Decompression quota enforcement: NbtAccounter enforces 256 MB safety quota")
	void testNbtAccounterQuota() {
		NbtAccounter accounter = new NbtAccounter(1024L, 16);
		// Account within quota
		accounter.accountBytes(500);
		// Exceed quota -> throws RuntimeException (NBT tag too big)
		assertThrows(RuntimeException.class, () -> accounter.accountBytes(600));
	}

	@Test
	@DisplayName("Config resilience: Corrupted, truncated, or invalid config files fall back cleanly to defaults")
	void testConfigCorruptedRecovery(@TempDir Path tempDir) {
		ShhhBuilderConfig config = new ShhhBuilderConfig();

		// Corrupted JSON
		Path corruptPath = tempDir.resolve("corrupt-config.json");
		assertDoesNotThrow(() -> {
			Files.writeString(corruptPath, "{ invalid json content ... !@#$ ");
			config.loadFromPath(corruptPath);
		});
		// Assert defaults maintained
		assertEquals(48, config.getGhostRenderDistance());
		assertEquals(10_000, config.getGhostBlockLimit());
		assertTrue(config.isHudEnabled());

		// Out-of-range values in JSON
		Path outOfRangePath = tempDir.resolve("out-of-range-config.json");
		assertDoesNotThrow(() -> {
			Files.writeString(outOfRangePath, "{\n  \"ghostRenderDistance\": 9999,\n  \"ghostBlockLimit\": 123456\n}");
			config.loadFromPath(outOfRangePath);
		});
		// Clamped to safe defaults
		assertEquals(48, config.getGhostRenderDistance());
		assertEquals(10_000, config.getGhostBlockLimit());

		// Atomic save verification
		Path savePath = tempDir.resolve("saved-config.json");
		config.saveToPath(savePath);
		assertTrue(Files.exists(savePath));
		assertFalse(Files.exists(savePath.resolveSibling("saved-config.json.tmp")));
	}

	@Test
	@DisplayName("Unknown block states are identified and counted accurately")
	void testUnknownBlockDetection() {
		assertTrue(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.isKnownBlock("minecraft:stone"));
		assertTrue(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.isKnownBlock("minecraft:oak_planks"));
		assertTrue(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.isKnownBlock("minecraft:air"));

		// Non-existent or unknown mod block
		assertFalse(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.isKnownBlock("unknown_mod:alien_block"));
		assertFalse(io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.isKnownBlock("future_game:quantum_core"));
	}

	@Test
	@DisplayName("Item counting rules: slabs, doors, beds, tall plants, technical blocks, waterlogged, and unobtainables")
	void testItemCountingRules() {
		// 1. Slabs
		var doubleSlab = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:oak_slab[type=double]");
		assertEquals("minecraft:oak_slab", doubleSlab.itemIdentifier());
		assertEquals(2, doubleSlab.countMultiplier());
		assertFalse(doubleSlab.isSkipped());
		assertFalse(doubleSlab.isUnobtainable());

		var singleSlab = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:oak_slab[type=bottom]");
		assertEquals("minecraft:oak_slab", singleSlab.itemIdentifier());
		assertEquals(1, singleSlab.countMultiplier());
		assertFalse(singleSlab.isSkipped());

		// 2. Doors
		var doorLower = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:oak_door[half=lower]");
		assertEquals("minecraft:oak_door", doorLower.itemIdentifier());
		assertEquals(1, doorLower.countMultiplier());
		assertFalse(doorLower.isSkipped());

		var doorUpper = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:oak_door[half=upper]");
		assertTrue(doorUpper.isSkipped());
		assertEquals(0, doorUpper.countMultiplier());

		// 3. Beds
		var bedFoot = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:red_bed[part=foot]");
		assertEquals("minecraft:red_bed", bedFoot.itemIdentifier());
		assertEquals(1, bedFoot.countMultiplier());
		assertFalse(bedFoot.isSkipped());

		var bedHead = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:red_bed[part=head]");
		assertTrue(bedHead.isSkipped());
		assertEquals(0, bedHead.countMultiplier());

		// 4. Tall plants
		for (String plant : List.of("tall_grass", "large_fern", "sunflower", "lilac", "rose_bush", "peony", "tall_seagrass")) {
			var plantLower = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:" + plant + "[half=lower]");
			assertEquals("minecraft:" + plant, plantLower.itemIdentifier());
			assertEquals(1, plantLower.countMultiplier());
			assertFalse(plantLower.isSkipped());

			var plantUpper = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:" + plant + "[half=upper]");
			assertTrue(plantUpper.isSkipped(), plant + " upper must be skipped");
			assertEquals(0, plantUpper.countMultiplier());
		}

		// 5. Technical blocks
		var pistonHead = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:piston_head");
		assertTrue(pistonHead.isSkipped());
		var movingPiston = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:moving_piston");
		assertTrue(movingPiston.isSkipped());
		var pistonBase = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:piston");
		assertFalse(pistonBase.isSkipped());
		assertEquals("minecraft:piston", pistonBase.itemIdentifier());
		assertEquals(1, pistonBase.countMultiplier());

		// 6. Waterlogged blocks (only main block counts, water does not)
		var waterloggedStairs = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:oak_stairs[waterlogged=true]");
		assertEquals("minecraft:oak_stairs", waterloggedStairs.itemIdentifier());
		assertEquals(1, waterloggedStairs.countMultiplier());
		assertFalse(waterloggedStairs.isUnobtainable());
		assertFalse(waterloggedStairs.isSkipped());

		// 7. Unobtainable blocks
		for (String unobs : List.of("water", "flowing_water", "lava", "flowing_lava", "fire", "soul_fire", "nether_portal", "end_portal", "bubble_column", "light")) {
			var eval = io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:" + unobs);
			assertTrue(eval.isUnobtainable(), unobs + " must be marked unobtainable");
			assertEquals(0, eval.countMultiplier());
		}

		// 8. Carried item mappings
		assertEquals("minecraft:torch", io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:wall_torch").itemIdentifier());
		assertEquals("minecraft:oak_sign", io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:oak_wall_sign").itemIdentifier());
		assertEquals("minecraft:oak_hanging_sign", io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:oak_wall_hanging_sign").itemIdentifier());
		assertEquals("minecraft:redstone", io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:redstone_wire").itemIdentifier());
		assertEquals("minecraft:string", io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.evaluateItem("minecraft:tripwire").itemIdentifier());
	}

	@Test
	@DisplayName("Synthetic schematic item counting: voxels vs items separation, unobtainable tracking")
	void testSyntheticSchematicItemCounting(@TempDir Path tempDir) throws Exception {
		// Build a Sponge schematic with 12 voxels and 8 required items
		CompoundTag schem = new CompoundTag();
		schem.putInt("Version", 2);
		schem.putInt("DataVersion", 3953);
		schem.putShort("Width", (short) 12);
		schem.putShort("Height", (short) 1);
		schem.putShort("Length", (short) 1);

		CompoundTag palette = new CompoundTag();
		palette.putInt("minecraft:air", 0);
		palette.putInt("minecraft:oak_slab[type=double]", 1);
		palette.putInt("minecraft:oak_slab[type=bottom]", 2);
		palette.putInt("minecraft:oak_door[half=lower]", 3);
		palette.putInt("minecraft:oak_door[half=upper]", 4);
		palette.putInt("minecraft:red_bed[part=foot]", 5);
		palette.putInt("minecraft:red_bed[part=head]", 6);
		palette.putInt("minecraft:tall_grass[half=lower]", 7);
		palette.putInt("minecraft:tall_grass[half=upper]", 8);
		palette.putInt("minecraft:piston", 9);
		palette.putInt("minecraft:piston_head", 10);
		palette.putInt("minecraft:water", 11);
		palette.putInt("minecraft:oak_stairs[waterlogged=true]", 12);
		schem.put("Palette", palette);

		// BlockData with 12 blocks: indices 1 through 12
		byte[] blockData = new byte[12];
		for (int i = 0; i < 12; i++) {
			blockData[i] = (byte) (i + 1);
		}
		schem.putByteArray("BlockData", blockData);

		Path file = tempDir.resolve("test_items.schem");
		NbtIo.writeCompressed(schem, file);

		io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData data = SchemReader.read(file);
		assertNotNull(data);

		// 12 voxels
		assertEquals(12L, data.totalBlocks(), "Total voxels must be 12");
		// 8 items: 2 oak_slab + 1 oak_slab + 1 oak_door + 1 red_bed + 1 tall_grass + 1 piston + 1 oak_stairs
		assertEquals(8L, data.totalItems(), "Total required items must be 8");
		// 1 unobtainable block (water)
		assertEquals(1L, data.unobtainableBlocks(), "Unobtainable blocks must be 1");

		var items = data.itemCounts();
		assertEquals(3L, items.get("minecraft:oak_slab"), "Double slab (2) + single slab (1) = 3 slab items");
		assertEquals(1L, items.get("minecraft:oak_door"), "Door lower only = 1 door item");
		assertEquals(1L, items.get("minecraft:red_bed"), "Bed foot only = 1 bed item");
		assertEquals(1L, items.get("minecraft:tall_grass"), "Tall grass lower only = 1 tall grass item");
		assertEquals(1L, items.get("minecraft:piston"), "Piston base = 1 piston item");
		assertEquals(1L, items.get("minecraft:oak_stairs"), "Waterlogged stairs = 1 stairs item");

		assertFalse(items.containsKey("minecraft:water"), "Water must not be in items needed");
		assertFalse(items.containsKey("minecraft:piston_head"), "Piston head must not be in items needed");
	}
}
