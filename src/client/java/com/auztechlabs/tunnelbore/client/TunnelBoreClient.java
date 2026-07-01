package com.auztechlabs.tunnelbore.client;

import com.auztechlabs.tunnelbore.TunnelBore;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint for Tunnel Bore.
 *
 * <p>Marking works WorldEdit-style: with Mark Mode on, left-click adds a block to the
 * selection (never breaking it) and right-click removes it. New blocks must touch the
 * existing selection. Block breaking is fully suppressed while Mark Mode is on (see
 * {@code MultiPlayerGameModeMixin}).
 */
public class TunnelBoreClient implements ClientModInitializer {
	private static KeyMapping toggleMarkKey;

	@Override
	public void onInitializeClient() {
		TunnelBore.LOGGER.info("Tunnel Bore initializing (client)");

		// Draw the red highlight over marked blocks every frame.
		BoreHighlightRenderer.register();

		// A rebindable keybind (Controls > "Tunnel Bore" category). Default: V.
		toggleMarkKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.tunnelbore.toggle_mark",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				"key.categories.tunnelbore"
		));

		// Toggle Mark Mode on keypress.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleMarkKey.consumeClick()) {
				boolean on = BoreClientState.INSTANCE.toggleMarkMode();
				sendActionBar(client, on
						? Component.literal("Mark Mode: ON  (left-click = mark, right-click = unmark)")
						: Component.literal("Mark Mode: OFF"));
			}
		});

		// LEFT-CLICK = mark (add). Never breaks the block; enforces the touching rule.
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

			// Always cancel the break while marking (the mixin also blocks hold-to-mine).
			return InteractionResult.SUCCESS;
		});

		// RIGHT-CLICK = unmark (remove). Also swallows the interaction so nothing gets placed/opened.
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
	}

	private static void sendActionBar(Minecraft client, Component text) {
		if (client.player != null) {
			client.player.displayClientMessage(text, true);
		}
	}
}
