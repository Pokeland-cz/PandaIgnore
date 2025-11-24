package me.TreeOfSelf.PandaIgnore.mixin;

import me.TreeOfSelf.PandaIgnore.StateSaverAndLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "EEssentials/util/IgnoreManager")
public class EEssentialsIgnoreMixin {

    @Inject(method = "hasIgnored", at = @At("HEAD"), cancellable = true, remap = false)
    private static void pandaIgnore$blockBothDirections(
            ServerPlayerEntity target,   // Who the message is going to
            ServerPlayerEntity sender,   // Who is sending
            CallbackInfoReturnable<Boolean> cir
    ) {
        var targetData = StateSaverAndLoader.getPlayerState(target);
        var senderData = StateSaverAndLoader.getPlayerState(sender);

        // Case 1: Target has ignored sender → block (original EEssentials behavior)
        if (targetData.ignoredPlayers.contains(sender.getUuid())) {
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // Case 2: Sender has ignored target → also block
        if (senderData.ignoredPlayers.contains(target.getUuid())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}