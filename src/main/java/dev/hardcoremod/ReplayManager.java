package dev.hardcoremod;

import dev.hardcoremod.mixin.CreeperAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Hardcore replay: checks, continue (+25%), backup load (-25%), world backups.
 */
public final class ReplayManager {
    public static final Identifier MOB_SPEED_ID = Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, "mspeed");
    public static final Identifier MOB_DMG_ID = Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, "mdmg");
    public static final Identifier MOB_HP_ID = Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, "mhp");

    private ReplayManager() {
    }

    public static boolean isHardcore(MinecraftServer server) {
        return server.overworld().getLevelData().isHardcore();
    }

    public static void openReplay(ServerPlayer p) {
        openReplay(p, false);
    }

    /** True when every online player counts as dead: spectator, or dead but still on the death screen. */
    private static boolean allDead(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .allMatch(p -> p.isSpectator() || !p.isAlive());
    }

    public static void openReplay(ServerPlayer p, boolean admin) {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) p.level()).getServer();
        if (!admin && server.isDedicatedServer()) {
            p.sendSystemMessage(Component.literal("§cReplay chỉ hoạt động ở chế độ đơn/LAN."));
            return;
        }
        if (!isHardcore(server)) {
            p.sendSystemMessage(Component.literal("§cWorld này không phải Hardcore."));
            return;
        }
        boolean allDead = allDead(server);
        if (!admin && !allDead) {
            p.sendSystemMessage(Component.literal("§cCần tất cả người chơi đã chết mới có thể replay."));
            return;
        }
        // Multi-player: unify the mode with a 180s majority vote.
        if (server.getPlayerList().getPlayers().size() > 1) {
            if (!ReplayVote.isActive(server)) {
                ReplayVote.start(server);
            }
            ReplayVote.sendState(p);
        }
        HcConfig cfg = HcConfig.INSTANCE;
        HcNetworking.send(p, new HcNetworking.OpenReplayS2C(cfg.backup != null, cfg.mult, cfg.netherMult, cfg.endMult));
    }

    public static void handleChoice(ServerPlayer p, boolean loadBackup) {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) p.level()).getServer();
        if (server.isDedicatedServer()) return;
        if (!isHardcore(server)) return;
        boolean allDead = allDead(server);
        if (!allDead) return;
        if (ReplayVote.isActive(server)) {
            ReplayVote.vote(p, loadBackup);
            return;
        }
        if (loadBackup) {
            loadBackup(server);
        } else {
            continueGame(server);
        }
    }

    /** Nút 2: chơi tiếp - survival, trả item, +25% quái (độ khó nether/end reset), tạo backup. */
    public static void continueGame(MinecraftServer server) {
        HcState st = HcState.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            HcState.PlayerEntry e = st.entry(p.getUUID());
            p.setGameMode(GameType.SURVIVAL);
            if (e.deathInv != null) {
                p.getInventory().clearContent();
                HcState.loadInventory(p.getInventory(), e.deathInv, p.registryAccess());
                e.deathInv = null;
            }
            p.setHealth(p.getMaxHealth());
            var respawn = server.overworld().getLevelData().getRespawnData();
            var pos = respawn.pos();
            p.teleportTo(server.overworld(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    Set.of(), respawn.yaw(), respawn.pitch(), false);
        }
        // Dọn item đã rơi xuống đất khi chết để không bị nhặt trùng với item đã trả về.
        removeDroppedItems(server);
        st.setDirty();
        server.saveAllChunks(false, true, false);
        // Replay chỉ tăng 25% độ khó — không cộng dồn hệ số nether/end.
        HcConfig cfg = HcConfig.INSTANCE;
        cfg.mult = 1.25;
        cfg.netherMult = 1.0;
        cfg.endMult = 1.0;
        cfg.save();
        applyMultToLoaded(server);
        backupWorld(server);
        broadcast(server, "§a[Bạn đã chơi tiếp! Quái mạnh hơn 25%. Backup đã tạo - sẵn sàng để replay.]");
    }

    /** Nút 1: load backup gần nhất, -25% quái. Không kick người chơi LAN. */
    public static void loadBackup(MinecraftServer server) {
        HcConfig cfg = HcConfig.INSTANCE;
        if (cfg.backup == null) {
            broadcast(server, "§cChưa có backup nào! Chọn \"Chơi tiếp\" trước để tạo backup.");
            return;
        }
        // Replay chỉ giảm 25% độ khó — không cộng dồn hệ số nether/end.
        cfg.mult = 0.75;
        cfg.netherMult = 1.0;
        cfg.endMult = 1.0;
        cfg.save();
        applyMultToLoaded(server);
        softRestore(server, backupPath(cfg.backup));
    }

    /** Remove every dropped item entity in loaded chunks (stale death drops). */
    private static void removeDroppedItems(MinecraftServer server) {
        for (ServerLevel w : server.getAllLevels()) {
            for (Entity e : w.getEntities(EntityTypeTest.forClass(Entity.class), x -> true)) {
                if (e instanceof net.minecraft.world.entity.item.ItemEntity) {
                    e.discard();
                }
            }
        }
    }

    // ---------- soft restore (in-place, no restart, no kick) ----------

    private static final Map<MinecraftServer, RestoreTask> PENDING_RESTORES = new java.util.HashMap<>();

    private record RestoreTask(long finishTick, Runnable action) {
    }

    /** Called from the server tick handler; runs the delayed half of the restore. */
    public static void tickRestores(MinecraftServer server) {
        RestoreTask t = PENDING_RESTORES.get(server);
        if (t != null && server.getTickCount() >= t.finishTick) {
            PENDING_RESTORES.remove(server);
            t.action().run();
        }
    }

    /**
     * In-place restore: extract backup files over the live world, mark all loaded
     * chunks clean (so they never overwrite the restored files), drop the spawn
     * ticket, park players in a far corner, then 2 s later warp them to the
     * restored spawn (which reloads chunks from the restored files).
     */
    public static void softRestore(MinecraftServer server, Path backupFile) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        try {
            extractZip(backupFile, worldDir);

            // 1. All loaded chunks: clear the unsaved flag so unloading never rewrites restored files.
            for (ServerLevel w : server.getAllLevels()) {
                for (var holder : ((dev.hardcoremod.mixin.ChunkMapAccessor) w.getChunkSource().chunkMap).hcGetVisibleChunks().values()) {
                    var chunk = w.getChunkSource().getChunkNow(ChunkPos.getX(holder.getPos().toLong()),
                            ChunkPos.getZ(holder.getPos().toLong()));
                    if (chunk != null) {
                        ((dev.hardcoremod.mixin.ChunkAccessor) chunk).hcSetUnsaved(false);
                    }
                }
            }

            // 2. Drop the force-loaded spawn chunks so they reload from the restored files.
            var overworld = server.overworld();
            var respawn = overworld.getLevelData().getRespawnData();
            var spawnPos = respawn.pos();
            var chunkSource = overworld.getChunkSource();
            chunkSource.removeTicketWithRadius(net.minecraft.server.level.TicketType.PLAYER_SPAWN,
                    new ChunkPos(spawnPos), 3);

            // 3. Park all players far away (fresh chunks) while old chunks unload.
            int parkX = 100000, parkZ = 100000;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                HcCombat.protect(p, 80);
                p.teleportTo(overworld, parkX, 120, parkZ, Set.of(), 0, 0, false);
            }

            // 4. Roll the in-memory HcState back to the backup's copy (mutate in place).
            rollbackState(server, worldDir);

            // 5. After 2 s: warp everyone to the restored spawn, restore survival, re-ticket spawn.
            long finish = server.getTickCount() + 40;
            PENDING_RESTORES.put(server, new RestoreTask(finish, () -> {
                try {
                    var ow = server.overworld();
                    var rs = ow.getLevelData().getRespawnData();
                    var pos = rs.pos();
                    ow.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.PLAYER_SPAWN,
                            new ChunkPos(pos), 3);
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        p.setGameMode(GameType.SURVIVAL);
                        p.setHealth(p.getMaxHealth());
                        p.teleportTo(ow, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                                Set.of(), rs.yaw(), rs.pitch(), false);
                        p.sendSystemMessage(Component.literal("§aĐã tải backup! Quái vật yếu hơn 25%. Chúc may mắn!"));
                    }
                } catch (Exception e) {
                    HardcoreMod.LOGGER.error("Restore finalize failed", e);
                }
            }));
        } catch (IOException e) {
            HardcoreMod.LOGGER.error("Soft restore failed", e);
            broadcast(server, "§cLoad backup thất bại: " + e.getMessage());
        }
    }

    /** Replace the cached HcState contents with the backup's copy from data/hardcoremod.dat. */
    private static void rollbackState(MinecraftServer server, Path worldDir) {
        try {
            Path stateFile = worldDir.resolve("data").resolve("hardcoremod.dat");
            if (!Files.exists(stateFile)) return;
            CompoundTag tag = NbtIo.readCompressed(stateFile, NbtAccounter.defaultQuota());
            var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            var restored = HcState.CODEC.parse(ops, tag).result().orElse(null);
            if (restored != null) {
                HcState cached = HcState.get(server);
                cached.players.clear();
                cached.players.putAll(restored.players);
                cached.setDirty();
            }
        } catch (IOException e) {
            HardcoreMod.LOGGER.error("State rollback failed", e);
        }
    }

    // ---------- world backup ----------

    public static Path backupDir() {
        return FabricLoader.getInstance().getGameDir().resolve("hardcore-mod-backups");
    }

    public static Path backupPath(String name) {
        return backupDir().resolve(name);
    }

    public static void backupWorld(MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        try {
            Files.createDirectories(backupDir());
            String name = "backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".zip";
            zip(worldDir, backupDir().resolve(name));
            HcConfig.INSTANCE.backup = name;
            // Optional: delete the previous backup file so a newer backup always replaces it.
            if (!HcConfig.INSTANCE.keepOldBackups) {
                for (String old : oldBackupNames(name)) {
                    try {
                        Files.deleteIfExists(backupPath(old));
                    } catch (IOException ignored) {
                    }
                }
            }
            HcConfig.INSTANCE.save();
        } catch (IOException e) {
            HardcoreMod.LOGGER.error("Backup failed", e);
            broadcast(server, "§cTạo backup thất bại: " + e.getMessage());
        }
    }

    /** Backup files in the backup dir (excluding the one just created). */
    private static java.util.List<String> oldBackupNames(String exclude) {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (var stream = Files.list(backupDir())) {
            for (var p : stream.toList()) {
                String n = p.getFileName().toString();
                if (n.endsWith(".zip") && !n.equals(exclude)) names.add(n);
            }
        } catch (IOException e) {
            HardcoreMod.LOGGER.error("List backups failed", e);
        }
        return names;
    }

    // ---------- difficulty multipliers ----------

    public static void applyMultToLoaded(MinecraftServer server) {
        double mult = HcConfig.MULT;
        for (ServerLevel w : server.getAllLevels()) {
            for (Entity e : w.getEntities(EntityTypeTest.forClass(Entity.class), x -> true)) {
                if (e instanceof Monster m) {
                    applyMonsterMult(m, mult);
                } else if (e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon d) {
                    applyMonsterMult(d, mult);
                } else if (e instanceof Creeper c) {
                    applyCreeperMult(c, mult);
                }
            }
        }
    }

    /** Applies to hostile mobs AND the Ender Dragon. */
    public static void applyMonsterMult(net.minecraft.world.entity.Mob m, double mult) {
        addMod(m, Attributes.MOVEMENT_SPEED, MOB_SPEED_ID, mult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addMod(m, Attributes.ATTACK_DAMAGE, MOB_DMG_ID, mult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addMod(m, Attributes.MAX_HEALTH, MOB_HP_ID, mult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        m.setHealth(m.getMaxHealth());
    }

    public static void applyCreeperMult(Creeper c, double mult) {
        ((CreeperAccessor) c).hcSetExplosionRadius(Math.max(1, Math.round(3.0f * (float) mult)));
        ((CreeperAccessor) c).hcSetMaxSwell(Math.max(10, Math.round(30.0f / (float) mult)));
    }

    private static void addMod(net.minecraft.world.entity.Mob m, Holder<Attribute> attr, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = m.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static void broadcast(MinecraftServer server, String msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal(msg));
        }
    }

    // ---------- zip helpers ----------

    private static void zip(Path dir, Path out) throws IOException {
        try (OutputStream os = Files.newOutputStream(out);
             ZipOutputStream zos = new ZipOutputStream(os);
             var walk = Files.walk(dir)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                if (p.getFileName().toString().equals("session.lock")) continue;
                String entry = dir.relativize(p).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entry));
                Files.copy(p, zos);
                zos.closeEntry();
            }
        }
    }

    private static void extractZip(Path zip, Path target) throws IOException {
        try (InputStream is = Files.newInputStream(zip);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals("session.lock")) continue;
                Path out = target.resolve(e.getName()).normalize();
                if (!out.startsWith(target)) continue;
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                    continue;
                }
                Files.createDirectories(out.getParent());
                Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
