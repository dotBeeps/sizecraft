package dev.sizecraft.player

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.sizecraft.SizeCraftMod
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

/**
 * Per-player size data stored as a NeoForge attachment.
 * Persisted to NBT automatically via codec, copied on death.
 */
data class SizeData(
    var scale: Double = 1.0,
    var minScale: Double = -1.0,  // -1.0 means "use global config value"
    var maxScale: Double = -1.0,  // -1.0 means "use global config value"
    var isPredator: Boolean = false,
    var isPrey: Boolean = false,
    var hammerspaceEscapable: Boolean = true,   // whether this player's hammerspace can be escaped
    var escapeDelayTicks: Int = -1,             // -1 means "use global config value"
    var previousGameMode: Int = 0,              // stored when captured (0=survival, 1=creative, 2=adventure, 3=spectator)
    var stomachSlot: String = "stomach",        // which compartment receives eaten players
    var returnDimension: String = "",            // dimension key before entering hammerspace ("" = no stored position)
    var returnX: Double = 0.0,
    var returnY: Double = 0.0,
    var returnZ: Double = 0.0,
    var returnYaw: Float = 0f,
    var returnPitch: Float = 0f,
) {

    companion object {

        val CODEC: Codec<SizeData> = RecordCodecBuilder.create { builder ->
            builder.group(
                Codec.DOUBLE.fieldOf("scale").forGetter(SizeData::scale),
                Codec.DOUBLE.fieldOf("min_scale").forGetter(SizeData::minScale),
                Codec.DOUBLE.fieldOf("max_scale").forGetter(SizeData::maxScale),
                Codec.BOOL.optionalFieldOf("is_predator", false).forGetter(SizeData::isPredator),
                Codec.BOOL.optionalFieldOf("is_prey", false).forGetter(SizeData::isPrey),
                Codec.BOOL.optionalFieldOf("hammerspace_escapable", true).forGetter(SizeData::hammerspaceEscapable),
                Codec.INT.optionalFieldOf("escape_delay_ticks", -1).forGetter(SizeData::escapeDelayTicks),
                Codec.INT.optionalFieldOf("previous_game_mode", 0).forGetter(SizeData::previousGameMode),
                Codec.STRING.optionalFieldOf("stomach_slot", "stomach").forGetter(SizeData::stomachSlot),
                Codec.STRING.optionalFieldOf("return_dimension", "").forGetter(SizeData::returnDimension),
                Codec.DOUBLE.optionalFieldOf("return_x", 0.0).forGetter(SizeData::returnX),
                Codec.DOUBLE.optionalFieldOf("return_y", 0.0).forGetter(SizeData::returnY),
                Codec.DOUBLE.optionalFieldOf("return_z", 0.0).forGetter(SizeData::returnZ),
                Codec.FLOAT.optionalFieldOf("return_yaw", 0f).forGetter(SizeData::returnYaw),
                Codec.FLOAT.optionalFieldOf("return_pitch", 0f).forGetter(SizeData::returnPitch),
            ).apply(builder, ::SizeData)
        }

        private val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SizeCraftMod.MOD_ID)

        val SIZE_DATA: Supplier<AttachmentType<SizeData>> = ATTACHMENT_TYPES.register("size_data") { ->
            AttachmentType.builder(Supplier { SizeData() })
                .serialize(CODEC)
                .copyOnDeath()
                .build()
        }

        fun register(modBus: IEventBus) {
            ATTACHMENT_TYPES.register(modBus)
        }
    }
}
