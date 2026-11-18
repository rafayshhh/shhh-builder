package io.github.rafayshhh.shhhbuilder.client.material;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages persistent user checklist progress (checked items) per schematic.
 * Saves to config/shhh-builder-progress.json with atomic writes, corruption recovery,
 * and a 50-schematic LRU size limit.
 */
public class MaterialProgressManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int MAX_SCHEMATICS = 50;
	private static final Map<String, SchematicProgressEntry> PROGRESS_MAP = new ConcurrentHashMap<>();
	private static boolean loaded = false;

	static {
		try {
			Runtime.getRuntime().addShutdownHook(new Thread(MaterialProgressManager::flush, "ShhhBuilder-ProgressFlush"));
		} catch (Throwable ignored) {
		}
	}

	public static class SchematicProgressEntry {
		public long lastAccessedMs;
		public Set<String> checkedItems;

		public SchematicProgressEntry() {
			this.lastAccessedMs = System.currentTimeMillis();
			this.checkedItems = new HashSet<>();
		}

		public SchematicProgressEntry(Set<String> items) {
			this.lastAccessedMs = System.currentTimeMillis();
			this.checkedItems = new HashSet<>(items);
		}
	}

	public static String buildKey(String fileName, long fileSize, long lastModified) {
		if (fileName == null) fileName = "unknown";
		return fileName + ":" + fileSize + ":" + lastModified;
	}

	public static synchronized boolean isChecked(String key, String itemId) {
		ensureLoaded();
		if (key == null || itemId == null) return false;
		SchematicProgressEntry entry = PROGRESS_MAP.get(key);
		return entry != null && entry.checkedItems.contains(itemId);
	}

	public static synchronized Set<String> getCheckedItems(String key) {
		ensureLoaded();
		if (key == null) return Collections.emptySet();
		SchematicProgressEntry entry = PROGRESS_MAP.get(key);
		if (entry == null) return Collections.emptySet();
		entry.lastAccessedMs = System.currentTimeMillis();
		return Collections.unmodifiableSet(entry.checkedItems);
	}

	public static synchronized void setItemChecked(String key, String itemId, boolean checked) {
		ensureLoaded();
		if (key == null || itemId == null) return;
		SchematicProgressEntry entry = PROGRESS_MAP.computeIfAbsent(key, k -> new SchematicProgressEntry());
		entry.lastAccessedMs = System.currentTimeMillis();
		if (checked) {
			entry.checkedItems.add(itemId);
		} else {
			entry.checkedItems.remove(itemId);
		}
		save();
	}

	public static synchronized void setChecked(String key, String itemId, boolean checked) {
		setItemChecked(key, itemId, checked);
	}

	public static synchronized boolean toggleItem(String key, String itemId) {
		ensureLoaded();
		if (key == null || itemId == null) return false;
		SchematicProgressEntry entry = PROGRESS_MAP.computeIfAbsent(key, k -> new SchematicProgressEntry());
		entry.lastAccessedMs = System.currentTimeMillis();
		boolean nowChecked;
		if (entry.checkedItems.contains(itemId)) {
			entry.checkedItems.remove(itemId);
			nowChecked = false;
		} else {
			entry.checkedItems.add(itemId);
			nowChecked = true;
		}
		save();
		return nowChecked;
	}

	public static synchronized void checkAll(String key, Collection<String> itemIds) {
		ensureLoaded();
		if (key == null || itemIds == null) return;
		SchematicProgressEntry entry = PROGRESS_MAP.computeIfAbsent(key, k -> new SchematicProgressEntry());
		entry.lastAccessedMs = System.currentTimeMillis();
		entry.checkedItems.addAll(itemIds);
		save();
	}

	public static synchronized void uncheckAll(String key) {
		ensureLoaded();
		if (key == null) return;
		SchematicProgressEntry entry = PROGRESS_MAP.get(key);
		if (entry != null) {
			entry.lastAccessedMs = System.currentTimeMillis();
			entry.checkedItems.clear();
			save();
		}
	}

	public static synchronized void resetProgress(String key) {
		ensureLoaded();
		if (key == null) return;
		PROGRESS_MAP.remove(key);
		save();
	}

	private static Path getConfigFile() {
		Path configDir = null;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null && mc.gameDirectory != null) {
				configDir = mc.gameDirectory.toPath().resolve("config");
			}
		} catch (Throwable ignored) {
		}
		if (configDir == null) {
			try {
				configDir = FabricLoader.getInstance().getConfigDir();
			} catch (Throwable ignored) {
				configDir = Path.of("config");
			}
		}
		return configDir.resolve("shhh-builder-progress.json");
	}

	private static synchronized void ensureLoaded() {
		if (loaded) return;
		loaded = true;

		Path file = getConfigFile();
		if (!Files.exists(file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(file)) {
			Type type = new TypeToken<Map<String, SchematicProgressEntry>>() {}.getType();
			Map<String, SchematicProgressEntry> data = GSON.fromJson(reader, type);
			if (data != null) {
				PROGRESS_MAP.putAll(data);
			}
		} catch (Throwable t) {
			ShhhBuilder.LOGGER.warn("Failed to load material progress file, creating backup and starting fresh: {}", t.getMessage());
			try {
				Path backup = file.resolveSibling(file.getFileName().toString() + ".corrupted");
				Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
			} catch (Throwable ignored) {
			}
			PROGRESS_MAP.clear();
		}
	}

	/**
	 * Flushes all pending checklist progress to disk immediately using atomic file replacement.
	 * Safe to call repeatedly during world-switch, disconnect, or game shutdown.
	 */
	public static synchronized void flush() {
		if (loaded) {
			save();
		}
	}

	private static synchronized void save() {
		Path file = getConfigFile();
		try {
			Path parent = file.getParent();
			if (parent != null && !Files.exists(parent)) {
				Files.createDirectories(parent);
			}

			// Enforce 50-schematic size limit
			if (PROGRESS_MAP.size() > MAX_SCHEMATICS) {
				List<Map.Entry<String, SchematicProgressEntry>> sorted = new ArrayList<>(PROGRESS_MAP.entrySet());
				sorted.sort(Comparator.comparingLong(e -> e.getValue().lastAccessedMs));
				while (sorted.size() > MAX_SCHEMATICS) {
					PROGRESS_MAP.remove(sorted.remove(0).getKey());
				}
			}

			Path tempFile = file.resolveSibling(file.getFileName().toString() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tempFile)) {
				GSON.toJson(PROGRESS_MAP, writer);
			}

			try {
				Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (Throwable fallback) {
				Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Throwable t) {
			ShhhBuilder.LOGGER.error("Failed to save material progress file: {}", t.getMessage(), t);
		}
	}
}
