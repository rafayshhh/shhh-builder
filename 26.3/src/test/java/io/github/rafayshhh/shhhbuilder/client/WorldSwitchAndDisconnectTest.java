package io.github.rafayshhh.shhhbuilder.client;

import io.github.rafayshhh.shhhbuilder.client.material.MaterialProgressManager;
import io.github.rafayshhh.shhhbuilder.client.render.GhostBlockRenderer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorldSwitchAndDisconnectTest {

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	public void reset() {
		PlacementManager.cleanupWorldSession();
	}

	@Test
	@DisplayName("Disconnect cleanup clears all active placements, cached transforms, and unloads schematic session")
	void testDisconnectCleanupClearsAllState() {
		// 1. Simulate an active in-game placement
		BlockPos pos = new BlockPos(250, 72, -180);
		ResourceKey<net.minecraft.world.level.Level> overworld = ResourceKey.create(
			Registries.DIMENSION, Identifier.parse("minecraft:overworld")
		);

		PlacementManager.setPlacement(pos, overworld);
		PlacementManager.setRotation(180);
		PlacementManager.setMirrored(true);
		PlacementManager.setCurrentLayer(4, 12);
		PlacementManager.setLayerMode(LayerMode.SINGLE);
		SchematicManager.setSelectedFileName("sample_house.litematic");

		assertTrue(PlacementManager.hasPlacement(), "Placement must be active before disconnect");
		assertEquals(pos, PlacementManager.getPosition());
		assertEquals(180, PlacementManager.getRotation());
		assertTrue(PlacementManager.isMirrored());
		assertEquals(LayerMode.SINGLE, PlacementManager.getLayerMode());
		assertEquals(4, PlacementManager.getCurrentLayer());
		assertEquals("sample_house.litematic", SchematicManager.getSelectedFileName());

		// 2. Trigger disconnect cleanup
		PlacementManager.cleanupWorldSession();

		// 3. Verify ALL placement state is completely cleared
		assertFalse(PlacementManager.hasPlacement(), "Placement must be inactive after disconnect");
		assertNull(PlacementManager.getPosition(), "Position must be null after disconnect");
		assertNull(PlacementManager.getDimension(), "Dimension must be null after disconnect");
		assertEquals(0, PlacementManager.getRotation(), "Rotation must reset to 0 after disconnect");
		assertFalse(PlacementManager.isMirrored(), "Mirrored must reset to false after disconnect");
		assertEquals(LayerMode.ALL, PlacementManager.getLayerMode(), "Layer mode must reset to ALL after disconnect");
		assertEquals(1, PlacementManager.getCurrentLayer(), "Current layer must reset to 1 after disconnect");

		// 4. Verify block cache and transforms are invalidated
		assertNull(PlacementManager.getBlockAt(0, 0, 0), "Cached block map must be null");
		assertFalse(PlacementManager.hasBlockAt(0, 0, 0), "hasBlockAt must return false");
		assertEquals(0, PlacementManager.getCachedBlocks(null).length, "Cached blocks must be empty");

		// 5. Verify active schematic session is unloaded
		assertNull(SchematicManager.getSelectedFileName(), "Active schematic file name must be null after disconnect");
		assertFalse(SchematicManager.hasLoadedSchematic(), "hasLoadedSchematic must return false after disconnect");

		// 6. Verify ghost renderer batched quads and cache are cleared
		assertEquals(0, GhostBlockRenderer.getBatchedBlockCount(), "Batched textured blocks must be cleared");
		assertFalse(GhostBlockRenderer.isTexturedPreviewDisabled(), "Circuit breaker must reset");
	}

	@Test
	@DisplayName("MaterialProgressManager safely flushes pending checklist progress to disk on disconnect")
	void testMaterialProgressFlushOnDisconnect() {
		String testKey = MaterialProgressManager.buildKey("disconnect_test.litematic", 4096, 999999L);
		MaterialProgressManager.setItemChecked(testKey, "minecraft:stone_bricks", true);
		MaterialProgressManager.setItemChecked(testKey, "minecraft:glass", true);

		// Trigger cleanup / flush
		PlacementManager.cleanupWorldSession();

		// Verify checklist state remains intact and persisted
		assertTrue(MaterialProgressManager.isChecked(testKey, "minecraft:stone_bricks"));
		assertTrue(MaterialProgressManager.isChecked(testKey, "minecraft:glass"));
		assertFalse(MaterialProgressManager.isChecked(testKey, "minecraft:dirt"));

		// Clean up test entry
		MaterialProgressManager.resetProgress(testKey);
		PlacementManager.cleanupWorldSession();
		assertFalse(MaterialProgressManager.isChecked(testKey, "minecraft:stone_bricks"));
	}

	@Test
	@DisplayName("Simulating a world switch ensures the new world has zero leftover placement or ghost blocks")
	void testSequentialWorldSwitchResetsCleanly() {
		// World 1 session
		PlacementManager.setPlacement(
			new BlockPos(10, 60, 10),
			ResourceKey.create(Registries.DIMENSION, Identifier.parse("minecraft:the_nether"))
		);
		PlacementManager.setRotation(270);
		PlacementManager.setMirrored(true);
		PlacementManager.setLayerMode(LayerMode.UP_TO);
		PlacementManager.setCurrentLayer(3, 8);
		SchematicManager.setSelectedFileName("nether_base.schem");

		// World 1 disconnects
		PlacementManager.cleanupWorldSession();

		// World 2 connects: must be completely pristine
		assertFalse(PlacementManager.hasPlacement(), "World 2 must have no active placement from World 1");
		assertNull(PlacementManager.getPosition(), "World 2 position must be null");
		assertNull(PlacementManager.getDimension(), "World 2 dimension must be null");
		assertEquals(0, PlacementManager.getRotation(), "World 2 rotation must be 0");
		assertFalse(PlacementManager.isMirrored(), "World 2 mirror must be false");
		assertEquals(LayerMode.ALL, PlacementManager.getLayerMode(), "World 2 layer mode must be ALL");
		assertNull(SchematicManager.getSelectedFileName(), "World 2 must have no active schematic selected");
		assertEquals(0, GhostBlockRenderer.getBatchedBlockCount(), "World 2 must have 0 batched ghost blocks");
	}
}
