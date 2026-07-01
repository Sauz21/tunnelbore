package com.auztechlabs.tunnelbore.client;

import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a red wireframe box around every marked block, refreshed each frame.
 *
 * <p>Purely visual and client-side. World rendering is camera-relative, so we translate
 * the pose stack by the negative camera position before drawing in world coordinates.
 */
public final class BoreHighlightRenderer {
	private static final float R = 1.0f;
	private static final float G = 0.15f;
	private static final float B = 0.15f;
	private static final float A = 1.0f;

	private BoreHighlightRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
			Set<BlockPos> marked = BoreClientState.INSTANCE.marked();
			if (marked.isEmpty()) {
				return;
			}

			PoseStack poseStack = context.matrixStack();
			MultiBufferSource consumers = context.consumers();
			if (poseStack == null || consumers == null) {
				return;
			}

			Vec3 cam = context.camera().getPosition();
			VertexConsumer lines = consumers.getBuffer(RenderType.lines());

			poseStack.pushPose();
			poseStack.translate(-cam.x, -cam.y, -cam.z);

			for (BlockPos pos : marked) {
				// Slight inflate so the outline doesn't z-fight with the block faces.
				AABB box = new AABB(pos).inflate(0.002);
				LevelRenderer.renderLineBox(poseStack, lines, box, R, G, B, A);
			}

			poseStack.popPose();
		});
	}
}
