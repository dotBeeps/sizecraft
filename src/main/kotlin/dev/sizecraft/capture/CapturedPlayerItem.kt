package dev.sizecraft.capture

import dev.sizecraft.SizeCraftMod
import dev.sizecraft.config.SizeCraftConfig
import dev.sizecraft.player.SizeDataAttachment
import dev.sizecraft.dimension.HammerspaceLayout
import dev.sizecraft.dimension.HammerspacePopulator
import dev.sizecraft.registry.SizeCraftDataComponents
import dev.sizecraft.registry.SizeCraftDimension
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.item.component.Consumables
import net.minecraft.world.food.FoodProperties

/**
 * An item representing a captured player. Appears in the carrier's inventory.
 * Eating it sends the captured player to the carrier's hammerspace.
 *
 * The item is not stackable (max stack 1) and uses the standard eating animation.
 */
class CapturedPlayerItem(properties: Properties) : Item(
    properties
        .stacksTo(1)
        .food(
            FoodProperties.Builder()
                .nutrition(0)
                .saturationModifier(0f)
                .alwaysEdible()
                .build(),
            Consumables.defaultFood().build()
        )
) {

    override fun getName(stack: ItemStack): Component {
        val data = stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get())
        return if (data != null) {
            Component.translatable("item.sizecraft.captured_player.named", data.name)
        } else {
            Component.translatable("item.sizecraft.captured_player")
        }
    }

    /**
     * Called every tick while this item is in a player's inventory.
     * Re-asserts spectator camera on the captured player so they
     * can't switch away from spectating the carrier.
     */
    override fun inventoryTick(stack: ItemStack, level: ServerLevel, entity: Entity, slot: EquipmentSlot?) {
        val carrier = entity as? ServerPlayer ?: return
        val data = stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get()) ?: return

        val server = level.server
        val capturedPlayer = server.playerList.getPlayer(data.uuid) ?: return

        val manager = CaptureManager.get(server.overworld())

        // Only manage spectator camera while the player is in "captured" state (not yet eaten)
        if (manager.isCaptured(data.uuid)) {
            // Re-assert spectator camera every tick
            if (capturedPlayer.camera != carrier) {
                capturedPlayer.setCamera(carrier)
            }
        }
    }

    /**
     * When the carrier drops the item, release the captured player and allow the drop.
     * The dropped ItemEntity will be canceled by CaptureEvents.onEntityJoinLevel()
     * so the item never materializes in the world — it simply vanishes.
     */
    override fun onDroppedByPlayer(stack: ItemStack, player: net.minecraft.world.entity.player.Player): Boolean {
        val serverPlayer = player as? ServerPlayer ?: return true
        val data = stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get()) ?: return true

        val server = serverPlayer.server
        val manager = CaptureManager.get(server.overworld())

        if (manager.isCaptured(data.uuid)) {
            val capturedPlayer = server.playerList.getPlayer(data.uuid)
            manager.release(data.uuid)
            if (capturedPlayer != null) {
                manager.releasePlayer(capturedPlayer, serverPlayer, server)
                CaptureEvents.sendCaptureStatus(capturedPlayer, manager)
                SizeCraftMod.LOGGER.info("Released ${data.name} (carrier ${serverPlayer.gameProfile.name} dropped the item)")
            }
        }

        // Return true to allow the drop (item leaves inventory).
        // onEntityJoinLevel will cancel the ItemEntity spawn so it never hits the ground.
        return true
    }

    companion object {
        /**
         * Creates a CapturedPlayerItem stack with the given player's data.
         */
        fun createStack(target: ServerPlayer): ItemStack {
            val stack = ItemStack(dev.sizecraft.registry.SizeCraftItems.CAPTURED_PLAYER.get())
            val playerData = target.getData(SizeDataAttachment.SIZE_DATA)
            stack.set(
                SizeCraftDataComponents.CAPTURED_PLAYER.get(),
                CapturedPlayerData(
                    uuid = target.uuid,
                    name = target.gameProfile.name,
                    capturedScale = playerData.scale,
                )
            )
            return stack
        }
    }
}
