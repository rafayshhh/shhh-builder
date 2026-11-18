package io.github.rafayshhh.shhhbuilder.client.schematic;

import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RealFilesTest {

	@BeforeAll
	public static void bootstrapMinecraft() {
		try {
			net.minecraft.SharedConstants.tryDetectVersion();
			Bootstrap.bootStrap();
		} catch (Throwable t) {
			System.out.println("Bootstrap note: " + t.getMessage());
		}
	}

	@Test
	public void testBootstrappedCobblestoneParsing() {
		if (BlockStateHelper.isBootstrapped() && BuiltInRegistries.BLOCK != null) {
			// 1. Litematica format tag: stores "Name" = "minecraft:cobblestone"
			CompoundTag litematicaTag = new CompoundTag();
			litematicaTag.putString("Name", "minecraft:cobblestone");
			BlockState parsedLite = BlockStateHelper.parseBlockState(litematicaTag);
			assertNotNull(parsedLite, "Parsed block state must not be null");
			assertEquals(Blocks.COBBLESTONE.defaultBlockState(), parsedLite,
				"Litematica tag with 'Name': 'minecraft:cobblestone' must parse as Blocks.COBBLESTONE, never stone");

			BlockStateHelper.ItemEvaluation evalLite = BlockStateHelper.evaluateItem(litematicaTag);
			assertEquals("minecraft:cobblestone", evalLite.itemIdentifier(),
				"Carried item must be 'minecraft:cobblestone', never 'minecraft:stone'");

			// 2. Sponge schematic format string: "minecraft:cobblestone"
			BlockState parsedSchem = BlockStateHelper.parseBlockStateString("minecraft:cobblestone");
			assertNotNull(parsedSchem, "Parsed Sponge state must not be null");
			assertEquals(Blocks.COBBLESTONE.defaultBlockState(), parsedSchem,
				"Sponge string 'minecraft:cobblestone' must parse as Blocks.COBBLESTONE, never stone");

			BlockStateHelper.ItemEvaluation evalSchem = BlockStateHelper.evaluateItem("minecraft:cobblestone");
			assertEquals("minecraft:cobblestone", evalSchem.itemIdentifier(),
				"Sponge carried item must be 'minecraft:cobblestone', never 'minecraft:stone'");
		}
	}

	@Test
	public void testAxeFilesIfPresent() throws Exception {
		Path userHome = Path.of(System.getProperty("user.home"));
		Path schematicsDir = userHome.resolve("AppData/Roaming/.minecraft/schematics");
		Path axeLite = schematicsDir.resolve("axe.litematic");
		Path axeSchem = schematicsDir.resolve("axe.schem");

		if (Files.exists(axeLite)) {
			LitematicData data = LitematicReader.read(axeLite);
			System.out.println("axe.litematic loaded:");
			System.out.println("  Dimensions: " + data.width() + "x" + data.height() + "x" + data.length());
			System.out.println("  Total blocks: " + data.totalBlocks());
			System.out.println("  Block counts: " + data.blockCounts());

			// axe.litematic stored size 160x160x32, 23578 cobblestone blocks
			assertEquals(23578, data.totalBlocks());
			assertTrue(data.blockCounts().containsKey("minecraft:cobblestone"),
				"Block counts must contain 'minecraft:cobblestone'");
			assertFalse(data.blockCounts().containsKey("minecraft:stone"),
				"Block counts must NOT contain 'minecraft:stone'");
			assertEquals(23578, data.blockCounts().get("minecraft:cobblestone"));

			if (data.blockData() != null && data.blockData().canPreview()) {
				// Verify palette contains cobblestone and not stone
				if (BlockStateHelper.isBootstrapped()) {
					boolean hasCobble = false;
					for (BlockState s : data.blockData().palette()) {
						if (s != null && s.getBlock() == Blocks.COBBLESTONE) {
							hasCobble = true;
						}
						assertNotEquals(Blocks.STONE.defaultBlockState(), s, "Palette must NOT contain Stone");
					}
					assertTrue(hasCobble, "Palette must contain Cobblestone");
				}

				PlacementManager.TransformedBlock[] blocks = PlacementManager.getCachedBlocks(data);
				System.out.println("  Visible blocks after culling: " + (blocks != null ? blocks.length : 0));
				assertNotNull(blocks);
				assertEquals(23576, blocks.length, "axe.litematic must have exactly 23,576 visible blocks after 6-neighbor culling");
				if (BlockStateHelper.isBootstrapped() && blocks.length > 0) {
					assertEquals(Blocks.COBBLESTONE, blocks[0].state().getBlock(),
						"TransformedBlock state must be Cobblestone, never Stone");
				}
			}
		} else {
			System.out.println("axe.litematic not found at " + axeLite + " (skipping real file check)");
		}

		if (Files.exists(axeSchem)) {
			LitematicData data = SchemReader.read(axeSchem);
			System.out.println("axe.schem loaded:");
			System.out.println("  Dimensions: " + data.width() + "x" + data.height() + "x" + data.length());
			System.out.println("  Total blocks: " + data.totalBlocks());
			System.out.println("  Block counts: " + data.blockCounts());

			// axe.schem: 153x160x29, 23578 cobblestone blocks
			assertEquals(153, data.width());
			assertEquals(160, data.height());
			assertEquals(29, data.length());
			assertEquals(23578, data.totalBlocks());
			assertTrue(data.blockCounts().containsKey("minecraft:cobblestone"),
				"axe.schem must contain 'minecraft:cobblestone'");
			assertFalse(data.blockCounts().containsKey("minecraft:stone"),
				"axe.schem must NOT contain 'minecraft:stone'");
			assertEquals(23578, data.blockCounts().get("minecraft:cobblestone"));

			if (data.blockData() != null && data.blockData().canPreview()) {
				if (BlockStateHelper.isBootstrapped()) {
					boolean hasCobble = false;
					for (BlockState s : data.blockData().palette()) {
						if (s != null && s.getBlock() == Blocks.COBBLESTONE) {
							hasCobble = true;
						}
						assertNotEquals(Blocks.STONE.defaultBlockState(), s, "axe.schem palette must NOT contain Stone");
					}
					assertTrue(hasCobble, "axe.schem palette must contain Cobblestone");
				}

				PlacementManager.TransformedBlock[] blocks = PlacementManager.getCachedBlocks(data);
				System.out.println("  axe.schem visible blocks after culling: " + (blocks != null ? blocks.length : 0));
				assertNotNull(blocks);
				assertEquals(23576, blocks.length, "axe.schem must have exactly 23,576 visible blocks after 6-neighbor culling");
				if (BlockStateHelper.isBootstrapped() && blocks.length > 0) {
					assertEquals(Blocks.COBBLESTONE, blocks[0].state().getBlock(),
						"TransformedBlock state for axe.schem must be Cobblestone, never Stone");
				}
			}
		} else {
			System.out.println("axe.schem not found at " + axeSchem + " (skipping real file check)");
		}
	}
}
