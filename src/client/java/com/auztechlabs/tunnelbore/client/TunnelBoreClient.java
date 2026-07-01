package com.auztechlabs.tunnelbore.client;

import com.auztechlabs.tunnelbore.TunnelBore;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint for Tunnel Bore.
 *
 * <p>Registers the Mark Mode keybind and the left-click marking handler. Rendering
 * of the red selection highlight and the bore networking are added in later steps.
 */
public class TunnelBoreClient implements ClientModInitializer {
	private static KeyMapping toggleMarkKey;

	@Override
	public void onInitializeClient() {
		TunnelBore.LOGGER.info("Tunnel Bore initializing (client)");

		// Draw the red highlight over marked blocks every frame.
		BoreHighlightRenderer.register();

		// A rebindable keybind (shows up under a "Tunnel Bore" category in Controls). Default: V.
		toggleMarkKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.tunnelbore.toggle_mark",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				"key.categories.tunnelbore"
		));

		// Toggle Mark Mode when the keybind is pressed.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleMarkKey.consumeClick()) {
				boolean on = BoreClientState.INSTANCE.toggleMarkMode();
				sendActionBar(client, Component.literal(
						"Tunnel Bore — Mark Mode: " + (on ? "ON" : "OFF")
								+ (on ? "  (left-click blocks to mark)" : "")));
			}
		});

		// While Mark Mode is on, left-clicking a block marks/unmarks it instead of breaking it.
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			// Only act on the client, and only while Mark Mode is active.
			if (!world.isClientSide() || !BoreClientState.INSTANCE.isMarkMode()) {
				return InteractionResult.PASS;
			}

			boolean nowMarked = BoreClientState.INSTANCE.toggleBlock(pos);
			sendActionBar(Minecraft.getInstance(), Component.literal(
					(nowMarked ? "Marked" : "Unmarked") + " block  •  "
							+ BoreClientState.INSTANCE.size() + " selected"));

			// Returning anything other than PASS cancels the vanilla break, so the block stays put.
			return InteractionResult.SUCCESS;
		});
	}

	private static void sendActionBar(Minecraft client, Component text) {
		if (client.player != null) {
			client.player.displayClientMessage(text, true);
		}
	}
}
