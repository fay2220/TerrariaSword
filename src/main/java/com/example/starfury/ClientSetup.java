package com.example.starfury;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StarfuryMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 3D animated star (GeckoLib): spins, tumbles and pulses while it falls.
        event.registerEntityRenderer(ModEntities.FALLEN_STAR.get(), FallenStarRenderer::new);
        // Zenith blades: glowing sword + afterimages + light ribbon trail.
        event.registerEntityRenderer(ModEntities.ZENITH_BLADE.get(), ZenithSwordRenderer::new);
    }

    /** Full-screen "impact frame" (flash + anime speed lines) for the Zenith sonic boom. */
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("zenith_impact", ZenithClientEffects::renderOverlay);
    }
}
