package com.levi.aeroradar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side per-world memory of ships: custom names, pin state and the last
 * position you were aboard each ship ("where I parked it"). Stored as JSON in
 * config/aeroradar/, one file per world/server, so it survives relogs and works
 * on servers without any server-side support.
 */
public final class ParkedStore {
    public static final class Entry {
        public String name;      // custom name given by the player, or null
        public boolean hasPos;   // whether x/y/z/dim below are valid
        public double x, y, z;
        public String dim;
        public long time;        // wall-clock millis of the last park update
        public boolean pinned;
    }

    private static final Logger LOG = LoggerFactory.getLogger("aeroradar");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Entry>>() {}.getType();

    private static Map<String, Entry> entries = new LinkedHashMap<>();
    private static String worldKey;
    private static boolean dirty;
    private static int saveCountdown;

    private ParkedStore() {}

    // ---- lifecycle ----

    public static void ensureLoaded(Minecraft mc) {
        String key = computeKey(mc);
        if (key == null || key.equals(worldKey)) return;
        saveNow();
        worldKey = key;
        entries = new LinkedHashMap<>();
        Path file = file();
        if (file != null && Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                Map<String, Entry> loaded = GSON.fromJson(r, MAP_TYPE);
                if (loaded != null) entries = loaded;
            } catch (Exception e) {
                LOG.warn("Could not read parked-ship file {}", file, e);
            }
        }
    }

    public static void onWorldLeave() {
        saveNow();
        worldKey = null;
        entries = new LinkedHashMap<>();
    }

    /** Called once per client tick; batches writes so we never save more than once per 5s. */
    public static void tickSave() {
        if (!dirty) return;
        if (++saveCountdown >= 100) saveNow();
    }

    public static void saveNow() {
        saveCountdown = 0;
        if (!dirty || worldKey == null) { dirty = false; return; }
        dirty = false;
        Path file = file();
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(entries, MAP_TYPE, w);
            }
        } catch (Exception e) {
            LOG.warn("Could not save parked-ship file {}", file, e);
        }
    }

    private static Path file() {
        if (worldKey == null) return null;
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("aeroradar").resolve(worldKey + ".json");
    }

    private static String computeKey(Minecraft mc) {
        if (mc.getSingleplayerServer() != null) {
            return "sp_" + sanitize(mc.getSingleplayerServer().getWorldData().getLevelName());
        }
        ServerData server = mc.getCurrentServer();
        if (server != null) return "mp_" + sanitize(server.ip);
        if (mc.getConnection() != null) return "mp_unknown";
        return null;
    }

    private static String sanitize(String s) {
        String clean = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return clean.length() > 60 ? clean.substring(0, 60) : clean;
    }

    // ---- data access ----

    public static Entry get(UUID id) {
        return entries.get(id.toString());
    }

    public static Map<String, Entry> all() {
        return entries;
    }

    public static void park(UUID id, Vector3d pos, String dim) {
        Entry e = entries.computeIfAbsent(id.toString(), k -> new Entry());
        e.hasPos = true;
        e.x = pos.x; e.y = pos.y; e.z = pos.z;
        e.dim = dim;
        e.time = System.currentTimeMillis();
        dirty = true;
    }

    public static void setName(UUID id, String name) {
        Entry e = entries.computeIfAbsent(id.toString(), k -> new Entry());
        e.name = name;
        dirty = true;
    }

    public static void setPinned(UUID id) {
        for (Entry e : entries.values()) e.pinned = false;
        Entry e = entries.computeIfAbsent(id.toString(), k -> new Entry());
        e.pinned = true;
        dirty = true;
    }

    public static void clearPin() {
        for (Entry e : entries.values()) e.pinned = false;
        dirty = true;
    }

    public static UUID pinnedId() {
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            if (e.getValue().pinned) {
                try { return UUID.fromString(e.getKey()); } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }

    public static void forget(UUID id) {
        Entry e = entries.get(id.toString());
        if (e == null) return;
        if (e.name != null) {
            // Keep the custom name, just drop the parked position.
            e.hasPos = false;
            e.pinned = false;
        } else {
            entries.remove(id.toString());
        }
        dirty = true;
    }
}
