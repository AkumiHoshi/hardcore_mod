package dev.hardcoremod;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Combat math: melee attack context, combo stacks, element damage + effects,
 * damage counter broadcast, post-element-choice protection.
 */
public final class HcCombat {
    /** Players currently inside Player#attack (melee), UUID -> game tick. */
    public static final Map<UUID, Long> attacking = new HashMap<>();
    private static final Map<UUID, int[]> STACKS = new HashMap<>(); // {count, expireTick}
    /** Accumulated damage for the counter, {total, lastHitTick}. */
    private static final Map<UUID, float[]> ACC = new HashMap<>();
    /** Players invulnerable during the fake loading screen, UUID -> tick until. */
    public static final Map<UUID, Long> PROTECT = new HashMap<>();
    public static final int STACK_DURATION = 6000; // 5 min in ticks
    public static final int ACC_WINDOW = 140; // 7 s

    private HcCombat() {
    }

    public static int addStack(UUID uuid, int max, long now) {
        int[] d = STACKS.get(uuid);
        if (d == null || d[1] < now) d = new int[]{0, 0};
        d[1] = (int) (now + STACK_DURATION);
        d[0] = Math.min(max, d[0] + 1);
        STACKS.put(uuid, d);
        return d[0];
    }

    /** Tick-based maintenance of the loading-screen protection. */
    public static void tickProtection(MinecraftServer server) {
        if (PROTECT.isEmpty()) return;
        long now = ((ServerLevel) server.overworld()).getLevelData().getGameTime();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Long until = PROTECT.get(p.getUUID());
            if (until != null && now >= until) {
                PROTECT.remove(p.getUUID());
                p.setInvulnerable(false);
            }
        }
    }

    public static void protect(ServerPlayer p, long ticks) {
        PROTECT.put(p.getUUID(), ((ServerLevel) p.level()).getLevelData().getGameTime() + ticks);
        p.setInvulnerable(true);
    }

    /**
     * Called from LivingEntityMixin @WrapMethod on hurtServer.
     * Applies: Tăng sát thương kèm theo enchants, element damage + effects.
     */
    public static float onHurtServer(ServerLevel level, DamageSource source, float amount, LivingEntity victim) {
        boolean playerAttack = source.getEntity() instanceof ServerPlayer attacker
                && attacking.get(attacker.getUUID()) != null
                && attacking.get(attacker.getUUID()) == level.getLevelData().getGameTime();

        float out = amount;
        int stacks = 0;
        int maxStacks = 0;
        float accTotal = 0;
        float accBonus = 0;
        String elementId = "";
        UUID attackerUuid = null;

        if (playerAttack) {
            ServerPlayer attacker = (ServerPlayer) source.getEntity();
            attackerUuid = attacker.getUUID();
            long now = level.getLevelData().getGameTime();

            HcState.PlayerEntry a = HcState.get(level.getServer()).entry(attackerUuid);
            elementId = a.element;
            ItemStack weapon = attacker.getMainHandItem();
            double attrTotal = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double statDmg = a.strLvl * 0.5;
            double weaponDmg = Math.max(0, attrTotal - 1.0 - statDmg);

            int combo = weapon.getEnchantments().getLevel(HcEnchantments.comboHolder());
            if (combo > 0 && combo <= 5) {
                double[] weaponPct = {0, 0.05, 0.07, 0.10, 0.12, 0.15};
                double[] statPct = {0, 0.10, 0.12, 0.17, 0.20, 0.25};
                double[] stackPct = {0, 0.02, 0.03, 0.04, 0.045, 0.05};
                int[] maxStack = {0, 5, 7, 10, 15, 20};
                out += (float) (weaponDmg * weaponPct[combo] + statDmg * statPct[combo]);
                stacks = addStack(attacker.getUUID(), maxStack[combo], now);
                maxStacks = maxStack[combo];
                out *= (float) (1.0 + stackPct[combo] * stacks);
            }

            Element el = Element.byId(a.element);
            if (el != null) {
                int lvl = weapon.getEnchantments().getLevel(el.enchantHolder);
                int chanceLvl = weapon.getEnchantments().getLevel(el.chanceHolder);
                out += (float) (weaponDmg * Element.bonus(lvl));
                double chance = Math.min(0.90, el.hitChance(lvl) + chanceLvl * 0.08);
                boolean frozenVictimImmune = victim instanceof ServerPlayer vp
                        && Element.byId(HcState.get(level.getServer()).entry(vp.getUUID()).element) == Element.ICE;
                if (attacker.getRandom().nextDouble() < chance) {
                    switch (el) {
                        case FIRE -> victim.setRemainingFireTicks(60);
                        case ICE -> {
                            if (!frozenVictimImmune) {
                                victim.setTicksFrozen(200);
                                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 8));
                            }
                        }
                        case QUANTUM -> out *= 2f;
                        case LIGHTNING -> {
                            spawnBolt(level, victim);
                            accBonus = 5.0f;
                        }
                        case WATER -> {
                            if (victim.isOnFire()) {
                                out *= 1.5f;
                                victim.clearFire();
                            }
                        }
                        case EARTH -> victim.knockback(1.4, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
                        case WIND -> victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.7, 0));
                        case PHYSICS -> {
                            out *= 1.5f;
                            victim.knockback(2.0, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
                        }
                    }
                }
            }

            float[] acc = ACC.computeIfAbsent(attackerUuid, u -> new float[2]);
            if (now - acc[1] > ACC_WINDOW) acc[0] = 0;
            acc[0] += out + accBonus;
            acc[1] = now;
            accTotal = acc[0];
        }

        // Damage counter & stacks are private per player: only the attacker sees their own hits.
        if (playerAttack && attackerUuid != null) {
            ServerPlayer attacker = (ServerPlayer) source.getEntity();
            HcNetworking.sendDmg(attacker, victim, out, stacks, maxStacks, elementId, accTotal);
        }
        return out;
    }

    /** Victim-side checks: element immunities + quantum dodge. Returns true to cancel damage. */
    public static boolean onHurtServerCancel(ServerLevel level, DamageSource source, LivingEntity victim) {
        if (!(victim instanceof ServerPlayer vp)) return false;
        Element el = Element.byId(HcState.get(level.getServer()).entry(vp.getUUID()).element);
        if (el == null) return false;
        if (el == Element.FIRE && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return true;
        if (el == Element.LIGHTNING && source.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING)) return true;
        if (el == Element.ICE && source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING)) return true;
        if (el == Element.QUANTUM && source.getEntity() != null && vp.getRandom().nextDouble() < 0.10) return true;
        if (el == Element.WIND && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return true;
        if (el == Element.PHYSICS && source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return true;
        return false;
    }

    private static void spawnBolt(ServerLevel level, LivingEntity victim) {
        var bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.setPos(victim.getX(), victim.getY(), victim.getZ());
        level.addFreshEntity(bolt);
    }

    /** Give an enchanted book of the given type+level. */
    public static ItemStack makeBook(Holder<Enchantment> holder, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        m.set(holder, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());
        return book;
    }

    public static void giveItem(ServerPlayer p, ItemStack stack) {
        boolean added = p.getInventory().add(stack);
        if (!added) {
            ItemEntity e = p.drop(stack, false);
            if (e != null) e.setPickUpDelay(0);
        }
    }
}
