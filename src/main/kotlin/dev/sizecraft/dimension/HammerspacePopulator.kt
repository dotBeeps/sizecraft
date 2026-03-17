package dev.sizecraft.dimension

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.sizecraft.SizeCraftMod
import dev.sizecraft.registry.SizeCraftBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.UUID

/**
 * Manages the initialization and allocation of hammerspace rooms.
 * Stores UUID → compartment map (slotId → CompartmentEntry) and tracks which rooms are initialized.
 *
 * Persisted as SavedData on the hammerspace dimension level.
 * Migrates old `player_rooms` (single room per player) to a `"stomach"` compartment automatically.
 */
class HammerspacePopulator(
    private val playerCompartments: MutableMap<UUID, MutableMap<String, CompartmentEntry>> = mutableMapOf(),
    private val initializedRooms: MutableSet<Int> = mutableSetOf(),
    private var nextIndex: Int = 0
) : SavedData() {

    companion object {
        private const val DATA_NAME = "sizecraft_hammerspace"

        val CODEC: Codec<HammerspacePopulator> = RecordCodecBuilder.create { builder ->
            builder.group(
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                    .optionalFieldOf("player_rooms", emptyMap())
                    .forGetter { emptyMap() }, // Never written — read-only migration path
                Codec.unboundedMap(
                    UUIDUtil.STRING_CODEC,
                    Codec.unboundedMap(Codec.STRING, CompartmentEntry.CODEC)
                )
                    .optionalFieldOf("player_compartments", emptyMap())
                    .forGetter { it.playerCompartments.mapValues { (_, m) -> m.toMap() } },
                Codec.INT.listOf()
                    .fieldOf("initialized_rooms")
                    .forGetter { it.initializedRooms.toList() },
                Codec.INT
                    .fieldOf("next_index")
                    .forGetter { it.nextIndex }
            ).apply(builder) { oldRooms, compartments, initialized, next ->
                val finalCompartments: MutableMap<UUID, MutableMap<String, CompartmentEntry>> =
                    if (compartments.isEmpty() && oldRooms.isNotEmpty()) {
                        oldRooms.mapValues { (_, index) ->
                            mutableMapOf("stomach" to CompartmentEntry(index, "Stomach"))
                        }.toMutableMap()
                    } else {
                        compartments.mapValues { (_, m) -> m.toMutableMap() }.toMutableMap()
                    }
                HammerspacePopulator(finalCompartments, initialized.toMutableSet(), next)
            }
        }

        private val SAVED_DATA_TYPE: SavedDataType<HammerspacePopulator> = SavedDataType(
            DATA_NAME,
            { HammerspacePopulator() },
            CODEC
        )

        fun get(level: ServerLevel): HammerspacePopulator {
            return level.dataStorage.computeIfAbsent(SAVED_DATA_TYPE)
        }
    }

    /**
     * Gets or allocates a room index for the given player UUID and slot ID.
     * If the slot doesn't exist, creates it with a capitalised display name.
     */
    fun getOrAllocateCompartment(playerUuid: UUID, slotId: String): Int {
        val compartments = playerCompartments.getOrPut(playerUuid) { mutableMapOf() }
        val existing = compartments[slotId]
        if (existing != null) return existing.roomIndex

        val index = nextIndex++
        compartments[slotId] = CompartmentEntry(index, slotId.replaceFirstChar { it.uppercase() })
        setDirty()
        return index
    }

    /**
     * Returns the room index for a player's slot, or null if it doesn't exist.
     */
    fun getCompartmentIndex(playerUuid: UUID, slotId: String): Int? =
        playerCompartments[playerUuid]?.get(slotId)?.roomIndex

    /**
     * Returns a snapshot of all compartments for the given player.
     */
    fun getCompartments(playerUuid: UUID): Map<String, CompartmentEntry> =
        playerCompartments[playerUuid]?.toMap() ?: emptyMap()

    /**
     * Deletes a compartment. Returns false if it doesn't exist.
     * Caller is responsible for checking occupancy before calling.
     */
    fun deleteCompartment(playerUuid: UUID, slotId: String): Boolean {
        val compartments = playerCompartments[playerUuid] ?: return false
        if (!compartments.containsKey(slotId)) return false
        compartments.remove(slotId)
        setDirty()
        return true
    }

    /**
     * Updates the display name of an existing compartment.
     */
    fun renameCompartment(playerUuid: UUID, slotId: String, displayName: String) {
        val compartments = playerCompartments[playerUuid] ?: return
        val entry = compartments[slotId] ?: return
        compartments[slotId] = entry.copy(displayName = displayName)
        setDirty()
    }

    /**
     * Returns true if the room has been physically built (walls placed).
     */
    fun isRoomInitialized(index: Int): Boolean = index in initializedRooms

    /**
     * Marks a room as initialized and physically builds the boundary walls
     * and spawn platform in the given level.
     */
    fun initializeRoom(level: ServerLevel, index: Int) {
        if (isRoomInitialized(index)) return

        SizeCraftMod.LOGGER.info("Initializing hammerspace room $index")

        val shellMin = HammerspaceLayout.getRoomShellMin(index)
        val shellMax = HammerspaceLayout.getRoomShellMax(index)
        val origin = HammerspaceLayout.getRoomOrigin(index)

        val wallBlock = SizeCraftBlocks.HAMMERSPACE_WALL.get().defaultBlockState()

        for (x in shellMin.x..shellMax.x) {
            for (z in shellMin.z..shellMax.z) {
                level.setBlock(BlockPos(x, shellMin.y, z), wallBlock, 0)
                level.setBlock(BlockPos(x, shellMax.y, z), wallBlock, 0)
            }
        }

        for (x in shellMin.x..shellMax.x) {
            for (y in (shellMin.y + 1)..(shellMax.y - 1)) {
                level.setBlock(BlockPos(x, y, shellMin.z), wallBlock, 0)
                level.setBlock(BlockPos(x, y, shellMax.z), wallBlock, 0)
            }
        }

        for (z in (shellMin.z + 1)..(shellMax.z - 1)) {
            for (y in (shellMin.y + 1)..(shellMax.y - 1)) {
                level.setBlock(BlockPos(shellMin.x, y, z), wallBlock, 0)
                level.setBlock(BlockPos(shellMax.x, y, z), wallBlock, 0)
            }
        }

        val platformCenter = HammerspaceLayout.getRoomSpawnPos(index)
        val obsidian = Blocks.OBSIDIAN.defaultBlockState()
        for (dx in -3..4) {
            for (dz in -3..4) {
                level.setBlock(
                    BlockPos(platformCenter.x + dx, origin.y, platformCenter.z + dz),
                    obsidian,
                    0
                )
            }
        }

        val chunkMinX = shellMin.x shr 4
        val chunkMaxX = shellMax.x shr 4
        val chunkMinZ = shellMin.z shr 4
        val chunkMaxZ = shellMax.z shr 4
        for (cx in chunkMinX..chunkMaxX) {
            for (cz in chunkMinZ..chunkMaxZ) {
                level.getChunk(cx, cz).markUnsaved()
            }
        }

        initializedRooms.add(index)
        setDirty()

        SizeCraftMod.LOGGER.info("Hammerspace room $index initialized at ${origin.x}, ${origin.y}, ${origin.z}")
    }
}
