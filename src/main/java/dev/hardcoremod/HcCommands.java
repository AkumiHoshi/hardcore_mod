package dev.hardcoremod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class HcCommands {
    public static final Set<UUID> PENDING_ELEMENT_RECHOOSE = new HashSet<>();

    private HcCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("replay")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        ReplayManager.openReplay(p);
                        return 1;
                    }));

            dispatcher.register(Commands.literal("akumiyuukiirecipe")
                    .then(Commands.literal("book")
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                HcNetworking.send(p, new HcNetworking.RecipeOpenS2C());
                                return 1;
                            })));

            dispatcher.register(Commands.literal("admin")
                    .then(Commands.argument("password", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                String pw = StringArgumentType.getString(ctx, "password");
                                if (!AdminManager.checkPassword(pw)) {
                                    p.sendSystemMessage(Component.literal("§cSai mật khẩu admin!"));
                                    return 0;
                                }
                                AdminManager.sendState(p);
                                HcNetworking.send(p, new HcNetworking.AdminOpenS2C());
                                return 1;
                            })));

            dispatcher.register(Commands.literal("element")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        HcState st = HcState.of(p);
                        HcState.PlayerEntry e = st.entry(p.getUUID());
                        if (e.points < 100) {
                            p.sendSystemMessage(Component.literal("§cCần 100 Points để chọn lại nguyên tố (bạn có " + e.points + ")."));
                            return 1;
                        }
                        e.points -= 100;
                        st.setDirty();
                        PENDING_ELEMENT_RECHOOSE.add(p.getUUID());
                        StatManager.sync(p);
                        HcNetworking.send(p, new HcNetworking.OpenElementS2C());
                        return 1;
                    }));

            dispatcher.register(Commands.literal("hc")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.literal("book")
                            .then(Commands.argument("type", StringArgumentType.word())
                                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 20))
                                            .executes(ctx -> giveBook(ctx, null))
                                            .then(Commands.argument("player", EntityArgument.player())
                                                    .executes(ctx -> giveBook(ctx, EntityArgument.getPlayer(ctx, "player")))))))
                    .then(Commands.literal("points")
                            .then(Commands.literal("add")
                                    .then(Commands.argument("n", IntegerArgumentType.integer(0))
                                            .executes(ctx -> points(ctx, true, null))
                                            .then(Commands.argument("player", EntityArgument.player())
                                                    .executes(ctx -> points(ctx, true, EntityArgument.getPlayer(ctx, "player"))))))
                            .then(Commands.literal("set")
                                    .then(Commands.argument("n", IntegerArgumentType.integer(0))
                                            .executes(ctx -> points(ctx, false, null))
                                            .then(Commands.argument("player", EntityArgument.player())
                                                    .executes(ctx -> points(ctx, false, EntityArgument.getPlayer(ctx, "player")))))))
                    .then(Commands.literal("element")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                        HcState st = HcState.of(p);
                                        st.entry(p.getUUID()).hasChosen = false;
                                        st.setDirty();
                                        HcNetworking.send(p, new HcNetworking.OpenElementS2C());
                                        return 1;
                                    }))));
        });
    }

    private static int giveBook(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        final ServerPlayer p = target != null ? target : ctx.getSource().getPlayerOrException();
        final String type = StringArgumentType.getString(ctx, "type").toLowerCase(Locale.ROOT);
        final int level = IntegerArgumentType.getInteger(ctx, "level");
        Holder<Enchantment> holder;
        int finalLevel = level;
        if (type.equals("combo")) {
            holder = HcEnchantments.comboHolder();
            finalLevel = Math.min(level, 5);
        } else {
            Element el = Element.byId(type);
            if (el == null) {
                ctx.getSource().sendFailure(Component.literal("Loại không hợp lệ: combo|fire|ice|quantum|lightning|water|earth"));
                return 0;
            }
            holder = el.enchantHolder;
        }
        HcCombat.giveItem(p, HcCombat.makeBook(holder, finalLevel));
        ctx.getSource().sendSuccess(() -> Component.literal("Đã cho " + p.getName().getString() + " sách " + type + " cấp " + level), true);
        return 1;
    }

    private static int points(CommandContext<CommandSourceStack> ctx, boolean add, ServerPlayer target) throws CommandSyntaxException {
        final ServerPlayer p = target != null ? target : ctx.getSource().getPlayerOrException();
        int n = IntegerArgumentType.getInteger(ctx, "n");
        HcState st = HcState.of(p);
        HcState.PlayerEntry e = st.entry(p.getUUID());
        e.points = add ? e.points + n : n;
        st.setDirty();
        StatManager.sync(p);
        ctx.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + " Points = " + e.points), true);
        return 1;
    }
}
