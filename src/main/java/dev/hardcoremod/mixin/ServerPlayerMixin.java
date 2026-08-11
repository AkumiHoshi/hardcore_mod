package dev.hardcoremod.mixin;

import dev.hardcoremod.HcConfig;
import dev.hardcoremod.HcState;
import dev.hardcoremod.ReplayManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * First-time Nether portal activation → difficulty ×2; first End entry → ×4.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "teleport", at = @At("HEAD"))
    private void hardcoremod$onTeleport(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (!(self.level() instanceof ServerLevel sl)) return;
        ResourceKey<Level> from = sl.dimension();
        ResourceKey<Level> to = transition.newLevel().dimension();
        if (from.equals(to)) return;

        HcState st = HcState.of(self);
        var server = sl.getServer();
        boolean changed = false;

        if (to == Level.NETHER && !st.netherActivated) {
            st.netherActivated = true;
            HcConfig.INSTANCE.netherMult = 2.0;
            changed = true;
            broadcast(server, "§c[⚠] Cổng Nether được kích hoạt lần đầu! Độ khó thế giới tăng gấp §e2§c lần!");
        }
        if (to == Level.END && !st.endEntered) {
            st.endEntered = true;
            HcConfig.INSTANCE.endMult = 4.0;
            changed = true;
            broadcast(server, "§c[⚠] Lần đầu tiên vào The End! Độ khó thế giới tăng gấp §e4§c lần!");
        }

        if (changed) {
            st.setDirty();
            HcConfig.INSTANCE.save();
            ReplayManager.applyMultToLoaded(server);
        }
    }

    private static void broadcast(net.minecraft.server.MinecraftServer server, String msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal(msg));
        }
    }
}
