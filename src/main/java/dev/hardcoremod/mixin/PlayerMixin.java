package dev.hardcoremod.mixin;

import dev.hardcoremod.HcCombat;
import dev.hardcoremod.StatManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void hardcoremod$attackStart(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level() instanceof ServerLevel sl) {
            HcCombat.attacking.put(self.getUUID(), sl.getLevelData().getGameTime());
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void hardcoremod$attackEnd(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level() instanceof ServerLevel) {
            HcCombat.attacking.remove(self.getUUID());
        }
    }

    @Inject(method = "giveExperienceLevels", at = @At("HEAD"))
    private void hardcoremod$levelUp(int levels, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (levels > 0 && self.level() instanceof ServerLevel && (Object) self instanceof ServerPlayer sp) {
            StatManager.grantPoints(sp, levels);
        }
    }
}
