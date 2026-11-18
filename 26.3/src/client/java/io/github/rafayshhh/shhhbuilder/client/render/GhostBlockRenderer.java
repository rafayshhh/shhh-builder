package io.github.rafayshhh.shhhbuilder.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import io.github.rafayshhh.shhhbuilder.client.LayerMode;
import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import io.github.rafayshhh.shhhbuilder.client.SchematicManager;
import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import io.github.rafayshhh.shhhbuilder.client.schematic.BlockCategoryHelper;
import io.github.rafayshhh.shhhbuilder.client.schematic.BlockStateHelper;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * In-world ghost block renderer.
 * Draws schematic blocks as see-through textured holograms (~0.45 alpha) or crisp MapColor wireframes.
 * Uses Fabric API's {@link LevelRenderEvents#BEFORE_GIZMOS}, Minecraft's
 * {@link SubmitNodeCollector#submitCustomGeometry} for textured blocks, and
 * {@link SubmitNodeCollector#submitShapeOutline} for outlines.
 * Respects 6-neighbor occlusion culling, face culling, configurable render distance,
 * distance limits, and block limits with protective try/catch circuit breaker fallback.
 */
public class GhostBlockRenderer {
	private static boolean capMessageShown = false;
	private static boolean placementPreviewLogged = false;
	private static BlockPos lastPlacementPos = null;
	private static String lastPlacementFile = null;

	// Circuit breaker state for experimental textured preview
	private static volatile boolean texturedPreviewDisabled = false;
	private static int texturedPipelineErrorCount = 0;
	private static boolean circuitBreakerAlertShown = false;
	private static final QuadInstance QUAD_INSTANCE = new QuadInstance();

	public record BatchedTexturedBlock(
		float camRelX,
		float camRelY,
		float camRelZ,
		int relX,
		int relY,
		int relZ,
		int blockLayer,
		net.minecraft.world.level.block.state.BlockState state,
		GhostBlockModelHelper.CachedGhostModel model
	) {}

	private static final java.util.List<BatchedTexturedBlock> TEXTURED_BATCH = new java.util.ArrayList<>(2048);

	public static int getBatchedBlockCount() {
		return TEXTURED_BATCH.size();
	}

	public static java.util.List<BatchedTexturedBlock> getBatchedBlocks() {
		return java.util.Collections.unmodifiableList(TEXTURED_BATCH);
	}

	public static void clearBatchedBlocks() {
		TEXTURED_BATCH.clear();
	}

	/**
	 * Determines whether a quad face should be culled because it touches an adjacent ghost block.
	 * When LayerMode is SINGLE or UP_TO, faces on the top slice boundary (y == currentLayer) are NEVER culled.
	 */
	public static boolean shouldCullFace(
		int relX, int relY, int relZ,
		Direction dir,
		int blockLayer,
		LayerMode layerMode,
		int currentLayer
	) {
		boolean isTopSliceBoundary = (layerMode == LayerMode.SINGLE || layerMode == LayerMode.UP_TO)
			&& (blockLayer == currentLayer);

		if (isTopSliceBoundary && dir == Direction.UP) {
			return false; // Top face on active slice boundary must stay visible
		}

		int nRelX = relX + dir.getStepX();
		int nRelY = relY + dir.getStepY();
		int nRelZ = relZ + dir.getStepZ();
		return PlacementManager.hasBlockAt(nRelX, nRelY, nRelZ);
	}

	// Failure logging sets: logs failed models and shapes at most once
	private static final java.util.Set<String> LOGGED_FAILED_MODELS = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static final java.util.Set<String> LOGGED_FAILED_BLOCKS = java.util.concurrent.ConcurrentHashMap.newKeySet();

	// Section sorting cache: only re-sort when player camera enters a new section or placement/distance changes
	private static int lastPlayerSecX = Integer.MIN_VALUE;
	private static int lastPlayerSecY = Integer.MIN_VALUE;
	private static int lastPlayerSecZ = Integer.MIN_VALUE;
	private static BlockPos lastCachedOrigin = null;
	private static int lastCachedMaxDist = -1;
	private static PlacementManager.BlockSection[] lastCachedSections = null;
	private static SectionDist[] cachedSortedSections = null;

	public static void resetCapMessage() {
		resetOnDisconnect();
	}

	public static void resetOnDisconnect() {
		capMessageShown = false;
		placementPreviewLogged = false;
		lastPlacementPos = null;
		lastPlacementFile = null;
		lastPlayerSecX = Integer.MIN_VALUE;
		lastPlayerSecY = Integer.MIN_VALUE;
		lastPlayerSecZ = Integer.MIN_VALUE;
		lastCachedOrigin = null;
		lastCachedMaxDist = -1;
		lastCachedSections = null;
		cachedSortedSections = null;
		TEXTURED_BATCH.clear();
		resetCircuitBreaker();
	}

	public static void resetCircuitBreaker() {
		texturedPreviewDisabled = false;
		texturedPipelineErrorCount = 0;
		circuitBreakerAlertShown = false;
	}

	public static boolean isTexturedPreviewDisabled() {
		return texturedPreviewDisabled;
	}

	public static int getTexturedErrorCount() {
		return texturedPipelineErrorCount;
	}

	public static void handleTexturedPipelineError(Throwable t, Minecraft mc) {
		texturedPipelineErrorCount++;
		if (texturedPipelineErrorCount >= 3) {
			if (!texturedPreviewDisabled) {
				texturedPreviewDisabled = true;
				ShhhBuilderConfig.getInstance().setPreviewStyle(ShhhBuilderConfig.PreviewStyle.OUTLINES);
				ShhhBuilder.LOGGER.error("Textured ghost preview failed 3 times, switching to outlines for the session: {}", t.getMessage(), t);
				if (!circuitBreakerAlertShown && mc != null && mc.player != null) {
					circuitBreakerAlertShown = true;
					mc.player.sendOverlayMessage(
						Component.literal("Textured preview turned off. Showing outlines.").withStyle(ChatFormatting.RED)
					);
				}
			}
		} else {
			ShhhBuilder.LOGGER.warn("Textured ghost preview error ({}/3): {}", texturedPipelineErrorCount, t.getMessage());
		}
	}

	public static void register() {
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.player == null) {
				return;
			}

			if (!PlacementManager.hasPlacement() || !PlacementManager.isGhostVisible()) {
				return;
			}

			if (PlacementManager.getDimension() == null || !mc.level.dimension().equals(PlacementManager.getDimension())) {
				return;
			}

			if (!SchematicManager.hasLoadedSchematic()) {
				return;
			}

			LitematicData data = SchematicManager.getLoadedData();
			if (data == null || data.blockData() == null || !data.blockData().canPreview()) {
				return;
			}

			PlacementManager.BlockSection[] sections = PlacementManager.getCachedSections(data);
			if (sections == null || sections.length == 0) {
				return;
			}

			BlockPos origin = PlacementManager.getPosition();
			String currentFile = SchematicManager.getSelectedFileName();
			if (!java.util.Objects.equals(origin, lastPlacementPos) || !java.util.Objects.equals(currentFile, lastPlacementFile)) {
				lastPlacementPos = origin;
				lastPlacementFile = currentFile;
				capMessageShown = false;
				placementPreviewLogged = false;
			}

			Vec3 camPos = context.levelState().cameraRenderState.pos;
			double camX = camPos.x;
			double camY = camPos.y;
			double camZ = camPos.z;

			double playerX = mc.player.getX();
			double playerY = mc.player.getY();
			double playerZ = mc.player.getZ();

			int playerSecX = (int) Math.floor(playerX) >> 4;
			int playerSecY = (int) Math.floor(playerY) >> 4;
			int playerSecZ = (int) Math.floor(playerZ) >> 4;

			int maxDist = ShhhBuilderConfig.getInstance().getGhostRenderDistance();
			double maxDistSq = (double) maxDist * maxDist;

			// Re-filter and re-sort sections ONLY when player crosses section boundaries or placement/distance changes
			SectionDist[] activeSections = cachedSortedSections;
			if (activeSections == null
				|| playerSecX != lastPlayerSecX
				|| playerSecY != lastPlayerSecY
				|| playerSecZ != lastPlayerSecZ
				|| !origin.equals(lastCachedOrigin)
				|| maxDist != lastCachedMaxDist
				|| sections != lastCachedSections) {

				double maxSecDist = maxDist + 14.0;
				double maxSecDistSq = maxSecDist * maxSecDist;

				java.util.List<SectionDist> list = new java.util.ArrayList<>(sections.length);
				for (PlacementManager.BlockSection sec : sections) {
					double secWorldX = origin.getX() + sec.centerX();
					double secWorldY = origin.getY() + sec.centerY();
					double secWorldZ = origin.getZ() + sec.centerZ();

					double dx = secWorldX - playerX;
					double dy = secWorldY - playerY;
					double dz = secWorldZ - playerZ;
					double distSq = dx * dx + dy * dy + dz * dz;

					if (distSq <= maxSecDistSq) {
						list.add(new SectionDist(sec, distSq));
					}
				}

				list.sort(java.util.Comparator.comparingDouble(SectionDist::distSq));
				activeSections = list.toArray(new SectionDist[0]);

				cachedSortedSections = activeSections;
				lastPlayerSecX = playerSecX;
				lastPlayerSecY = playerSecY;
				lastPlayerSecZ = playerSecZ;
				lastCachedOrigin = origin;
				lastCachedMaxDist = maxDist;
				lastCachedSections = sections;
			}

			SubmitNodeCollector collector = context.submitNodeCollector();
			PoseStack poseStack = context.poseStack();
			TEXTURED_BATCH.clear();

			ShhhBuilderConfig config = ShhhBuilderConfig.getInstance();
			ShhhBuilderConfig.PreviewStyle previewStyle = config.getPreviewStyle();
			int texturedLimit = config.getTexturedBlockLimit();
			int texturedDist = config.getTexturedDistance();
			double texturedDistSq = (double) texturedDist * texturedDist;
			int texturedDrawn = 0;

			// Dynamic opacity and border settings computed once per frame
			int previewAlpha = config.getPreviewAlpha();
			GhostBlockModelHelper.setPreviewAlpha(previewAlpha);

			boolean bordersOn = config.isTexturedBorders();
			ShhhBuilderConfig.BorderColor borderMode = config.getBorderColor();
			int borderAlpha = config.getBorderAlpha();
			int fixedBorderColor;
			switch (borderMode) {
				case WHITE -> fixedBorderColor = (borderAlpha << 24) | 0xFFFFFF;
				case BLACK -> fixedBorderColor = (borderAlpha << 24) | 0x000000;
				case CYAN -> fixedBorderColor = (borderAlpha << 24) | 0x00FFFF;
				default -> fixedBorderColor = -1; // BLOCK_COLOR
			}

			int drawn = 0;
			int skipped = 0;
			BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

			for (SectionDist sd : activeSections) {
				for (PlacementManager.TransformedBlock block : sd.section.blocks()) {
					int wx = origin.getX() + block.relX();
					int wy = origin.getY() + block.relY();
					int wz = origin.getZ() + block.relZ();

					double dx = (wx + 0.5) - playerX;
					double dy = (wy + 0.5) - playerY;
					double dz = (wz + 0.5) - playerZ;
					double blockDistSq = dx * dx + dy * dy + dz * dz;

					if (blockDistSq > maxDistSq) {
						continue;
					}

					int blockLimit = config.getGhostBlockLimit();
					if (drawn >= blockLimit) {
						skipped++;
						continue;
					}

					BlockCategoryHelper.Category category = (block.state() != null)
						? BlockCategoryHelper.getCategory(block.state())
						: BlockCategoryHelper.Category.AIR;

					if (category == BlockCategoryHelper.Category.AIR) {
						continue;
					}

					// Live world verification
					mPos.set(wx, wy, wz);
					net.minecraft.world.level.block.state.BlockState worldState = mc.level.getBlockState(mPos);
					WorldBlockValidator.MatchStatus matchStatus = WorldBlockValidator.check(worldState, block.state());

					if (matchStatus == WorldBlockValidator.MatchStatus.EXACT_MATCH) {
						// Exact Match: already built correctly by the player, skip preview completely
						continue;
					}

					if (matchStatus == WorldBlockValidator.MatchStatus.WRONG_STATE) {
						// Wrong State (Same Block): draw bright Amber/Orange wireframe outline (0xFFFFAA00) with linesDepthBias
						try {
							VoxelShape shape = worldState.getShape(mc.level, mPos);
							if (shape == null || shape.isEmpty()) {
								shape = Shapes.block();
							}
							poseStack.pushPose();
							poseStack.translate(wx - camX, wy - camY, wz - camZ);
							collector.submitShapeOutline(poseStack, shape, RenderTypes.linesDepthBias(), WorldBlockValidator.COLOR_WRONG_STATE, 1.5f, false);
							poseStack.popPose();
							drawn++;
						} catch (Throwable t) {
							handleTexturedPipelineError(t, mc);
						}
						continue;
					}

					if (matchStatus == WorldBlockValidator.MatchStatus.WRONG_BLOCK) {
						// Wrong Block: draw bright Red wireframe outline (0xFFFF3333) with linesDepthBias
						try {
							VoxelShape shape = worldState.getShape(mc.level, mPos);
							if (shape == null || shape.isEmpty()) {
								shape = Shapes.block();
							}
							poseStack.pushPose();
							poseStack.translate(wx - camX, wy - camY, wz - camZ);
							collector.submitShapeOutline(poseStack, shape, RenderTypes.linesDepthBias(), WorldBlockValidator.COLOR_WRONG_BLOCK, 1.5f, false);
							poseStack.popPose();
							drawn++;
						} catch (Throwable t) {
							handleTexturedPipelineError(t, mc);
						}
						continue;
					}

					boolean wantTextured = (previewStyle == ShhhBuilderConfig.PreviewStyle.TEXTURED)
						&& !texturedPreviewDisabled
						&& (texturedDrawn < texturedLimit)
						&& (blockDistSq <= texturedDistSq);

					boolean drewTextured = false;
					if (wantTextured && block.state() != null) {
						if (category == BlockCategoryHelper.Category.TECHNICAL) {
							// Technical marker (barrier, light, structure void, jigsaw): small clear inner marker
							try {
								VoxelShape marker = Shapes.box(0.3, 0.3, 0.3, 0.7, 0.7, 0.7);
								int markerColor = (borderAlpha << 24) | 0x55FFFF;
								poseStack.pushPose();
								poseStack.translate(wx - camX, wy - camY, wz - camZ);
								collector.submitShapeOutline(poseStack, marker, RenderTypes.lines(), markerColor, 1.0f, false);
								poseStack.popPose();
								texturedDrawn++;
								drewTextured = true;
							} catch (Throwable t) {
								handleTexturedPipelineError(t, mc);
							}
						} else if (category == BlockCategoryHelper.Category.FLUID) {
							// Fluids (water, lava): translucent colored box
							try {
								String bName = block.state().getBlock().toString().toLowerCase(java.util.Locale.ROOT);
								int fluidRgb = bName.contains("lava") ? 0xE05810 : 0x3F76E4;
								int fluidColor = (previewAlpha << 24) | fluidRgb;
								VoxelShape fluidBox = Shapes.block();
								poseStack.pushPose();
								poseStack.translate(wx - camX, wy - camY, wz - camZ);
								collector.submitShapeOutline(poseStack, fluidBox, RenderTypes.lines(), fluidColor, 1.0f, false);
								if (bordersOn) {
									int bColor = (fixedBorderColor != -1) ? fixedBorderColor : ((borderAlpha << 24) | fluidRgb);
									collector.submitShapeOutline(poseStack, fluidBox, RenderTypes.lines(), bColor, 1.0f, false);
								}
								poseStack.popPose();
								texturedDrawn++;
								drewTextured = true;
							} catch (Throwable t) {
								handleTexturedPipelineError(t, mc);
							}
						} else if (category == BlockCategoryHelper.Category.BLOCK_ENTITY) {
							// Block entities with no standard model (chests, beds, signs): colored box + border
							try {
								VoxelShape beShape = block.state().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
								if (beShape == null || beShape.isEmpty()) {
									beShape = Shapes.block();
								}
								int blockRgb = BlockStateHelper.getBlockColor(block.state()) & 0x00FFFFFF;
								int beColor = (previewAlpha << 24) | blockRgb;
								poseStack.pushPose();
								poseStack.translate(wx - camX, wy - camY, wz - camZ);
								collector.submitShapeOutline(poseStack, beShape, RenderTypes.lines(), beColor, 1.0f, false);
								if (bordersOn) {
									int bColor = (fixedBorderColor != -1) ? fixedBorderColor : ((borderAlpha << 24) | blockRgb);
									collector.submitShapeOutline(poseStack, beShape, RenderTypes.lines(), bColor, 1.0f, false);
								}
								poseStack.popPose();
								texturedDrawn++;
								drewTextured = true;
							} catch (Throwable t) {
								handleTexturedPipelineError(t, mc);
							}
						} else {
							// Category.NORMAL: standard baked models
							GhostBlockModelHelper.CachedGhostModel model = null;
							try {
								model = GhostBlockModelHelper.getModel(block.state());
							} catch (Throwable t) {
								if (block.state() != null && LOGGED_FAILED_MODELS.add(block.state().toString())) {
									ShhhBuilder.LOGGER.warn("Failed to get model for block state {}, drawing colored box: {}", block.state(), t.getMessage());
								}
							}

							if (model == null || model.isEmpty()) {
								if (block.state() != null && LOGGED_FAILED_MODELS.add(block.state().toString())) {
									ShhhBuilder.LOGGER.warn("Model unavailable for block state {}, drawing colored box with border", block.state());
								}
								try {
									VoxelShape fbShape = block.state().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
									if (fbShape == null || fbShape.isEmpty()) {
										fbShape = Shapes.block();
									}
									int blockRgb = BlockStateHelper.getBlockColor(block.state()) & 0x00FFFFFF;
									int fbColor = (previewAlpha << 24) | blockRgb;
									poseStack.pushPose();
									poseStack.translate(wx - camX, wy - camY, wz - camZ);
									collector.submitShapeOutline(poseStack, fbShape, RenderTypes.lines(), fbColor, 1.0f, false);
									if (bordersOn) {
										int bColor = (fixedBorderColor != -1) ? fixedBorderColor : ((borderAlpha << 24) | blockRgb);
										collector.submitShapeOutline(poseStack, fbShape, RenderTypes.lines(), bColor, 1.0f, false);
									}
									poseStack.popPose();
									texturedDrawn++;
									drewTextured = true;
								} catch (Throwable t) {
									handleTexturedPipelineError(t, mc);
								}
							} else {
								// Batch valid textured model for a single batched submitCustomGeometry call per frame
								float rx = (float) (wx - camX);
								float ry = (float) (wy - camY);
								float rz = (float) (wz - camZ);
								int blockLayer = block.relY() + 1;
								TEXTURED_BATCH.add(new BatchedTexturedBlock(rx, ry, rz, block.relX(), block.relY(), block.relZ(), blockLayer, block.state(), model));

								// Draw thin border line around textured block if enabled
								if (bordersOn) {
									try {
										VoxelShape borderShape = block.state().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
										if (borderShape == null || borderShape.isEmpty()) {
											borderShape = Shapes.block();
										}
										int bColor = (fixedBorderColor != -1) ? fixedBorderColor : ((borderAlpha << 24) | (BlockStateHelper.getBlockColor(block.state()) & 0x00FFFFFF));
										poseStack.pushPose();
										poseStack.translate(wx - camX, wy - camY, wz - camZ);
										collector.submitShapeOutline(poseStack, borderShape, RenderTypes.lines(), bColor, 1.0f, false);
										poseStack.popPose();
									} catch (Throwable ignored) {
									}
								}

								texturedDrawn++;
								drewTextured = true;
							}
						}
					}

					if (!drewTextured) {
						VoxelShape shape;
						try {
							shape = (block.state() != null) ? block.state().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) : Shapes.block();
							if (shape == null || shape.isEmpty()) {
								shape = Shapes.block();
							}
						} catch (Throwable t) {
							shape = Shapes.block();
							if (block.state() != null && LOGGED_FAILED_BLOCKS.add(block.state().toString())) {
								ShhhBuilder.LOGGER.warn("Failed to get shape for block state {}, falling back to full cube: {}", block.state(), t.getMessage());
							}
						}

						int color = BlockStateHelper.ensureAlpha(BlockStateHelper.getBlockColor(block.state()));

						poseStack.pushPose();
						poseStack.translate(wx - camX, wy - camY, wz - camZ);
						collector.submitShapeOutline(poseStack, shape, RenderTypes.lines(), color, 1.0f, false);
						poseStack.popPose();
					}

					drawn++;
				}
			}

			// Emit batched textured blocks in a SINGLE submitCustomGeometry call per frame
			if (!TEXTURED_BATCH.isEmpty() && !texturedPreviewDisabled) {
				try {
					LayerMode layerMode = PlacementManager.getLayerMode();
					int currentLayer = PlacementManager.getCurrentLayer();
					collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (basePose, consumer) -> {
						PoseStack.Pose localPose = new PoseStack.Pose();
						for (BatchedTexturedBlock entry : TEXTURED_BATCH) {
							if (texturedPreviewDisabled) {
								break;
							}
							try {
								localPose.set(basePose);
								localPose.translate(entry.camRelX(), entry.camRelY(), entry.camRelZ());

								int tintRgb = entry.model().tintRgb();

								// 1. Render unculled quads
								for (BakedQuad q : entry.model().unculledQuads()) {
									GhostBlockModelHelper.setupQuadInstance(QUAD_INSTANCE, q, tintRgb, OverlayTexture.NO_OVERLAY);
									consumer.putBakedQuad(localPose, q, QUAD_INSTANCE);
								}

								// 2. Render face quads skipping faces that touch another ghost block
								for (Direction dir : Direction.values()) {
									if (shouldCullFace(entry.relX(), entry.relY(), entry.relZ(), dir, entry.blockLayer(), layerMode, currentLayer)) {
										continue; // Skip faces that touch another ghost block
									}

									List<BakedQuad> fQuads = entry.model().faceQuads().get(dir);
									if (fQuads != null && !fQuads.isEmpty()) {
										for (BakedQuad q : fQuads) {
											GhostBlockModelHelper.setupQuadInstance(QUAD_INSTANCE, q, tintRgb, OverlayTexture.NO_OVERLAY);
											consumer.putBakedQuad(localPose, q, QUAD_INSTANCE);
										}
									}
								}
							} catch (Throwable t) {
								handleTexturedPipelineError(t, mc);
							}
						}
					});
				} catch (Throwable t) {
					handleTexturedPipelineError(t, mc);
				}
			}

			if (!placementPreviewLogged && drawn > 0) {
				placementPreviewLogged = true;
				int outlinesDrawn = drawn - texturedDrawn;
				String fallbackReason;
				if (previewStyle == ShhhBuilderConfig.PreviewStyle.OUTLINES) {
					fallbackReason = "preview style is set to outlines";
				} else if (texturedPreviewDisabled) {
					fallbackReason = "circuit breaker tripped";
				} else if (outlinesDrawn > 0) {
					fallbackReason = "outlines used for blocks beyond " + texturedDist + "m or exceeding limit of " + texturedLimit;
				} else {
					fallbackReason = "none (all rendered textured)";
				}
				ShhhBuilder.LOGGER.info("Ghost preview active for placement at {}: style={}, textured={}, outlines={}, reason: {}",
					origin, previewStyle.getDisplayName(), texturedDrawn, outlinesDrawn, fallbackReason);
			}

			if (skipped > 0) {
				if (!capMessageShown && mc.player != null) {
					capMessageShown = true;
					int blockLimit = ShhhBuilderConfig.getInstance().getGhostBlockLimit();
					String limitStr = String.format(java.util.Locale.ROOT, "%,d", blockLimit);
					mc.player.sendOverlayMessage(
						Component.literal("Showing the closest " + limitStr + " blocks. You can raise this in Mod Menu > Shhh Builder.")
							.withStyle(ChatFormatting.GOLD)
					);
				}
			}
		});
	}

	private record SectionDist(PlacementManager.BlockSection section, double distSq) {}
}
