package dev.hardcoremod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardcoreMod implements ModInitializer {
    public static final String MOD_ID = "hardcoremod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        HcConfig.INSTANCE = HcConfig.load();
        HcNetworking.register();
        HcCommands.register();

        // Resolve enchantment holders from the (data-driven) enchantment registry.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> HcEnchantments.refresh(server));

        // Expire the loading-screen protection + run pending soft-restore finalizers + replay votes.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            HcCombat.tickProtection(server);
            ReplayManager.tickRestores(server);
            ReplayVote.tick(server);
        });

        // Snapshot inventory before hardcore death; hint about /replay.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer sp && ReplayManager.isHardcore(((ServerLevel) sp.level()).getServer())) {
                HcState st = HcState.get(((ServerLevel) sp.level()).getServer());
                st.entry(sp.getUUID()).deathInv = HcState.saveInventory(sp.getInventory(), sp.registryAccess());
                st.setDirty();
                sp.sendSystemMessage(Component.literal(
                        "§cBạn đã chết! Khi tất cả người chơi đều chết, gõ §e/replay §cđể chọn chơi lại."));
            }
            return true;
        });

        // Kill rewards: shards / boss points.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer killer)) return;
            if (entity.getType() == EntityType.WARDEN) {
                StatManager.grantPoints(killer, 20);
            } else if (entity.getType() == EntityType.ENDER_DRAGON) {
                StatManager.grantPoints(killer, 50);
            } else if (entity instanceof Monster) {
                StatManager.grantShard(killer);
            }
        });

        // Apply difficulty multiplier to newly loaded mobs.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            double mult = HcConfig.MULT;
            if (mult == 1.0) return;
            if (entity instanceof Monster m) ReplayManager.applyMonsterMult(m, mult);
            if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon d) ReplayManager.applyMonsterMult(d, mult);
            if (entity instanceof Creeper c) ReplayManager.applyCreeperMult(c, mult);
        });

        // On join: apply stat/element modifiers, open element selector on first join.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            StatManager.applyAll(p);
            HcState st = HcState.get(server);
            if (!st.entry(p.getUUID()).hasChosen) {
                HcNetworking.send(p, new HcNetworking.OpenElementS2C());
            }
        });
    }
}
