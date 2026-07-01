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

import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint for Tunnel Bore.
 *
 * <p>Mark Mode ON: left-click marks a block (never breaks it), right-click unmarks it.
 * Mark Mode OFF: mining a marked block proceeds at normal vanilla speed; the moment it
 * finishes breaking (see {@code MultiPlayerGameModeMixin}), the whole layer bores and the
 * selection advances into the broken face.
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
						? Component.literal("Mark Mode: ON  (left-click = mark, right-click = unmark)")
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
		});

		// LEFT-CLICK while Mark Mode is on: mark the block (never breaks it). While Mark Mode is off
		// we return PASS so vanilla mines at normal speed; a *completed* break of a marked block
		// triggers the bore (see MultiPlayerGameModeMixin -> onClientBlockDestroyed).
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!world.isClientSide() || !BoreClientState.INSTANCE.isMarkMode()) {
				return InteractionResult.PASS;
			}
			BoreClientState state = BoreClientState.INSTANCE;
			if (!state.contains(pos)) {
				if (state.isConnectedTo(pos)) {
					state.add(pos);
					sendActionBar(Minecraft.getInstance(),
							Component.literal("Marked  •  " + state.size() + " blocks"));
				} else {
					sendActionBar(Minecraft.getInstance(),
							Component.literal("Must touch your selection"));
				}
			}
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
