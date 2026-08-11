package dev.hardcoremod.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Accessor("explosionRadius")
    void hcSetExplosionRadius(int radius);

    @Accessor("maxSwell")
    void hcSetMaxSwell(int maxSwell);
}
