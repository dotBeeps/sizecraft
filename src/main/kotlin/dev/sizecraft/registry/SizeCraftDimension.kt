package dev.sizecraft.registry

import dev.sizecraft.SizeCraftMod
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.dimension.LevelStem

/**
 * Resource keys for the hammerspace void dimension.
 */
object SizeCraftDimension {

    val HAMMERSPACE_LEVEL_KEY: ResourceKey<Level> = ResourceKey.create(
        Registries.DIMENSION,
        SizeCraftMod.id("hammerspace")
    )

    val HAMMERSPACE_DIM_TYPE_KEY: ResourceKey<DimensionType> = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        SizeCraftMod.id("hammerspace")
    )

    val HAMMERSPACE_LEVEL_STEM_KEY: ResourceKey<LevelStem> = ResourceKey.create(
        Registries.LEVEL_STEM,
        SizeCraftMod.id("hammerspace")
    )
}
