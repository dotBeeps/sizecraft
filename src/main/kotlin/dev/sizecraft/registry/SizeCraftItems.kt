package dev.sizecraft.registry

import dev.sizecraft.SizeCraftMod
import dev.sizecraft.capture.CapturedPlayerItem
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Item registry for sizecraft.
 */
object SizeCraftItems {

    private val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(SizeCraftMod.MOD_ID)

    /**
     * The captured player item — represents a player being carried in another player's inventory.
     */
    val CAPTURED_PLAYER: DeferredItem<CapturedPlayerItem> = ITEMS.register("captured_player") { registryName ->
        CapturedPlayerItem(
            Item.Properties().setId(ResourceKey.create(Registries.ITEM, registryName))
        )
    }

    fun register(modBus: IEventBus) {
        ITEMS.register(modBus)
    }
}
