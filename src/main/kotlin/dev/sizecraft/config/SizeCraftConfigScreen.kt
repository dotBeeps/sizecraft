package dev.sizecraft.config

import dev.sizecraft.SizeCraftMod
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.Requirement
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object SizeCraftConfigScreen {

    fun createConfigScreen(parent: Screen): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("sizecraft.config.title"))
            .setSavingRunnable(::saveConfig)

        val entryBuilder = builder.entryBuilder()
        val isAdmin = Minecraft.getInstance().player?.hasPermissions(2) ?: false

        // === Size Category (admin only) ===
        if (isAdmin) {
            val sizeCategory = builder.getOrCreateCategory(Component.translatable("sizecraft.config.category.size"))

            sizeCategory.addEntry(
                entryBuilder.startDoubleField(
                    Component.translatable("sizecraft.config.default_scale"),
                    SizeCraftConfig.defaultScale
                )
                    .setDefaultValue { SizeCraftConfig._defaultScale.default }
                    .setMin(0.001)
                    .setMax(100.0)
                    .setTooltip(Component.translatable("sizecraft.config.default_scale.tooltip"))
                    .setSaveConsumer { SizeCraftConfig._defaultScale.set(it) }
                    .build()
            )

            sizeCategory.addEntry(
                entryBuilder.startDoubleField(
                    Component.translatable("sizecraft.config.global_min"),
                    SizeCraftConfig.globalMinScale
                )
                    .setDefaultValue { SizeCraftConfig._globalMinScale.default }
                    .setMin(0.001)
                    .setMax(100.0)
                    .setTooltip(Component.translatable("sizecraft.config.global_min.tooltip"))
                    .setSaveConsumer { SizeCraftConfig._globalMinScale.set(it) }
                    .build()
            )

            sizeCategory.addEntry(
                entryBuilder.startDoubleField(
                    Component.translatable("sizecraft.config.global_max"),
                    SizeCraftConfig.globalMaxScale
                )
                    .setDefaultValue { SizeCraftConfig._globalMaxScale.default }
                    .setMin(0.001)
                    .setMax(100.0)
                    .setTooltip(Component.translatable("sizecraft.config.global_max.tooltip"))
                    .setSaveConsumer { SizeCraftConfig._globalMaxScale.set(it) }
                    .build()
            )
        }

        // === Behaviour Category (admin only) ===
        if (isAdmin) {
            val behaviourCategory = builder.getOrCreateCategory(Component.translatable("sizecraft.config.category.behaviour"))

            behaviourCategory.addEntry(
                entryBuilder.startBooleanToggle(
                    Component.translatable("sizecraft.config.allow_self_resize"),
                    SizeCraftConfig.allowSelfResize
                )
                    .setDefaultValue { SizeCraftConfig._allowSelfResize.default }
                    .setTooltip(Component.translatable("sizecraft.config.allow_self_resize.tooltip"))
                    .setSaveConsumer { SizeCraftConfig._allowSelfResize.set(it) }
                    .build()
            )
        }

        // === Visuals Category (always shown) ===
        val visualsCategory = builder.getOrCreateCategory(Component.translatable("sizecraft.config.category.visuals"))

        visualsCategory.addEntry(
            entryBuilder.startIntSlider(
                Component.translatable("sizecraft.config.animation_ticks"),
                SizeCraftConfig.animationDurationTicks,
                0, 100
            )
                .setDefaultValue { SizeCraftConfig._animationDurationTicks.default }
                .setTooltip(Component.translatable("sizecraft.config.animation_ticks.tooltip"))
                .setSaveConsumer { SizeCraftConfig._animationDurationTicks.set(it) }
                .build()
        )

        visualsCategory.addEntry(
            entryBuilder.startBooleanToggle(
                Component.translatable("sizecraft.config.enable_particles"),
                SizeCraftConfig.enableParticles
            )
                .setDefaultValue { SizeCraftConfig._enableParticles.default }
                .setTooltip(Component.translatable("sizecraft.config.enable_particles.tooltip"))
                .setSaveConsumer { SizeCraftConfig._enableParticles.set(it) }
                .build()
        )

        // === Capture Category (admin only) ===
        if (isAdmin) {
            val captureCategory = builder.getOrCreateCategory(Component.translatable("sizecraft.config.category.capture"))

            val captureEnabledEntry = entryBuilder.startBooleanToggle(
                Component.translatable("sizecraft.config.capture_enabled"),
                SizeCraftConfig.captureEnabled
            )
                .setDefaultValue { SizeCraftConfig._captureEnabled.default }
                .setTooltip(Component.translatable("sizecraft.config.capture_enabled.tooltip"))
                .setSaveConsumer { SizeCraftConfig._captureEnabled.set(it) }
                .build()
            captureCategory.addEntry(captureEnabledEntry)

            val captureReq = Requirement.isTrue(captureEnabledEntry::getValue)

            val captureMinRatioEntry = entryBuilder.startDoubleField(
                Component.translatable("sizecraft.config.capture_min_ratio"),
                SizeCraftConfig.captureMinSizeRatio
            )
                .setDefaultValue { SizeCraftConfig._captureMinSizeRatio.default }
                .setMin(1.0)
                .setMax(10.0)
                .setTooltip(Component.translatable("sizecraft.config.capture_min_ratio.tooltip"))
                .setSaveConsumer { SizeCraftConfig._captureMinSizeRatio.set(it) }
                .build()
            captureMinRatioEntry.setRequirement(captureReq)
            captureCategory.addEntry(captureMinRatioEntry)

            val captureRequireShiftEntry = entryBuilder.startBooleanToggle(
                Component.translatable("sizecraft.config.capture_require_shift"),
                SizeCraftConfig.captureRequireShift
            )
                .setDefaultValue { SizeCraftConfig._captureRequireShift.default }
                .setTooltip(Component.translatable("sizecraft.config.capture_require_shift.tooltip"))
                .setSaveConsumer { SizeCraftConfig._captureRequireShift.set(it) }
                .build()
            captureRequireShiftEntry.setRequirement(captureReq)
            captureCategory.addEntry(captureRequireShiftEntry)

            val defaultHammerspaceEscapableEntry = entryBuilder.startBooleanToggle(
                Component.translatable("sizecraft.config.default_hammerspace_escapable"),
                SizeCraftConfig.defaultHammerspaceEscapable
            )
                .setDefaultValue { SizeCraftConfig._defaultHammerspaceEscapable.default }
                .setTooltip(Component.translatable("sizecraft.config.default_hammerspace_escapable.tooltip"))
                .setSaveConsumer { SizeCraftConfig._defaultHammerspaceEscapable.set(it) }
                .build()
            defaultHammerspaceEscapableEntry.setRequirement(captureReq)
            captureCategory.addEntry(defaultHammerspaceEscapableEntry)

            val defaultEscapeDelayEntry = entryBuilder.startIntSlider(
                Component.translatable("sizecraft.config.default_escape_delay"),
                SizeCraftConfig.defaultEscapeDelayTicks,
                0, 12000
            )
                .setDefaultValue { SizeCraftConfig._defaultEscapeDelayTicks.default }
                .setTooltip(Component.translatable("sizecraft.config.default_escape_delay.tooltip"))
                .setSaveConsumer { SizeCraftConfig._defaultEscapeDelayTicks.set(it) }
                .build()
            defaultEscapeDelayEntry.setRequirement(
                Requirement.all(captureReq, Requirement.isTrue(defaultHammerspaceEscapableEntry::getValue))
            )
            captureCategory.addEntry(defaultEscapeDelayEntry)

            val maxEscapeDelayEntry = entryBuilder.startIntSlider(
                Component.translatable("sizecraft.config.max_escape_delay"),
                SizeCraftConfig.maxEscapeDelayTicks,
                0, 72000
            )
                .setDefaultValue { SizeCraftConfig._maxEscapeDelayTicks.default }
                .setTooltip(Component.translatable("sizecraft.config.max_escape_delay.tooltip"))
                .setSaveConsumer { SizeCraftConfig._maxEscapeDelayTicks.set(it) }
                .build()
            maxEscapeDelayEntry.setRequirement(captureReq)
            captureCategory.addEntry(maxEscapeDelayEntry)

            val forceHammerspaceEscapableEntry = entryBuilder.startBooleanToggle(
                Component.translatable("sizecraft.config.force_hammerspace_escapable"),
                SizeCraftConfig.forceHammerspaceEscapable
            )
                .setDefaultValue { SizeCraftConfig._forceHammerspaceEscapable.default }
                .setTooltip(Component.translatable("sizecraft.config.force_hammerspace_escapable.tooltip"))
                .setSaveConsumer { SizeCraftConfig._forceHammerspaceEscapable.set(it) }
                .build()
            forceHammerspaceEscapableEntry.setRequirement(captureReq)
            captureCategory.addEntry(forceHammerspaceEscapableEntry)
        }

        return builder.build()
    }

    private fun saveConfig() {
        SizeCraftMod.LOGGER.info("SizeCraft config saved")
    }
}
