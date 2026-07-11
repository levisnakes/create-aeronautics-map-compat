package com.levi.aeroradar;

import com.levi.aeroradar.net.Network;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Common entry point. The mod loads on both sides: the client draws the map markers,
 * the (optional) server side broadcasts a shared, persistent ship registry. Neither
 * side is required for the other to work.
 */
@Mod(AeroRadar.MODID)
public final class AeroRadar {
    public static final String MODID = "aeroradar";

    public AeroRadar(IEventBus modBus, ModContainer container) {
        modBus.addListener(Network::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
