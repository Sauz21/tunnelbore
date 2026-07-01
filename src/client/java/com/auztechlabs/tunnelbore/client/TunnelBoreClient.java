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

import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint for Tunnel Bore.
 *
 * <p>Mark Mode ON: left-click marks a block (never breaks it), right-click unmarks it.
 * Mark Mode OFF: breaking a marked block sends a bore request to the server, which mines
 * the whole layer; the server's reply advances the selection into the broken face.
 */
public class TunnelBoreClient implements ClientModInitializer {
	private static KeyMapping toggleMarkKey;

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

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleMarkKey.consumeClick()) {
				boolean on = BoreClientState.INSTANCE.toggleMarkMode();
				sendActionBar(client, on
						? Component.literal("Mark Mode: ON  (left-click = mark, right-click = unmark)")
						: Component.literal("Mark Mode: OFF  (break a marked block to bore)"));
			}
		});

		// LEFT-CLICK: mark (Mark Mode on) or trigger a bore on a marked block (Mark Mode off).
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!world.isClientSide()) {
				return InteractionResult.PASS;
			}
			BoreClientState state = BoreClientState.INSTANCE;

			if (state.isMarkMode()) {
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
			}

			// Mark Mode off: breaking a marked block bores the layer instead of breaking one block.
			if (state.contains(pos)) {
				Direction boreDir = direction.getOpposite();
				ClientPlayNetworking.send(new BoreTriggerPayload(new ArrayList<>(state.marked()), boreDir));
				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});

		// RIGHT-CLICK: unmark (Mark Mode on only).
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

		// Server's bore result: advance the selection into the broken face and show status.
		ClientPlayNetworking.registerGlobalReceiver(BoreResultPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				if (payload.advanced()) {
					BoreClientState.INSTANCE.advance(payload.boreDir());
				}
				sendActionBar(context.client(), Component.literal(payload.message()));
			});
		});
	}

	private static void sendActionBar(Minecraft client, Component text) {
		if (client.player != null) {
			client.player.displayClientMessage(text, true);
		}
	}
}
