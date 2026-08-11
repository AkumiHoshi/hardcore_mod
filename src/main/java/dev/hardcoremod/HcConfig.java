package dev.hardcoremod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config stored OUTSIDE the world (game dir) so it survives world restores.
 * Holds the current world difficulty multiplier and latest backup name.
 */
public class HcConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("hardcoremod.json");

    public double mult = 1.0;
    public String backup = null;
    /** false: xóa backup cũ khi tạo backup mới (nút Chơi tiếp). true: giữ lại. */
    public boolean keepOldBackups = false;
    /** Tăng độ khó khi kích hoạt Nether lần đầu (×2) và vào The End lần đầu (×4). */
    public double netherMult = 1.0;
    public double endMult = 1.0;

    /** Current multiplier, read by mixins (mob spawn scaling). */
    public static volatile double MULT = 1.0;

    /** Effective difficulty = replay mult × nether × end. */
    public static double effective() {
        HcConfig c = INSTANCE;
        return c == null ? 1.0 : c.mult * c.netherMult * c.endMult;
    }

    /** Loaded once at mod init. */
    public static HcConfig INSTANCE;

    public static HcConfig load() {
        HcConfig cfg = new HcConfig();
        try {
            if (Files.exists(PATH)) {
                cfg = GSON.fromJson(Files.readString(PATH), HcConfig.class);
                if (cfg == null) cfg = new HcConfig();
            }
        } catch (Exception e) {
            HardcoreMod.LOGGER.error("Failed to load hardcoremod config", e);
        }
        cfg.save();
        MULT = effective();
        return cfg;
    }

    public void save() {
        MULT = effective();
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException e) {
            HardcoreMod.LOGGER.error("Failed to save hardcoremod config", e);
        }
    }
}
