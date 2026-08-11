package dev.hardcoremod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hardcoremod.HcConfig;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scales natural spawn amounts by the world difficulty multiplier.
 */
@Mixin(MobCategory.class)
public abstract class MobCategoryMixin {

    @ModifyReturnValue(method = "getMaxInstancesPerChunk", at = @At("RETURN"))
    private int hardcoremod$scaleSpawns(int original) {
        double mult = HcConfig.MULT;
        if (mult == 1.0) return original;
        return Math.max(1, (int) Math.round(original * mult));
    }
}
