package com.auztechlabs.tunnelbore.client.mixin;

import com.auztechlabs.tunnelbore.client.BoreClientState;
import com.auztechlabs.tunnelbore.client.TunnelBoreClient;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two client-side hooks for Tunnel Bore:
 * <ul>
 *   <li>While Mark Mode is on, suppress hold-to-mine so marking never breaks a block.</li>
 *   <li>While Mark Mode is off, when the player finishes mining a <em>marked</em> block, cancel
 *       that single break and bore the whole layer instead — so a layer takes the normal mining
 *       time of one block rather than breaking instantly on click.</li>
 * </ul>
 *
 * <p>{@code require = 0} keeps these non-fatal if a target ever moves on a future MC version.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
	private void tunnelbore$blockContinueDestroy(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
		if (BoreClientState.INSTANCE.isMarkMode()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
	private void tunnelbore$boreOnBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (TunnelBoreClient.onClientBlockDestroyed(pos)) {
			cir.setReturnValue(false);
		}
	}
}
