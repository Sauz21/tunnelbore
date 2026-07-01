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
 * While Mark Mode is on, suppress client block breaking entirely, so left-clicking (or
 * holding it) only marks — it never breaks a block. Covers both the initial click and
 * the continued hold-to-mine path (which bypasses AttackBlockCallback).
 *
 * <p>{@code require = 0} makes these injections non-fatal: if a target ever can't be
 * found on a future MC version, the game still loads (falling back to click-only safety).
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
	private void tunnelbore$blockStartDestroy(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
		if (BoreClientState.INSTANCE.isMarkMode()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
	private void tunnelbore$blockContinueDestroy(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
		if (BoreClientState.INSTANCE.isMarkMode()) {
			cir.setReturnValue(false);
		}
	}
}
