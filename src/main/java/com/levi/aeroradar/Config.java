package com.levi.aeroradar;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config (map behaviour). Only registered on the client. */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MAP_LIVE_SHIPS = BUILDER
            .comment("Show every loaded ship as a live marker on Xaero's Minimap / World Map.")
            .define("mapLiveShips", true);

    public static final ModConfigSpec.BooleanValue MAP_PARKED_SHIPS = BUILDER
            .comment("Keep a marker at each ship's last-known position, even after it unloads.")
            .define("mapParkedShips", true);

    public static final ModConfigSpec.BooleanValue USE_SERVER_SHIPS = BUILDER
            .comment("When the server runs AeroRadar, show ALL of its ships (including far-away and other players' ships).")
            .define("useServerShips", true);

    public static final ModConfigSpec.BooleanValue HIDE_SHIP_YOU_ARE_ON = BUILDER
            .comment("Hide the map marker for the ship you are currently standing on.")
            .define("hideShipYouAreOn", true);

    public static final ModConfigSpec.BooleanValue AUTO_PARK = BUILDER
            .comment("Locally remember a ship's position when you step off it (used when the server has no AeroRadar).")
            .define("autoPark", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
