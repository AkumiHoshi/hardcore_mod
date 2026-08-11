package dev.hardcoremod;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-world persistent data: element choice, stat points, death inventory snapshots.
 * 1.21.11 uses a codec-based SavedDataType.
 */
public class HcState extends SavedData {
    public static final String NAME = "hardcoremod";

    /** First nether portal activation / first end entry (per world). */
    public boolean netherActivated = false;
    public boolean endEntered = false;

    public final Map<UUID, PlayerEntry> players = new HashMap<>();

    public static class PlayerEntry {
        public String element = "";
        public boolean hasChosen = false;
        public int points = 0;
        public int shards = 0;
        public int strLvl = 0;
        public int digLvl = 0;
        public int hpLvl = 0;
        public ListTag deathInv = null;
    }

    public PlayerEntry entry(UUID uuid) {
        return players.computeIfAbsent(uuid, u -> new PlayerEntry());
    }

    public static final Codec<HcState> CODEC = CompoundTag.CODEC.xmap(HcState::load, HcState::toTag);

    public static final SavedDataType<HcState> TYPE =
            new SavedDataType<>(NAME, HcState::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    public static HcState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static HcState of(ServerPlayer p) {
        return get(((ServerLevel) p.level()).getServer());
    }

    private static HcState load(CompoundTag tag) {
        HcState state = new HcState();
        state.netherActivated = tag.getBooleanOr("nether", false);
        state.endEntered = tag.getBooleanOr("end", false);
        for (Tag t : tag.getListOrEmpty("players")) {
            if (!(t instanceof CompoundTag ct)) continue;
            String uuidStr = ct.getStringOr("uuid", "");
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            PlayerEntry e = new PlayerEntry();
            e.element = ct.getStringOr("element", "");
            e.hasChosen = ct.getBooleanOr("chosen", false);
            e.points = ct.getIntOr("points", 0);
            e.shards = ct.getIntOr("shards", 0);
            e.strLvl = ct.getIntOr("str", 0);
            e.digLvl = ct.getIntOr("dig", 0);
            e.hpLvl = ct.getIntOr("hp", 0);
            ListTag inv = ct.getListOrEmpty("inv");
            if (!inv.isEmpty()) e.deathInv = inv;
            state.players.put(uuid, e);
        }
        return state;
    }

    private static CompoundTag toTag(HcState state) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("nether", state.netherActivated);
        tag.putBoolean("end", state.endEntered);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PlayerEntry> e : state.players.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("uuid", e.getKey().toString());
            PlayerEntry p = e.getValue();
            t.putString("element", p.element);
            t.putBoolean("chosen", p.hasChosen);
            t.putInt("points", p.points);
            t.putInt("shards", p.shards);
            t.putInt("str", p.strLvl);
            t.putInt("dig", p.digLvl);
            t.putInt("hp", p.hpLvl);
            if (p.deathInv != null) t.put("inv", p.deathInv);
            list.add(t);
        }
        tag.put("players", list);
        return tag;
    }

    /** Serialize an inventory as slot-tagged item NBT, using registry-aware ops. */
    public static ListTag saveInventory(net.minecraft.world.entity.player.Inventory inv, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putInt("slot", i);
            ItemStack.CODEC.encodeStart(ops, s).result().ifPresent(v -> t.put("item", v));
            list.add(t);
        }
        return list;
    }

    public static void loadInventory(net.minecraft.world.entity.player.Inventory inv, ListTag list, HolderLookup.Provider registries) {
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (Tag t : list) {
            if (!(t instanceof CompoundTag ct)) continue;
            int slot = ct.getIntOr("slot", -1);
            if (slot < 0) continue;
            Tag itemTag = ct.get("item");
            if (itemTag == null) continue;
            ItemStack stack = ItemStack.CODEC.parse(ops, itemTag).result().orElse(ItemStack.EMPTY);
            inv.setItem(slot, stack);
        }
    }
}
