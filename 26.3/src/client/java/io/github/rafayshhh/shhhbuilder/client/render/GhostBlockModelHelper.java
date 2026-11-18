package io.github.rafayshhh.shhhbuilder.client.render;

import com.mojang.blaze3d.vertex.QuadInstance;
import io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for caching baked quads of distinct BlockStates for ghost block rendering.
 * Avoids per-frame allocations and repeated model lookups across thousands of schematic voxels.
 */
public class GhostBlockModelHelper {
	public static final int GHOST_ALPHA = 0x73; // ~0.45 default
	private static volatile int currentGhostAlpha = GHOST_ALPHA;
	public static final int PACKED_FULL_BRIGHT = 0x00F000F0; // Full block (15) and sky (15) light

	public static void setPreviewAlpha(int alpha) {
		currentGhostAlpha = alpha & 0xFF;
	}

	public static int getPreviewAlpha() {
		return currentGhostAlpha;
	}

	public record CachedGhostModel(
		List<BakedQuad> unculledQuads,
		Map<Direction, List<BakedQuad>> faceQuads,
		int tintRgb
	) {
		public CachedGhostModel(List<BakedQuad> unculledQuads, Map<Direction, List<BakedQuad>> faceQuads) {
			this(unculledQuads, faceQuads, 0x80C755);
		}

		public boolean isEmpty() {
			if (!unculledQuads.isEmpty()) {
				return false;
			}
			for (List<BakedQuad> list : faceQuads.values()) {
				if (!list.isEmpty()) {
					return false;
				}
			}
			return true;
		}
	}

	private static final CachedGhostModel EMPTY_MODEL;

	static {
		Map<Direction, List<BakedQuad>> emptyFaces = new EnumMap<>(Direction.class);
		for (Direction d : Direction.values()) {
			emptyFaces.put(d, List.of());
		}
		EMPTY_MODEL = new CachedGhostModel(List.of(), Collections.unmodifiableMap(emptyFaces), 0x80C755);
	}

	private static final Map<BlockState, CachedGhostModel> CACHE = new ConcurrentHashMap<>();

	public static void clearCache() {
		CACHE.clear();
	}

	public static int getCacheSize() {
		return CACHE.size();
	}

	/**
	 * Retrieves or builds the cached quad model for a given BlockState.
	 */
	public static CachedGhostModel getModel(BlockState state) {
		if (state == null) {
			return EMPTY_MODEL;
		}
		return CACHE.computeIfAbsent(state, GhostBlockModelHelper::buildModel);
	}

	private static CachedGhostModel buildModel(BlockState state) {
		if (!BlockStateHelper.isBootstrapped()) {
			return EMPTY_MODEL;
		}

		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.getModelManager() == null) {
				return EMPTY_MODEL;
			}

			BlockStateModelSet modelSet = mc.getModelManager().getBlockStateModelSet();
			if (modelSet == null) {
				return EMPTY_MODEL;
			}

			BlockStateModel model = modelSet.get(state);
			if (model == null) {
				return EMPTY_MODEL;
			}

			List<BlockStateModelPart> parts = new ArrayList<>();
			model.collectParts(RandomSource.create(42L), parts);

			List<BakedQuad> unculled = new ArrayList<>();
			Map<Direction, List<BakedQuad>> faceMap = new EnumMap<>(Direction.class);
			for (Direction d : Direction.values()) {
				faceMap.put(d, new ArrayList<>());
			}

			for (BlockStateModelPart part : parts) {
				List<BakedQuad> unculledPart = part.getQuads(null);
				if (unculledPart != null && !unculledPart.isEmpty()) {
					unculled.addAll(unculledPart);
				}
				for (Direction dir : Direction.values()) {
					List<BakedQuad> facePart = part.getQuads(dir);
					if (facePart != null && !facePart.isEmpty()) {
						faceMap.get(dir).addAll(facePart);
					}
				}
			}

			Map<Direction, List<BakedQuad>> finalFaceMap = new EnumMap<>(Direction.class);
			for (Direction d : Direction.values()) {
				finalFaceMap.put(d, List.copyOf(faceMap.get(d)));
			}

			int tintRgb = computeBlockTint(state);
			return new CachedGhostModel(List.copyOf(unculled), Collections.unmodifiableMap(finalFaceMap), tintRgb);
		} catch (Throwable t) {
			return EMPTY_MODEL;
		}
	}

	/**
	 * Computes the 24-bit RGB tint for a given BlockState.
	 * Evaluated once when caching the model to prevent per-quad string allocations.
	 */
	public static int computeBlockTint(BlockState state) {
		if (state == null) {
			return 0x80C755;
		}
		String blockStr = state.getBlock().toString().toLowerCase(Locale.ROOT);
		if (blockStr.contains("grass")) {
			return 0x7CBD6B; // Grass green
		} else if (blockStr.contains("leaves") || blockStr.contains("vine")) {
			return 0x59C93C; // Foliage green
		} else if (blockStr.contains("water")) {
			return 0x3F76E4; // Water blue
		} else if (blockStr.contains("redstone")) {
			return 0xFF2200; // Redstone red
		} else {
			return 0x80C755; // General foliage fallback
		}
	}

	/**
	 * Calculates the 32-bit ARGB color (with ~0.45 alpha) for rendering a baked quad using a precomputed tint RGB.
	 */
	public static int getQuadColor(BakedQuad quad, int tintRgb) {
		int rgb = (quad != null && quad.materialInfo() != null && quad.materialInfo().isTinted()) ? tintRgb : 0xFFFFFF;
		return (currentGhostAlpha << 24) | (rgb & 0x00FFFFFF);
	}

	/**
	 * Calculates the 32-bit ARGB color (with ~0.45 alpha) for rendering a baked quad.
	 */
	public static int getQuadColor(BakedQuad quad, BlockState state) {
		int tintRgb = computeBlockTint(state);
		return getQuadColor(quad, tintRgb);
	}

	/**
	 * Configures a QuadInstance with the precomputed tint RGB color, full-bright light, and no overlay.
	 */
	public static void setupQuadInstance(QuadInstance quadInstance, BakedQuad quad, int tintRgb, int overlayCoords) {
		int color = getQuadColor(quad, tintRgb);
		quadInstance.setColor(color);
		quadInstance.setLightCoords(PACKED_FULL_BRIGHT);
		quadInstance.setOverlayCoords(overlayCoords);
	}

	/**
	 * Configures a QuadInstance with the ~0.45 alpha color, full-bright light, and no overlay.
	 */
	public static void setupQuadInstance(QuadInstance quadInstance, BakedQuad quad, BlockState state, int overlayCoords) {
		int color = getQuadColor(quad, state);
		quadInstance.setColor(color);
		quadInstance.setLightCoords(PACKED_FULL_BRIGHT);
		quadInstance.setOverlayCoords(overlayCoords);
	}
}
