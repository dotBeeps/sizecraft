package dev.sizecraft.dimension

import net.minecraft.core.BlockPos

/**
 * Computes hammerspace room coordinates from a sequential room index.
 *
 * Each room is a 16x16x16 cube. Grid spacing is 80 blocks
 * (16 room + 64 buffer wall). Rooms are laid out on a 1024-wide grid.
 */
object HammerspaceLayout {

    /** Interior size of each room in blocks. */
    const val ROOM_SIZE: Int = 16

    /** Distance between room origins (room + buffer). */
    const val GRID_SPACING: Int = 80

    /** Width of the grid in rooms before wrapping to next row. */
    const val GRID_WIDTH: Int = 1024

    /** Y level of the room floor (slightly above world min to avoid bedrock issues). */
    const val ROOM_FLOOR_Y: Int = 64

    /**
     * Returns the origin (min corner) of the room for the given index.
     * The origin is the bottom-northwest corner of the interior space.
     */
    fun getRoomOrigin(index: Int): BlockPos {
        val gridX = index % GRID_WIDTH
        val gridZ = index / GRID_WIDTH
        return BlockPos(
            gridX * GRID_SPACING,
            ROOM_FLOOR_Y,
            gridZ * GRID_SPACING,
        )
    }

    /**
     * Returns the center of the room's floor for a spawn position.
     * Player spawns on top of the obsidian platform at the center.
     */
    fun getRoomSpawnPos(index: Int): BlockPos {
        val origin = getRoomOrigin(index)
        return BlockPos(
            origin.x + ROOM_SIZE / 2,
            origin.y + 1, // On top of the platform
            origin.z + ROOM_SIZE / 2,
        )
    }

    /**
     * Returns the min corner of the room shell (including walls).
     * The wall is 1 block thick around the interior.
     */
    fun getRoomShellMin(index: Int): BlockPos {
        val origin = getRoomOrigin(index)
        return BlockPos(origin.x - 1, origin.y - 1, origin.z - 1)
    }

    /**
     * Returns the max corner of the room shell (including walls).
     */
    fun getRoomShellMax(index: Int): BlockPos {
        val origin = getRoomOrigin(index)
        return BlockPos(
            origin.x + ROOM_SIZE,
            origin.y + ROOM_SIZE,
            origin.z + ROOM_SIZE,
        )
    }
}
