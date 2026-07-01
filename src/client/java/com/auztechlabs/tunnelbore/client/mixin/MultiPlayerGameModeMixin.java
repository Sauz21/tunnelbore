package com.auztechlabs.tunnelbore.client.mixin;

import com.auztechlabs.tunnelbore.client.BoreClientState;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops "hold left-click to keep mining" from breaking blocks while Mark Mode is on.
 *
 * <p>The initial left-click is already handled by {@code AttackBlockCallback} returning
 * SUCCESS, which cancels the break and does the marking. We deliberately do NOT touch
 * {@code startDestroyBlock} here: Fabric fires AttackBlockCallback from inside that method,
 * so cancelling it at HEAD would eat the mark event. We only guard the continued
 * hold-to-mine path, which doesn't fire that event.
 *
 * <p>{@code require = 0} keeps this non-fatal if the target ever moves on a future MC version.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
	private void tunnelbore$blockContinueDestroy(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
		if (BoreClientState.INSTANCE.isMarkMode()) {
			cir.setReturnValue(false);
		}
	}
}
