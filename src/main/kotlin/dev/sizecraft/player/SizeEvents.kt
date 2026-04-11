package dev.sizecraft.player

import dev.sizecraft.SizeCraftMod
import dev.sizecraft.config.SizeCraftConfig
import dev.sizecraft.network.SizeSyncPacket
import kotlin.math.pow
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.network.PacketDistributor

/**
 * Handles applying and syncing player scale on login, death, and dimension change.
 */
object SizeEvents {

    /** Unique modifier ID for the sizecraft scale modifier. */
    val SCALE_MODIFIER_ID: ResourceLocation = SizeCraftMod.id("player_scale")

    /**
     * Applies the given scale to a player by modifying the vanilla SCALE attribute.
     * Should only be called server-side.
     */
    fun applyScale(player: ServerPlayer, scale: Double, animationTicks: Int = 20) {
        val attr = player.getAttribute(Attributes.SCALE) ?: return
        attr.removeModifier(SCALE_MODIFIER_ID)

        if (scale != 1.0) {
            // SCALE attribute base is 1.0; ADD_MULTIPLIED_BASE means final = base * (1 + value)
            // So to reach target `scale`, we set value = scale - 1.0
            attr.addTransientModifier(
                AttributeModifier(
                    SCALE_MODIFIER_ID,
                    scale - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
            )
        }

        // Sync to client for lerp animation
        PacketDistributor.sendToPlayer(player, SizeSyncPacket(scale, animationTicks))
    }

    fun getEffectiveMinScale(data: SizeData): Double {
        val minSteps = data.minSteps ?: SizeCraftConfig.globalMinScale
        return 6.0.pow(minSteps)
    }

    fun getEffectiveMaxScale(data: SizeData): Double {
        val maxSteps = data.maxSteps ?: SizeCraftConfig.globalMaxScale
        return 6.0.pow(maxSteps)
    }

    fun clampScale(scale: Double, data: SizeData): Double {
        val min = getEffectiveMinScale(data)
        val max = getEffectiveMaxScale(data)
        return scale.coerceIn(min, max)
    }

    // --- Event Handlers ---

    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        applyScale(player, data.scale, animationTicks = 0) // Instant on login, no animation
    }

    fun onPlayerClone(event: PlayerEvent.Clone) {
        // copyOnDeath handles data transfer, but we need to reapply the attribute modifier
        if (!event.isWasDeath) return
        val newPlayer = event.entity as? ServerPlayer ?: return
        val data = newPlayer.getData(SizeDataAttachment.SIZE_DATA)
        // Delay by 1 tick to ensure the player entity is fully initialized
        newPlayer.server?.execute {
            applyScale(newPlayer, data.scale, animationTicks = 0)
        }
    }

    fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        applyScale(player, data.scale, animationTicks = 0)
    }
}
