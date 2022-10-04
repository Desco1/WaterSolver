package com.desco.watersolver.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object LocUtil {

    var inSkyblock = false
    var location = ""
    var locraw = JsonObject()

    private var ticks = 0
    private var listening = false
    private val parser = JsonParser()

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (event.entity == Minecraft.getMinecraft().thePlayer) {
            ticks = 0
            inSkyblock = false
            location = ""
        }
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (Loader.isModLoaded("skytils") || event.phase == TickEvent.Phase.END || Minecraft.getMinecraft().thePlayer == null || ticks >= 20 || !isOnHypixel()) return

        ticks++
        if (ticks == 20) {
            listening = true
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/locraw")
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onLocraw(event: ClientChatReceivedEvent) {
        if (event.type.toInt() != 0) return
        val msg = event.message.unformattedText
        if (!msg.startsWith("{") || !msg.endsWith("}")) return

        locraw = parser.parse(msg).asJsonObject

        if (locraw.has("gametype") && locraw.get("gametype").asString.equals("SKYBLOCK")) {
            inSkyblock = true
            location = locraw.get("mode").asString
        } else {
            inSkyblock = false
            location = ""
        }

        if (listening) {
            listening = false
            event.isCanceled = true
        }
    }

    private fun isOnHypixel(): Boolean {
        val server = Minecraft.getMinecraft().currentServerData
        return server?.serverIP?.contains("hypixel", true) == true
    }
}