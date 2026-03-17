package dev.sizecraft.registry

import dev.sizecraft.SizeCraftMod
import dev.sizecraft.gui.HammerspaceViewMenu
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object SizeCraftMenuTypes {

    private val MENUS: DeferredRegister<MenuType<*>> =
        DeferredRegister.create(Registries.MENU, SizeCraftMod.MOD_ID)

    val HAMMERSPACE_VIEW: Supplier<MenuType<HammerspaceViewMenu>> =
        MENUS.register("hammerspace_view", Supplier {
            IMenuTypeExtension.create { containerId, inventory, data ->
                HammerspaceViewMenu(containerId, inventory, data)
            }
        })

    fun register(modBus: IEventBus) {
        MENUS.register(modBus)
    }
}
