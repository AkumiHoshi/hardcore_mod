package dev.hardcoremod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.List;

/**
 * /admin gui — password-gated admin panel (server side).
 */
public final class AdminManager {
    public static final String PASSWORD = "TakanashiHoshiles";

    /** Gamerule toggles shown in the admin GUI. */
    public record RuleEntry(String label, GameRule<Boolean> rule) {
    }

    public static final List<RuleEntry> RULES = List.of(
            new RuleEntry("Send Command Output", GameRules.SEND_COMMAND_FEEDBACK),
            new RuleEntry("Mob Griefing", GameRules.MOB_GRIEFING),
            new RuleEntry("Spawn Monsters", GameRules.SPAWN_MONSTERS),
            new RuleEntry("Fire Damage", GameRules.FIRE_DAMAGE),
            new RuleEntry("Keep Inventory", GameRules.KEEP_INVENTORY),
            new RuleEntry("Daylight Cycle", GameRules.ADVANCE_TIME),
            new RuleEntry("Weather Cycle", GameRules.ADVANCE_WEATHER),
            new RuleEntry("Natural Regen", GameRules.NATURAL_HEALTH_REGENERATION),
            new RuleEntry("Mob Loot", GameRules.MOB_DROPS),
            new RuleEntry("Block Drops", GameRules.BLOCK_DROPS));

    private AdminManager() {
    }

    public static boolean checkPassword(String password) {
        return PASSWORD.equals(password);
    }

    /** Execute an arbitrary command from the admin GUI command box (console source). */
    public static void runCommand(ServerPlayer p, String password, String command) {
        if (!checkPassword(password)) {
            p.sendSystemMessage(Component.literal("§cSai mật khẩu admin!"));
            return;
        }
        if (command == null || command.isBlank()) return;
        var server = ((net.minecraft.server.level.ServerLevel) p.level()).getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command.trim());
    }

    /** Send full admin state to the player. */
    public static void sendState(ServerPlayer p) {
        HcState.PlayerEntry e = HcState.of(p).entry(p.getUUID());
        int rules = 0;
        var gameRules = ((net.minecraft.server.level.ServerLevel) p.level()).getGameRules();
        for (int i = 0; i < RULES.size(); i++) {
            if (gameRules.get(RULES.get(i).rule())) rules |= (1 << i);
        }
        HcNetworking.send(p, new HcNetworking.AdminStateS2C(e.points, e.shards, e.strLvl, e.digLvl, e.hpLvl,
                p.gameMode().getId(), e.element, rules, HcConfig.INSTANCE.keepOldBackups));
    }

    public static void handle(ServerPlayer p, String password, String action, int value) {
        if (!checkPassword(password)) {
            p.sendSystemMessage(Component.literal("§cSai mật khẩu admin!"));
            return;
        }
        HcState st = HcState.of(p);
        HcState.PlayerEntry e = st.entry(p.getUUID());
        var server = ((net.minecraft.server.level.ServerLevel) p.level()).getServer();
        var gameRules = ((net.minecraft.server.level.ServerLevel) p.level()).getGameRules();

        switch (action) {
            case "gamemode" -> {
                GameType gt = GameType.byId(value);
                if (gt != null) p.setGameMode(gt);
            }
            case "points" -> {
                e.points = Math.max(0, e.points + value);
                st.setDirty();
            }
            case "shards" -> {
                e.shards = Math.max(0, e.shards + value);
                st.setDirty();
            }
            case "str" -> {
                e.strLvl = Math.max(0, e.strLvl + value);
                st.setDirty();
                StatManager.applyAll(p);
            }
            case "dig" -> {
                e.digLvl = Math.max(0, e.digLvl + value);
                st.setDirty();
                StatManager.applyAll(p);
            }
            case "hp" -> {
                e.hpLvl = Math.max(0, e.hpLvl + value);
                st.setDirty();
                StatManager.applyAll(p);
                p.setHealth(p.getMaxHealth());
            }
            case "element" -> {
                e.hasChosen = false;
                st.setDirty();
                HcNetworking.send(p, new HcNetworking.OpenElementS2C());
                HcCombat.PROTECT.remove(p.getUUID());
                p.setInvulnerable(false);
            }
            case "replay" -> ReplayManager.openReplay(p, true);
            case "keepbackup" -> {
                HcConfig.INSTANCE.keepOldBackups = !HcConfig.INSTANCE.keepOldBackups;
                HcConfig.INSTANCE.save();
            }
            case "rule_on" -> setRule(gameRules, server, value, true);
            case "rule_off" -> setRule(gameRules, server, value, false);
            default -> {
                return;
            }
        }
        sendState(p);
    }

    private static void setRule(GameRules rules, net.minecraft.server.MinecraftServer server, int index, boolean on) {
        if (index < 0 || index >= RULES.size()) return;
        rules.set(RULES.get(index).rule(), on, server);
    }
}
