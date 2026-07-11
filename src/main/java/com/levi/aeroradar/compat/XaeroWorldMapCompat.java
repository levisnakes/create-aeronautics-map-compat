package com.levi.aeroradar.compat;

import xaero.common.HudMod;
import xaero.map.WorldMap;
import xaero.map.mods.minimap.element.MinimapElementRendererWrapper;

/**
 * Puts the SAME ship element renderer on Xaero's fullscreen World Map, using the
 * wrapper Xaero itself uses to show the minimap radar there. Only classloaded when
 * "xaeroworldmap" is installed (guarded in ClientEvents).
 */
public final class XaeroWorldMapCompat {

    private static boolean registered;

    private XaeroWorldMapCompat() {}

    /** Called each client tick until registration succeeds (after the minimap side). */
    public static boolean tryRegister() {
        if (registered) return true;
        if (WorldMap.mapElementRenderHandler == null || HudMod.INSTANCE == null) return false;
        WorldMap.mapElementRenderHandler.add(
                MinimapElementRendererWrapper.Builder
                        .begin(XaeroMinimapCompat.getRenderer())
                        .setModMain(HudMod.INSTANCE)
                        .setShouldRenderSupplier(() -> true)
                        .setOrder(1)
                        .build());
        registered = true;
        return true;
    }
}
