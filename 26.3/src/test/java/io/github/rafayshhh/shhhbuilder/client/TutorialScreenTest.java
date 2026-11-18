package io.github.rafayshhh.shhhbuilder.client;

import io.github.rafayshhh.shhhbuilder.client.gui.TutorialScreen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TutorialScreenTest {

	@Test
	@DisplayName("Tutorial screen page navigation respects bounds (first page, last page, clamp)")
	void testPageNavigationBounds() {
		TutorialScreen screen = new TutorialScreen(null);

		// Initial state is page 0 (Page 1 in 1-based display)
		assertEquals(0, screen.getPage(), "Initial page should be 0");

		// previousPage() on page 0 must stay on page 0
		screen.previousPage();
		assertEquals(0, screen.getPage(), "Page must not decrement below 0");

		// Step forward through all 6 pages
		for (int i = 1; i < TutorialScreen.TOTAL_PAGES; i++) {
			screen.nextPage();
			assertEquals(i, screen.getPage(), "Page should advance to " + i);
		}

		// On last page (page 5), nextPage() must not advance past 5
		assertEquals(TutorialScreen.TOTAL_PAGES - 1, screen.getPage());
		screen.nextPage();
		assertEquals(TutorialScreen.TOTAL_PAGES - 1, screen.getPage(), "Page must not advance past TOTAL_PAGES - 1");

		// setPage() clamps invalid values
		screen.setPage(-10);
		assertEquals(0, screen.getPage(), "Negative page should clamp to 0");

		screen.setPage(999);
		assertEquals(TutorialScreen.TOTAL_PAGES - 1, screen.getPage(), "Large page should clamp to TOTAL_PAGES - 1");

		screen.setPage(3);
		assertEquals(3, screen.getPage());
	}

	@Test
	@DisplayName("Each tutorial page has a valid title and between 4 and 6 short lines of plain text")
	void testPageContentStructure() {
		TutorialScreen screen = new TutorialScreen(null);

		for (int page = 0; page < TutorialScreen.TOTAL_PAGES; page++) {
			Component title = screen.getPageTitle(page);
			assertNotNull(title, "Page " + page + " title must not be null");

			List<Component> lines = screen.getPageLines(page);
			assertNotNull(lines, "Page " + page + " lines must not be null");
			assertTrue(lines.size() >= 4 && lines.size() <= 6,
				"Page " + page + " must have between 4 and 6 lines (had " + lines.size() + ")");

			for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
				Component line = lines.get(lineIndex);
				assertNotNull(line, "Page " + page + " line " + lineIndex + " must not be null");
			}
		}
	}

	@Test
	@DisplayName("All TutorialScreen text and card fill colors have non-zero alpha in ARGB")
	void testTutorialColorsAlpha() {
		int[] textColors = new int[]{
			TutorialScreen.COLOR_TITLE,
			TutorialScreen.COLOR_SUBTITLE,
			TutorialScreen.COLOR_TEXT_PRIMARY,
			TutorialScreen.COLOR_TEXT_SECONDARY,
			TutorialScreen.COLOR_PAGE_COUNTER
		};

		for (int color : textColors) {
			int alpha = (color >>> 24) & 0xFF;
			assertEquals(0xFF, alpha, "Tutorial text color 0x" + Integer.toHexString(color) + " must have 0xFF alpha");
		}

		// Card background (translucent dark)
		int cardBgAlpha = (TutorialScreen.COLOR_CARD_BG >>> 24) & 0xFF;
		assertTrue(cardBgAlpha > 0, "Tutorial card background must have non-zero alpha");
		assertEquals(0x80, cardBgAlpha, "Tutorial card background alpha should be 0x80");

		// Card border (translucent white)
		int cardBorderAlpha = (TutorialScreen.COLOR_CARD_BORDER >>> 24) & 0xFF;
		assertTrue(cardBorderAlpha > 0, "Tutorial card border must have non-zero alpha");
		assertEquals(0x40, cardBorderAlpha, "Tutorial card border alpha should be 0x40");
	}

	@Test
	@DisplayName("Each tutorial page has interactive keycaps with non-empty key, label, and tooltip")
	void testInteractiveKeycaps() {
		TutorialScreen screen = new TutorialScreen(null);

		for (int page = 0; page < TutorialScreen.TOTAL_PAGES; page++) {
			List<TutorialScreen.TutorialKeycap> keycaps = screen.getPageKeycaps(page);
			assertNotNull(keycaps, "Page " + page + " keycaps list must not be null");
			assertFalse(keycaps.isEmpty(), "Page " + page + " must have at least one interactive keycap");

			for (TutorialScreen.TutorialKeycap cap : keycaps) {
				assertNotNull(cap.key(), "Keycap key must not be null on page " + page);
				assertFalse(cap.key().isBlank(), "Keycap key must not be blank on page " + page);

				assertNotNull(cap.label(), "Keycap label must not be null on page " + page);
				assertFalse(cap.label().isBlank(), "Keycap label must not be blank on page " + page);

				assertNotNull(cap.tooltip(), "Keycap tooltip must not be null on page " + page);
				assertFalse(cap.tooltip().isBlank(), "Keycap tooltip must not be blank on page " + page);
			}
		}
	}
}
