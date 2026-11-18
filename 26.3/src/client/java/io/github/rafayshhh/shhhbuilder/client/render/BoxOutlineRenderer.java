package io.github.rafayshhh.shhhbuilder.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import io.github.rafayshhh.shhhbuilder.client.SchematicManager;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * In-world schematic footprint bounding box outline renderer.
 * Uses Fabric API's {@link LevelRenderEvents#BEFORE_GIZMOS} and Minecraft's
 * {@link SubmitNodeCollector#submitShapeOutline} with depth-biased lines.
 */
public class BoxOutlineRenderer {
	// Footprint bounding box outline color: Vibrant crisp cyan in 32-bit ARGB (alpha = 0xFF)
	public static final int BOX_COLOR = 0xFF00E5FF;

	public static void register() {
		LevelRenderEvents.BEFORE_GIZMOS.register(BoxOutlineRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		// 6. Must not draw if no placement exists, or when the toggle is off
		if (!PlacementManager.isVisible() || !PlacementManager.hasPlacement()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		// 6. Must not draw if player is in a different dimension
		if (PlacementManager.getDimension() != null && !mc.level.dimension().equals(PlacementManager.getDimension())) {
			return;
		}

		LitematicData data = SchematicManager.getSelectedSchematicData();
		if (data == null) {
			return;
		}

		BlockPos pos = PlacementManager.getPosition();
		if (pos == null) {
			return;
		}

		if (context.levelState() == null || context.levelState().cameraRenderState == null) {
			return;
		}

		Vec3 camPos = context.levelState().cameraRenderState.pos;
		if (camPos == null) {
			return;
		}

		PoseStack poseStack = context.poseStack();
		SubmitNodeCollector collector = context.submitNodeCollector();
		if (poseStack == null || collector == null) {
			return;
		}

		// 3. Respect rotation: 90 and 270 degrees swap width and length
		int rotation = PlacementManager.getRotation();
		int boxWidth = (rotation == 90 || rotation == 270) ? data.length() : data.width();
		int boxHeight = data.height();
		int boxLength = (rotation == 90 || rotation == 270) ? data.width() : data.length();

		VoxelShape shape = Shapes.create(0, 0, 0, boxWidth, boxHeight, boxLength);

		poseStack.pushPose();
		poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

		// Bright Cyan outline box (0xFF00E5FF, guaranteed non-zero alpha in 26.3)
		collector.submitShapeOutline(
			poseStack,
			shape,
			RenderTypes.linesDepthBias(),
			BOX_COLOR,
			2.5f,
			false
		);

		poseStack.popPose();
	}
}
