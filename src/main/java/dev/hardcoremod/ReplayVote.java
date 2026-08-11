package dev.hardcoremod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Vote system: when multiple players are online, the replay mode is chosen by
 * majority vote within 180 seconds. Tie → "Chơi tiếp".
 */
public final class ReplayVote {
    public static final int DURATION_TICKS = 180 * 20;

    private static MinecraftServer activeServer;
    private static boolean active;
    private static boolean resolved;
    private static int votesLoad;
    private static int votesContinue;
    private static long endTick;
    private static final Set<UUID> voted = new HashSet<>();

    private ReplayVote() {
    }

    public static boolean isActive(MinecraftServer server) {
        return active && activeServer == server;
    }

    public static void start(MinecraftServer server) {
        activeServer = server;
        active = true;
        resolved = false;
        votesLoad = 0;
        votesContinue = 0;
        voted.clear();
        endTick = server.getTickCount() + DURATION_TICKS;
        broadcast(server, "§e[VOTE] Chọn chế độ replay trong 180 giây! Mở /replay và bấm chọn. Bên nhiều phiếu hơn sẽ thắng.");
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sendState(p);
        }
    }

    public static void vote(ServerPlayer p, boolean loadBackup) {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) p.level()).getServer();
        if (!isActive(server)) return;
        if (!voted.add(p.getUUID())) {
            p.sendSystemMessage(Component.literal("§cBạn đã vote rồi!"));
            return;
        }
        if (loadBackup) votesLoad++;
        else votesContinue++;
        p.sendSystemMessage(Component.literal("§aBạn đã vote: " + (loadBackup ? "Chơi lại từ backup" : "Chơi tiếp")));
        broadcastState(server);
        int players = server.getPlayerList().getPlayers().size();
        if (voted.size() >= players) resolve(server);
    }

    public static void tick(MinecraftServer server) {
        if (!isActive(server)) return;
        if (server.getTickCount() >= endTick) {
            resolve(server);
            return;
        }
        if (server.getTickCount() % 20 == 0) {
            broadcastState(server);
        }
    }

    private static void resolve(MinecraftServer server) {
        if (resolved) return;
        resolved = true;
        active = false;
        boolean load = votesLoad > votesContinue; // tie → continue
        broadcast(server, "§e[VOTE] Kết quả: Chơi lại " + votesLoad + " - Chơi tiếp " + votesContinue
                + " → " + (load ? "§aChơi lại từ backup (-25%)" : "§aChơi tiếp (+25%)"));
        // Close everyone's replay screen.
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sendState(p);
        }
        if (load) {
            ReplayManager.loadBackup(server);
        } else {
            ReplayManager.continueGame(server);
        }
    }

    public static void sendState(ServerPlayer p) {
        int secondsLeft = active ? Math.max(0, (int) ((endTick - ((net.minecraft.server.level.ServerLevel) p.level()).getServer().getTickCount()) / 20)) : 0;
        HcNetworking.send(p, new HcNetworking.ReplayVoteStateS2C(active, votesLoad, votesContinue,
                secondsLeft, voted.contains(p.getUUID())));
    }

    private static void broadcastState(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sendState(p);
        }
    }

    private static void broadcast(MinecraftServer server, String msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal(msg));
        }
    }
}
