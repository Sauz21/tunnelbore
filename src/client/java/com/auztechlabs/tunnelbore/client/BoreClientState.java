package com.auztechlabs.tunnelbore.client;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * Client-side state for Tunnel Bore: whether Mark Mode is active, and the set of
 * blocks the player has marked (the cross-section to bore through).
 *
 * <p>This lives only on the client for now — it drives the red highlight rendering
 * and, later, gets sent to the server when the player triggers a bore.
 */
public final class BoreClientState {
	public static final BoreClientState INSTANCE = new BoreClientState();

	// LinkedHashSet keeps insertion order stable (nice for future previews) and gives O(1) contains/add/remove.
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

	/**
	 * Adds the block if unmarked, removes it if already marked.
	 *
	 * @return true if the block is now marked, false if it was just unmarked
	 */
	public boolean toggleBlock(BlockPos pos) {
		BlockPos key = pos.immutable();
		if (marked.contains(key)) {
			marked.remove(key);
			return false;
		}
		marked.add(key);
		return true;
	}

	public boolean isMarked(BlockPos pos) {
		return marked.contains(pos);
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
