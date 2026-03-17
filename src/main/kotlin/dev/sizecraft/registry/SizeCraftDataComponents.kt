package dev.sizecraft.registry

import dev.sizecraft.SizeCraftMod
import dev.sizecraft.capture.CapturedPlayerData
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Custom DataComponentTypes for sizecraft items.
 */
object SizeCraftDataComponents {

    private val DATA_COMPONENTS: DeferredRegister<DataComponentType<*>> =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SizeCraftMod.MOD_ID)

    /**
     * Stores captured player info (UUID, name, scale) on a CapturedPlayerItem stack.
     */
    val CAPTURED_PLAYER: DeferredHolder<DataComponentType<*>, DataComponentType<CapturedPlayerData>> =
        DATA_COMPONENTS.register("captured_player") { ->
            DataComponentType.builder<CapturedPlayerData>()
                .persistent(CapturedPlayerData.CODEC)
                .networkSynchronized(CapturedPlayerData.STREAM_CODEC)
                .cacheEncoding()
                .build()
        }

    fun register(modBus: IEventBus) {
        DATA_COMPONENTS.register(modBus)
    }
}
