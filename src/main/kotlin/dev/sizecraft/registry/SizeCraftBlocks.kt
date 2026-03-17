package dev.sizecraft.registry

import dev.sizecraft.SizeCraftMod
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister

object SizeCraftBlocks {

    private val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(SizeCraftMod.MOD_ID)
    private val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(SizeCraftMod.MOD_ID)

    /**
     * Hammerspace boundary wall — unbreakable like bedrock.
     * Surrounds each player's personal pocket dimension room.
     */
    val HAMMERSPACE_WALL: DeferredBlock<Block> = BLOCKS.register("hammerspace_wall") { registryName ->
        Block(
            BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, registryName))
                .mapColor(MapColor.COLOR_BLACK)
                .strength(-1.0f, 3600000.0f) // Unbreakable (same as bedrock)
                .sound(SoundType.STONE)
                .noLootTable()
                .pushReaction(PushReaction.BLOCK)
                .isValidSpawn { _, _, _, _ -> false }
                .noOcclusion()
        )
    }

    val HAMMERSPACE_WALL_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(HAMMERSPACE_WALL)

    fun register(modBus: IEventBus) {
        BLOCKS.register(modBus)
        ITEMS.register(modBus)
    }
}
