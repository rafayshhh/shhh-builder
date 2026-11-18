package io.github.rafayshhh.shhhbuilder.client;

/**
 * Defines the layer filtering mode for in-world ghost block rendering:
 * - ALL: show all layers (default)
 * - SINGLE: show only the active Y layer
 * - UP_TO: show the active Y layer and all layers below it
 */
public enum LayerMode {
	ALL("All layers"),
	SINGLE("One layer"),
	UP_TO("Up to layer");

	private final String displayName;

	LayerMode(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public LayerMode next() {
		LayerMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
