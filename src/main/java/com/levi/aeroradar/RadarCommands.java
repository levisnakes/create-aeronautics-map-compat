package com.levi.aeroradar;

import com.levi.aeroradar.client.ClientShipData;
import com.levi.aeroradar.net.RenameShipPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Client-side /aeroradar command tree. */
public final class RadarCommands {

    private static final SuggestionProvider<CommandSourceStack> SHIP_NAMES = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (String name : knownNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(name);
        }
        return builder.buildFuture();
    };

    private RadarCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aeroradar")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("name")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> name(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("pin")
                        .executes(ctx -> pinContext(ctx.getSource()))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(SHIP_NAMES)
                                .executes(ctx -> pinByName(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("unpin").executes(ctx -> unpin(ctx.getSource())))
                .then(Commands.literal("park").executes(ctx -> park(ctx.getSource())))
                .then(Commands.literal("forget")
                        .executes(ctx -> forgetContext(ctx.getSource()))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(SHIP_NAMES)
                                .executes(ctx -> forgetByName(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))));
    }

    // ---- subcommands ----

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("AeroRadar — ship radar & map markers").withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal(
                "/aeroradar list — all tracked ships\n" +
                "/aeroradar name <name> — name the ship you're on (shared if server supports it)\n" +
                "/aeroradar pin [name] — mark a ship green on the map\n" +
                "/aeroradar unpin — clear the mark\n" +
                "/aeroradar park — save this ship's position now\n" +
                "/aeroradar forget [name] — drop a parked position").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        Minecraft mc = Minecraft.getInstance();
        boolean enhanced = ClientShipData.serverEnhanced();
        source.sendSuccess(() -> Component.literal("Server radar: " + (enhanced ? "ON (shared, all ships)" : "OFF (local scan only)"))
                .withStyle(enhanced ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY), false);

        var targets = ShipTracker.allTargets(mc);
        Set<String> shown = new LinkedHashSet<>();
        Vec3 eye = ShipTracker.effectivePlayerPos(mc);
        for (ShipTracker.Target t : targets) {
            shown.add(t.id().toString());
            double dx = t.pos().x - eye.x;
            double dz = t.pos().z - eye.z;
            int dist = (int) t.pos().distanceTo(eye);
            String sym = t.aboard() ? "▶" : (t.live() ? "●" : "⚓");
            String extra = t.aboard() ? " — aboard"
                    : " — " + dist + "m " + ShipTracker.compassDir(dx, dz)
                      + (t.parked() ? " (last seen " + ago(parkedTime(t.id())) + ")" : "");
            String pin = t.pinned() ? " ★" : "";
            ChatFormatting color = t.aboard() || t.pinned() ? ChatFormatting.GREEN
                    : (t.parked() ? ChatFormatting.GOLD : ChatFormatting.AQUA);
            MutableComponent line = Component.literal(sym + " " + t.name() + extra + pin).withStyle(color);
            source.sendSuccess(() -> line, false);
        }

        String dim = ShipTracker.dimKey(mc);
        for (Map.Entry<String, ParkedStore.Entry> e : ParkedStore.all().entrySet()) {
            ParkedStore.Entry entry = e.getValue();
            if (!entry.hasPos || dim.equals(entry.dim) || shown.contains(e.getKey())) continue;
            UUID id;
            try { id = UUID.fromString(e.getKey()); } catch (IllegalArgumentException ex) { continue; }
            MutableComponent line = Component.literal(
                    "⚓ " + ShipTracker.resolveName(id) + " — in " + entry.dim
                    + " (parked " + ago(entry.time) + ")").withStyle(ChatFormatting.DARK_GRAY);
            source.sendSuccess(() -> line, false);
        }
        if (targets.isEmpty() && shown.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No ships tracked yet. Board a ship to start tracking it.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int name(CommandSourceStack source, String newName) {
        Minecraft mc = Minecraft.getInstance();
        UUID id = ShipTracker.contextShip(mc, 64);
        if (id == null) {
            source.sendFailure(Component.literal("Stand on (or near) a ship to name it."));
            return 0;
        }
        String trimmed = newName.trim();
        ParkedStore.setName(id, trimmed);
        boolean shared = sendRename(id, trimmed);
        source.sendSuccess(() -> Component.literal("Ship named \"" + trimmed + "\"" + (shared ? " (shared)" : ""))
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int pinContext(CommandSourceStack source) {
        Minecraft mc = Minecraft.getInstance();
        UUID id = ShipTracker.contextShip(mc, 64);
        if (id == null) {
            var targets = ShipTracker.allTargets(mc);
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("No ship to pin. Stand on one or use /aeroradar pin <name>."));
                return 0;
            }
            id = targets.get(0).id();
        }
        return doPin(source, id);
    }

    private static int pinByName(CommandSourceStack source, String query) {
        UUID id = findByName(query);
        if (id == null) {
            source.sendFailure(Component.literal("No ship named \"" + query + "\"."));
            return 0;
        }
        return doPin(source, id);
    }

    private static int doPin(CommandSourceStack source, UUID id) {
        ParkedStore.setPinned(id);
        String name = ShipTracker.resolveName(id);
        source.sendSuccess(() -> Component.literal("★ Pinned " + name + " (green on the map)").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int unpin(CommandSourceStack source) {
        ParkedStore.clearPin();
        source.sendSuccess(() -> Component.literal("Ship unpinned.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int park(CommandSourceStack source) {
        Minecraft mc = Minecraft.getInstance();
        UUID id = ShipTracker.contextShip(mc, 48);
        if (id == null) {
            source.sendFailure(Component.literal("Stand on (or near) a ship to park it."));
            return 0;
        }
        ShipTracker.LiveShip ship = ShipTracker.live().get(id);
        if (ship == null) {
            source.sendFailure(Component.literal("That ship is not loaded."));
            return 0;
        }
        ParkedStore.park(id, ship.center(), ShipTracker.dimKey(mc));
        String name = ShipTracker.resolveName(id);
        source.sendSuccess(() -> Component.literal("⚓ Parked position saved for " + name).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int forgetContext(CommandSourceStack source) {
        Minecraft mc = Minecraft.getInstance();
        UUID id = ShipTracker.contextShip(mc, 64);
        if (id == null) {
            var targets = ShipTracker.allTargets(mc);
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("Nothing to forget."));
                return 0;
            }
            id = targets.get(0).id();
        }
        String name = ShipTracker.resolveName(id);
        ParkedStore.forget(id);
        source.sendSuccess(() -> Component.literal("Forgot parked position of " + name).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int forgetByName(CommandSourceStack source, String query) {
        UUID id = findByName(query);
        if (id == null) {
            source.sendFailure(Component.literal("No ship named \"" + query + "\"."));
            return 0;
        }
        String name = ShipTracker.resolveName(id);
        ParkedStore.forget(id);
        source.sendSuccess(() -> Component.literal("Forgot parked position of " + name).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    // ---- helpers ----

    /** Share a name to the server if it advertises the rename channel. Returns true if sent. */
    private static boolean sendRename(UUID id, String name) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (mc.getConnection() != null && mc.getConnection().hasChannel(RenameShipPayload.TYPE)) {
                PacketDistributor.sendToServer(new RenameShipPayload(id, name));
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static Set<String> knownNames() {
        Minecraft mc = Minecraft.getInstance();
        Set<String> names = new LinkedHashSet<>();
        if (mc.level == null) return names;
        for (ShipTracker.Target t : ShipTracker.allTargets(mc)) names.add(t.name());
        for (String key : ParkedStore.all().keySet()) {
            try { names.add(ShipTracker.resolveName(UUID.fromString(key))); } catch (IllegalArgumentException ignored) {}
        }
        return names;
    }

    private static UUID findByName(String query) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        String q = query.trim().toLowerCase(Locale.ROOT);
        UUID prefix = null;
        for (ShipTracker.Target t : ShipTracker.allTargets(mc)) {
            String n = t.name().toLowerCase(Locale.ROOT);
            if (n.equals(q)) return t.id();
            if (prefix == null && n.startsWith(q)) prefix = t.id();
        }
        for (String key : ParkedStore.all().keySet()) {
            UUID id;
            try { id = UUID.fromString(key); } catch (IllegalArgumentException ex) { continue; }
            String n = ShipTracker.resolveName(id).toLowerCase(Locale.ROOT);
            if (n.equals(q)) return id;
            if (prefix == null && n.startsWith(q)) prefix = id;
        }
        return prefix;
    }

    private static long parkedTime(UUID id) {
        ParkedStore.Entry e = ParkedStore.get(id);
        return e != null ? e.time : 0;
    }

    private static String ago(long millis) {
        if (millis <= 0) return "just now";
        long mins = (System.currentTimeMillis() - millis) / 60000L;
        if (mins < 1) return "just now";
        if (mins < 60) return mins + "m ago";
        long hours = mins / 60;
        if (hours < 48) return hours + "h ago";
        return (hours / 24) + "d ago";
    }
}
