package com.auztechlabs.tunnelbore;

import java.util.List;

import com.auztechlabs.tunnelbore.net.BoreResultPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-side, authoritative mining for one bore. Mines the given layer of blocks with full
 * vanilla pickaxe behaviour: correct-tier gating, Fortune/Silk Touch drops routed to the
 * player's inventory, per-block durability (Unbreaking-aware), and safe stops at unbreakable
 * blocks or before the tool breaks.
 */
public final class BoreEngine {
	private static final int MAX_BLOCKS = 4096;

	private BoreEngine() {
	}

	public static void executeBore(ServerPlayer player, List<BlockPos> positions, Direction boreDir) {
		if (positions.isEmpty()) {
			return;
		}
		if (positions.size() > MAX_BLOCKS) {
			send(player, false, boreDir, 0, "Selection too large (max " + MAX_BLOCKS + ")");
			return;
		}

		ServerLevel level = player.serverLevel();
		ItemStack tool = player.getMainHandItem();

		int mined = 0;
		int skipped = 0;
		boolean stopped = false;
		String reason = "";

		for (BlockPos pos : positions) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) {
				continue;
			}

			// Unbreakable (bedrock, barrier, ...) -> stop the bore.
			if (state.getDestroySpeed(level, pos) < 0.0f) {
				stopped = true;
				reason = "Hit an unbreakable block";
				break;
			}

			// Respect tool tier: skip blocks this tool can't harvest for drops.
			if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
				skipped++;
				continue;
			}

			// Stop before the pickaxe would break.
			if (tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= 1) {
				stopped = true;
				reason = "Your pickaxe is about to break";
				break;
			}

			// Correct drops for this block + tool (respects Fortune / Silk Touch).
			List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);

			// Remove the block without spilling items on the ground.
			level.destroyBlock(pos, false, player);

			// Route drops into the inventory; overflow pops out at the player's feet.
			for (ItemStack drop : drops) {
				player.getInventory().add(drop);
				if (!drop.isEmpty()) {
					player.drop(drop, false);
				}
			}

			// Apply durability (Unbreaking-aware).
			if (tool.isDamageableItem()) {
				tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			}

			mined++;
		}

		// Advance unless we stopped, or we mined nothing because the tool couldn't harvest the layer.
		boolean advanced = !stopped && (mined > 0 || skipped == 0);
		String message;
		if (stopped) {
			message = reason + " — bored " + mined;
		} else if (mined == 0 && skipped > 0) {
			message = "Wrong tool for these blocks";
		} else {
			message = "Bored " + mined + " blocks";
		}
		send(player, advanced, boreDir, mined, message);
	}

	private static void send(ServerPlayer player, boolean advanced, Direction boreDir, int mined, String message) {
		ServerPlayNetworking.send(player, new BoreResultPayload(advanced, boreDir, mined, message));
	}
}
