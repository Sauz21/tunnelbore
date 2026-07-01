package com.auztechlabs.tunnelbore;

import com.auztechlabs.tunnelbore.net.BoreResultPayload;
import com.auztechlabs.tunnelbore.net.BoreTriggerPayload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (server + client) entrypoint for Tunnel Bore.
 *
 * <p>Registers the bore networking payloads on both sides and the server-side receiver that
 * runs the authoritative mining. Marking/rendering/input all live on the client side.
 */
public class TunnelBore implements ModInitializer {
	public static final String MOD_ID = "tunnelbore";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Tunnel Bore initializing (common)");

		// Register the bore payloads in both directions.
		PayloadTypeRegistry.playC2S().register(BoreTriggerPayload.TYPE, BoreTriggerPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BoreResultPayload.TYPE, BoreResultPayload.CODEC);

		// The server runs the actual mining when a client triggers a bore.
		ServerPlayNetworking.registerGlobalReceiver(BoreTriggerPayload.TYPE, (payload, context) -> {
			ServerPlayer serverPlayer = context.player();
			serverPlayer.server.execute(() ->
					BoreEngine.executeBore(serverPlayer, payload.positions(), payload.boreDir()));
		});
	}

	/** Helper for building namespaced ids under this mod, e.g. tunnelbore:bore_trigger. */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
