package io.github.rafayshhh.shhhbuilder.client.hud;

import io.github.rafayshhh.shhhbuilder.client.PlacementManager;
import io.github.rafayshhh.shhhbuilder.client.material.MaterialCountingEngine;
import io.github.rafayshhh.shhhbuilder.client.schematic.WorldBlockValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Handles crosshair raycasting against active schematic blocks using VoxelShape.clip,
 * providing accurate hit detection for thin blocks, live world verification status,
 * and client-side Ghost Pick-Block hotkey handling.
 */
public class GhostBlockRaycastHelper {

	public record GhostHit(
		PlacementManager.TransformedBlock block,
		BlockPos worldPos,
		WorldBlockValidator.MatchStatus status,
		double distance
	) {}

	/**
	 * Raycasts from the player's eyes up to 5.0 blocks using VoxelShape.clip
	 * to find the closest matching active schematic block under the crosshair.
	 */
	public static GhostHit raycast(Minecraft mc) {
		if (mc == null || mc.player == null || mc.level == null) {
			return null;
		}

		if (!PlacementManager.hasPlacement() || !PlacementManager.isGhostVisible()) {
			return null;
		}

		BlockPos origin = PlacementManager.getPosition();
		if (origin == null) {
			return null;
		}

		try {
			Vec3 eyePos = mc.player.getEyePosition(1.0f);
			Vec3 viewVec = mc.player.getViewVector(1.0f);
			double reach = 5.0;
			Vec3 reachEndPos = eyePos.add(viewVec.scale(reach));

			// Distance to physical obstacle in the world (if any)
			double worldHitDist = reach;
			BlockPos worldHitPos = null;
			if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
				BlockHitResult bhr = (BlockHitResult) mc.hitResult;
				worldHitPos = bhr.getBlockPos();
				worldHitDist = eyePos.distanceTo(bhr.getLocation());
			}

			// Step along the ray to collect candidate BlockPositions
			Set<BlockPos> candidatePositions = new LinkedHashSet<>();
			for (double d = 0.0; d <= reach; d += 0.1) {
				double px = eyePos.x + (viewVec.x * d);
				double py = eyePos.y + (viewVec.y * d);
				double pz = eyePos.z + (viewVec.z * d);
				candidatePositions.add(BlockPos.containing(px, py, pz));
			}
			if (worldHitPos != null && eyePos.distanceTo(Vec3.atCenterOf(worldHitPos)) <= reach + 1.0) {
				candidatePositions.add(worldHitPos);
			}

			GhostHit bestHit = null;
			double closestDist = Double.MAX_VALUE;

			for (BlockPos worldPos : candidatePositions) {
				int relX = worldPos.getX() - origin.getX();
				int relY = worldPos.getY() - origin.getY();
				int relZ = worldPos.getZ() - origin.getZ();

				PlacementManager.TransformedBlock tb = PlacementManager.getBlockAt(relX, relY, relZ);
				if (tb == null || tb.state() == null || tb.state().isAir()) {
					continue;
				}

				BlockState worldState = mc.level.getBlockState(worldPos);
				WorldBlockValidator.MatchStatus status = WorldBlockValidator.check(worldState, tb.state());

				// If already placed identically, ignore (already built)
				if (status == WorldBlockValidator.MatchStatus.EXACT_MATCH) {
					continue;
				}

				VoxelShape shape = null;
				if (status == WorldBlockValidator.MatchStatus.MISSING) {
					try {
						shape = tb.state().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
					} catch (Throwable ignored) {
					}
				} else {
					// WRONG_STATE or WRONG_BLOCK: test world block's physical shape first
					try {
						shape = worldState.getShape(mc.level, worldPos);
					} catch (Throwable ignored) {
					}
					if (shape == null || shape.isEmpty()) {
						try {
							shape = tb.state().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
						} catch (Throwable ignored) {
						}
					}
				}

				if (shape == null || shape.isEmpty()) {
					shape = Shapes.block();
				}

				BlockHitResult hit = shape.clip(eyePos, reachEndPos, worldPos);
				if (hit != null) {
					double dist = eyePos.distanceTo(hit.getLocation());
					// If candidate is behind an unrelated world obstacle, skip it
					if (dist > worldHitDist + 0.05 && !worldPos.equals(worldHitPos)) {
						continue;
					}
					if (dist < closestDist) {
						closestDist = dist;
						bestHit = new GhostHit(tb, worldPos, status, dist);
					}
				}
			}

			return bestHit;
		} catch (Throwable ignored) {
			return null;
		}
	}

	/**
	 * Formats HUD component for the block currently being looked at.
	 */
	public static Component formatLookingAtComponent(GhostHit hit) {
		if (hit == null || hit.block() == null || hit.block().state() == null) {
			return null;
		}

		if (hit.status() == WorldBlockValidator.MatchStatus.WRONG_STATE) {
			String expected = WorldBlockValidator.formatExpectedDetails(hit.block().state());
			return Component.literal("Looking at: Wrong orientation (Expected: " + expected + ")")
				.withColor(WorldBlockValidator.COLOR_HUD_WRONG);
		}

		if (hit.status() == WorldBlockValidator.MatchStatus.WRONG_BLOCK) {
			String expected = WorldBlockValidator.formatExpectedDetails(hit.block().state());
			return Component.literal("Looking at: Wrong block (Expected: " + expected + ")")
				.withColor(WorldBlockValidator.COLOR_HUD_WRONG);
		}

		if (hit.status() == WorldBlockValidator.MatchStatus.MISSING) {
			int layer = hit.block().relY() + 1;
			String blockName = hit.block().state().getBlock().getName().getString();
			return Component.literal("Looking at: ")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(blockName).withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" (layer " + layer + ")").withStyle(ChatFormatting.AQUA));
		}

		return null;
	}

	/**
	 * Performs client-side Ghost Pick-Block: finds matching item in hotbar slots 0-8 and selects it.
	 * Sends zero network packets.
	 *
	 * @return true if an item was successfully selected in the hotbar
	 */
	public static boolean performGhostPickBlock(Minecraft mc, GhostHit hit) {
		if (mc == null || mc.player == null || hit == null || hit.block() == null || hit.block().state() == null) {
			return false;
		}

		BlockState expectedState = hit.block().state();
		Set<String> targetItemIds = new HashSet<>();

		try {
			MaterialCountingEngine.BlockEvaluation eval = MaterialCountingEngine.evaluate(expectedState);
			if (eval != null && eval.items() != null) {
				for (MaterialCountingEngine.ItemRequirement req : eval.items()) {
					if (req.itemIdentifier() != null) {
						targetItemIds.add(req.itemIdentifier());
					}
				}
			}
		} catch (Throwable ignored) {
		}

		if (targetItemIds.isEmpty()) {
			Identifier blockId = BuiltInRegistries.BLOCK.getKey(expectedState.getBlock());
			if (blockId != null) {
				targetItemIds.add(blockId.toString());
			}
			try {
				var asItem = expectedState.getBlock().asItem();
				if (asItem != null) {
					Identifier itemId = BuiltInRegistries.ITEM.getKey(asItem);
					if (itemId != null) {
						targetItemIds.add(itemId.toString());
					}
				}
			} catch (Throwable ignored) {
			}
		}

		Inventory inv = mc.player.getInventory();
		if (inv == null) {
			return false;
		}

		// Search hotbar slots 0 through 8
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = inv.getItem(slot);
			if (stack != null && !stack.isEmpty()) {
				Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
				if (id != null && targetItemIds.contains(id.toString())) {
					inv.setSelectedSlot(slot);
					return true;
				}
			}
		}

		return false;
	}
}
