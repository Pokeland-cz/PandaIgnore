package me.TreeOfSelf.PandaIgnore.mixin;

import me.TreeOfSelf.PandaIgnore.StateSaverAndLoader;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.MessageCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(MessageCommand.class)
public class MessageCommandMixin {

    /**
     * Intercepts private messages (/msg, /tell, /w) and prevents sending
     * to players who have ignored the sender.
     * Works with both vanilla and Fuji (and any other mod using vanilla PM system).
     */
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void pandaIgnore$blockIgnoredPrivateMessages(
            ServerCommandSource source,
            Collection<ServerPlayerEntity> targets,
            SignedMessage message,
            CallbackInfo ci
    ) {
        ServerPlayerEntity sender = source.getPlayer();
        if (sender == null) {
            return; // Console or command block — let vanilla handle
        }

        var senderUuid = sender.getUuid();
        var sentMessage = SentMessage.of(message);
        boolean anyFullyFiltered = false;

        for (ServerPlayerEntity target : targets) {
            var ignoreData = StateSaverAndLoader.getPlayerState(target);

            // If target has ignored the sender → skip completely
            if (ignoreData.ignoredPlayers.contains(senderUuid)) {
                continue;
            }

            // Profanity filter check
            boolean shouldFilter = source.shouldFilterText(target);
            if (shouldFilter && message.isFullyFiltered()) {
                anyFullyFiltered = true;
            }

            // Send to recipient with correct incoming format
            var incomingParams = MessageType.params(MessageType.MSG_COMMAND_INCOMING, source)
                    .withTargetName(target.getDisplayName());
            target.sendChatMessage(sentMessage, shouldFilter, incomingParams);
        }

        // Always show sender their outgoing message
        var outgoingParams = MessageType.params(MessageType.MSG_COMMAND_OUTGOING, source);
        source.sendChatMessage(sentMessage, false, outgoingParams);

        // Show sender their own outgoing message
        if (anyFullyFiltered) {
            sender.sendMessage(PlayerManager.FILTERED_FULL_TEXT);
        }

        ci.cancel(); // We fully handled it — stop vanilla from running
    }
}