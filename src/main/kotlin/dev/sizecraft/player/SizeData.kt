package dev.sizecraft.player

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.sizecraft.SizeCraftMod
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.Optional
import java.util.function.Supplier
import kotlin.math.ceil
import kotlin.math.pow

/**
 * Per-player size data stored as a NeoForge attachment.
 * Persisted to NBT automatically via codec, copied on death.
 *
 * Size is stored as [steps] on a base-6 logarithmic scale:
 *   scale = 6^steps
 * Each integer step multiplies or divides size by 6.
 * Fractional steps are supported (e.g. 0.5 = √6 ≈ 2.45x).
 */
data class SizeData(
    var steps: Double = 0.0,
    var minSteps: Double? = null,  // null = use global config value
    var maxSteps: Double? = null,  // null = use global config value
    var isPredator: Boolean = false,
    var isPrey: Boolean = false,
    var hammerspaceEscapable: Boolean = true,
    var escapeDelayTicks: Int = -1,             // -1 = use global config value
    var previousGameMode: Int = 0,
    var stomachSlot: String = "stomach",
    var returnDimension: String = "",
    var returnX: Double = 0.0,
    var returnY: Double = 0.0,
    var returnZ: Double = 0.0,
    var returnYaw: Float = 0f,
    var returnPitch: Float = 0f,
) {
    /** Minecraft SCALE attribute value derived from steps: 6^steps. */
    val scale: Double get() = 6.0.pow(steps)

    /**
     * Block-interaction grid tier for this player.
     * Fractional steps snap upward to the next tier (ceil).
     * Examples: steps=0.0 → 0, steps=0.5 → 1, steps=1.0 → 1, steps=-0.5 → 0, steps=-1.0 → -1
     */
    val gridTier: Int get() = ceil(steps).toInt()

    companion object {

        val CODEC: Codec<SizeData> = RecordCodecBuilder.create { builder ->
            builder.group(
                Codec.DOUBLE.fieldOf("steps").forGetter(SizeData::steps),
                Codec.DOUBLE.optionalFieldOf("min_steps")
                    .forGetter { Optional.ofNullable(it.minSteps) },
                Codec.DOUBLE.optionalFieldOf("max_steps")
                    .forGetter { Optional.ofNullable(it.maxSteps) },
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
            ).apply(builder, ::fromCodec)
        }

        private fun fromCodec(
            steps: Double,
            minSteps: Optional<Double>,
            maxSteps: Optional<Double>,
            isPredator: Boolean,
            isPrey: Boolean,
            hammerspaceEscapable: Boolean,
            escapeDelayTicks: Int,
            previousGameMode: Int,
            stomachSlot: String,
            returnDimension: String,
            returnX: Double,
            returnY: Double,
            returnZ: Double,
            returnYaw: Float,
            returnPitch: Float,
        ): SizeData = SizeData(
            steps = steps,
            minSteps = minSteps.orElse(null),
            maxSteps = maxSteps.orElse(null),
            isPredator = isPredator,
            isPrey = isPrey,
            hammerspaceEscapable = hammerspaceEscapable,
            escapeDelayTicks = escapeDelayTicks,
            previousGameMode = previousGameMode,
            stomachSlot = stomachSlot,
            returnDimension = returnDimension,
            returnX = returnX,
            returnY = returnY,
            returnZ = returnZ,
            returnYaw = returnYaw,
            returnPitch = returnPitch,
        )

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
