package com.auztechlabs.tunnelbore;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (server + client) entrypoint for Tunnel Bore.
 *
 * <p>Runs on both the logical client and the logical server. The actual mining,
 * inventory handling, and durability logic will live on the server side (added later),
 * because Minecraft only trusts the server to modify the world and player inventory.
 */
public class TunnelBore implements ModInitializer {
	public static final String MOD_ID = "tunnelbore";

	// It is best practice to name the logger after the mod id so console/log lines
	// are clearly attributed to this mod.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Tunnel Bore initializing (common)");
	}

	/** Helper for building namespaced ids under this mod, e.g. tunnelbore:bore_trigger. */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
