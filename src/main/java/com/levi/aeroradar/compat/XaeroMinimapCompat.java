package com.levi.aeroradar.compat;

import xaero.common.HudMod;
import xaero.hud.minimap.Minimap;

/**
 * Registers the ship element renderer with Xaero's Minimap. Only classloaded when
 * "xaerominimap" is installed (guarded in ClientEvents).
 */
public final class XaeroMinimapCompat {

    private static ShipElementRenderer renderer;
    private static boolean registered;

    private XaeroMinimapCompat() {}

    /** Called each client tick until registration succeeds (Xaero inits lazily). */
    public static boolean tryRegister() {
        if (registered) return true;
        HudMod hudMod = HudMod.INSTANCE;
        if (hudMod == null) return false;
        Minimap minimap;
        try {
            minimap = hudMod.getMinimap();
        } catch (Throwable t) {
            return false;
        }
        if (minimap == null || minimap.getOverMapRendererHandler() == null) return false;
        minimap.getOverMapRendererHandler().add(getRenderer());
        registered = true;
        return true;
    }

    public static ShipElementRenderer getRenderer() {
        if (renderer == null) renderer = ShipElementRenderer.create();
        return renderer;
    }
}
