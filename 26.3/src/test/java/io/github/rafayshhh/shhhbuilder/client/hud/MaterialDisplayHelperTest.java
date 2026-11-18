package io.github.rafayshhh.shhhbuilder.client.hud;

import io.github.rafayshhh.shhhbuilder.client.config.ShhhBuilderConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaterialDisplayHelperTest {

	@Test
	public void testFormatCountUnderStack() {
		assertEquals("0", MaterialDisplayHelper.formatCount(0));
		assertEquals("1", MaterialDisplayHelper.formatCount(1));
		assertEquals("53", MaterialDisplayHelper.formatCount(53));
		assertEquals("63", MaterialDisplayHelper.formatCount(63));
	}

	@Test
	public void testFormatCountExactStacks() {
		assertEquals("64 (1 stack)", MaterialDisplayHelper.formatCount(64));
		assertEquals("128 (2 stacks)", MaterialDisplayHelper.formatCount(128));
		assertEquals("192 (3 stacks)", MaterialDisplayHelper.formatCount(192));
	}

	@Test
	public void testFormatCountStacksWithRemainder() {
		assertEquals("65 (1 stack + 1)", MaterialDisplayHelper.formatCount(65));
		assertEquals("212 (3 stacks + 20)", MaterialDisplayHelper.formatCount(212));
		assertEquals("130 (2 stacks + 2)", MaterialDisplayHelper.formatCount(130));
	}

	@Test
	public void testFormatBlockNameFallback() {
		assertEquals("Oak Planks", MaterialDisplayHelper.formatBlockName("minecraft:oak_planks"));
		assertEquals("Stone Bricks", MaterialDisplayHelper.formatBlockName("minecraft:stone_bricks"));
		assertEquals("Glass", MaterialDisplayHelper.formatBlockName("minecraft:glass"));
		assertEquals("Torch", MaterialDisplayHelper.formatBlockName("minecraft:torch"));
		assertEquals("Cobblestone Stairs", MaterialDisplayHelper.formatBlockName("minecraft:cobblestone_stairs"));
	}

	@Test
	public void testConfigDefaultsAndCycling() {
		ShhhBuilderConfig config = new ShhhBuilderConfig();
		assertTrue(config.isHudEnabled());
		assertEquals(ShhhBuilderConfig.HudPosition.TOP_LEFT, config.getHudPosition());
		assertEquals(ShhhBuilderConfig.HudScale.NORMAL, config.getHudScale());

		// Test position cycle
		ShhhBuilderConfig.HudPosition pos = ShhhBuilderConfig.HudPosition.TOP_LEFT;
		pos = pos.next();
		assertEquals(ShhhBuilderConfig.HudPosition.TOP_RIGHT, pos);
		pos = pos.next();
		assertEquals(ShhhBuilderConfig.HudPosition.BOTTOM_LEFT, pos);
		pos = pos.next();
		assertEquals(ShhhBuilderConfig.HudPosition.BOTTOM_RIGHT, pos);
		pos = pos.next();
		assertEquals(ShhhBuilderConfig.HudPosition.TOP_LEFT, pos);

		// Test scale cycle
		ShhhBuilderConfig.HudScale scale = ShhhBuilderConfig.HudScale.SMALL;
		scale = scale.next();
		assertEquals(ShhhBuilderConfig.HudScale.NORMAL, scale);
		scale = scale.next();
		assertEquals(ShhhBuilderConfig.HudScale.LARGE, scale);
		scale = scale.next();
		assertEquals(ShhhBuilderConfig.HudScale.SMALL, scale);
	}
}
