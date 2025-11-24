package me.TreeOfSelf.PandaIgnore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class IgnoreCommand {

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_NOT_IGNORED = (ctx, builder) -> {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        var data = StateSaverAndLoader.getPlayerState(player);
        String remaining = builder.getRemainingLowerCase();

        ctx.getSource().getServer().getPlayerManager().getPlayerList().stream()
                .filter(p -> !p.getUuid().equals(player.getUuid()))
                .filter(p -> !data.ignoredPlayers.contains(p.getUuid()))
                .map(p -> p.getGameProfile().getName())
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_IGNORED = (ctx, builder) -> {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        var data = StateSaverAndLoader.getPlayerState(player);
        String remaining = builder.getRemainingLowerCase();

        ctx.getSource().getServer().getPlayerManager().getPlayerList().stream()
                .filter(p -> data.ignoredPlayers.contains(p.getUuid()))
                .map(p -> p.getGameProfile().getName())
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("ignore")
                .requires(src -> src.getPlayer() != null) // only players

                .then(argument("player", EntityArgumentType.player())
                        .suggests(SUGGEST_NOT_IGNORED)
                        .executes(ctx -> addIgnore(ctx, EntityArgumentType.getPlayer(ctx, "player"))))

                .then(literal("list")
                        .executes(IgnoreCommand::listIgnored))

                .then(literal("remove")
                        .then(argument("player", EntityArgumentType.player())
                                .suggests(SUGGEST_IGNORED)
                                .executes(ctx -> removeIgnore(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
        );
    }

    private static int addIgnore(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        if (player.equals(target)) {
            player.sendMessage(Text.literal("You cannot ignore yourself!").formatted(Formatting.RED));
            return 0;
        }

        var data = StateSaverAndLoader.getPlayerState(player);
        if (data.ignoredPlayers.add(target.getUuid())) {
            StateSaverAndLoader.getServerState(player.getServer()).markDirty();
            player.sendMessage(Text.literal("Now ignoring ").append(target.getDisplayName()).append("."));
            return 1;
        }
        player.sendMessage(Text.literal("You are already ignoring ").append(target.getDisplayName()).append("."));
        return 0;
    }

    private static int listIgnored(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        var data = StateSaverAndLoader.getPlayerState(player);

        if (data.ignoredPlayers.isEmpty()) {
            player.sendMessage(Text.literal("You are not ignoring anyone.").formatted(Formatting.GRAY));
            return 0;
        }

        player.sendMessage(Text.literal("Ignored players:").formatted(Formatting.YELLOW));

        MinecraftServer server = player.getServer();
        UserCache userCache = server.getUserCache();
        int count = 0;

        for (UUID uuid : data.ignoredPlayers) {
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
            String name;

            if (online != null) {
                name = online.getName().getString();
            } else {
                name = userCache.getByUuid(uuid)
                        .map(profile -> profile.getName())
                        .orElse("??? (" + uuid.toString().substring(0, 8) + ")");
            }

            player.sendMessage(Text.literal(" - ").append(Text.literal(name).formatted(Formatting.WHITE)));
            count++;
        }

        player.sendMessage(Text.literal("Total: " + count).formatted(Formatting.GRAY));
        return count;
    }

    private static int removeIgnore(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        var data = StateSaverAndLoader.getPlayerState(player);

        if (data.ignoredPlayers.remove(target.getUuid())) {
            StateSaverAndLoader.getServerState(player.getServer()).markDirty();
            player.sendMessage(Text.literal("No longer ignoring ").append(target.getDisplayName()).append("."));
            return 1;
        }
        player.sendMessage(Text.literal("You weren't ignoring ").append(target.getDisplayName()).append("."));
        return 0;
    }
}