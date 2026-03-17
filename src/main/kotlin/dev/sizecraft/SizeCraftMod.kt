package dev.sizecraft

import dev.sizecraft.capture.CaptureEvents
import dev.sizecraft.command.SizeCraftCommand
import dev.sizecraft.config.SizeCraftConfig
import dev.sizecraft.network.CaptureStatusPacket
import dev.sizecraft.network.SizeSyncPacket
import dev.sizecraft.player.SizeData
import dev.sizecraft.player.SizeEvents
import dev.sizecraft.registry.SizeCraftBlocks
import dev.sizecraft.registry.SizeCraftDataComponents
import dev.sizecraft.registry.SizeCraftDimension
import dev.sizecraft.registry.SizeCraftItems
import dev.sizecraft.registry.SizeCraftMenuTypes
import dev.sizecraft.registry.SizeCraftPermissions
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(SizeCraftMod.MOD_ID)
class SizeCraftMod(modBus: IEventBus, container: ModContainer) {

    companion object {
        const val MOD_ID = "sizecraft"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        fun id(path: String): ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
    }

    init {
        LOGGER.info("Initializing SizeCraft - player size manipulation mod")

        container.registerConfig(ModConfig.Type.SERVER, SizeCraftConfig.SERVER_SPEC)
        container.registerConfig(ModConfig.Type.CLIENT, SizeCraftConfig.CLIENT_SPEC)

        // Register deferred registries on the mod bus
        SizeCraftBlocks.register(modBus)
        SizeCraftItems.register(modBus)
        SizeCraftDataComponents.register(modBus)
        SizeData.register(modBus)
        SizeSyncPacket.register(modBus)
        CaptureStatusPacket.register(modBus)
        SizeCraftMenuTypes.register(modBus)

        // Register event handlers on the game event bus
        val forgeBus = NeoForge.EVENT_BUS
        forgeBus.addListener(SizeEvents::onPlayerLogin)
        forgeBus.addListener(SizeEvents::onPlayerClone)
        forgeBus.addListener(SizeEvents::onPlayerChangedDimension)
        forgeBus.addListener(SizeCraftCommand::onRegisterCommands)

        // Capture system event handlers
        forgeBus.addListener(CaptureEvents::onEntityInteract)
        forgeBus.addListener(CaptureEvents::onItemUseFinish)
        forgeBus.addListener(CaptureEvents::onServerTick)
        forgeBus.addListener(CaptureEvents::onEntityJoinLevel)
        forgeBus.addListener(CaptureEvents::onPlayerLogout)
        forgeBus.addListener(CaptureEvents::onPlayerLogin)
        forgeBus.addListener(CaptureEvents::onPlayerClone)

        // Register permission nodes (fires on mod bus, not game bus)
        modBus.addListener(SizeCraftPermissions::onRegisterPermissions)

        // Hammerspace block placement restriction
        forgeBus.addListener(CaptureEvents::onBlockPlace)
    }
}
