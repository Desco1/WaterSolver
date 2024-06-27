package dev.desco.watersolver.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.serializer.PacketSerializer
import net.minecraft.client.Minecraft
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S3FPacketCustomPayload

class HypixelPacketHandler: SimpleChannelInboundHandler<Packet<*>>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet<*>) {
        ctx.fireChannelRead(msg)

        if (msg !is S3FPacketCustomPayload) {
            return
        }

        val identifier = msg.channelName
        if (!HypixelModAPI.getInstance().registry.isRegistered(identifier)) {
            return
        }

        val buffer = msg.bufferData
        buffer.retain()
        Minecraft.getMinecraft().addScheduledTask {
            try {
                HypixelModAPI.getInstance().handle(identifier, PacketSerializer(buffer))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                buffer.release()
            }
        }
    }
}