package io.github.rafayshhh.shhhbuilder.client.schematic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * Categorizes Minecraft blocks into rendering categories for the ghost block preview pipeline.
 * Ensures every registered block is handled without crashing.
 */
public class BlockCategoryHelper {

	public enum Category {
		/** Normal blocks with baked 3D models (stairs, slabs, fences, walls, doors, plants, etc.) */
		NORMAL("Normal"),
		/** Fluids (water, lava) drawn as translucent colored boxes */
		FLUID("Fluid"),
		/** Blocks rendered by BlockEntityRenderers with no standard baked block model (chests, beds, signs, etc.) */
		BLOCK_ENTITY("Block Entity"),
		/** Technical or invisible blocks (barrier, light, structure void, jigsaw) drawn as small clear markers */
		TECHNICAL("Technical Marker"),
		/** Air variants skipped entirely */
		AIR("Air");

		private final String displayName;

		Category(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	/**
	 * Categorizes a block state by its identifier and properties.
	 */
	public static Category getCategory(BlockState state) {
		if (state == null) {
			return Category.AIR;
		}

		if (state.isAir()) {
			return Category.AIR;
		}

		String idStr = "";
		if (BlockStateHelper.isBootstrapped() && BuiltInRegistries.BLOCK != null) {
			try {
				Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
				if (id != null) {
					idStr = id.toString();
				}
			} catch (Throwable ignored) {
			}
		}

		if (idStr.isEmpty()) {
			idStr = state.getBlock().toString();
		}

		return getCategory(idStr, state);
	}

	/**
	 * Categorizes a block by identifier string and optional BlockState.
	 */
	public static Category getCategory(String idStr, BlockState state) {
		if (idStr == null || idStr.isBlank()) {
			return Category.AIR;
		}

		String lower = idStr.toLowerCase(Locale.ROOT).trim();
		String path = lower.contains(":") ? lower.substring(lower.indexOf(':') + 1) : lower;

		// 1. Air variants
		if (isAir(path)) {
			return Category.AIR;
		}

		// 2. Technical / invisible marker blocks
		if (isTechnical(path)) {
			return Category.TECHNICAL;
		}

		// 3. Fluids
		if (isFluid(path)) {
			return Category.FLUID;
		}

		// 4. Block entity blocks with no standard baked block models
		if (isBlockEntity(path)) {
			return Category.BLOCK_ENTITY;
		}

		// 5. Default standard block models (stairs, slabs, fences, walls, doors, plants, etc.)
		return Category.NORMAL;
	}

	public static boolean isAir(String path) {
		return path.equals("air") || path.equals("cave_air") || path.equals("void_air");
	}

	public static boolean isTechnical(String path) {
		return path.equals("barrier")
			|| path.equals("light")
			|| path.equals("structure_void")
			|| path.equals("structure_block")
			|| path.equals("jigsaw");
	}

	public static boolean isFluid(String path) {
		return path.equals("water")
			|| path.equals("flowing_water")
			|| path.equals("lava")
			|| path.equals("flowing_lava");
	}

	public static boolean isFluid(BlockState state) {
		if (state == null) {
			return false;
		}
		try {
			if (state.getFluidState() != null && !state.getFluidState().isEmpty()) {
				return true;
			}
		} catch (Throwable ignored) {
		}
		return getCategory(state) == Category.FLUID;
	}

	public static boolean isBlockEntity(String path) {
		return path.equals("chest")
			|| path.equals("trapped_chest")
			|| path.equals("ender_chest")
			|| path.endsWith("_bed")
			|| path.equals("bed")
			|| path.endsWith("_sign")
			|| path.endsWith("_wall_sign")
			|| path.endsWith("_hanging_sign")
			|| path.endsWith("_wall_hanging_sign")
			|| path.endsWith("_banner")
			|| path.endsWith("_wall_banner")
			|| path.endsWith("_skull")
			|| path.endsWith("_wall_skull")
			|| path.endsWith("_head")
			|| path.endsWith("_wall_head")
			|| path.endsWith("_shulker_box")
			|| path.equals("shulker_box")
			|| path.equals("bell")
			|| path.equals("enchanting_table")
			|| path.equals("conduit")
			|| path.equals("decorated_pot")
			|| path.equals("lectern")
			|| path.equals("trial_spawner")
			|| path.equals("vault")
			|| path.equals("spawner")
			|| path.equals("piston_head")
			|| path.equals("moving_piston");
	}
}
