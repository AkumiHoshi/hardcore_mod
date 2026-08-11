package dev.hardcoremod;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class HcNetworking {

    public static void register() {
        PayloadTypeRegistry.playC2S().register(OpenStatC2S.ID, OpenStatC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(StatUpgradeC2S.ID, StatUpgradeC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(ReplayChoiceC2S.ID, ReplayChoiceC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(ElementChoiceC2S.ID, ElementChoiceC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminActionC2S.ID, AdminActionC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminCommandC2S.ID, AdminCommandC2S.CODEC);

        PayloadTypeRegistry.playS2C().register(OpenReplayS2C.ID, OpenReplayS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenElementS2C.ID, OpenElementS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(StatSyncS2C.ID, StatSyncS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(DmgCounterS2C.ID, DmgCounterS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(AdminStateS2C.ID, AdminStateS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(AdminOpenS2C.ID, AdminOpenS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(RecipeOpenS2C.ID, RecipeOpenS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(ReplayVoteStateS2C.ID, ReplayVoteStateS2C.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OpenStatC2S.ID, (payload, context) ->
                context.server().execute(() -> StatManager.sync(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(StatUpgradeC2S.ID, (payload, context) ->
                context.server().execute(() -> StatManager.upgrade(context.player(), payload.stat())));
        ServerPlayNetworking.registerGlobalReceiver(ReplayChoiceC2S.ID, (payload, context) ->
                context.server().execute(() -> ReplayManager.handleChoice(context.player(), payload.loadBackup())));
        ServerPlayNetworking.registerGlobalReceiver(ElementChoiceC2S.ID, (payload, context) ->
                context.server().execute(() -> StatManager.chooseElement(context.player(), payload.element())));
        ServerPlayNetworking.registerGlobalReceiver(AdminActionC2S.ID, (payload, context) ->
                context.server().execute(() -> AdminManager.handle(context.player(), payload.password(), payload.action(), payload.value())));
        ServerPlayNetworking.registerGlobalReceiver(AdminCommandC2S.ID, (payload, context) ->
                context.server().execute(() -> AdminManager.runCommand(context.player(), payload.password(), payload.command())));
    }

    public static void send(ServerPlayer p, CustomPacketPayload payload) {
        ServerPlayNetworking.send(p, payload);
    }

    /** Send the damage counter update to a single player (private per-player counter). */
    public static void sendDmg(ServerPlayer target, Entity victim, float amount, int stacks, int maxStacks,
                               String elementId, float accTotal) {
        DmgCounterS2C pkt = new DmgCounterS2C(victim.getId(), amount, stacks, maxStacks, elementId, accTotal);
        ServerPlayNetworking.send(target, pkt);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(HardcoreMod.MOD_ID, path);
    }

    // ---------- C2S ----------

    public record OpenStatC2S() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenStatC2S> ID = new CustomPacketPayload.Type<>(id("open_stat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenStatC2S> CODEC = StreamCodec.unit(new OpenStatC2S());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record StatUpgradeC2S(String stat) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StatUpgradeC2S> ID = new CustomPacketPayload.Type<>(id("stat_upgrade"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StatUpgradeC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, StatUpgradeC2S::stat, StatUpgradeC2S::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ReplayChoiceC2S(boolean loadBackup) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ReplayChoiceC2S> ID = new CustomPacketPayload.Type<>(id("replay_choice"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReplayChoiceC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ReplayChoiceC2S::loadBackup, ReplayChoiceC2S::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ElementChoiceC2S(String element) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ElementChoiceC2S> ID = new CustomPacketPayload.Type<>(id("element_choice"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ElementChoiceC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ElementChoiceC2S::element, ElementChoiceC2S::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    // ---------- S2C ----------

    public record OpenReplayS2C(boolean hasBackup, double mult, double netherMult, double endMult) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenReplayS2C> ID = new CustomPacketPayload.Type<>(id("open_replay"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenReplayS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, OpenReplayS2C::hasBackup,
                ByteBufCodecs.DOUBLE, OpenReplayS2C::mult,
                ByteBufCodecs.DOUBLE, OpenReplayS2C::netherMult,
                ByteBufCodecs.DOUBLE, OpenReplayS2C::endMult,
                OpenReplayS2C::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ReplayVoteStateS2C(boolean active, int votesLoad, int votesContinue,
                                     int secondsLeft, boolean voted) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ReplayVoteStateS2C> ID = new CustomPacketPayload.Type<>(id("replay_vote_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReplayVoteStateS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ReplayVoteStateS2C::active,
                ByteBufCodecs.INT, ReplayVoteStateS2C::votesLoad,
                ByteBufCodecs.INT, ReplayVoteStateS2C::votesContinue,
                ByteBufCodecs.INT, ReplayVoteStateS2C::secondsLeft,
                ByteBufCodecs.BOOL, ReplayVoteStateS2C::voted,
                ReplayVoteStateS2C::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record OpenElementS2C() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenElementS2C> ID = new CustomPacketPayload.Type<>(id("open_element"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenElementS2C> CODEC = StreamCodec.unit(new OpenElementS2C());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record StatSyncS2C(int points, int shards, int strLvl, int digLvl, int hpLvl) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StatSyncS2C> ID = new CustomPacketPayload.Type<>(id("stat_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StatSyncS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, StatSyncS2C::points,
                ByteBufCodecs.INT, StatSyncS2C::shards,
                ByteBufCodecs.INT, StatSyncS2C::strLvl,
                ByteBufCodecs.INT, StatSyncS2C::digLvl,
                ByteBufCodecs.INT, StatSyncS2C::hpLvl,
                StatSyncS2C::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record DmgCounterS2C(int entityId, float amount, int stacks, int maxStacks,
                                String elementId, float accTotal) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DmgCounterS2C> ID = new CustomPacketPayload.Type<>(id("dmg_counter"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DmgCounterS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, DmgCounterS2C::entityId,
                ByteBufCodecs.FLOAT, DmgCounterS2C::amount,
                ByteBufCodecs.INT, DmgCounterS2C::stacks,
                ByteBufCodecs.INT, DmgCounterS2C::maxStacks,
                ByteBufCodecs.STRING_UTF8, DmgCounterS2C::elementId,
                ByteBufCodecs.FLOAT, DmgCounterS2C::accTotal,
                DmgCounterS2C::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record AdminActionC2S(String password, String action, int value) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AdminActionC2S> ID = new CustomPacketPayload.Type<>(id("admin_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdminActionC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AdminActionC2S::password,
                ByteBufCodecs.STRING_UTF8, AdminActionC2S::action,
                ByteBufCodecs.INT, AdminActionC2S::value,
                AdminActionC2S::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record AdminCommandC2S(String password, String command) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AdminCommandC2S> ID = new CustomPacketPayload.Type<>(id("admin_command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdminCommandC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AdminCommandC2S::password,
                ByteBufCodecs.STRING_UTF8, AdminCommandC2S::command,
                AdminCommandC2S::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record AdminOpenS2C() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AdminOpenS2C> ID = new CustomPacketPayload.Type<>(id("admin_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdminOpenS2C> CODEC = StreamCodec.unit(new AdminOpenS2C());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record RecipeOpenS2C() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RecipeOpenS2C> ID = new CustomPacketPayload.Type<>(id("recipe_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeOpenS2C> CODEC = StreamCodec.unit(new RecipeOpenS2C());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    /** Player stats + gamerule states for the admin screen. */
    public record AdminStateS2C(int points, int shards, int strLvl, int digLvl, int hpLvl,
                                int gameMode, String element, int rules, boolean keepOldBackups) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AdminStateS2C> ID = new CustomPacketPayload.Type<>(id("admin_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AdminStateS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, AdminStateS2C::points,
                ByteBufCodecs.INT, AdminStateS2C::shards,
                ByteBufCodecs.INT, AdminStateS2C::strLvl,
                ByteBufCodecs.INT, AdminStateS2C::digLvl,
                ByteBufCodecs.INT, AdminStateS2C::hpLvl,
                ByteBufCodecs.INT, AdminStateS2C::gameMode,
                ByteBufCodecs.STRING_UTF8, AdminStateS2C::element,
                ByteBufCodecs.INT, AdminStateS2C::rules,
                ByteBufCodecs.BOOL, AdminStateS2C::keepOldBackups,
                AdminStateS2C::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}
