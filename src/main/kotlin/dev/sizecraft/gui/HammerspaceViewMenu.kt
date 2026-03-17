package dev.sizecraft.gui

import dev.sizecraft.capture.CaptureEvents
import dev.sizecraft.capture.CaptureManager
import dev.sizecraft.capture.CapturedPlayerItem
import dev.sizecraft.dimension.HammerspacePopulator
import dev.sizecraft.registry.SizeCraftDataComponents
import dev.sizecraft.registry.SizeCraftDimension
import dev.sizecraft.registry.SizeCraftMenuTypes
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.UUID

class HammerspaceViewMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    val ownerUuid: UUID,
    val slotId: String,
    private val container: SimpleContainer = SimpleContainer(27),
) : AbstractContainerMenu(SizeCraftMenuTypes.HAMMERSPACE_VIEW.get(), containerId) {

    constructor(containerId: Int, playerInventory: Inventory, data: FriendlyByteBuf)
            : this(containerId, playerInventory, data.readUUID(), data.readUtf())

    init {
        checkContainerSize(container, 27)
        container.startOpen(playerInventory.player)

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(CompartmentSlot(container, row * 9 + col, 8 + col * 18, 18 + row * 18))
            }
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }

        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY

        val stack = slot.item
        val copy = stack.copy()

        if (index < 27) {
            // Compartment → player inventory (onTake fires, handles handleUneat)
            if (!moveItemStackTo(stack, 27, slots.size, true)) return ItemStack.EMPTY
        } else {
            // Player inventory → compartment not allowed
            return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.set(ItemStack.EMPTY)
        else slot.setChanged()

        return copy
    }

    override fun stillValid(player: Player): Boolean = !playerInventory.player.isRemoved

    override fun clicked(slotIndex: Int, button: Int, clickType: ClickType, player: Player) {
        if (clickType == ClickType.THROW && slotIndex in 0 until 27) {
            val slot = slots[slotIndex]
            val stack = slot.item
            if (!stack.isEmpty) {
                handleRelease(stack, player as? ServerPlayer)
                slot.set(ItemStack.EMPTY)
                return
            }
        }
        super.clicked(slotIndex, button, clickType, player)
    }

    override fun removed(player: Player) {
        super.removed(player)
        container.stopOpen(player)
    }

    private fun handleRelease(stack: ItemStack, carrier: ServerPlayer?) {
        carrier ?: return
        val data = stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get()) ?: return
        val server = carrier.server
        val manager = CaptureManager.get(server.overworld())
        val capturedPlayer = server.playerList.getPlayer(data.uuid) ?: return
        manager.release(data.uuid)
        manager.releasePlayer(capturedPlayer, carrier, server)
        CaptureEvents.sendCaptureStatus(capturedPlayer, manager)
    }

    inner class CompartmentSlot(container: SimpleContainer, index: Int, x: Int, y: Int) :
        Slot(container, index, x, y) {

        override fun onTake(player: Player, stack: ItemStack) {
            super.onTake(player, stack)
            val carrier = player as? ServerPlayer ?: return
            val data = stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get()) ?: return
            val server = carrier.server
            val manager = CaptureManager.get(server.overworld())
            if (manager.isEaten(data.uuid)) {
                manager.handleUneat(carrier, data.uuid, server)
            }
        }
    }

    companion object {
        fun openFor(carrier: ServerPlayer, ownerUuid: UUID, slotId: String) {
            val server = carrier.server
            val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY) ?: return
            val populator = HammerspacePopulator.get(hammerspace)
            val captureManager = CaptureManager.get(server.overworld())

            val compartmentIndex = populator.getCompartmentIndex(ownerUuid, slotId)
            val compartmentEntry = populator.getCompartments(ownerUuid)[slotId]
            val displayName = compartmentEntry?.displayName ?: slotId

            carrier.openMenu(object : MenuProvider {
                override fun getDisplayName(): Component = Component.literal(displayName)

                override fun createMenu(
                    containerId: Int,
                    inventory: Inventory,
                    player: Player,
                ): AbstractContainerMenu {
                    val container = SimpleContainer(27)

                    if (compartmentIndex != null) {
                        val eaten = captureManager.getEatenBy(ownerUuid, slotId)
                        for ((i, capturedUuid) in eaten.withIndex()) {
                            if (i >= 27) break
                            val capturedPlayer = server.playerList.getPlayer(capturedUuid)
                            if (capturedPlayer != null) {
                                container.setItem(i, CapturedPlayerItem.createStack(capturedPlayer))
                            }
                        }
                    }

                    return HammerspaceViewMenu(containerId, inventory, ownerUuid, slotId, container)
                }
            }, { buf ->
                buf.writeUUID(ownerUuid)
                buf.writeUtf(slotId)
            })
        }
    }
}
