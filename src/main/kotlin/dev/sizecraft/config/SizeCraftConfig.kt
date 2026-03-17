package dev.sizecraft.config

import net.neoforged.neoforge.common.ModConfigSpec

object SizeCraftConfig {

    val SERVER_SPEC: ModConfigSpec
    val CLIENT_SPEC: ModConfigSpec

    internal val _defaultScale: ModConfigSpec.DoubleValue
    internal val _globalMinScale: ModConfigSpec.DoubleValue
    internal val _globalMaxScale: ModConfigSpec.DoubleValue
    internal val _allowSelfResize: ModConfigSpec.BooleanValue
    internal val _captureEnabled: ModConfigSpec.BooleanValue
    internal val _captureMinSizeRatio: ModConfigSpec.DoubleValue
    internal val _captureRequireShift: ModConfigSpec.BooleanValue
    internal val _defaultHammerspaceEscapable: ModConfigSpec.BooleanValue
    internal val _defaultEscapeDelayTicks: ModConfigSpec.IntValue
    internal val _maxEscapeDelayTicks: ModConfigSpec.IntValue
    internal val _forceHammerspaceEscapable: ModConfigSpec.BooleanValue
    internal val _animationDurationTicks: ModConfigSpec.IntValue
    internal val _enableParticles: ModConfigSpec.BooleanValue

    val defaultScale: Double get() = _defaultScale.get()
    val globalMinScale: Double get() = _globalMinScale.get()
    val globalMaxScale: Double get() = _globalMaxScale.get()
    val allowSelfResize: Boolean get() = _allowSelfResize.get()
    val captureEnabled: Boolean get() = _captureEnabled.get()
    val captureMinSizeRatio: Double get() = _captureMinSizeRatio.get()
    val captureRequireShift: Boolean get() = _captureRequireShift.get()
    val defaultHammerspaceEscapable: Boolean get() = _defaultHammerspaceEscapable.get()
    val defaultEscapeDelayTicks: Int get() = _defaultEscapeDelayTicks.get()
    val maxEscapeDelayTicks: Int get() = _maxEscapeDelayTicks.get()
    val forceHammerspaceEscapable: Boolean get() = _forceHammerspaceEscapable.get()
    val animationDurationTicks: Int get() = _animationDurationTicks.get()
    val enableParticles: Boolean get() = _enableParticles.get()

    const val hammerspaceRoomSize: Int = 128

    init {
        val serverBuilder = ModConfigSpec.Builder()

        serverBuilder.push("size")
        _defaultScale = serverBuilder
            .comment("Default scale for new players")
            .defineInRange("defaultScale", 1.0, 0.001, 100.0)
        _globalMinScale = serverBuilder
            .comment("Global minimum scale (unless per-player override is set)")
            .defineInRange("globalMinScale", 0.1, 0.001, 100.0)
        _globalMaxScale = serverBuilder
            .comment("Global maximum scale (unless per-player override is set)")
            .defineInRange("globalMaxScale", 5.0, 0.001, 100.0)
        serverBuilder.pop()

        serverBuilder.push("permissions")
        _allowSelfResize = serverBuilder
            .comment("Whether non-op players can resize themselves via command")
            .define("allowSelfResize", true)
        serverBuilder.pop()

        serverBuilder.push("capture")
        _captureEnabled = serverBuilder
            .comment("Master toggle for the capture/eat mechanic")
            .define("captureEnabled", true)
        _captureMinSizeRatio = serverBuilder
            .comment("Minimum size ratio (carrier/target) required to capture")
            .defineInRange("captureMinSizeRatio", 1.5, 1.0, 10.0)
        _captureRequireShift = serverBuilder
            .comment("Whether the carrier must be sneaking to capture")
            .define("captureRequireShift", true)
        _defaultHammerspaceEscapable = serverBuilder
            .comment("Default: whether a player's hammerspace is escapable by eaten players")
            .define("defaultHammerspaceEscapable", true)
        _defaultEscapeDelayTicks = serverBuilder
            .comment("Default escape delay in ticks before an eaten player can escape (600 = 30 seconds)")
            .defineInRange("defaultEscapeDelayTicks", 600, 0, 72000)
        _maxEscapeDelayTicks = serverBuilder
            .comment("Server-imposed maximum escape delay in ticks (6000 = 5 minutes)")
            .defineInRange("maxEscapeDelayTicks", 6000, 0, 72000)
        _forceHammerspaceEscapable = serverBuilder
            .comment("If true, all hammerspaces are forcibly escapable regardless of player setting")
            .define("forceHammerspaceEscapable", false)
        serverBuilder.pop()

        SERVER_SPEC = serverBuilder.build()

        val clientBuilder = ModConfigSpec.Builder()

        clientBuilder.push("visuals")
        _animationDurationTicks = clientBuilder
            .comment("Number of ticks for the resize lerp animation (0 = instant)")
            .defineInRange("animationDurationTicks", 20, 0, 100)
        _enableParticles = clientBuilder
            .comment("Whether to show sparkle particles on resize")
            .define("enableParticles", true)
        clientBuilder.pop()

        CLIENT_SPEC = clientBuilder.build()
    }
}
