package dev.sizecraft.rendering

import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.ClientTickEvent

/**
 * Client-side animator that smoothly lerps the player's visual scale
 * from current to target over a configurable number of ticks.
 */
@OnlyIn(Dist.CLIENT)
object SizeAnimator {

    private var currentScale: Double = 1.0
    private var targetScale: Double = 1.0
    private var ticksRemaining: Int = 0
    private var totalTicks: Int = 0

    /**
     * Called when a SizeSyncPacket arrives from the server.
     */
    fun onSizeSync(target: Double, animationTicks: Int) {
        if (animationTicks <= 0) {
            // Instant change, no animation
            currentScale = target
            targetScale = target
            ticksRemaining = 0
            return
        }
        targetScale = target
        totalTicks = animationTicks
        ticksRemaining = animationTicks
    }

    /**
     * Ticks the animation forward. Called every client tick.
     */
    fun onClientTick(event: ClientTickEvent.Post) {
        if (ticksRemaining <= 0) return

        ticksRemaining--
        if (ticksRemaining <= 0) {
            currentScale = targetScale
        } else {
            // Linear interpolation toward target
            val progress = 1.0 - (ticksRemaining.toDouble() / totalTicks.toDouble())
            // Use ease-out quadratic for smoother feel
            val eased = 1.0 - (1.0 - progress) * (1.0 - progress)
            val startScale = currentScale
            currentScale = startScale + (targetScale - startScale) * (eased - ((totalTicks - ticksRemaining - 1).toDouble() / totalTicks.toDouble()).let { prev ->
                1.0 - (1.0 - prev) * (1.0 - prev)
            }.let { prevEased ->
                (eased - prevEased) / (1.0 - prevEased).coerceAtLeast(0.001)
            }).coerceIn(0.0, 1.0)
        }
    }

    /**
     * Returns the current interpolated scale for rendering purposes.
     */
    fun getCurrentScale(): Double = currentScale

    /**
     * Returns the target scale being animated toward.
     */
    fun getTargetScale(): Double = targetScale

    /**
     * Returns true if an animation is currently in progress.
     */
    fun isAnimating(): Boolean = ticksRemaining > 0
}
