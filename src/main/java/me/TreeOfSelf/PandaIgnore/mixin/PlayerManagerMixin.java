package me.TreeOfSelf.PandaIgnore.mixin;

import me.TreeOfSelf.PandaIgnore.StateSaverAndLoader;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private List<ServerPlayerEntity> players;
    @Shadow @Final public static Text FILTERED_FULL_TEXT;

    @Shadow
    protected abstract boolean verify(SignedMessage message);

    @Inject(
            method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void pandaIgnore$interceptBroadcast(
            SignedMessage message,
            Predicate<ServerPlayerEntity> shouldSendFiltered,
            ServerPlayerEntity sender,
            MessageType.Parameters params,
            CallbackInfo ci
    ) {
        // Allow system messages (join, death, death, etc.) to pass through vanilla
        if (sender == null) {
            return;
        }

        // Verify chat signature (same as vanilla)
        this.verify(message);

        // Log to console — use sender's name as prefix (exactly like vanilla does)
        String prefix = sender.getName().getString();
        this.server.logChatMessage(message.getContent(), params, prefix);

        SentMessage sentMessage = SentMessage.of(message);
        boolean anyFullyFiltered = false;

        for (ServerPlayerEntity receiver : players) {
            // Sender always sees their own message, unfiltered
            if (receiver == sender) {
                receiver.sendChatMessage(sentMessage, false, params);
                continue;
            }

            // Check ignore list
            StateSaverAndLoader.PlayerIgnoreData data = StateSaverAndLoader.getPlayerState(receiver);
            if (data.ignoredPlayers.contains(sender.getUuid())) {
                continue; // Message completely hidden from this player
            }

            // Apply profanity filter if needed
            boolean filter = shouldSendFiltered.test(receiver);
            if (filter && message.isFullyFiltered()) {
                anyFullyFiltered = true;
            }

            receiver.sendChatMessage(sentMessage, filter, params);
        }

        // Notify sender if their message was fully filtered for at least one player
        if (anyFullyFiltered) {
            sender.sendMessage(FILTERED_FULL_TEXT);
        }

        ci.cancel(); // We handled everything
    }
}