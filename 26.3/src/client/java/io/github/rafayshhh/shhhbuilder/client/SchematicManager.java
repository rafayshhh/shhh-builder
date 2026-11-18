package io.github.rafayshhh.shhhbuilder.client;

import com.mojang.blaze3d.Blaze3D;
import io.github.rafayshhh.shhhbuilder.ShhhBuilder;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicData;
import io.github.rafayshhh.shhhbuilder.client.schematic.LitematicReader;
import io.github.rafayshhh.shhhbuilder.client.schematic.SchemReader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SchematicManager {
	private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "ShhhBuilder-Reader");
		t.setDaemon(true);
		return t;
	});

	private static final Map<Path, CachedSchematic> CACHE = new ConcurrentHashMap<>();
	private static final int MAX_CACHE_ENTRIES = 50;
	private static volatile String selectedFileName = null;

	private static void putCache(Path path, CachedSchematic entry) {
		if (path == null || entry == null) return;
		if (CACHE.size() >= MAX_CACHE_ENTRIES && !CACHE.containsKey(path)) {
			var it = CACHE.keySet().iterator();
			if (it.hasNext()) {
				CACHE.remove(it.next());
			}
		}
		CACHE.put(path, entry);
	}

	public static class CachedSchematic {
		public final long lastModified;
		public final long fileSize;
		public final LitematicData data;
		public final String error;

		public CachedSchematic(long lastModified, long fileSize, LitematicData data, String error) {
			this.lastModified = lastModified;
			this.fileSize = fileSize;
			this.data = data;
			this.error = error;
		}
	}

	public static Path getSchematicsDirectory() {
		Path gameDir = null;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null && mc.gameDirectory != null) {
				gameDir = mc.gameDirectory.toPath();
			}
		} catch (Throwable ignored) {
		}
		if (gameDir == null) {
			try {
				gameDir = FabricLoader.getInstance().getGameDir();
			} catch (Throwable ignored) {
				gameDir = Path.of(".");
			}
		}
		return gameDir.resolve("schematics").toAbsolutePath().normalize();
	}

	public static boolean openSchematicsFolder() {
		Path dir = getSchematicsDirectory();
		try {
			if (!Files.exists(dir)) {
				Files.createDirectories(dir);
			}
			Blaze3D.openPath(dir);
			return true;
		} catch (Exception e) {
			ShhhBuilder.LOGGER.error("Failed to open schematics directory: {}", dir, e);
			return false;
		}
	}

	public static String getSelectedFileName() {
		return selectedFileName;
	}

	public static void setSelectedFileName(String fileName) {
		selectedFileName = fileName;
		PlacementManager.invalidateBlockCache();
	}

	public static boolean hasLoadedSchematic() {
		return getSelectedSchematicData() != null;
	}

	public static LitematicData getLoadedData() {
		return getSelectedSchematicData();
	}

	/**
	 * Returns the parsed LitematicData for the currently selected schematic file,
	 * or null if none is selected or not yet loaded.
	 */
	public static LitematicData getSelectedSchematicData() {
		if (selectedFileName == null || selectedFileName.isBlank()) {
			return null;
		}

		Path dir = getSchematicsDirectory();
		Path path = dir.resolve(selectedFileName).normalize();
		if (!path.startsWith(dir)) {
			ShhhBuilder.LOGGER.warn("Rejected path traversal attempt in selected file: {}", selectedFileName);
			return null;
		}

		CachedSchematic cached = getCached(path);
		if (cached != null) {
			return cached.data;
		}

		// If exists on disk but not yet loaded into cache, trigger async load
		if (Files.exists(path) && (selectedFileName.toLowerCase(Locale.ROOT).endsWith(".litematic") || selectedFileName.toLowerCase(Locale.ROOT).endsWith(".schem"))) {
			loadSchematicAsync(path);
		}

		return null;
	}

	public static void clearCache() {
		CACHE.clear();
	}

	/**
	 * Returns cached data for the given path if available and still fresh on disk, or null.
	 */
	public static CachedSchematic getCached(Path path) {
		if (path == null) {
			return null;
		}
		CachedSchematic cached = CACHE.get(path);
		if (cached != null) {
			try {
				if (Files.exists(path)
					&& Files.getLastModifiedTime(path).toMillis() == cached.lastModified
					&& Files.size(path) == cached.fileSize) {
					return cached;
				}
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	/**
	 * Asynchronously loads a .litematic file on a background thread.
	 */
	public static CompletableFuture<CachedSchematic> loadSchematicAsync(Path path) {
		Path dir = getSchematicsDirectory();
		Path normalized = (path != null) ? path.toAbsolutePath().normalize() : null;
		if (normalized == null || !normalized.startsWith(dir)) {
			return CompletableFuture.completedFuture(
				new CachedSchematic(0, 0, null, "Security error: file path is outside schematics directory.")
			);
		}

		return CompletableFuture.supplyAsync(() -> {
			try {
				if (!Files.exists(path)) {
					return new CachedSchematic(0, 0, null, "File does not exist: " + path.getFileName());
				}
				long lastModified = Files.getLastModifiedTime(path).toMillis();
				long fileSize = Files.size(path);

				CachedSchematic cached = CACHE.get(path);
				if (cached != null && cached.lastModified == lastModified && cached.fileSize == fileSize) {
					return cached;
				}

				String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
				LitematicData data;
				if (lower.endsWith(".schem")) {
					data = SchemReader.read(path);
				} else {
					data = LitematicReader.read(path);
				}
				CachedSchematic result = new CachedSchematic(lastModified, fileSize, data, null);
				putCache(path, result);
				return result;
			} catch (Exception e) {
				long lastModified = 0;
				long fileSize = 0;
				try {
					lastModified = Files.getLastModifiedTime(path).toMillis();
					fileSize = Files.size(path);
				} catch (Exception ignored) {
				}

				String msg = e.getMessage() != null ? e.getMessage() : "Error reading schematic file.";
				CachedSchematic result = new CachedSchematic(lastModified, fileSize, null, msg);
				putCache(path, result);
				return result;
			}
		}, IO_EXECUTOR);
	}

	public static List<SchematicFileEntry> scanSchematics(ErrorConsumer errorConsumer) {
		Path dir = getSchematicsDirectory();
		boolean exists = Files.exists(dir);
		List<String> allFoundNames = new ArrayList<>();
		List<SchematicFileEntry> results = new ArrayList<>();

		if (!exists) {
			try {
				Files.createDirectories(dir);
				exists = Files.exists(dir);
			} catch (Exception e) {
				ShhhBuilder.LOGGER.error("Failed to create schematics folder: {}", dir, e);
				if (errorConsumer != null) {
					errorConsumer.onError("Could not create schematics folder: " + e.getMessage());
				}
				ShhhBuilder.LOGGER.info("[Schematics Scan] Folder: {} | Exists: false (create failed: {}) | All files found (0): [] | Accepted (0): []",
					dir, e.getMessage());
				return results;
			}
		}

		if (exists) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
				for (Path path : stream) {
					String name = path.getFileName().toString();
					allFoundNames.add(name);

					// Check if regular file or symlink to file (guards against unusual filesystem flags on Windows)
					boolean isFile = Files.isRegularFile(path) || (!Files.isDirectory(path) && Files.exists(path));
					if (isFile) {
						String lower = name.trim().toLowerCase(Locale.ROOT);
						if (lower.endsWith(".litematic") || lower.endsWith(".schem")) {
							results.add(new SchematicFileEntry(path, name));
						}
					}
				}
			} catch (Exception e) {
				ShhhBuilder.LOGGER.error("Failed to read schematics folder: {}", dir, e);
				if (errorConsumer != null) {
					errorConsumer.onError("Could not read schematics folder: " + e.getMessage());
				}
			}
		}

		results.sort(Comparator.comparing(SchematicFileEntry::fileName, String.CASE_INSENSITIVE_ORDER));

		List<String> acceptedNames = results.stream().map(SchematicFileEntry::fileName).toList();
		ShhhBuilder.LOGGER.info("[Schematics Scan] Folder: {} | Exists: {} | All files found ({}): {} | Accepted ({}): {}",
			dir, exists, allFoundNames.size(), allFoundNames, results.size(), acceptedNames);

		return results;
	}

	@FunctionalInterface
	public interface ErrorConsumer {
		void onError(String message);
	}
}
