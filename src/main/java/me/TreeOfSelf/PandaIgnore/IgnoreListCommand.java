package me.TreeOfSelf.PandaIgnore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class IgnoreListCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("ignorelist")
                .requires(src -> src.getPlayer() != null)  // Only players can use this
                .executes(IgnoreListCommand::listIgnored)
        );
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
}