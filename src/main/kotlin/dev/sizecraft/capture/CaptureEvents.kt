package dev.sizecraft.capture

import dev.sizecraft.SizeCraftMod
import dev.sizecraft.config.SizeCraftConfig
import dev.sizecraft.dimension.HammerspaceLayout
import dev.sizecraft.network.CaptureStatusPacket
import dev.sizecraft.player.SizeData
import dev.sizecraft.registry.SizeCraftDataComponents
import dev.sizecraft.registry.SizeCraftDimension
import dev.sizecraft.registry.SizeCraftItems
import dev.sizecraft.registry.SizeCraftPermissions
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.GameType
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.server.permission.PermissionAPI

/**
 * Event handlers for the capture/eat system.
 *
 * Handles:
 * - Shift+right-click on player to capture
 * - Server tick to validate capture state
 * - Player logout/login for cleanup
 * - Player death (clone) for release on death
 * - Food consumption completion for eat transition
 */
object CaptureEvents {

    /**
     * Shift+right-click on a player entity to capture them.
     */
    fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
        if (!SizeCraftConfig.captureEnabled) return
        if (event.level.isClientSide) return
        if (event.hand != net.minecraft.world.InteractionHand.MAIN_HAND) return

        val carrier = event.entity as? ServerPlayer ?: return
        val target = event.target as? ServerPlayer ?: return

        // Require shift if configured
        if (SizeCraftConfig.captureRequireShift && !carrier.isShiftKeyDown) return

        // Check permission
        if (!PermissionAPI.getPermission(carrier, SizeCraftPermissions.CAPTURE)) return

        // Can't capture yourself
        if (carrier.uuid == target.uuid) return

        val server = carrier.server
        val manager = CaptureManager.get(server.overworld())

        // Can't capture someone already held
        if (manager.isHeld(target.uuid)) {
            carrier.sendSystemMessage(Component.translatable("sizecraft.capture.already_held"))
            return
        }

        // Can't capture while you are captured/eaten yourself
        if (manager.isHeld(carrier.uuid)) return

        // Check predator/prey flags
        val carrierData = carrier.getData(SizeData.SIZE_DATA)
        val targetData = target.getData(SizeData.SIZE_DATA)

        if (!carrierData.isPredator) {
            carrier.sendSystemMessage(Component.translatable("sizecraft.capture.not_predator"))
            return
        }

        if (!targetData.isPrey) {
            carrier.sendSystemMessage(Component.translatable("sizecraft.capture.not_prey", target.displayName))
            return
        }

        // Check size ratio
        val sizeRatio = carrierData.scale / targetData.scale
        if (sizeRatio < SizeCraftConfig.captureMinSizeRatio) {
            carrier.sendSystemMessage(
                Component.translatable(
                    "sizecraft.capture.too_small",
                    String.format("%.1f", SizeCraftConfig.captureMinSizeRatio)
                )
            )
            return
        }

        // Perform capture
        performCapture(carrier, target, manager)
        event.cancellationResult = InteractionResult.SUCCESS
        event.isCanceled = true
    }

    /**
     * Executes the capture: stores state, gives item, sets spectator mode.
     */
    private fun performCapture(carrier: ServerPlayer, target: ServerPlayer, manager: CaptureManager) {
        // Store the target's current game mode before switching to spectator
        val targetData = target.getData(SizeData.SIZE_DATA)
        targetData.previousGameMode = when (target.gameMode.gameModeForPlayer) {
            GameType.CREATIVE -> 1
            GameType.ADVENTURE -> 2
            GameType.SPECTATOR -> 3
            else -> 0
        }
        target.setData(SizeData.SIZE_DATA, targetData)

        // Register capture in manager
        manager.capture(target.uuid, carrier.uuid)

        // Create and give item to carrier
        val itemStack = CapturedPlayerItem.createStack(target)
        if (!carrier.inventory.add(itemStack)) {
            // Inventory full — drop at carrier's feet
            carrier.drop(itemStack, false)
        }

        // Set target to spectator mode and force camera on carrier
        target.setGameMode(GameType.SPECTATOR)
        target.setCamera(carrier)

        // Notify both players
        carrier.sendSystemMessage(
            Component.translatable("sizecraft.capture.captured", target.displayName)
        )
        target.sendSystemMessage(
            Component.translatable("sizecraft.capture.captured_by", carrier.displayName)
        )

        // Sync capture status to captured player's client
        sendCaptureStatus(target, manager)

        SizeCraftMod.LOGGER.info("${carrier.gameProfile.name} captured ${target.gameProfile.name}")
    }

    /**
     * When a player finishes eating a CapturedPlayerItem, transition them from
     * "captured" to "eaten" (teleport to hammerspace).
     */
    fun onItemUseFinish(event: LivingEntityUseItemEvent.Finish) {
        val entity = event.entity
        if (entity.level().isClientSide) return
        val carrier = entity as? ServerPlayer ?: return

        val stack = event.item
        if (stack.item !is CapturedPlayerItem) return

        val data = stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get()) ?: return
        val server = carrier.server

        val manager = CaptureManager.get(server.overworld())

        // Only process if the player is in "captured" (not already "eaten") state
        if (!manager.isCaptured(data.uuid)) return

        manager.handleEat(carrier, data.uuid, server)

        // Sync capture status to eaten player's client
        val capturedPlayer = server.playerList.getPlayer(data.uuid)
        if (capturedPlayer != null) {
            sendCaptureStatus(capturedPlayer, manager)
        }

        // The item is consumed by the eating mechanic (stack shrinks to 0),
        // but set result stack to empty just in case
        event.resultStack = net.minecraft.world.item.ItemStack.EMPTY

        SizeCraftMod.LOGGER.info("${carrier.gameProfile.name} ate ${data.name}")
    }

    /**
     * Server tick: let CaptureManager verify capture state integrity.
     */
    fun onServerTick(event: ServerTickEvent.Post) {
        val server = event.server
        val manager = CaptureManager.get(server.overworld())
        manager.tick(server)
    }

    /**
     * Prevent CapturedPlayerItem from existing as an ItemEntity in the world.
     * Covers death drops, hoppers, dispensers, or any other source.
     */
    fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        val entity = event.entity
        if (entity !is net.minecraft.world.entity.item.ItemEntity) return
        if (entity.item.item !is CapturedPlayerItem) return

        // Cancel the spawn so the item never enters the world
        event.isCanceled = true
    }

    /**
     * When a carrier disconnects, release all their captured/eaten players.
     */
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val server = player.server ?: return
        val manager = CaptureManager.get(server.overworld())

        // Release all players captured by this carrier
        val capturedPlayers = manager.getCapturedBy(player.uuid)
        val eatenPlayers = manager.getEatenBy(player.uuid)

        for (capturedUuid in capturedPlayers) {
            val capturedPlayer = server.playerList.getPlayer(capturedUuid)
            manager.release(capturedUuid)
            if (capturedPlayer != null) {
                manager.releasePlayer(capturedPlayer, player, server)
                sendCaptureStatus(capturedPlayer, manager)
                SizeCraftMod.LOGGER.info("Released ${capturedPlayer.gameProfile.name} (carrier ${player.gameProfile.name} disconnected)")
            }
        }

        for (eatenUuid in eatenPlayers) {
            val eatenPlayer = server.playerList.getPlayer(eatenUuid)
            manager.release(eatenUuid)
            if (eatenPlayer != null) {
                manager.releasePlayer(eatenPlayer, player, server)
                sendCaptureStatus(eatenPlayer, manager)
                SizeCraftMod.LOGGER.info("Released ${eatenPlayer.gameProfile.name} from hammerspace (carrier ${player.gameProfile.name} disconnected)")
            }
        }

        // If this player was captured/eaten themselves, release them
        if (manager.isHeld(player.uuid)) {
            val carrierUuid = manager.getCarrier(player.uuid) ?: manager.getEater(player.uuid)
            val carrier = carrierUuid?.let { server.playerList.getPlayer(it) }
            manager.release(player.uuid)

            // Remove the item from the carrier's inventory
            if (carrier != null) {
                removeCapturedPlayerItem(carrier, player.uuid)
            }

            SizeCraftMod.LOGGER.info("Released ${player.gameProfile.name} (they disconnected)")
        }
    }

    /**
     * When a captured/eaten player logs back in, restore their state.
     * If their carrier is offline, release them to world spawn.
     */
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val server = player.server ?: return
        val manager = CaptureManager.get(server.overworld())

        if (manager.isCaptured(player.uuid)) {
            val carrierUuid = manager.getCarrier(player.uuid) ?: return
            val carrier = server.playerList.getPlayer(carrierUuid)

            if (carrier != null) {
                // Carrier still online — re-apply spectator mode and camera
                player.setGameMode(GameType.SPECTATOR)
                player.server.execute {
                    player.setCamera(carrier)
                }
            } else {
                // Carrier offline — release the player
                manager.release(player.uuid)
                manager.releasePlayer(player, null, server)
                SizeCraftMod.LOGGER.info("Released ${player.gameProfile.name} on login (carrier offline)")
            }
        } else if (manager.isEaten(player.uuid)) {
            // Player was eaten — they should remain in hammerspace
            // No special action needed; they're already in the hammerspace dimension
        }
    }

    /**
     * On death (clone event), release captured players.
     * The carrier's items drop on death, so CaptureManager.tick() will also detect
     * the missing item and release. But handling it here is more immediate.
     */
    fun onPlayerClone(event: PlayerEvent.Clone) {
        if (!event.isWasDeath) return
        val oldPlayer = event.original as? ServerPlayer ?: return
        val server = oldPlayer.server ?: return
        val manager = CaptureManager.get(server.overworld())

        // If the dead player was a carrier, release all captured players
        // (Items drop on death, so tick() would catch this too, but this is faster)
        val capturedPlayers2 = manager.getCapturedBy(oldPlayer.uuid)
        for (capturedUuid in capturedPlayers2) {
            val capturedPlayer = server.playerList.getPlayer(capturedUuid)
            manager.release(capturedUuid)
            if (capturedPlayer != null) {
                manager.releasePlayer(capturedPlayer, oldPlayer, server)
                sendCaptureStatus(capturedPlayer, manager)
                SizeCraftMod.LOGGER.info("Released ${capturedPlayer.gameProfile.name} (carrier ${oldPlayer.gameProfile.name} died)")
            }
        }

        val eatenPlayers = manager.getEatenBy(oldPlayer.uuid)
        for (eatenUuid in eatenPlayers) {
            val eatenPlayer = server.playerList.getPlayer(eatenUuid)
            manager.release(eatenUuid)
            if (eatenPlayer != null) {
                manager.releasePlayer(eatenPlayer, oldPlayer, server)
                sendCaptureStatus(eatenPlayer, manager)
                SizeCraftMod.LOGGER.info("Released ${eatenPlayer.gameProfile.name} from hammerspace (carrier ${oldPlayer.gameProfile.name} died)")
            }
        }

        // If the dead player was captured/eaten, release them
        if (manager.isHeld(oldPlayer.uuid)) {
            val carrierUuid = manager.getCarrier(oldPlayer.uuid) ?: manager.getEater(oldPlayer.uuid)
            val carrier = carrierUuid?.let { server.playerList.getPlayer(it) }
            manager.release(oldPlayer.uuid)

            // Remove the item from the carrier's inventory
            if (carrier != null) {
                removeCapturedPlayerItem(carrier, oldPlayer.uuid)
            }
        }
    }

    /**
     * Removes the CapturedPlayerItem for a specific UUID from a carrier's inventory.
     */
    fun removeCapturedPlayerItem(carrier: ServerPlayer, capturedUuid: java.util.UUID) {
        for (i in 0 until carrier.inventory.containerSize) {
            val stack = carrier.inventory.getItem(i)
            if (stack.isEmpty) continue
            if (stack.item !is CapturedPlayerItem) continue
            val data = stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get()) ?: continue
            if (data.uuid == capturedUuid) {
                carrier.inventory.setItem(i, net.minecraft.world.item.ItemStack.EMPTY)
                return
            }
        }
    }

    /**
     * Removes all CapturedPlayerItem stacks from a carrier's inventory.
     */
    fun removeAllCapturedItems(carrier: ServerPlayer) {
        for (i in 0 until carrier.inventory.containerSize) {
            val stack = carrier.inventory.getItem(i)
            if (stack.isEmpty) continue
            if (stack.item !is CapturedPlayerItem) continue
            carrier.inventory.setItem(i, net.minecraft.world.item.ItemStack.EMPTY)
        }
    }

    /**
     * Sends a CaptureStatusPacket to the given player reflecting their current state.
     */
    fun sendCaptureStatus(player: ServerPlayer, manager: CaptureManager) {
        val server = player.server
        val currentTick = server.overworld().gameTime

        val packet = when {
            manager.isCaptured(player.uuid) -> {
                val carrierUuid = manager.getCarrier(player.uuid)
                val carrier = carrierUuid?.let { server.playerList.getPlayer(it) }
                val carrierName = carrier?.gameProfile?.name ?: "Unknown"
                CaptureStatusPacket(CaptureStatusPacket.STATUS_CAPTURED, carrierName, 0)
            }
            manager.isEaten(player.uuid) -> {
                val eaterUuid = manager.getEater(player.uuid)
                val eater = eaterUuid?.let { server.playerList.getPlayer(it) }
                val eaterName = eater?.gameProfile?.name ?: "Unknown"
                val remaining = manager.getEscapeTicksRemaining(player.uuid, currentTick)
                CaptureStatusPacket(CaptureStatusPacket.STATUS_EATEN, eaterName, remaining)
            }
            else -> {
                CaptureStatusPacket(CaptureStatusPacket.STATUS_FREE, "", 0)
            }
        }

        PacketDistributor.sendToPlayer(player, packet)
    }

    /**
     * Prevent players from placing blocks outside their hammerspace room interior.
     */
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val level = player.serverLevel()
        if (level.dimension() != SizeCraftDimension.HAMMERSPACE_LEVEL_KEY) return

        val pos = event.pos
        val playerX = Math.floorDiv(player.blockPosition().x, HammerspaceLayout.GRID_SPACING)
        val playerZ = Math.floorDiv(player.blockPosition().z, HammerspaceLayout.GRID_SPACING)
        val roomIndex = playerX + playerZ * HammerspaceLayout.GRID_WIDTH

        val origin = HammerspaceLayout.getRoomOrigin(roomIndex)
        val inBounds = pos.x >= origin.x && pos.x < origin.x + HammerspaceLayout.ROOM_SIZE &&
                pos.y >= origin.y && pos.y < origin.y + HammerspaceLayout.ROOM_SIZE &&
                pos.z >= origin.z && pos.z < origin.z + HammerspaceLayout.ROOM_SIZE

        if (!inBounds) {
            event.isCanceled = true
        }
    }
}
