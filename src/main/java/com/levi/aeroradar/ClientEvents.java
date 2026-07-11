package com.levi.aeroradar;

import com.levi.aeroradar.client.ClientShipData;
import com.levi.aeroradar.compat.JourneyMapCompat;
import com.levi.aeroradar.compat.XaeroMinimapCompat;
import com.levi.aeroradar.compat.XaeroWorldMapCompat;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = AeroRadar.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    /** Guards so the compat classes are only classloaded when the map mods exist. */
    private static final boolean MINIMAP_LOADED = ModList.get().isLoaded("xaerominimap");
    private static final boolean WORLDMAP_LOADED = ModList.get().isLoaded("xaeroworldmap");
    private static final boolean JOURNEYMAP_LOADED = ModList.get().isLoaded("journeymap");

    private static boolean wasInWorld;

    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inWorld = mc.level != null && mc.player != null;
        if (!inWorld) {
            if (wasInWorld) leaveWorld();
            wasInWorld = false;
            return;
        }
        wasInWorld = true;

        // One-time renderer registration; Xaero constructs its handlers lazily.
        if (MINIMAP_LOADED && XaeroMinimapCompat.tryRegister() && WORLDMAP_LOADED) {
            XaeroWorldMapCompat.tryRegister();
        }

        ParkedStore.ensureLoaded(mc);
        ShipTracker.tick(mc);
        if (JOURNEYMAP_LOADED) JourneyMapCompat.refresh(mc);
        ParkedStore.tickSave();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        leaveWorld();
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        RadarCommands.register(event.getDispatcher());
    }

    private static void leaveWorld() {
        // Xaero's element renderer reads live tracker state, so clearing the
        // trackers covers it; JourneyMap holds overlay objects that need removing.
        if (JOURNEYMAP_LOADED) JourneyMapCompat.clearAll();
        ShipTracker.reset();
        ClientShipData.reset();
        ParkedStore.onWorldLeave();
    }
}
