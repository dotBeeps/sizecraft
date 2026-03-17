package dev.sizecraft

import dev.sizecraft.config.SizeCraftConfigScreen
import dev.sizecraft.gui.HammerspaceViewScreen
import dev.sizecraft.rendering.CapturedPlayerRenderer
import dev.sizecraft.rendering.SizeAnimator
import dev.sizecraft.rendering.SizeParticleEffect
import dev.sizecraft.registry.SizeCraftMenuTypes
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge

@Mod(value = SizeCraftMod.MOD_ID, dist = [Dist.CLIENT])
class SizeCraftClient(modBus: IEventBus, container: ModContainer) {

    init {
        SizeCraftMod.LOGGER.info("Initializing SizeCraft client")

        val forgeBus = NeoForge.EVENT_BUS
        forgeBus.addListener(SizeAnimator::onClientTick)
        forgeBus.addListener(SizeParticleEffect::onClientTick)

        // Register special model renderer for captured player item (mod bus event)
        modBus.addListener(::onRegisterSpecialModelRenderer)
        modBus.addListener(::onRegisterMenuScreens)

        // Register config screen factory directly on the mod container
        container.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { _, parent -> SizeCraftConfigScreen.createConfigScreen(parent) }
        )
    }

    private fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {
        event.register(SizeCraftMenuTypes.HAMMERSPACE_VIEW.get(), ::HammerspaceViewScreen)
    }

    private fun onRegisterSpecialModelRenderer(event: RegisterSpecialModelRendererEvent) {
        event.register(
            SizeCraftMod.id("captured_player"),
            CapturedPlayerRenderer.Unbaked.MAP_CODEC
        )
    }
}
