package io.github.rafayshhh.shhhbuilder.client.hud;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Optional;

public class MaterialDisplayHelper {

	/**
	 * Formats a block count in a beginner-friendly way:
	 * Under 64: "53"
	 * 64 or more: "212 (3 stacks + 20)", "64 (1 stack)", "128 (2 stacks)"
	 */
	public static String formatCount(long count) {
		if (count < 64) {
			return String.valueOf(count);
		}

		long stacks = count / 64;
		long remainder = count % 64;
		String stackWord = (stacks == 1) ? "stack" : "stacks";

		if (remainder == 0) {
			return count + " (" + stacks + " " + stackWord + ")";
		} else {
			return count + " (" + stacks + " " + stackWord + " + " + remainder + ")";
		}
	}

	/**
	 * Converts a block identifier like "minecraft:oak_planks" into a human-friendly display name,
	 * such as "Oak Planks", using the client's localized block registry when available.
	 */
	public static String formatBlockName(String blockId) {
		if (blockId == null || blockId.isBlank()) {
			return "";
		}

		if (io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.isBootstrapped()) {
			try {
				Identifier id = Identifier.tryParse(blockId);
				if (id != null) {
					if (BuiltInRegistries.ITEM != null) {
						Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(id);
						if (itemOpt.isPresent() && itemOpt.get() != Items.AIR) {
							Item it = itemOpt.get();
							return it.getName(new ItemStack(it)).getString();
						}
					}
					if (BuiltInRegistries.BLOCK != null) {
						Optional<Block> opt = BuiltInRegistries.BLOCK.getOptional(id);
						if (opt.isPresent()) {
							return opt.get().getName().getString();
						}
					}
				}
			} catch (Throwable ignored) {
				// Registry lookup fallback for unit testing or non-bootstrapped environments
			}
		}

		// Fallback formatting: extract path, remove underscores, title-case words
		String path = blockId.contains(":") ? blockId.substring(blockId.indexOf(':') + 1) : blockId;
		String[] words = path.split("_");
		StringBuilder sb = new StringBuilder();
		for (String word : words) {
			if (!word.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(Character.toUpperCase(word.charAt(0)));
				if (word.length() > 1) {
					sb.append(word.substring(1).toLowerCase(Locale.ROOT));
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Retrieves an ItemStack for the given item or block ID to render its icon on the HUD.
	 */
	public static ItemStack getItemStack(String blockId) {
		if (blockId == null || blockId.isBlank()) {
			return ItemStack.EMPTY;
		}

		if (io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper.isBootstrapped()) {
			try {
				Identifier id = Identifier.tryParse(blockId);
				if (id != null) {
					if (BuiltInRegistries.ITEM != null) {
						Optional<Item> opt = BuiltInRegistries.ITEM.getOptional(id);
						if (opt.isPresent() && opt.get() != Items.AIR) {
							return new ItemStack(opt.get());
						}
					}
					if (BuiltInRegistries.BLOCK != null) {
						Optional<Block> opt = BuiltInRegistries.BLOCK.getOptional(id);
						if (opt.isPresent()) {
							Item item = opt.get().asItem();
							if (item != null && item != Items.AIR) {
								return new ItemStack(item);
							}
						}
					}
				}
			} catch (Throwable ignored) {
			}
		}

		return ItemStack.EMPTY;
	}
}
