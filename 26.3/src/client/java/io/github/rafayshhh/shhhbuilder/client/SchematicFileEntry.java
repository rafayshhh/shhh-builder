package io.github.rafayshhh.shhhbuilder.client;

import java.nio.file.Path;

/**
 * Represents a schematic file (.litematic or .schem) discovered in the schematics folder.
 */
public record SchematicFileEntry(Path path, String fileName) {
	/**
	 * Returns the base name of the file without its extension.
	 * For example, "castle.litematic" returns "castle".
	 */
	public String baseName() {
		int dot = fileName.lastIndexOf('.');
		return dot > 0 ? fileName.substring(0, dot) : fileName;
	}

	/**
	 * Returns the file extension including the leading dot.
	 * For example, "castle.litematic" returns ".litematic".
	 */
	public String extension() {
		int dot = fileName.lastIndexOf('.');
		return dot >= 0 ? fileName.substring(dot) : "";
	}
}
