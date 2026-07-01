package com.auztechlabs.tunnelbore.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Client-side state for Tunnel Bore: whether Mark Mode is active and the set of
 * marked blocks (the cross-section to bore through).
 */
public final class BoreClientState {
	public static final BoreClientState INSTANCE = new BoreClientState();

	// LinkedHashSet keeps insertion order stable and gives O(1) contains/add/remove.
	private final Set<BlockPos> marked = new LinkedHashSet<>();
	private boolean markMode = false;

	// Debounce so a single completed break can't double-fire a bore for the same block.
	private BlockPos lastBorePos = null;
	private long lastBoreMs = 0L;

	private BoreClientState() {
	}

	public boolean isMarkMode() {
		return markMode;
	}

	/** Flips Mark Mode and returns the new state. */
	public boolean toggleMarkMode() {
		markMode = !markMode;
		return markMode;
	}

	public boolean contains(BlockPos pos) {
		return marked.contains(pos);
	}

	/**
	 * Whether {@code pos} is allowed to join the selection: true if the selection is
	 * empty, or if the block touches an already-marked block (faces, edges, or corners).
	 */
	public boolean isConnectedTo(BlockPos pos) {
		if (marked.isEmpty()) {
			return true;
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}
					if (marked.contains(pos.offset(dx, dy, dz))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Adds a block. Returns true if it was newly added. */
	public boolean add(BlockPos pos) {
		return marked.add(pos.immutable());
	}

	/** Removes a block. Returns true if it was present. */
	public boolean remove(BlockPos pos) {
		return marked.remove(pos);
	}

	/** Shifts the whole selection one block in {@code dir} (used after a successful bore). */
	public void advance(Direction dir) {
		List<BlockPos> shifted = new ArrayList<>(marked.size());
		for (BlockPos pos : marked) {
			shifted.add(pos.relative(dir));
		}
		marked.clear();
		marked.addAll(shifted);
	}

	public void clear() {
		marked.clear();
	}

	public int size() {
		return marked.size();
	}

	/** Read-only view of the marked blocks, for rendering. */
	public Set<BlockPos> marked() {
		return Collections.unmodifiableSet(marked);
	}

	/**
	 * Guards against one finished break firing a bore twice within a few ticks (the vanilla
	 * mining state can briefly re-enter). Different positions always pass, so boring down layer
	 * by layer is unaffected.
	 */
	public boolean canBoreAt(BlockPos pos) {
		long now = System.currentTimeMillis();
		if (pos.equals(lastBorePos) && now - lastBoreMs < 300L) {
			return false;
		}
		lastBorePos = pos.immutable();
		lastBoreMs = now;
		return true;
	}
}
