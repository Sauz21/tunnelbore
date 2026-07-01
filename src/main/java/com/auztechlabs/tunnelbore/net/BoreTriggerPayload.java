package com.auztechlabs.tunnelbore.net;

import java.util.ArrayList;
import java.util.List;

import com.auztechlabs.tunnelbore.TunnelBore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: "bore this set of marked blocks, advancing in boreDir." */
public record BoreTriggerPayload(List<BlockPos> positions, Direction boreDir) implements CustomPacketPayload {
	public static final Type<BoreTriggerPayload> TYPE = new Type<>(TunnelBore.id("bore_trigger"));

	public static final StreamCodec<RegistryFriendlyByteBuf, BoreTriggerPayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeVarInt(payload.positions.size());
				for (BlockPos pos : payload.positions) {
					buf.writeBlockPos(pos);
				}
				buf.writeByte(payload.boreDir.get3DDataValue());
			},
			buf -> {
				int count = buf.readVarInt();
				List<BlockPos> positions = new ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					positions.add(buf.readBlockPos());
				}
				Direction dir = Direction.from3DDataValue(buf.readByte());
				return new BoreTriggerPayload(positions, dir);
			}
	);

	@Override
	public Type<BoreTriggerPayload> type() {
		return TYPE;
	}
}
