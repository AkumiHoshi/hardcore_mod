package dev.hardcoremod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Custom enchantments are data-driven in 1.21.11: they live in
 * data/hardcoremod/enchantment/*.json. This class resolves their holders
 * from the server registry once a server is up.
 */
public final class HcEnchantments {
    private static MinecraftServer serverRef;
    private static Holder<Enchantment> comboHolder;
    private static boolean refreshed = false;

    private HcEnchantments() {
    }

    public static void refresh(MinecraftServer server) {
        serverRef = server;
        comboHolder = holder("damage_combo");
        for (Element el : Element.values()) {
            el.enchantHolder = holder(el.enchantId());
            el.chanceHolder = holder(el.chanceEnchantId());
        }
        refreshed = true;
    }

    public static Holder<Enchantment> comboHolder() {
        return comboHolder;
    }

    public static boolean isRefreshed() {
        return refreshed;
    }

    private static Holder<Enchantment> holder(String id) {
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, id));
        return serverRef.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
