package io.github.rafayshhh.shhhbuilder.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import io.github.rafayshhh.shhhbuilder.client.SchematicFileEntry;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ShhhBuilderCommandTest {

	@Test
	@DisplayName("stripExtension removes .litematic and .schem extensions correctly")
	void testStripExtension() {
		assertEquals("sample_house", ShhhBuilderCommand.stripExtension("sample_house.litematic"));
		assertEquals("axe", ShhhBuilderCommand.stripExtension("axe.schem"));
		assertEquals("my.build", ShhhBuilderCommand.stripExtension("my.build.litematic"));
		assertEquals("UPPERCASE", ShhhBuilderCommand.stripExtension("UPPERCASE.LITEMATIC"));
		assertEquals("notes.txt", ShhhBuilderCommand.stripExtension("notes.txt"));
		assertEquals("", ShhhBuilderCommand.stripExtension(null));
	}

	@Test
	@DisplayName("cleanArg trims whitespace and strips enclosing quotes")
	void testCleanArg() {
		assertEquals("sample_house", ShhhBuilderCommand.cleanArg("  sample_house  "));
		assertEquals("starter house", ShhhBuilderCommand.cleanArg("\"starter house\""));
		assertEquals("plain", ShhhBuilderCommand.cleanArg("plain"));
		assertEquals("", ShhhBuilderCommand.cleanArg("   "));
		assertEquals("", ShhhBuilderCommand.cleanArg(null));
	}

	@Test
	@DisplayName("computeSuggestions suggests base names without extension where unambiguous, and full names where ambiguous")
	void testComputeSuggestions() {
		List<SchematicFileEntry> files = List.of(
			new SchematicFileEntry(Path.of("schematics/sample_house.litematic"), "sample_house.litematic"),
			new SchematicFileEntry(Path.of("schematics/axe.litematic"), "axe.litematic"),
			new SchematicFileEntry(Path.of("schematics/axe.schem"), "axe.schem")
		);

		// When remaining is empty: sample_house is unambiguous (no extension), axe is ambiguous (shows both extensions)
		List<String> suggestions = ShhhBuilderCommand.computeSuggestions(files, "");
		assertEquals(3, suggestions.size());
		assertTrue(suggestions.contains("sample_house"), "Unambiguous file must be suggested without extension");
		assertFalse(suggestions.contains("sample_house.litematic"), "Unambiguous file should not be suggested with extension by default");
		assertTrue(suggestions.contains("axe.litematic"), "Ambiguous file must be suggested with extension");
		assertTrue(suggestions.contains("axe.schem"), "Ambiguous file must be suggested with extension");

		// Filter by prefix "s"
		List<String> sFiltered = ShhhBuilderCommand.computeSuggestions(files, "s");
		assertEquals(1, sFiltered.size());
		assertEquals("sample_house", sFiltered.get(0));

		// Filter by prefix "a"
		List<String> aFiltered = ShhhBuilderCommand.computeSuggestions(files, "a");
		assertEquals(2, aFiltered.size());
		assertTrue(aFiltered.contains("axe.litematic"));
		assertTrue(aFiltered.contains("axe.schem"));

		// When user explicitly types the dot in "sample_house."
		List<String> dotFiltered = ShhhBuilderCommand.computeSuggestions(files, "sample_house.");
		assertEquals(1, dotFiltered.size());
		assertEquals("sample_house.litematic", dotFiltered.get(0));
	}

	@Test
	@DisplayName("resolveFile handles exact full names, unambiguous base names, ambiguous matches, and missing files")
	void testResolveFile() {
		List<SchematicFileEntry> files = List.of(
			new SchematicFileEntry(Path.of("schematics/sample_house.litematic"), "sample_house.litematic"),
			new SchematicFileEntry(Path.of("schematics/axe.litematic"), "axe.litematic"),
			new SchematicFileEntry(Path.of("schematics/axe.schem"), "axe.schem")
		);

		// 1. Unambiguous base name match
		var resUnambiguous = ShhhBuilderCommand.resolveFile("sample_house", files);
		assertTrue(resUnambiguous.isSuccess());
		assertEquals("sample_house.litematic", resUnambiguous.entry().fileName());
		assertNull(resUnambiguous.error());

		// 2. Case-insensitive match
		var resCase = ShhhBuilderCommand.resolveFile("SAMPLE_HOUSE", files);
		assertTrue(resCase.isSuccess());
		assertEquals("sample_house.litematic", resCase.entry().fileName());

		// 3. Exact full name match
		var resFull = ShhhBuilderCommand.resolveFile("sample_house.litematic", files);
		assertTrue(resFull.isSuccess());
		assertEquals("sample_house.litematic", resFull.entry().fileName());

		// 4. Ambiguous match returns clear error mentioning both options
		var resAmbiguous = ShhhBuilderCommand.resolveFile("axe", files);
		assertFalse(resAmbiguous.isSuccess());
		assertNotNull(resAmbiguous.error());
		assertTrue(resAmbiguous.error().contains("Multiple files match 'axe'"), "Error should explain ambiguity");
		assertTrue(resAmbiguous.error().contains("axe.litematic"));
		assertTrue(resAmbiguous.error().contains("axe.schem"));

		// 5. Explicitly specified extension resolves ambiguous base name
		var resExplicitLitematic = ShhhBuilderCommand.resolveFile("axe.litematic", files);
		assertTrue(resExplicitLitematic.isSuccess());
		assertEquals("axe.litematic", resExplicitLitematic.entry().fileName());

		var resExplicitSchem = ShhhBuilderCommand.resolveFile("axe.schem", files);
		assertTrue(resExplicitSchem.isSuccess());
		assertEquals("axe.schem", resExplicitSchem.entry().fileName());

		// 6. Missing file gives helpful error
		var resMissing = ShhhBuilderCommand.resolveFile("castle", files);
		assertFalse(resMissing.isSuccess());
		assertTrue(resMissing.error().contains("Schematic not found: 'castle'"));
		assertTrue(resMissing.error().contains("/shhhbuilder list"));

		// 7. Blank input gives usage hint
		var resBlank = ShhhBuilderCommand.resolveFile("", files);
		assertFalse(resBlank.isSuccess());
		assertTrue(resBlank.error().contains("/shhhbuilder select <file>"));
	}

	@Test
	@DisplayName("validateCoordinates validates against world height and world borders")
	void testValidateCoordinates() {
		int minH = -64;
		int maxH = 319;

		// Valid coordinate
		var valid = ShhhBuilderCommand.validateCoordinates(100, 64, -200, minH, maxH);
		assertTrue(valid.valid());
		assertNull(valid.error());

		// Exact boundary extremes
		assertTrue(ShhhBuilderCommand.validateCoordinates(0, -64, 0, minH, maxH).valid());
		assertTrue(ShhhBuilderCommand.validateCoordinates(0, 319, 0, minH, maxH).valid());

		// Out of height limits
		var tooLow = ShhhBuilderCommand.validateCoordinates(0, -65, 0, minH, maxH);
		assertFalse(tooLow.valid());
		assertTrue(tooLow.error().contains("Y coordinate -65 is outside world height limits"));

		var tooHigh = ShhhBuilderCommand.validateCoordinates(0, 320, 0, minH, maxH);
		assertFalse(tooHigh.valid());
		assertTrue(tooHigh.error().contains("Y coordinate 320 is outside world height limits"));

		// Out of horizontal world borders (±30,000,000)
		var outOfX = ShhhBuilderCommand.validateCoordinates(30_000_001, 64, 0, minH, maxH);
		assertFalse(outOfX.valid());
		assertTrue(outOfX.error().contains("outside the world limits"));

		var outOfZ = ShhhBuilderCommand.validateCoordinates(0, 64, -30_000_001, minH, maxH);
		assertFalse(outOfZ.valid());
		assertTrue(outOfZ.error().contains("outside the world limits"));
	}

	@Test
	@DisplayName("validateLayer parses 'all' and layer numbers within valid range")
	void testValidateLayer() {
		int maxHeight = 5;

		// "all" mode
		var resAll = ShhhBuilderCommand.validateLayer("all", maxHeight);
		assertTrue(resAll.valid());
		assertTrue(resAll.isAll());
		assertNull(resAll.error());

		var resAllUpper = ShhhBuilderCommand.validateLayer("ALL", maxHeight);
		assertTrue(resAllUpper.valid());
		assertTrue(resAllUpper.isAll());

		// Valid layer numbers
		var res1 = ShhhBuilderCommand.validateLayer("1", maxHeight);
		assertTrue(res1.valid());
		assertFalse(res1.isAll());
		assertEquals(1, res1.layerNumber());

		var res5 = ShhhBuilderCommand.validateLayer("5", maxHeight);
		assertTrue(res5.valid());
		assertEquals(5, res5.layerNumber());

		// Out of range layer numbers
		var res0 = ShhhBuilderCommand.validateLayer("0", maxHeight);
		assertFalse(res0.valid());
		assertTrue(res0.error().contains("Layer 0 is out of range"));

		var res6 = ShhhBuilderCommand.validateLayer("6", maxHeight);
		assertFalse(res6.valid());
		assertTrue(res6.error().contains("Layer 6 is out of range"));

		// Non-number argument
		var resInvalid = ShhhBuilderCommand.validateLayer("half", maxHeight);
		assertFalse(resInvalid.valid());
		assertTrue(resInvalid.error().contains("Invalid layer: 'half'"));

		// Blank argument
		var resBlank = ShhhBuilderCommand.validateLayer("", maxHeight);
		assertFalse(resBlank.valid());
		assertTrue(resBlank.error().contains("/shhhbuilder layer <all|number>"));
	}

	@Test
	@DisplayName("ShhhBuilderCommand registers all required subcommands on the dispatcher")
	void testCommandTreeRegistration() {
		CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
		ShhhBuilderCommand.register(dispatcher);

		CommandNode<FabricClientCommandSource> root = dispatcher.getRoot().getChild("shhhbuilder");
		assertNotNull(root, "Root command /shhhbuilder must be registered");

		// Subcommands: help, list, select, place, move, rotate, flip, layer, clear, folder, selftest
		String[] requiredSubcommands = {
			"help", "list", "select", "place", "move", "rotate", "flip", "layer", "clear", "folder", "selftest"
		};

		for (String sub : requiredSubcommands) {
			CommandNode<FabricClientCommandSource> child = root.getChild(sub);
			assertNotNull(child, "Subcommand /shhhbuilder " + sub + " must be registered");
		}

		// select <file>
		CommandNode<FabricClientCommandSource> selectNode = root.getChild("select");
		assertNotNull(selectNode.getChild("file"), "select must have 'file' argument child");

		// place <x> <y> <z>
		CommandNode<FabricClientCommandSource> placeNode = root.getChild("place");
		CommandNode<FabricClientCommandSource> placeX = placeNode.getChild("x");
		assertNotNull(placeX, "place must have 'x' argument child");
		CommandNode<FabricClientCommandSource> placeY = placeX.getChild("y");
		assertNotNull(placeY, "place x must have 'y' argument child");
		CommandNode<FabricClientCommandSource> placeZ = placeY.getChild("z");
		assertNotNull(placeZ, "place x y must have 'z' argument child");

		// move <x> <y> <z> (dx dy dz)
		CommandNode<FabricClientCommandSource> moveNode = root.getChild("move");
		CommandNode<FabricClientCommandSource> moveDx = moveNode.getChild("dx");
		assertNotNull(moveDx, "move must have 'dx' argument child");
		CommandNode<FabricClientCommandSource> moveDy = moveDx.getChild("dy");
		assertNotNull(moveDy, "move dx must have 'dy' argument child");
		CommandNode<FabricClientCommandSource> moveDz = moveDy.getChild("dz");
		assertNotNull(moveDz, "move dx dy must have 'dz' argument child");

		// layer <all|number>
		CommandNode<FabricClientCommandSource> layerNode = root.getChild("layer");
		assertNotNull(layerNode.getChild("layer"), "layer must have 'layer' argument child");
	}
}
