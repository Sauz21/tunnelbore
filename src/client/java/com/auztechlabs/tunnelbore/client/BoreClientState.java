package com.auztechlabs.tunnelbore.client;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * Client-side state for Tunnel Bore: whether Mark Mode is active and the set of
 * marked blocks (the cross-section to bore through).
 *
 * <p>Lives only on the client for now — it drives the red highlight rendering and,
 * later, gets sent to the server when the player triggers a bore.
 */
public final class BoreClientState {
	public static final BoreClientState INSTANCE = new BoreClientState();

	// LinkedHashSet keeps insertion order stable and gives O(1) contains/add/remove.
	private final Set<BlockPos> marked = new LinkedHashSet<>();
	private boolean markMode = false;

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
	 * This keeps the selection contiguous — you can't mark blocks far from the group.
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
}
