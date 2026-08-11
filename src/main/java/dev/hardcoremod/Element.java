package dev.hardcoremod;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Locale;

/**
 * Playable elements. Each has a passive benefit, a hit chance that scales with the
 * matching element enchantment level, and a unique on-hit effect.
 */
public enum Element {
    FIRE("Lửa", 0xFF4500, 0.20, 0.02),
    ICE("Băng", 0x7FD4FF, 0.25, 0.02),
    QUANTUM("Lượng tử", 0xAA66FF, 0.10, 0.01),
    LIGHTNING("Sét", 0xFFD700, 0.12, 0.02),
    WATER("Nước", 0x3399FF, 0.15, 0.02),
    EARTH("Đất", 0x8B5A2B, 0.25, 0.02),
    WIND("Gió", 0x98FB98, 0.20, 0.02),
    PHYSICS("Vật lý", 0xC0C0C0, 0.25, 0.02);

    public final String name;
    public final int color;
    public final double baseChance;
    public final double chancePerLevel;
    public Holder<Enchantment> enchantHolder;
    public Holder<Enchantment> chanceHolder;

    Element(String name, int color, double baseChance, double chancePerLevel) {
        this.name = name;
        this.color = color;
        this.baseChance = baseChance;
        this.chancePerLevel = chancePerLevel;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String enchantId() {
        return "element_" + id();
    }

    public String chanceEnchantId() {
        return "chance_" + id();
    }

    public static Element byId(String id) {
        for (Element e : values()) if (e.id().equals(id)) return e;
        return null;
    }

    /** I-X: +10%/level. From XI onward: +5% per extra level. */
    public static double bonus(int level) {
        if (level <= 0) return 0;
        return Math.min(level, 10) * 0.10 + Math.max(0, level - 10) * 0.05;
    }

    public double hitChance(int level) {
        return Math.min(0.60, baseChance + level * chancePerLevel);
    }
}
