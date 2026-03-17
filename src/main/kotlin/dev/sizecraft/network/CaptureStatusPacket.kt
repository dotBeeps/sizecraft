package dev.sizecraft.network

import dev.sizecraft.SizeCraftMod
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar

/**
 * Network packet sent server → client to notify the client of their capture status.
 *
 * Used to:
 * - Show/hide the "you are captured" HUD overlay
 * - Display escape timer when eaten
 * - Restrict certain client-side inputs while captured
 *
 * Status values:
 *   0 = free (not captured)
 *   1 = captured (in carrier's inventory, spectating)
 *   2 = eaten (in carrier's hammerspace)
 */
data class CaptureStatusPacket(
    val status: Int,
    val carrierName: String,
    val escapeTicksRemaining: Long,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<CaptureStatusPacket> = TYPE

    companion object {

        const val STATUS_FREE = 0
        const val STATUS_CAPTURED = 1
        const val STATUS_EATEN = 2

        val TYPE: CustomPacketPayload.Type<CaptureStatusPacket> = CustomPacketPayload.Type(
            SizeCraftMod.id("capture_status")
        )

        val STREAM_CODEC: StreamCodec<ByteBuf, CaptureStatusPacket> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CaptureStatusPacket::status,
            ByteBufCodecs.STRING_UTF8, CaptureStatusPacket::carrierName,
            ByteBufCodecs.VAR_LONG, CaptureStatusPacket::escapeTicksRemaining,
            ::CaptureStatusPacket,
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
                context.enqueueWork {
                    CaptureStatusClientState.update(payload)
                }
            }
        }
    }
}

/**
 * Client-side state tracking for capture status.
 * Updated by incoming CaptureStatusPackets.
 */
object CaptureStatusClientState {
    var status: Int = CaptureStatusPacket.STATUS_FREE
        private set
    var carrierName: String = ""
        private set
    var escapeTicksRemaining: Long = 0
        private set

    val isCaptured: Boolean get() = status == CaptureStatusPacket.STATUS_CAPTURED
    val isEaten: Boolean get() = status == CaptureStatusPacket.STATUS_EATEN
    val isFree: Boolean get() = status == CaptureStatusPacket.STATUS_FREE

    fun update(packet: CaptureStatusPacket) {
        status = packet.status
        carrierName = packet.carrierName
        escapeTicksRemaining = packet.escapeTicksRemaining
    }

    fun reset() {
        status = CaptureStatusPacket.STATUS_FREE
        carrierName = ""
        escapeTicksRemaining = 0
    }
}
