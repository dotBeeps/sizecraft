package dev.sizecraft.capture

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.util.UUID

/**
 * Data stored on a CapturedPlayerItem via DataComponents.
 * Records who was captured, their display name, and their scale at capture time.
 */
data class CapturedPlayerData(
    val uuid: UUID,
    val name: String,
    val capturedScale: Double,
) {
    companion object {
        val CODEC: Codec<CapturedPlayerData> = RecordCodecBuilder.create { builder ->
            builder.group(
                UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(CapturedPlayerData::uuid),
                Codec.STRING.fieldOf("name").forGetter(CapturedPlayerData::name),
                Codec.DOUBLE.fieldOf("scale").forGetter(CapturedPlayerData::capturedScale),
            ).apply(builder, ::CapturedPlayerData)
        }

        val STREAM_CODEC: StreamCodec<ByteBuf, CapturedPlayerData> = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CapturedPlayerData::uuid,
            ByteBufCodecs.stringUtf8(64), CapturedPlayerData::name,
            ByteBufCodecs.DOUBLE, CapturedPlayerData::capturedScale,
            ::CapturedPlayerData,
        )
    }
}
