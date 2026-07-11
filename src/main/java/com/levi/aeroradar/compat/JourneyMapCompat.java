package com.levi.aeroradar.compat;

import com.levi.aeroradar.AeroRadar;
import com.levi.aeroradar.ShipTracker;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.TextProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JourneyMap integration: ships as MarkerOverlays (fullscreen map + JM minimap +
 * webmap). Marker overlays are map-view-only by design - no JM waypoints are
 * created, nothing renders in the world, nothing is saved by JourneyMap.
 *
 * JourneyMap discovers this class itself through the @JourneyMapPlugin annotation
 * scan and only instantiates it when JM is installed; the rest of the mod talks to
 * it through the static methods, guarded by ModList.isLoaded("journeymap").
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapCompat implements IClientPlugin {

    private static final ResourceLocation SHIP_ICON =
            ResourceLocation.fromNamespaceAndPath(AeroRadar.MODID, "textures/map/ship.png");
    private static final ResourceLocation PARKED_ICON =
            ResourceLocation.fromNamespaceAndPath(AeroRadar.MODID, "textures/map/ship_parked.png");

    private static JourneyMapCompat instance;

    private IClientAPI api;
    private final Map<UUID, Entry> markers = new HashMap<>();
    private ResourceKey<Level> markersDimension;

    private record Entry(MarkerOverlay overlay, boolean parked) {}

    @Override
    public String getModId() {
        return AeroRadar.MODID;
    }

    @Override
    public void initialize(IClientAPI clientApi) {
        this.api = clientApi;
        instance = this;
    }

    /** Called every client tick while in a world (guarded by isLoaded("journeymap")). */
    public static void refresh(Minecraft mc) {
        if (instance != null) instance.update(mc);
    }

    /** Called on logout/world leave. */
    public static void clearAll() {
        if (instance != null) instance.clear();
    }

    private void update(Minecraft mc) {
        if (api == null || mc.level == null) return;
        ResourceKey<Level> dim = mc.level.dimension();
        if (markersDimension != null && markersDimension != dim) {
            clear(); // markers carry a dimension; rebuild cleanly after dimension change
        }
        markersDimension = dim;

        Set<UUID> wanted = new HashSet<>();
        for (ShipTracker.Target t : ShipTracker.mapTargets()) {
            wanted.add(t.id());
            int color = t.color() & 0xFFFFFF;
            BlockPos pos = BlockPos.containing(t.pos().x, t.pos().y, t.pos().z);
            Entry entry = markers.get(t.id());

            if (entry != null && entry.parked() != t.parked()) {
                // Icon texture changes between live/parked - recreate the marker.
                api.remove(entry.overlay());
                markers.remove(t.id());
                entry = null;
            }

            if (entry == null) {
                MapImage icon = new MapImage(t.parked() ? PARKED_ICON : SHIP_ICON, 32, 32)
                        .setColor(color)
                        .centerAnchors();
                MarkerOverlay overlay = new MarkerOverlay(AeroRadar.MODID, pos, icon);
                overlay.setDimension(dim)
                        .setLabel(t.name())
                        .setTitle(t.name())
                        .setOverlayGroupName("Ships")
                        .setTextProperties(new TextProperties()
                                .setColor(0xFFFFFF)
                                .setBackgroundColor(0x000000)
                                .setBackgroundOpacity(0.6f)
                                .setFontShadow(true)
                                .setScale(1.0f)
                                .setMinZoom(1));
                try {
                    api.show(overlay);
                    markers.put(t.id(), new Entry(overlay, t.parked()));
                } catch (Exception ignored) {
                    // JM can reject overlays while it (re)loads; retried next tick.
                }
            } else {
                MarkerOverlay overlay = entry.overlay();
                boolean dirty = false;
                if (!pos.equals(overlay.getPoint())) {
                    overlay.setPoint(pos);
                    dirty = true;
                }
                if (!t.name().equals(overlay.getLabel())) {
                    overlay.setLabel(t.name());
                    overlay.setTitle(t.name());
                    dirty = true;
                }
                if (overlay.getIcon().getColor() != color) {
                    overlay.getIcon().setColor(color);
                    dirty = true;
                }
                if (dirty) overlay.flagForRerender();
            }
        }

        Iterator<Map.Entry<UUID, Entry>> it = markers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Entry> e = it.next();
            if (!wanted.contains(e.getKey())) {
                api.remove(e.getValue().overlay());
                it.remove();
            }
        }
    }

    private void clear() {
        if (api != null) {
            try {
                api.removeAll(AeroRadar.MODID);
            } catch (Throwable ignored) {}
        }
        markers.clear();
        markersDimension = null;
    }
}
