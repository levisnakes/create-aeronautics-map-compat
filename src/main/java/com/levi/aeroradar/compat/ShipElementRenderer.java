package com.levi.aeroradar.compat;

import com.levi.aeroradar.ShipTracker;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import xaero.common.IXaeroMinimap;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.element.render.MinimapElementReader;
import xaero.common.minimap.element.render.MinimapElementRenderLocation;
import xaero.common.minimap.element.render.MinimapElementRenderProvider;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.common.minimap.render.MinimapRendererHelper;

import java.util.Iterator;
import java.util.List;

/**
 * Draws ships as native map ELEMENTS - the same mechanism Xaero uses for radar
 * dots and player-tracker icons. Ships appear only in map views (minimap +
 * fullscreen world map): no waypoints, no in-world beacons, no waypoint-list
 * entries, nothing saved to Xaero's files.
 *
 * Extends the xaero.common (bridge) renderer base so the SAME instance can be
 * registered on the minimap's over-map handler AND wrapped for the world map
 * via MinimapElementRendererWrapper.
 */
public final class ShipElementRenderer extends MinimapElementRenderer<ShipTracker.Target, ShipElementRenderer.Context> {

    /** Render context required by the element API; we keep no per-render state in it. */
    public static final class Context {}

    private ShipElementRenderer(Reader reader, Provider provider, Context context) {
        super(reader, provider, context);
    }

    public static ShipElementRenderer create() {
        return new ShipElementRenderer(new Reader(), new Provider(), new Context());
    }

    @Override
    public boolean shouldRender(int location) {
        return location == MinimapElementRenderLocation.OVER_MINIMAP
                || location == MinimapElementRenderLocation.WORLD_MAP;
    }

    @Override
    public void preRender(int location, Entity renderEntity, Player player,
                          double renderX, double renderY, double renderZ,
                          IXaeroMinimap modMain, MultiBufferSource.BufferSource bufferSource,
                          MultiTextureRenderTypeRendererProvider rendererProvider) {
    }

    @Override
    public void postRender(int location, Entity renderEntity, Player player,
                           double renderX, double renderY, double renderZ,
                           IXaeroMinimap modMain, MultiBufferSource.BufferSource bufferSource,
                           MultiTextureRenderTypeRendererProvider rendererProvider) {
    }

    @Override
    public boolean renderElement(int location, boolean hovered, boolean outOfBounds,
                                 GuiGraphics graphics, MultiBufferSource.BufferSource bufferSource,
                                 Font font, RenderTarget framebuffer, MinimapRendererHelper helper,
                                 Entity renderEntity, Player player,
                                 double renderX, double renderY, double renderZ,
                                 int elementIndex, double optionalDepth, float optionalScale,
                                 ShipTracker.Target element, double partialX, double partialY,
                                 boolean cave, float partialTicks) {
        // The element handler has already translated the pose to the ship's position
        // on the map, so everything below draws around (0, 0).
        PoseStack pose = graphics.pose();
        int color = element.color();

        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(45));
        int r = element.pinned() ? 5 : 4;
        graphics.fill(-r - 1, -r - 1, r + 1, r + 1, 0xE0101418);   // dark diamond outline
        graphics.fill(-r, -r, r, r, color);                        // colored diamond
        pose.popPose();

        if (element.parked()) {
            // Hollow center marks a last-known (not currently loaded) position.
            graphics.fill(-1, -1, 1, 1, 0xE0101418);
        }

        if (location == MinimapElementRenderLocation.WORLD_MAP) {
            pose.pushPose();
            pose.translate(0, r + 3, 0);
            pose.scale(0.65f, 0.65f, 1.0f);
            graphics.drawCenteredString(font, element.name(), 0, 0, 0xFFFFFFFF);
            pose.popPose();
        }
        // Flush so our shapes don't bleed into Xaero's own batched rendering.
        graphics.flush();
        return false; // purely visual, never consumes the hover slot
    }

    // ---- element source: pulls the CURRENT ship list every frame ----

    public static final class Provider extends MinimapElementRenderProvider<ShipTracker.Target, Context> {
        private Iterator<ShipTracker.Target> iterator;

        @Override
        public void begin(int location, Context context) {
            List<ShipTracker.Target> targets = ShipTracker.mapTargets();
            iterator = targets.iterator();
        }

        @Override
        public boolean hasNext(int location, Context context) {
            return iterator != null && iterator.hasNext();
        }

        @Override
        public ShipTracker.Target getNext(int location, Context context) {
            return iterator.next();
        }

        @Override
        public void end(int location, Context context) {
            iterator = null;
        }
    }

    // ---- element geometry / metadata ----

    public static final class Reader extends MinimapElementReader<ShipTracker.Target, Context> {
        @Override
        public boolean isHidden(ShipTracker.Target element, Context context) {
            return false;
        }

        @Override
        public double getRenderX(ShipTracker.Target element, Context context, float partialTicks) {
            return element.pos().x;
        }

        @Override
        public double getRenderY(ShipTracker.Target element, Context context, float partialTicks) {
            return element.pos().y;
        }

        @Override
        public double getRenderZ(ShipTracker.Target element, Context context, float partialTicks) {
            return element.pos().z;
        }

        @Override
        public int getInteractionBoxLeft(ShipTracker.Target element, Context context, float partialTicks) {
            return -6;
        }

        @Override
        public int getInteractionBoxRight(ShipTracker.Target element, Context context, float partialTicks) {
            return 6;
        }

        @Override
        public int getInteractionBoxTop(ShipTracker.Target element, Context context, float partialTicks) {
            return -6;
        }

        @Override
        public int getInteractionBoxBottom(ShipTracker.Target element, Context context, float partialTicks) {
            return 6;
        }

        @Override
        public int getRenderBoxLeft(ShipTracker.Target element, Context context, float partialTicks) {
            return -32;
        }

        @Override
        public int getRenderBoxRight(ShipTracker.Target element, Context context, float partialTicks) {
            return 32;
        }

        @Override
        public int getRenderBoxTop(ShipTracker.Target element, Context context, float partialTicks) {
            return -8;
        }

        @Override
        public int getRenderBoxBottom(ShipTracker.Target element, Context context, float partialTicks) {
            return 16;
        }

        @Override
        public int getLeftSideLength(ShipTracker.Target element, Minecraft mc) {
            return mc.font.width(element.name());
        }

        @Override
        public String getMenuName(ShipTracker.Target element) {
            return element.name();
        }

        @Override
        public String getFilterName(ShipTracker.Target element) {
            return element.name();
        }

        @Override
        public int getMenuTextFillLeftPadding(ShipTracker.Target element) {
            return 0;
        }

        @Override
        public int getRightClickTitleBackgroundColor(ShipTracker.Target element) {
            return 0xAA000000;
        }

        @Override
        public boolean shouldScaleBoxWithOptionalScale() {
            return true;
        }

        @Override
        public boolean isInteractable(xaero.hud.minimap.element.render.MinimapElementRenderLocation location,
                                      ShipTracker.Target element) {
            return false; // pure visual - no hover menus, no right-click entries
        }
    }
}
