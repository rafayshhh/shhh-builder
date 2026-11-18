package io.github.rafayshhh.shhhbuilder;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShhhBuilder {
	public static final String MOD_ID = "shhh-builder";
	public static final String MOD_NAME = "Shhh Builder";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
