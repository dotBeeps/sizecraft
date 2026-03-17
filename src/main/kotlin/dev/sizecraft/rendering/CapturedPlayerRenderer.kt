package dev.sizecraft.rendering

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.mojang.serialization.MapCodec
import dev.sizecraft.SizeCraftMod
import dev.sizecraft.capture.CapturedPlayerData
import dev.sizecraft.registry.SizeCraftDataComponents
import net.minecraft.client.Minecraft
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.GameType
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

/**
 * Custom SpecialModelRenderer that renders a tiny player model for the CapturedPlayerItem.
 *
 * Extracts the CapturedPlayerData from the item stack and renders the captured player's
 * skin model at a small scale within the item frame.
 */
@OnlyIn(Dist.CLIENT)
class CapturedPlayerRenderer : SpecialModelRenderer<CapturedPlayerData> {

    override fun extractArgument(stack: ItemStack): CapturedPlayerData? {
        return stack.get(SizeCraftDataComponents.CAPTURED_PLAYER.get())
    }

    override fun render(
        data: CapturedPlayerData?,
        displayContext: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
        hasFoilType: Boolean,
    ) {
        if (data == null) return

        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        val entityRenderDispatcher = minecraft.entityRenderDispatcher

        // Create a fake remote player entity for rendering
        // Uses FakeRenderPlayer to override isSpectator/gameMode so the renderer
        // shows full survival appearance (armor, layers, opacity) even though the
        // real player is in spectator mode.
        val gameProfile = com.mojang.authlib.GameProfile(data.uuid, data.name)
        val fakePlayer = FakeRenderPlayer(level, gameProfile)

        poseStack.pushPose()

        // Center the model in the item space
        poseStack.translate(0.5, 0.0, 0.5)

        // Scale down the player model to fit within item bounds
        val modelScale = 0.4f
        poseStack.scale(modelScale, modelScale, modelScale)

        // Rotate to face the viewer depending on display context
        when (displayContext) {
            ItemDisplayContext.GUI -> {
                // In inventory GUI — rotate slightly for a 3/4 view
                poseStack.mulPose(Axis.YP.rotationDegrees(210f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-10f))
            }
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            }
            else -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            }
        }

        // Render the player entity
        renderFakePlayer(fakePlayer, entityRenderDispatcher, poseStack, bufferSource, packedLight)

        poseStack.popPose()
    }

    private fun renderFakePlayer(
        player: net.minecraft.client.player.RemotePlayer,
        dispatcher: EntityRenderDispatcher,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
    ) {
        try {
            dispatcher.render(
                player,
                0.0, 0.0, 0.0,
                1f,       // partialTick
                poseStack,
                bufferSource,
                packedLight,
            )
        } catch (_: Exception) {
            // Silently ignore render failures (e.g., missing skin data)
        }
    }

    /**
     * A RemotePlayer subclass that forces non-spectator rendering.
     * The real captured player is in spectator mode, but we want the item
     * to show their normal survival appearance (armor, skin layers, full opacity).
     */
    private class FakeRenderPlayer(
        level: ClientLevel,
        gameProfile: com.mojang.authlib.GameProfile,
    ) : net.minecraft.client.player.RemotePlayer(level, gameProfile) {
        override fun isSpectator(): Boolean = false
        override fun gameMode(): GameType = GameType.SURVIVAL
    }

    /**
     * Unbaked form — registered via RegisterSpecialModelRendererEvent.
     * Has no configuration fields, so uses an empty MapCodec.
     */
    @OnlyIn(Dist.CLIENT)
    class Unbaked : SpecialModelRenderer.Unbaked {

        override fun bake(modelSet: EntityModelSet): SpecialModelRenderer<*> {
            return CapturedPlayerRenderer()
        }

        override fun type(): MapCodec<out SpecialModelRenderer.Unbaked> = MAP_CODEC

        companion object {
            val MAP_CODEC: MapCodec<Unbaked> = MapCodec.unit(::Unbaked)
        }
    }
}
