package com.levi.aeroradar.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-dimension, server-persisted registry of ships: last-known position and the
 * shared custom name. Survives ships unloading and server restarts, so a parked
 * airship keeps its marker for every player until it moves again.
 */
public final class AeroRadarSavedData extends SavedData {

    public static final String ID = "aeroradar_ships";

    public static final class Rec {
        public String name;
        public double x, y, z;
        public long time;
    }

    private final Map<UUID, Rec> ships = new HashMap<>();

    public static Factory<AeroRadarSavedData> factory() {
        return new Factory<>(AeroRadarSavedData::new, AeroRadarSavedData::load, null);
    }

    public Map<UUID, Rec> ships() {
        return ships;
    }

    public void updatePosition(UUID id, String name, double x, double y, double z) {
        Rec r = ships.computeIfAbsent(id, k -> new Rec());
        r.x = x; r.y = y; r.z = z;
        r.time = System.currentTimeMillis();
        if (name != null && !name.isBlank() && (r.name == null || r.name.isBlank())) {
            r.name = name; // don't overwrite a player-given name with a default one
        }
        setDirty();
    }

    public void rename(UUID id, String name) {
        Rec r = ships.computeIfAbsent(id, k -> new Rec());
        r.name = name;
        if (r.time == 0) r.time = System.currentTimeMillis();
        setDirty();
    }

    private static AeroRadarSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        AeroRadarSavedData data = new AeroRadarSavedData();
        ListTag list = tag.getList("ships", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            try {
                Rec r = new Rec();
                r.name = c.contains("name") ? c.getString("name") : null;
                r.x = c.getDouble("x");
                r.y = c.getDouble("y");
                r.z = c.getDouble("z");
                r.time = c.getLong("time");
                data.ships.put(c.getUUID("id"), r);
            } catch (Exception ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Rec> e : ships.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            Rec r = e.getValue();
            if (r.name != null && !r.name.isBlank()) c.putString("name", r.name);
            c.putDouble("x", r.x);
            c.putDouble("y", r.y);
            c.putDouble("z", r.z);
            c.putLong("time", r.time);
            list.add(c);
        }
        tag.put("ships", list);
        return tag;
    }
}
