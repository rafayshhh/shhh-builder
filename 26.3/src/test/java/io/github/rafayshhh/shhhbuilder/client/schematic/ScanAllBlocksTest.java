package io.github.rafayshhh.shhhbuilder.client.schematic;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

public class ScanAllBlocksTest {

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	public void scanAllBlocks() {
		System.out.println("=== SCANNING ALL BLOCKS IN 26.3 ===");
		Set<String> integerPropertyNames = new TreeSet<>();
		Map<String, List<String>> intPropToBlocks = new TreeMap<>();
		Map<String, String> blockToDifferingItem = new TreeMap<>();
		List<String> airItemBlocks = new ArrayList<>();
		List<String> pottedBlocks = new ArrayList<>();

		for (Block block : BuiltInRegistries.BLOCK) {
			Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
			String bId = blockId.toString();

			// Check item
			Item item = block.asItem();
			Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
			String iId = itemId != null ? itemId.toString() : "null";

			if (item == Items.AIR) {
				airItemBlocks.add(bId);
			} else if (!bId.equals(iId)) {
				blockToDifferingItem.put(bId, iId);
			}

			if (block instanceof FlowerPotBlock pot) {
				pottedBlocks.add(bId + " -> content: " + BuiltInRegistries.BLOCK.getKey(pot.getPotted()));
			}

			// Check state properties
			for (Property<?> prop : block.getStateDefinition().getProperties()) {
				if (prop instanceof IntegerProperty intProp) {
					integerPropertyNames.add(prop.getName());
					intPropToBlocks.computeIfAbsent(prop.getName(), k -> new ArrayList<>()).add(bId);
				}
			}
		}

		System.out.println("\n--- ALL INTEGER PROPERTIES FOUND ---");
		for (String propName : integerPropertyNames) {
			List<String> blocks = intPropToBlocks.get(propName);
			System.out.println("Property: '" + propName + "' on " + blocks.size() + " blocks: " + blocks);
		}

		System.out.println("\n--- BLOCKS WITH DIFFERING ITEM (block.asItem() != block id) ---");
		blockToDifferingItem.forEach((b, i) -> System.out.println(b + " -> " + i));

		System.out.println("\n--- BLOCKS WHERE block.asItem() == AIR ---");
		for (String b : airItemBlocks) {
			System.out.println(b);
		}

		System.out.println("\n--- POTTED BLOCKS ---");
		for (String p : pottedBlocks) {
			System.out.println(p);
		}
	}
}
