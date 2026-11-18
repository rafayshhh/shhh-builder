package io.github.rafayshhh.shhhbuilder.client.schematic;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Validates in-world placed block states against schematic expected block states.
 * Used for live in-world verification and HUD crosshair inspection.
 */
public class WorldBlockValidator {

	public static final int COLOR_WRONG_STATE = 0xFFFFAA00; // Bright Amber/Orange
	public static final int COLOR_WRONG_BLOCK = 0xFFFF3333; // Bright Red
	public static final int COLOR_HUD_WRONG = 0xFFFF5555;   // HUD warning text

	public enum MatchStatus {
		/** The in-world block matches the schematic block state identically (already built). */
		EXACT_MATCH,
		/** The in-world block has the same block type but differing state/orientation (e.g. wrong facing). */
		WRONG_STATE,
		/** The in-world block is a completely different solid block. */
		WRONG_BLOCK,
		/** The in-world position is air, empty, or fluid (preview block needed). */
		MISSING
	}

	/**
	 * Evaluates the match status between the current world block state and the expected schematic block state.
	 *
	 * @param worldState the physical block in the level
	 * @param expectedState the block state required by the schematic
	 * @return the verification match status
	 */
	public static MatchStatus check(BlockState worldState, BlockState expectedState) {
		if (expectedState == null || expectedState.isAir()) {
			return MatchStatus.EXACT_MATCH;
		}

		// Exact state match: identical block and properties
		if (worldState == expectedState) {
			return MatchStatus.EXACT_MATCH;
		}

		// Same block type placed, but wrong orientation or properties
		if (worldState != null && !worldState.isAir() && worldState.getBlock() == expectedState.getBlock()) {
			return MatchStatus.WRONG_STATE;
		}

		// Air or fluid in world: player hasn't built it yet
		if (worldState == null || worldState.isAir() || BlockCategoryHelper.isFluid(worldState)) {
			return MatchStatus.MISSING;
		}

		// Different solid block placed
		return MatchStatus.WRONG_BLOCK;
	}

	/**
	 * Formats friendly name and key orientation properties of an expected block state.
	 * E.g., "Oak Stairs [north, bottom]".
	 */
	public static String formatExpectedDetails(BlockState expectedState) {
		if (expectedState == null) {
			return "";
		}
		String blockName = expectedState.getBlock().getName().getString();
		List<String> propValues = new ArrayList<>();
		try {
			expectedState.getValues().forEach(val -> {
				String pName = val.property().getName().toLowerCase(Locale.ROOT);
				if (pName.equals("facing") || pName.equals("half") || pName.equals("axis")
					|| pName.equals("shape") || pName.equals("type") || pName.equals("part")) {
					propValues.add(val.value().toString().toLowerCase(Locale.ROOT));
				}
			});
		} catch (Throwable ignored) {
		}

		if (!propValues.isEmpty()) {
			return blockName + " [" + String.join(", ", propValues) + "]";
		}
		return blockName;
	}
}
