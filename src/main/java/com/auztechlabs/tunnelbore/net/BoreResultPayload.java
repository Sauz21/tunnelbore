package com.auztechlabs.tunnelbore.net;

import com.auztechlabs.tunnelbore.TunnelBore;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: result of a bore — whether to advance the selection, plus a status message. */
public record BoreResultPayload(boolean advanced, Direction boreDir, int minedCount, String message) implements CustomPacketPayload {
	public static final Type<BoreResultPayload> TYPE = new Type<>(TunnelBore.id("bore_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, BoreResultPayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeBoolean(payload.advanced);
				buf.writeByte(payload.boreDir.get3DDataValue());
				buf.writeVarInt(payload.minedCount);
				buf.writeUtf(payload.message);
			},
			buf -> new BoreResultPayload(
					buf.readBoolean(),
					Direction.from3DDataValue(buf.readByte()),
					buf.readVarInt(),
					buf.readUtf()
			)
	);

	@Override
	public Type<BoreResultPayload> type() {
		return TYPE;
	}
}
