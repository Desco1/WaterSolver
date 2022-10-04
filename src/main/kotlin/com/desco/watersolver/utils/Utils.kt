package com.desco.watersolver.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color

object Utils {

    fun drawLabel(pos: Vec3, text: String, partialTicks: Float, shadow: Boolean = false, scale: Float = 1f) {
        val player = Minecraft.getMinecraft().thePlayer
        val x = pos.xCoord - player.lastTickPosX + (pos.xCoord - player.posX - (pos.xCoord - player.lastTickPosX)) * partialTicks
        val y = pos.yCoord - player.lastTickPosY + (pos.yCoord - player.posY - (pos.yCoord - player.lastTickPosY)) * partialTicks
        val z = pos.zCoord - player.lastTickPosZ + (pos.zCoord - player.posZ - (pos.zCoord - player.lastTickPosZ)) * partialTicks
        val renderManager = Minecraft.getMinecraft().renderManager
        val f1 = 0.0266666688
        val width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) / 2
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, z)
        GL11.glNormal3f(0f, 1f, 0f)
        GlStateManager.rotate(-renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.rotate(renderManager.playerViewX, 1f, 0f, 0f)
        GlStateManager.scale(-f1, -f1, -f1)
        GlStateManager.scale(scale, scale, scale)
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.enableTexture2D()
        Minecraft.getMinecraft().fontRendererObj.drawString(text, (-width).toFloat(), 0f, Color.WHITE.rgb, shadow)
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }
}