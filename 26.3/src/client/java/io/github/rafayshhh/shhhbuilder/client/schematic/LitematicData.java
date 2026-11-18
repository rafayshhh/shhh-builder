package io.github.rafayshhh.shhhbuilder.client.schematic;

import io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Holds metadata, block statistics, required item breakdown, and block structure data parsed from a schematic file.
 */
public record LitematicData(
	String name,
	String author,
	int regionCount,
	int width,
	int height,
	int length,
	long totalBlocks,
	long totalItems,
	long unobtainableBlocks,
	long unknownBlocks,
	Map<String, Long> itemCounts,
	List<Map.Entry<String, Long>> topItems,
	SchematicBlockData blockData,
	MaterialCountingEngine.MaterialCountResult materialResult
) {
	public LitematicData {
		itemCounts = Collections.unmodifiableMap(itemCounts != null ? itemCounts : Collections.emptyMap());
		topItems = Collections.unmodifiableList(topItems != null ? topItems : Collections.emptyList());
	}

	public LitematicData(
		String name,
		String author,
		int regionCount,
		int width,
		int height,
		int length,
		long totalBlocks,
		long totalItems,
		long unobtainableBlocks,
		long unknownBlocks,
		Map<String, Long> itemCounts,
		List<Map.Entry<String, Long>> topItems,
		SchematicBlockData blockData
	) {
		this(name, author, regionCount, width, height, length, totalBlocks, totalItems, unobtainableBlocks, unknownBlocks, itemCounts, topItems, blockData, null);
	}

	/**
	 * Backwards-compatible alias for itemCounts (items needed to place).
	 */
	public Map<String, Long> blockCounts() {
		return itemCounts;
	}

	/**
	 * Backwards-compatible alias for topItems (items needed sorted descending).
	 */
	public List<Map.Entry<String, Long>> topBlocks() {
		return topItems;
	}

	/**
	 * Retrieves material counts for a given layer (or overall if layer < 0).
	 */
	public Map<String, Long> getMaterialCountsForLayer(int layer) {
		if (materialResult != null) {
			return materialResult.getCountsForLayer(layer);
		}
		return MaterialCountingEngine.getMaterialCounts(this, layer);
	}

	public LitematicData(
		String name,
		String author,
		int regionCount,
		int width,
		int height,
		int length,
		long totalBlocks,
		long unknownBlocks,
		Map<String, Long> blockCounts,
		List<Map.Entry<String, Long>> topBlocks,
		SchematicBlockData blockData
	) {
		this(name, author, regionCount, width, height, length, totalBlocks, totalBlocks, 0L, unknownBlocks, blockCounts, topBlocks, blockData);
	}

	public LitematicData(
		String name,
		String author,
		int regionCount,
		int width,
		int height,
		int length,
		long totalBlocks,
		Map<String, Long> blockCounts,
		List<Map.Entry<String, Long>> topBlocks,
		SchematicBlockData blockData
	) {
		this(name, author, regionCount, width, height, length, totalBlocks, totalBlocks, 0L, 0L, blockCounts, topBlocks, blockData);
	}

	public LitematicData(
		String name,
		String author,
		int regionCount,
		int width,
		int height,
		int length,
		long totalBlocks,
		Map<String, Long> blockCounts,
		List<Map.Entry<String, Long>> topBlocks
	) {
		this(name, author, regionCount, width, height, length, totalBlocks, totalBlocks, 0L, 0L, blockCounts, topBlocks, null);
	}
}
