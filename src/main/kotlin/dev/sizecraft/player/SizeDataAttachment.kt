package dev.sizecraft.player

import dev.sizecraft.SizeCraftMod
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

/**
 * NeoForge attachment registration for [SizeData].
 * Kept separate so that instantiating [SizeData] in unit tests does not
 * trigger the Minecraft bootstrap via [DeferredRegister].
 */
object SizeDataAttachment {

    private val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SizeCraftMod.MOD_ID)

    val SIZE_DATA: Supplier<AttachmentType<SizeData>> = ATTACHMENT_TYPES.register("size_data") { ->
        AttachmentType.builder(Supplier { SizeData() })
            .serialize(SizeData.CODEC)
            .copyOnDeath()
            .build()
    }

    fun register(modBus: IEventBus) {
        ATTACHMENT_TYPES.register(modBus)
    }
}
