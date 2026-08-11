package dev.hardcoremod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.hardcoremod.HcCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapMethod(method = "hurtServer")
    private boolean hardcoremod$hurtServer(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (HcCombat.onHurtServerCancel(level, source, self)) {
            return false;
        }
        float boosted = HcCombat.onHurtServer(level, source, amount, self);
        return original.call(level, source, boosted);
    }
}
