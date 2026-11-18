package io.github.rafayshhh.shhhbuilder.client.material;

import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import io.github.rafayshhh.shhhbuilder.client.SchematicManager;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Safely scans the client player's inventory and in-world placed blocks on demand with zero network packets.
 * Optionally inspects container components (shulker boxes) to count stored items.
 * Deducts blocks already correctly placed in the world so placing blocks in Survival never increases missing count.
 */
public class PlayerInventoryScanner {

	private static long lastScanTimeMs = 0;
	private static boolean lastIncludeShulkers = true;
	private static Map<String, Integer> cachedCounts = Collections.emptyMap();
	private static final long SCAN_CACHE_DURATION_MS = 250; // Max 4 scans per second

	private static long lastPlacedScanTimeMs = 0;
	private static int lastPlacedLayerFilter = -2;
	private static Map<String, Integer> cachedPlacedCounts = Collections.emptyMap();

	public static void clearPlacedCache() {
		lastPlacedScanTimeMs = 0;
		lastPlacedLayerFilter = -2;
		cachedPlacedCounts = Collections.emptyMap();
	}

	/**
	 * Scans the active placement in mc.level, counting blocks that match the expected schematic state
	 * and converting them to carried item counts via MaterialCountingEngine.evaluate(expectedState).
	 *
	 * @param mc Minecraft client instance
	 * @param layerFilter 0-based layer index, or -1 for all layers
	 * @param forceRefresh whether to bypass the 250ms cache throttle
	 * @return map of item identifier -> count of blocks already placed in the world
	 */
	public static Map<String, Integer> scanPlacedBlocks(Minecraft mc, int layerFilter, boolean forceRefresh) {
		long now = System.currentTimeMillis();
		if (!forceRefresh && (now - lastPlacedScanTimeMs < SCAN_CACHE_DURATION_MS) && (lastPlacedLayerFilter == layerFilter)) {
			return cachedPlacedCounts;
		}

		if (mc == null || mc.level == null || !PlacementManager.hasPlacement()) {
			cachedPlacedCounts = Collections.emptyMap();
			lastPlacedScanTimeMs = now;
			lastPlacedLayerFilter = layerFilter;
			return cachedPlacedCounts;
		}

		if (PlacementManager.getDimension() != null && !mc.level.dimension().equals(PlacementManager.getDimension())) {
			cachedPlacedCounts = Collections.emptyMap();
			lastPlacedScanTimeMs = now;
			lastPlacedLayerFilter = layerFilter;
			return cachedPlacedCounts;
		}

		LitematicData data = SchematicManager.getSelectedSchematicData();
		if (data == null) {
			data = SchematicManager.getLoadedData();
		}
		if (data == null || data.blockData() == null || !data.blockData().canPreview()) {
			cachedPlacedCounts = Collections.emptyMap();
			lastPlacedScanTimeMs = now;
			lastPlacedLayerFilter = layerFilter;
			return cachedPlacedCounts;
		}

		Map<String, Integer> placedCounts = new HashMap<>();
		try {
			BlockPos origin = PlacementManager.getPosition();
			List<PlacementManager.TransformedBlock> blocks = PlacementManager.getTransformedBlocks(data, layerFilter);
			BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

			for (PlacementManager.TransformedBlock tb : blocks) {
				mPos.set(origin.getX() + tb.relX(), origin.getY() + tb.relY(), origin.getZ() + tb.relZ());
				BlockState worldState = mc.level.getBlockState(mPos);

				if (WorldBlockValidator.check(worldState, tb.state()) == WorldBlockValidator.MatchStatus.EXACT_MATCH) {
					MaterialCountingEngine.BlockEvaluation eval = MaterialCountingEngine.evaluate(tb.state());
					if (!eval.isAir() && !eval.isSkipped() && !eval.isUnobtainable()) {
						for (MaterialCountingEngine.ItemRequirement req : eval.items()) {
							placedCounts.merge(req.itemIdentifier(), req.count(), Integer::sum);
						}
					}
				}
			}
		} catch (Throwable t) {
			// Catch any unexpected exceptions to ensure client stability
		}

		cachedPlacedCounts = Collections.unmodifiableMap(placedCounts);
		lastPlacedScanTimeMs = now;
		lastPlacedLayerFilter = layerFilter;
		return cachedPlacedCounts;
	}

	public static Map<String, Integer> scanPlacedBlocks(Minecraft mc) {
		return scanPlacedBlocks(mc, -1, false);
	}

	/**
	 * Scans the player's inventory, returning a map of item identifier -> total count.
	 *
	 * @param mc Minecraft client instance
	 * @param includeShulkers whether to inspect shulker boxes in the inventory
	 * @param forceRefresh whether to bypass the 250ms throttle
	 */
	public static Map<String, Integer> scanInventory(Minecraft mc, boolean includeShulkers, boolean forceRefresh) {
		long now = System.currentTimeMillis();
		if (!forceRefresh && (now - lastScanTimeMs < SCAN_CACHE_DURATION_MS) && (lastIncludeShulkers == includeShulkers)) {
			return cachedCounts;
		}

		if (mc == null || mc.player == null) {
			return Collections.emptyMap();
		}

		Map<String, Integer> counts = new HashMap<>();

		try {
			Inventory inv = mc.player.getInventory();
			if (inv != null) {
				int size = inv.getContainerSize();
				for (int i = 0; i < size; i++) {
					ItemStack stack = inv.getItem(i);
					if (stack != null && !stack.isEmpty()) {
						countStack(stack, counts, includeShulkers);
					}
				}
			}
		} catch (Throwable t) {
			// Catch any unexpected exceptions to ensure client stability
		}

		cachedCounts = Collections.unmodifiableMap(counts);
		lastScanTimeMs = now;
		lastIncludeShulkers = includeShulkers;
		return cachedCounts;
	}

	private static void countStack(ItemStack stack, Map<String, Integer> counts, boolean includeShulkers) {
		if (stack == null || stack.isEmpty()) return;

		try {
			var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (itemId != null) {
				counts.merge(itemId.toString(), stack.getCount(), Integer::sum);
			}

			// If stack contains container contents (shulker boxes)
			if (includeShulkers) {
				ItemContainerContents container = stack.get(DataComponents.CONTAINER);
				if (container != null) {
					container.nonEmptyItemCopyStream().forEach(inner -> {
						if (inner != null && !inner.isEmpty()) {
							var innerId = BuiltInRegistries.ITEM.getKey(inner.getItem());
							if (innerId != null) {
								counts.merge(innerId.toString(), inner.getCount(), Integer::sum);
							}
						}
					});
				}
			}
		} catch (Throwable ignored) {
		}
	}
}
