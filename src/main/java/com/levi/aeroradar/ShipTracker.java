package com.levi.aeroradar;

import com.levi.aeroradar.client.ClientShipData;
import com.levi.aeroradar.net.ShipInfo;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side ship tracking. Enumerates the ships loaded near the player (via Sable
 * sub-levels), detects boarding/leaving, and merges in the server-synced registry
 * (when the server runs AeroRadar) so far-away and other players' ships also appear.
 */
public final class ShipTracker {

    public record LiveShip(UUID id, Vector3d center, BoundingBox3dc bounds, String sableName) {}

    /** A resolved map/list entry. */
    public record Target(UUID id, String name, Vec3 pos, boolean live, boolean parked, boolean pinned, boolean aboard) {
        public int color() {
            if (aboard || pinned) return 0xFF55FF55;
            if (parked) return 0xFFFFAA00;
            return 0xFF55FFFF;
        }
    }

    private static final Map<UUID, LiveShip> LIVE = new LinkedHashMap<>();
    private static final List<ShipClustering.Member> MEMBERS = new ArrayList<>();
    private static volatile List<Target> mapTargets = List.of();
    private static UUID aboardId;
    private static int tickCount;

    private ShipTracker() {}

    // ---- per-tick update ----

    public static void tick(Minecraft mc) {
        tickCount++;
        rebuildLive(mc);
        updateAboard(mc);
        rebuildMapTargets(mc);
    }

    /** Snapshot consumed by the Xaero element renderer every frame. */
    public static List<Target> mapTargets() {
        return mapTargets;
    }

    private static void rebuildMapTargets(Minecraft mc) {
        List<Target> out = new ArrayList<>();
        boolean hideAboard = Config.HIDE_SHIP_YOU_ARE_ON.get();
        boolean live = Config.MAP_LIVE_SHIPS.get();
        boolean parked = Config.MAP_PARKED_SHIPS.get();
        for (Target t : allTargets(mc)) {
            if (t.aboard() && hideAboard) continue;
            if (t.parked() && !parked) continue;
            if (!t.parked() && !live) continue;
            out.add(t);
        }
        mapTargets = out;
    }

    private static void rebuildLive(Minecraft mc) {
        LIVE.clear();
        MEMBERS.clear();
        List<? extends SubLevel> subs = SubLevelContainer.getContainer(mc.level).getAllSubLevels();
        ShipClustering.Result result = ShipClustering.cluster(subs, 1.0);
        for (ShipClustering.Cluster c : result.clusters()) {
            LIVE.put(c.repId(), new LiveShip(c.repId(), c.center(), c.bounds(), c.name()));
        }
        MEMBERS.addAll(result.members());
    }

    private static void updateAboard(Minecraft mc) {
        UUID prev = aboardId;
        aboardId = detectAboard(mc);

        if (aboardId != null) {
            if (Config.AUTO_PARK.get() && (tickCount % 20 == 0 || !aboardId.equals(prev))) {
                LiveShip ship = LIVE.get(aboardId);
                if (ship != null) ParkedStore.park(aboardId, ship.center(), dimKey(mc));
            }
        } else if (prev != null && Config.AUTO_PARK.get()) {
            LiveShip ship = LIVE.get(prev);
            if (ship != null) {
                ParkedStore.park(prev, ship.center(), dimKey(mc));
                mc.player.displayClientMessage(Component.literal(
                        "⚓ Parked: " + resolveName(prev)), true);
            }
        }
    }

    private static UUID detectAboard(Minecraft mc) {
        try {
            SubLevel containing = Sable.HELPER.getContainingClient(mc.player);
            if (containing != null && containing.getUniqueId() != null) {
                UUID rep = representativeOf(containing.getUniqueId());
                return rep != null ? rep : containing.getUniqueId();
            }
        } catch (Throwable ignored) {
            // fall through to geometry
        }
        Vec3 p = mc.player.position();
        for (ShipClustering.Member m : MEMBERS) {
            BoundingBox3dc bb = m.bounds();
            if (p.x >= bb.minX() - 1.0 && p.x <= bb.maxX() + 1.0
                    && p.z >= bb.minZ() - 1.0 && p.z <= bb.maxZ() + 1.0
                    && p.y >= bb.minY() - 1.0 && p.y <= bb.maxY() + 2.5) {
                return m.repId();
            }
        }
        return null;
    }

    public static void reset() {
        LIVE.clear();
        MEMBERS.clear();
        mapTargets = List.of();
        aboardId = null;
        tickCount = 0;
    }

    // ---- queries ----

    public static Map<UUID, LiveShip> live() {
        return LIVE;
    }

    public static UUID aboardId() {
        return aboardId;
    }

    public static String dimKey(Minecraft mc) {
        return mc.level.dimension().location().toString();
    }

    public static Vec3 effectivePlayerPos(Minecraft mc) {
        if (aboardId != null) {
            LiveShip s = LIVE.get(aboardId);
            if (s != null) return new Vec3(s.center().x, s.center().y, s.center().z);
        }
        return mc.player.position();
    }

    public static String resolveName(UUID id) {
        ParkedStore.Entry e = ParkedStore.get(id);
        if (e != null && e.name != null && !e.name.isBlank()) return e.name;
        LiveShip s = LIVE.get(id);
        if (s != null && s.sableName() != null && !s.sableName().isBlank()) return s.sableName();
        ShipInfo si = ClientShipData.shipsFor(dimKey(Minecraft.getInstance())).get(id);
        if (si != null && si.name() != null && !si.name().isBlank()) return si.name();
        return "Ship-" + id.toString().substring(0, 4);
    }

    /** Kept for callers that already have the sable name in hand. */
    public static String resolveName(UUID id, String sableName) {
        ParkedStore.Entry e = ParkedStore.get(id);
        if (e != null && e.name != null && !e.name.isBlank()) return e.name;
        if (sableName != null && !sableName.isBlank()) return sableName;
        return resolveName(id);
    }

    /** Resolve one ship id to the best current target across all sources. */
    public static Target resolve(Minecraft mc, UUID id) {
        UUID pinned = ParkedStore.pinnedId();
        LiveShip s = LIVE.get(id);
        if (s != null) {
            return new Target(id, resolveName(id, s.sableName()),
                    new Vec3(s.center().x, s.center().y, s.center().z),
                    true, false, id.equals(pinned), id.equals(aboardId));
        }
        ShipInfo si = ClientShipData.shipsFor(dimKey(mc)).get(id);
        if (si != null) {
            return new Target(id, resolveName(id, si.name()), new Vec3(si.x(), si.y(), si.z()),
                    si.loaded(), !si.loaded(), id.equals(pinned), false);
        }
        ParkedStore.Entry e = ParkedStore.get(id);
        if (e != null && e.hasPos && dimKey(mc).equals(e.dim)) {
            return new Target(id, resolveName(id), new Vec3(e.x, e.y, e.z),
                    false, true, id.equals(pinned), false);
        }
        return null;
    }

    /** Every ship we can point at in the current dimension: local + server + parked, nearest first. */
    public static List<Target> allTargets(Minecraft mc) {
        String dim = dimKey(mc);
        Vec3 eye = effectivePlayerPos(mc);
        Map<UUID, Target> out = new LinkedHashMap<>();
        for (UUID id : LIVE.keySet()) put(mc, out, id);
        if (Config.USE_SERVER_SHIPS.get()) {
            for (UUID id : ClientShipData.shipsFor(dim).keySet()) put(mc, out, id);
        }
        for (String key : ParkedStore.all().keySet()) {
            UUID id;
            try { id = UUID.fromString(key); } catch (IllegalArgumentException ex) { continue; }
            ParkedStore.Entry e = ParkedStore.get(id);
            if (e == null || !e.hasPos || !dim.equals(e.dim)) continue;
            put(mc, out, id);
        }
        List<Target> list = new ArrayList<>(out.values());
        list.sort(Comparator.comparingDouble(t -> t.pos().distanceToSqr(eye)));
        return list;
    }

    private static void put(Minecraft mc, Map<UUID, Target> out, UUID id) {
        if (out.containsKey(id)) return;
        Target t = resolve(mc, id);
        if (t != null) out.put(id, t);
    }

    /** Best ship to act on for commands: the one you're on, else the nearest live one. */
    public static UUID contextShip(Minecraft mc, double maxDistance) {
        if (aboardId != null) return aboardId;
        Vec3 eye = mc.player.position();
        UUID best = null;
        double bestSq = maxDistance * maxDistance;
        for (LiveShip s : LIVE.values()) {
            double d = eye.distanceToSqr(s.center().x, s.center().y, s.center().z);
            if (d < bestSq) { bestSq = d; best = s.id(); }
        }
        return best;
    }

    public static String compassDir(double dx, double dz) {
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        int idx = Math.floorMod(Math.round(yaw / 45.0f), 8);
        return new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"}[idx];
    }

    private static UUID representativeOf(UUID memberId) {
        if (LIVE.containsKey(memberId)) return memberId;
        for (ShipClustering.Member m : MEMBERS) {
            if (m.id().equals(memberId)) return m.repId();
        }
        return null;
    }
}
