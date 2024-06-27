package dev.desco.watersolver.utils

import net.hypixel.data.type.GameType
import net.hypixel.modapi.handler.ClientboundPacketHandler
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import kotlin.jvm.optionals.getOrNull

object LocUtil: ClientboundPacketHandler<ClientboundLocationPacket> {

    var inSkyblock = false
    var location = ""

    override fun handle(packet: ClientboundLocationPacket) {
        inSkyblock = packet.serverType?.getOrNull() == GameType.SKYBLOCK
        location = packet.mode.getOrNull() ?: ""
    }
}