package dev.hardcoremod.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hardcoremod.HcNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class HcClient implements ClientModInitializer {
    public static KeyMapping statKey;

    private static java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> pendingScreen;
    private static int pendingDelay;

    /** Open a screen the same way the G-key handler does (clean tick boundary), after a delay. */
    private static void openScreen(java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> factory, int delayTicks) {
        pendingScreen = factory;
        pendingDelay = delayTicks;
    }

    @Override
    public void onInitializeClient() {
        statKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.hardcoremod.stats", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (statKey.consumeClick()) {
                if (client.player != null) {
                    ClientPlayNetworking.send(new HcNetworking.OpenStatC2S());
                    client.setScreen(new StatScreen());
                }
            }
            if (pendingScreen != null) {
                if (pendingDelay > 0) {
                    pendingDelay--;
                } else if (client.screen == null && client.player != null) {
                    client.setScreen(pendingScreen.get());
                    pendingScreen = null;
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(HcNetworking.OpenReplayS2C.ID, (payload, context) ->
                context.client().execute(() -> openScreen(() -> new ReplayScreen(payload.hasBackup(), payload.mult(), payload.netherMult(), payload.endMult()), 0)));
        ClientPlayNetworking.registerGlobalReceiver(HcNetworking.ReplayVoteStateS2C.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().screen instanceof ReplayScreen rs) rs.onVoteState(payload);
                }));
        ClientPlayNetworking.registerGlobalReceiver(HcNetworking.OpenElementS2C.ID, (payload, context) ->
                context.client().execute(() -> openScreen(ElementSelectScreen::new, 60)));
        ClientPlayNetworking.registerGlobalReceiver(HcNetworking.StatSyncS2C.ID, (payload, context) ->
                context.client().execute(() -> StatScreen.receive(payload)));
        ClientPlayNetworking.registerGlobalReceiver(HcNetworking.DmgCounterS2C.ID, (payload, context) ->
                context.client().execute(() -> DmgCounter.receive(payload)));
        ClientPlayNetworking.registerGlobalReceiver(HcNetworking.AdminOpenS2C.ID, (payload, context) ->
                context.client().execute(() -> openScreen(() -> new AdminScreen("TakanashiHoshiles"), 0)));
        ClientPlayNetworking.registerGlobalReceiver(HcNetworking.AdminStateS2C.ID, (payload, context) ->
                context.client().execute(() -> AdminScreen.receive(payload)));
        ClientPlayNetworking.registerGlobalReceiver(HcNetworking.RecipeOpenS2C.ID, (payload, context) ->
                context.client().execute(() -> openScreen(RecipeBookScreen::new, 60)));

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> DmgCounter.renderHud(graphics, 0));

        // Debug hook for headless screenshot testing: -Dhc.test=element
        String test = System.getProperty("hc.test");
        if (test != null) {
            dev.hardcoremod.HardcoreMod.LOGGER.info("hc.test hook enabled: " + test);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (++testTicks == 300) {
                    dev.hardcoremod.HardcoreMod.LOGGER.info("hc.test hook firing: " + test);
                    client.setScreen(new ElementSelectScreen());
                }
            });
        }
    }

    private static int testTicks;
}
