package dev.sizecraft.capture

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.sizecraft.SizeCraftMod
import dev.sizecraft.config.SizeCraftConfig
import dev.sizecraft.dimension.HammerspaceLayout
import dev.sizecraft.dimension.HammerspacePopulator
import dev.sizecraft.player.SizeData
import dev.sizecraft.registry.SizeCraftDataComponents
import dev.sizecraft.registry.SizeCraftDimension
import net.minecraft.core.UUIDUtil
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.GameType
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.UUID

/**
 * Server-side state manager tracking active capture/eaten relationships.
 * Persisted as SavedData on the overworld level.
 *
 * - capturedBy: player UUID -> carrier UUID (player is in carrier's inventory, spectating them)
 * - eatenBy: player UUID -> carrier UUID (player was eaten, now in carrier's hammerspace)
 * - eatenInCompartment: player UUID -> slot ID (which compartment the eaten player is in)
 * - escapeAvailableAt: player UUID -> game tick when escape becomes available
 */
class CaptureManager(
    val capturedBy: MutableMap<UUID, UUID> = mutableMapOf(),
    val eatenBy: MutableMap<UUID, UUID> = mutableMapOf(),
    val eatenInCompartment: MutableMap<UUID, String> = mutableMapOf(),
    val escapeAvailableAt: MutableMap<UUID, Long> = mutableMapOf(),
) : SavedData() {

    companion object {
        private const val DATA_NAME = "sizecraft_captures"

        val CODEC: Codec<CaptureManager> = RecordCodecBuilder.create { builder ->
            builder.group(
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC)
                    .fieldOf("captured_by")
                    .forGetter { it.capturedBy.toMap() },
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC)
                    .fieldOf("eaten_by")
                    .forGetter { it.eatenBy.toMap() },
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING)
                    .optionalFieldOf("eaten_in_compartment", emptyMap())
                    .forGetter { it.eatenInCompartment.toMap() },
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG)
                    .fieldOf("escape_available_at")
                    .forGetter { it.escapeAvailableAt.toMap() },
            ).apply(builder) { captured, eaten, compartments, escape ->
                CaptureManager(
                    captured.toMutableMap(),
                    eaten.toMutableMap(),
                    compartments.toMutableMap(),
                    escape.toMutableMap(),
                )
            }
        }

        private val SAVED_DATA_TYPE: SavedDataType<CaptureManager> = SavedDataType(
            DATA_NAME,
            { CaptureManager() },
            CODEC,
        )

        fun get(level: ServerLevel): CaptureManager {
            return level.dataStorage.computeIfAbsent(SAVED_DATA_TYPE)
        }
    }

    /** Returns true if the player is currently captured (in someone's inventory). */
    fun isCaptured(playerUuid: UUID): Boolean = playerUuid in capturedBy

    /** Returns true if the player was eaten (in someone's hammerspace). */
    fun isEaten(playerUuid: UUID): Boolean = playerUuid in eatenBy

    /** Returns true if the player is captured or eaten. */
    fun isHeld(playerUuid: UUID): Boolean = isCaptured(playerUuid) || isEaten(playerUuid)

    /** Returns the carrier UUID if the player is captured. */
    fun getCarrier(playerUuid: UUID): UUID? = capturedBy[playerUuid]

    /** Returns the eater UUID if the player was eaten. */
    fun getEater(playerUuid: UUID): UUID? = eatenBy[playerUuid]

    /** Returns the compartment slot ID for an eaten player, or null. */
    fun getEatenInCompartment(capturedUuid: UUID): String? = eatenInCompartment[capturedUuid]

    /** Returns all UUIDs currently captured by the given carrier. */
    fun getCapturedBy(carrierUuid: UUID): List<UUID> =
        capturedBy.filter { it.value == carrierUuid }.keys.toList()

    /** Returns all UUIDs currently eaten by the given carrier. */
    fun getEatenBy(carrierUuid: UUID): List<UUID> =
        eatenBy.filter { it.value == carrierUuid }.keys.toList()

    /** Returns all UUIDs eaten by the given carrier in a specific compartment slot. */
    fun getEatenBy(carrierUuid: UUID, slotId: String): List<UUID> =
        eatenBy.filter { it.value == carrierUuid && eatenInCompartment[it.key] == slotId }.keys.toList()

    /**
     * Registers a new capture: target is now carried by carrier.
     */
    fun capture(targetUuid: UUID, carrierUuid: UUID) {
        capturedBy[targetUuid] = carrierUuid
        setDirty()
    }

    /**
     * Moves a captured player from "captured" to "eaten" state in the given compartment.
     */
    fun eat(targetUuid: UUID, carrierUuid: UUID, currentTick: Long, compartmentId: String, server: MinecraftServer) {
        capturedBy.remove(targetUuid)
        eatenBy[targetUuid] = carrierUuid
        eatenInCompartment[targetUuid] = compartmentId

        val carrierData = getCarrierSizeData(carrierUuid, server)
        val escapeDelay = if (carrierData != null && carrierData.escapeDelayTicks >= 0) {
            carrierData.escapeDelayTicks.toLong()
        } else {
            SizeCraftConfig.defaultEscapeDelayTicks.toLong()
        }.coerceAtMost(SizeCraftConfig.maxEscapeDelayTicks.toLong())

        escapeAvailableAt[targetUuid] = currentTick + escapeDelay
        setDirty()
    }

    /**
     * Releases a player from either captured or eaten state.
     */
    fun release(targetUuid: UUID) {
        capturedBy.remove(targetUuid)
        eatenBy.remove(targetUuid)
        eatenInCompartment.remove(targetUuid)
        escapeAvailableAt.remove(targetUuid)
        setDirty()
    }

    /**
     * Returns true if the eaten player can currently escape.
     */
    fun canEscape(playerUuid: UUID, currentTick: Long, server: MinecraftServer): Boolean {
        if (!isEaten(playerUuid)) return false

        val eaterUuid = eatenBy[playerUuid] ?: return false

        val eaterPlayer = server.playerList.getPlayer(eaterUuid)
        val eaterData = eaterPlayer?.getData(SizeData.SIZE_DATA)

        val escapable = SizeCraftConfig.forceHammerspaceEscapable ||
                (eaterData?.hammerspaceEscapable ?: SizeCraftConfig.defaultHammerspaceEscapable)

        if (!escapable) return false

        val availableAt = escapeAvailableAt[playerUuid] ?: return true
        return currentTick >= availableAt
    }

    /**
     * Returns remaining ticks before escape is available, or 0 if already available.
     */
    fun getEscapeTicksRemaining(playerUuid: UUID, currentTick: Long): Long {
        val availableAt = escapeAvailableAt[playerUuid] ?: return 0
        return (availableAt - currentTick).coerceAtLeast(0)
    }

    /**
     * Server tick handler — checks that captured players are still properly tracked.
     */
    fun tick(server: MinecraftServer) {
        val toRelease = mutableListOf<Pair<UUID, ReleaseReason>>()

        for ((capturedUuid, carrierUuid) in capturedBy.toMap()) {
            val carrier = server.playerList.getPlayer(carrierUuid)
            if (carrier == null) continue

            if (!carrierHasItem(carrier, capturedUuid)) {
                toRelease.add(capturedUuid to ReleaseReason.ITEM_LOST)
            }
        }

        for ((uuid, reason) in toRelease) {
            val carrierUuid = capturedBy[uuid] ?: continue
            val carrier = server.playerList.getPlayer(carrierUuid)
            val capturedPlayer = server.playerList.getPlayer(uuid)

            release(uuid)

            if (capturedPlayer != null) {
                releasePlayer(capturedPlayer, carrier, server)
                SizeCraftMod.LOGGER.info("Released ${capturedPlayer.gameProfile.name} (reason: $reason)")
            }
        }
    }

    /**
     * Handles eating a captured player item.
     * Reads the carrier's stomachSlot and teleports prey to that compartment.
     */
    fun handleEat(carrier: ServerPlayer, capturedUuid: UUID, server: MinecraftServer) {
        val capturedPlayer = server.playerList.getPlayer(capturedUuid)
        val currentTick = server.overworld().gameTime

        val carrierData = carrier.getData(SizeData.SIZE_DATA)
        val stomachSlot = carrierData.stomachSlot

        val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY)
        if (hammerspace != null) {
            val populator = HammerspacePopulator.get(hammerspace)
            val roomIndex = populator.getOrAllocateCompartment(carrier.uuid, stomachSlot)

            if (!populator.isRoomInitialized(roomIndex)) {
                populator.initializeRoom(hammerspace, roomIndex)
            }

            eat(capturedUuid, carrier.uuid, currentTick, stomachSlot, server)

            if (capturedPlayer != null) {
                val data = capturedPlayer.getData(SizeData.SIZE_DATA)
                data.returnDimension = capturedPlayer.serverLevel().dimension().location().toString()
                data.returnX = capturedPlayer.x
                data.returnY = capturedPlayer.y
                data.returnZ = capturedPlayer.z
                data.returnYaw = capturedPlayer.yRot
                data.returnPitch = capturedPlayer.xRot
                capturedPlayer.setData(SizeData.SIZE_DATA, data)

                val spawnPos = HammerspaceLayout.getRoomSpawnPos(roomIndex)
                restoreGameMode(capturedPlayer)

                capturedPlayer.teleportTo(
                    hammerspace,
                    spawnPos.x.toDouble() + 0.5,
                    spawnPos.y.toDouble(),
                    spawnPos.z.toDouble() + 0.5,
                    setOf(),
                    capturedPlayer.yRot,
                    capturedPlayer.xRot,
                    false,
                )

                capturedPlayer.setCamera(capturedPlayer)

                val compartmentEntry = populator.getCompartments(carrier.uuid)[stomachSlot]
                val hsName = compartmentEntry?.let { Component.literal(it.displayName) }
                    ?: Component.translatable("sizecraft.hammerspace.default_name")

                capturedPlayer.sendSystemMessage(
                    Component.translatable("sizecraft.capture.eaten_by", carrier.displayName, hsName)
                )
                carrier.sendSystemMessage(
                    Component.translatable("sizecraft.capture.ate", capturedPlayer.displayName, hsName)
                )
            }
        }
    }

    /**
     * Moves an eaten player back from hammerspace to the carrier's inventory state.
     * Prey transitions from eaten → captured, spectating the carrier again.
     * Item stays in carrier's inventory.
     */
    fun handleUneat(carrier: ServerPlayer, capturedUuid: UUID, server: MinecraftServer) {
        eatenBy.remove(capturedUuid)
        eatenInCompartment.remove(capturedUuid)
        capturedBy[capturedUuid] = carrier.uuid
        escapeAvailableAt.remove(capturedUuid)
        setDirty()

        val capturedPlayer = server.playerList.getPlayer(capturedUuid) ?: return
        capturedPlayer.setGameMode(GameType.SPECTATOR)
        capturedPlayer.teleportTo(
            carrier.serverLevel(),
            carrier.x, carrier.y, carrier.z,
            setOf(), carrier.yRot, carrier.xRot, false
        )
        capturedPlayer.setCamera(carrier)
    }

    /**
     * Releases a player from capture/eaten state, restoring their position and gamemode.
     */
    fun releasePlayer(player: ServerPlayer, carrier: ServerPlayer?, server: MinecraftServer) {
        restoreGameMode(player)
        player.setCamera(player)

        if (carrier != null) {
            val carrierLevel = carrier.serverLevel()
            player.teleportTo(
                carrierLevel,
                carrier.x,
                carrier.y,
                carrier.z,
                setOf(),
                carrier.yRot,
                carrier.xRot,
                false,
            )
        } else {
            val overworld = server.overworld()
            val spawnPos = overworld.sharedSpawnPos
            player.teleportTo(
                overworld,
                spawnPos.x.toDouble() + 0.5,
                spawnPos.y.toDouble(),
                spawnPos.z.toDouble() + 0.5,
                setOf(),
                player.yRot,
                player.xRot,
                false,
            )
        }

        player.sendSystemMessage(
            Component.translatable("sizecraft.capture.released")
        )
    }

    /**
     * Restores a player's game mode from their stored previousGameMode.
     */
    private fun restoreGameMode(player: ServerPlayer) {
        val data = player.getData(SizeData.SIZE_DATA)
        val gameType = when (data.previousGameMode) {
            1 -> GameType.CREATIVE
            2 -> GameType.ADVENTURE
            3 -> GameType.SPECTATOR
            else -> GameType.SURVIVAL
        }
        player.setGameMode(gameType)
    }

    /**
     * Checks if a carrier player has a CapturedPlayerItem for the given UUID in their inventory.
     */
    private fun carrierHasItem(carrier: ServerPlayer, capturedUuid: UUID): Boolean {
        for (i in 0 until carrier.inventory.containerSize) {
            val stack = carrier.inventory.getItem(i)
            if (stack.isEmpty) continue
            if (stack.item !is CapturedPlayerItem) continue
            val data = stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get()) ?: continue
            if (data.uuid == capturedUuid) return true
        }
        return false
    }

    private fun getCarrierSizeData(carrierUuid: UUID, server: MinecraftServer): SizeData? {
        val carrier = server.playerList.getPlayer(carrierUuid) ?: return null
        return carrier.getData(SizeData.SIZE_DATA)
    }

    enum class ReleaseReason {
        ITEM_LOST,
        CARRIER_DISCONNECT,
        COMMAND,
        ESCAPE,
    }
}
