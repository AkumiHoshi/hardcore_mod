package dev.hardcoremod;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Stat points: strength / dig speed / max health upgrades, shards, element passives.
 */
public final class StatManager {
    public static final Identifier STR_ID = Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, "str");
    public static final Identifier DIG_ID = Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, "dig");
    public static final Identifier HP_ID = Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, "hp");
    public static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, "element");

    public static final int SHARDS_PER_POINT = 10;

    private StatManager() {
    }

    /** Re-apply everything on login. */
    public static void applyAll(ServerPlayer p) {
        HcState.PlayerEntry e = HcState.of(p).entry(p.getUUID());
        applyElementPassive(p, Element.byId(e.element));
        addMod(p, Attributes.ATTACK_DAMAGE, STR_ID, e.strLvl * 0.5, AttributeModifier.Operation.ADD_VALUE);
        addMod(p, Attributes.BLOCK_BREAK_SPEED, DIG_ID, e.digLvl * 0.15, AttributeModifier.Operation.ADD_VALUE);
        addMod(p, Attributes.MAX_HEALTH, HP_ID, e.hpLvl * 2.0, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void applyElementPassive(ServerPlayer p, Element el) {
        AttributeInstance armor = p.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(ELEMENT_ID);
        if (el == null) return;
        Holder<Attribute> attr;
        double amount = 0;
        AttributeModifier.Operation op = AttributeModifier.Operation.ADD_VALUE;
        switch (el) {
            case FIRE -> {
                attr = Attributes.ATTACK_DAMAGE;
                amount = 1.5;
            }
            case ICE -> {
                attr = Attributes.ARMOR;
                amount = 2.0;
            }
            case QUANTUM -> {
                attr = Attributes.MOVEMENT_SPEED;
                amount = 0.10;
                op = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            }
            case LIGHTNING -> {
                attr = Attributes.ATTACK_SPEED;
                amount = 0.20;
                op = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            }
            case WATER -> {
                attr = Attributes.MAX_HEALTH;
                amount = 4.0;
            }
            case EARTH -> {
                attr = Attributes.KNOCKBACK_RESISTANCE;
                amount = 0.5;
            }
            case WIND -> {
                attr = Attributes.JUMP_STRENGTH;
                amount = 0.3;
            }
            case PHYSICS -> {
                attr = Attributes.ARMOR_TOUGHNESS;
                amount = 2.0;
            }
            default -> {
                return;
            }
        }
        AttributeInstance i = p.getAttribute(attr);
        if (i != null) {
            i.removeModifier(ELEMENT_ID);
            i.addPermanentModifier(new AttributeModifier(ELEMENT_ID, amount, op));
        }
    }

    public static boolean upgrade(ServerPlayer p, String stat) {
        HcState st = HcState.of(p);
        HcState.PlayerEntry e = st.entry(p.getUUID());
        int cost = switch (stat) {
            case "strength" -> e.strLvl + 1;
            case "dig" -> e.digLvl + 1;
            case "hp" -> e.hpLvl + 1;
            default -> -1;
        };
        if (cost < 0) return false;
        if (e.points < cost) {
            p.sendSystemMessage(Component.literal("§cKhông đủ Points! Cần " + cost + ", bạn có " + e.points + "."));
            return false;
        }
        e.points -= cost;
        switch (stat) {
            case "strength" -> {
                e.strLvl++;
                addMod(p, Attributes.ATTACK_DAMAGE, STR_ID, e.strLvl * 0.5, AttributeModifier.Operation.ADD_VALUE);
            }
            case "dig" -> {
                e.digLvl++;
                addMod(p, Attributes.BLOCK_BREAK_SPEED, DIG_ID, e.digLvl * 0.15, AttributeModifier.Operation.ADD_VALUE);
            }
            case "hp" -> {
                e.hpLvl++;
                addMod(p, Attributes.MAX_HEALTH, HP_ID, e.hpLvl * 2.0, AttributeModifier.Operation.ADD_VALUE);
                p.setHealth(p.getMaxHealth());
            }
        }
        st.setDirty();
        sync(p);
        return true;
    }

    public static void grantShard(ServerPlayer p) {
        HcState st = HcState.of(p);
        HcState.PlayerEntry e = st.entry(p.getUUID());
        e.shards++;
        if (e.shards >= SHARDS_PER_POINT) {
            e.shards -= SHARDS_PER_POINT;
            e.points++;
            p.sendSystemMessage(Component.literal("§b[10 Points Shard → +1 Points! Bạn có " + e.points + " Points.]"));
        } else {
            p.sendSystemMessage(Component.literal("§b[+1 Points Shard (" + e.shards + "/" + SHARDS_PER_POINT + ")]"));
        }
        st.setDirty();
        sync(p);
    }

    public static void grantPoints(ServerPlayer p, int n) {
        HcState st = HcState.of(p);
        HcState.PlayerEntry e = st.entry(p.getUUID());
        e.points += n;
        p.sendSystemMessage(Component.literal("§b[+" + n + " Points! Bạn có " + e.points + " Points.]"));
        st.setDirty();
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        HcState.PlayerEntry e = HcState.of(p).entry(p.getUUID());
        HcNetworking.send(p, new HcNetworking.StatSyncS2C(e.points, e.shards, e.strLvl, e.digLvl, e.hpLvl));
    }

    /** Apply element choice. Returns true if this was the first-time choice. */
    public static boolean chooseElement(ServerPlayer p, String id) {
        Element el = Element.byId(id);
        if (el == null) return false;
        HcState st = HcState.of(p);
        HcState.PlayerEntry e = st.entry(p.getUUID());
        if (e.hasChosen && !HcCommands.PENDING_ELEMENT_RECHOOSE.remove(p.getUUID())) {
            p.sendSystemMessage(Component.literal("§cBạn cần 100 Points và gõ /element để chọn lại nguyên tố."));
            return false;
        }
        boolean firstTime = !e.hasChosen;
        e.hasChosen = true;
        e.element = el.id();
        st.setDirty();
        applyElementPassive(p, el);
        p.sendSystemMessage(Component.literal("§aBạn đã chọn nguyên tố: " + el.name));
        return firstTime;
    }

    private static void addMod(ServerPlayer p, Holder<Attribute> attr, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }
}
