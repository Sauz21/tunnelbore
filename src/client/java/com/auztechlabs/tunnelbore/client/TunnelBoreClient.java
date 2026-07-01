package com.auztechlabs.tunnelbore.client;

import java.util.ArrayList;

import com.auztechlabs.tunnelbore.TunnelBore;
import com.auztechlabs.tunnelbore.net.BoreResultPayload;
import com.auztechlabs.tunnelbore.net.BoreTriggerPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint for Tunnel Bore.
 *
 * <p>Mark Mode ON: left-click (or hold and sweep) marks blocks; right-click unmarks. Mark Mode
 * OFF: mining a marked block at normal speed bores the whole layer and advances the selection.
 */
public class TunnelBoreClient implements ClientModInitializer {
	private static KeyMapping toggleMarkKey;
	private static KeyMapping clearSelectionKey;

	@Override
	public void onInitializeClient() {
		TunnelBore.LOGGER.info("Tunnel Bore initializing (client)");

		BoreHighlightRenderer.register();

		toggleMarkKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.tunnelbore.toggle_mark",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				"key.categories.tunnelbore"
		));

		clearSelectionKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.tunnelbore.clear",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_DELETE,
				"key.categories.tunnelbore"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleMarkKey.consumeClick()) {
				boolean on = BoreClientState.INSTANCE.toggleMarkMode();
				sendActionBar(client, on
						? Component.literal("Mark Mode: ON  (hold left-click to paint, right-click to unmark)")
						: Component.literal("Mark Mode: OFF  (mine a marked block to bore)"));
			}

			while (clearSelectionKey.consumeClick()) {
				int cleared = BoreClientState.INSTANCE.size();
				if (cleared > 0) {
					BoreClientState.INSTANCE.clear();
					sendActionBar(client, Component.literal("Selection cleared  •  " + cleared + " blocks"));
				} else {
					sendActionBar(client, Component.literal("Nothing to clear"));
				}
			}

			// Paint-to-mark: hold left-click in Mark Mode and the block under the crosshair gets
			// marked each tick, so you can sweep or walk along to mark a big area at once.
			if (BoreClientState.INSTANCE.isMarkMode()
					&& client.screen == null
					&& client.options.keyAttack.isDown()
					&& client.hitResult instanceof BlockHitResult hit
					&& hit.getType() == HitResult.Type.BLOCK) {
				tryMarkBlock(hit.getBlockPos(), false);
			}
		});

		// LEFT-CLICK while Mark Mode is on: mark the block (never breaks it). The initial click marks
		// here; holding to paint is handled by the tick loop above.
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!world.isClientSide() || !BoreClientState.INSTANCE.isMarkMode()) {
				return InteractionResult.PASS;
			}
			tryMarkBlock(pos, true);
			return InteractionResult.SUCCESS;
		});

		// RIGHT-CLICK while Mark Mode is on: unmark the block.
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClientSide() || !BoreClientState.INSTANCE.isMarkMode()) {
				return InteractionResult.PASS;
			}
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.SUCCESS;
			}
			BlockPos pos = hitResult.getBlockPos();
			BoreClientState state = BoreClientState.INSTANCE;
			if (state.remove(pos)) {
				sendActionBar(Minecraft.getInstance(),
						Component.literal("Unmarked  •  " + state.size() + " blocks"));
			}
			return InteractionResult.SUCCESS;
		});

		// Bore result: we advance optimistically on the client, so only act if the bore did NOT
		// proceed — roll the highlight back onto the un-bored layer.
		ClientPlayNetworking.registerGlobalReceiver(BoreResultPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				if (!payload.advanced()) {
					BoreClientState.INSTANCE.advance(payload.boreDir().getOpposite());
				}
				sendActionBar(context.client(), Component.literal(payload.message()));
			});
		});
	}

	/**
	 * Marks the block if it's new and touches the selection. Returns true if newly marked.
	 * {@code showReject} shows the "must touch" hint — used for deliberate clicks, silent while painting.
	 */
	private static boolean tryMarkBlock(BlockPos pos, boolean showReject) {
		BoreClientState state = BoreClientState.INSTANCE;
		if (state.contains(pos)) {
			return false;
		}
		if (!state.isConnectedTo(pos)) {
			if (showReject) {
				sendActionBar(Minecraft.getInstance(), Component.literal("Must touch your selection"));
			}
			return false;
		}
		state.add(pos);
		sendActionBar(Minecraft.getInstance(), Component.literal("Marked  •  " + state.size() + " blocks"));
		return true;
	}

	/**
	 * Called from {@code MultiPlayerGameModeMixin} when the player finishes mining a block.
	 * If it's a marked block (Mark Mode off), fire a bore of the whole layer and return true so
	 * the caller cancels the single vanilla break (the bore routes all drops to the inventory).
	 */
	public static boolean onClientBlockDestroyed(BlockPos pos) {
		BoreClientState state = BoreClientState.INSTANCE;
		if (state.isMarkMode() || !state.contains(pos)) {
			return false;
		}
		if (state.canBoreAt(pos)) {
			Minecraft mc = Minecraft.getInstance();
			Direction boreDir = mc.hitResult instanceof BlockHitResult hit
					? hit.getDirection().getOpposite()
					: Direction.DOWN;
			ClientPlayNetworking.send(new BoreTriggerPayload(new ArrayList<>(state.marked()), boreDir));
			state.advance(boreDir);
		}
		return true;
	}

	private static void sendActionBar(Minecraft client, Component text) {
		if (client.player != null) {
			client.player.displayClientMessage(text, true);
		}
	}
}
