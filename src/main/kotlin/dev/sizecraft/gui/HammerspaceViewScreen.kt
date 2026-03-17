package dev.sizecraft.gui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class HammerspaceViewScreen(
    menu: HammerspaceViewMenu,
    inventory: Inventory,
    title: Component,
) : AbstractContainerScreen<HammerspaceViewMenu>(menu, inventory, title) {

    companion object {
        private val TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png")
    }

    init {
        imageHeight = 168
        inventoryLabelY = imageHeight - 94
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        val topHeight = 17 + 3 * 18 + 7
        // Top section: header + 3 rows of chest slots
        guiGraphics.blit(RenderType::guiTextured, TEXTURE, x, y, 0f, 0f, imageWidth, topHeight, 256, 256)
        // Bottom section: player inventory
        guiGraphics.blit(RenderType::guiTextured, TEXTURE, x, y + topHeight, 0f, 126f, imageWidth, 97, 256, 256)
    }
}
