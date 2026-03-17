package dev.sizecraft.network

import dev.sizecraft.SizeCraftMod
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler
import net.neoforged.neoforge.network.registration.PayloadRegistrar

/**
 * Network packet sent from server to client to synchronize a player's scale
 * and trigger the lerp animation + particle effect.
 */
data class SizeSyncPacket(
    val targetScale: Double,
    val animationTicks: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SizeSyncPacket> = TYPE

    companion object {

        val TYPE: CustomPacketPayload.Type<SizeSyncPacket> = CustomPacketPayload.Type(
            SizeCraftMod.id("size_sync")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, SizeSyncPacket> = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SizeSyncPacket::targetScale,
            ByteBufCodecs.VAR_INT, SizeSyncPacket::animationTicks,
            ::SizeSyncPacket,
        )

        fun register(modBus: IEventBus) {
            modBus.addListener(::onRegisterPayloads)
        }

        private fun onRegisterPayloads(event: RegisterPayloadHandlersEvent) {
            val registrar: PayloadRegistrar = event.registrar(SizeCraftMod.MOD_ID)

            registrar.playToClient(
                TYPE,
                STREAM_CODEC,
            ) { payload, context ->
                // Client-side handling: update the SizeAnimator
                context.enqueueWork {
                    dev.sizecraft.rendering.SizeAnimator.onSizeSync(payload.targetScale, payload.animationTicks)
                    dev.sizecraft.rendering.SizeParticleEffect.onSizeChanged(
                        context.player(),
                        payload.targetScale,
                    )
                }
            }
        }
    }
}
