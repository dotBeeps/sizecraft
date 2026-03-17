package dev.sizecraft.rendering

import net.minecraft.client.Minecraft
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.player.Player
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.ClientTickEvent

/**
 * Spawns sparkle particles around the player when their size changes.
 * Uses END_ROD particles for a subtle magical effect.
 */
@OnlyIn(Dist.CLIENT)
object SizeParticleEffect {

    private var pendingParticles: Boolean = false
    private var particleScale: Double = 1.0
    private var growing: Boolean = true
    private var ticksRemaining: Int = 0

    /**
     * Called when a size change is received from the server.
     */
    fun onSizeChanged(player: Player?, targetScale: Double) {
        if (player == null) return
        val mc = Minecraft.getInstance()
        val localPlayer = mc.player ?: return
        if (player.id != localPlayer.id) return

        val currentScale = SizeAnimator.getCurrentScale()
        if (targetScale == currentScale) return

        growing = targetScale > currentScale
        particleScale = targetScale
        pendingParticles = true
        ticksRemaining = 10 // Emit particles over 10 ticks
    }

    /**
     * Tick handler — emits a few particles each tick during the effect.
     */
    fun onClientTick(event: ClientTickEvent.Post) {
        if (!pendingParticles || ticksRemaining <= 0) {
            pendingParticles = false
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return

        ticksRemaining--

        val particlesPerTick = 4
        val radius = 0.8 * particleScale.coerceAtLeast(0.5)

        for (i in 0 until particlesPerTick) {
            val angle = Math.random() * Math.PI * 2
            val yOffset = (Math.random() - 0.2) * player.bbHeight * particleScale
            val xOff = Math.cos(angle) * radius
            val zOff = Math.sin(angle) * radius

            // Velocity: inward for shrinking, outward for growing
            val velocityMult = if (growing) 0.05 else -0.05
            val vx = Math.cos(angle) * velocityMult
            val vy = (Math.random() - 0.3) * 0.04
            val vz = Math.sin(angle) * velocityMult

            level.addParticle(
                ParticleTypes.END_ROD,
                player.x + xOff,
                player.y + yOffset,
                player.z + zOff,
                vx, vy, vz
            )
        }

        if (ticksRemaining <= 0) {
            pendingParticles = false
        }
    }
}
