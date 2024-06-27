package dev.desco.watersolver

import dev.desco.watersolver.handler.HypixelPacketHandler
import dev.desco.watersolver.handler.PuzzleHandler
import dev.desco.watersolver.utils.LocUtil
import io.netty.buffer.Unpooled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.hypixel.modapi.serializer.PacketSerializer
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.C17PacketCustomPayload
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

@Mod(
    modid = WaterSolverMod.MODID,
    version = WaterSolverMod.VERSION,
    name = WaterSolverMod.NAME,
    clientSideOnly = true,
    acceptedMinecraftVersions = "[1.8.9]",
    modLanguageAdapter = "gg.essential.api.utils.KotlinAdapter"
)
object WaterSolverMod: CoroutineScope {
    const val NAME = "SomeWaterSolver"
    const val MODID = "somewatersolver"
    const val VERSION = "1.1.1"

    override val coroutineContext: CoroutineContext = Executors.newFixedThreadPool(10).asCoroutineDispatcher() + SupervisorJob()

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent?) {
        MinecraftForge.EVENT_BUS.register(this)
        MinecraftForge.EVENT_BUS.register(PuzzleHandler)

        HypixelModAPI.getInstance().setPacketSender {
            val buf = PacketBuffer(Unpooled.buffer())
            val serializer = PacketSerializer(buf)
            it.write(serializer)
            Minecraft.getMinecraft().netHandler?.takeIf { it.networkManager.isChannelOpen }
                ?.addToSendQueue(C17PacketCustomPayload(it.identifier, buf)) ?: return@setPacketSender false
            return@setPacketSender true
        }
        HypixelModAPI.getInstance().registerHandler(ClientboundLocationPacket::class.java, LocUtil)
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket::class.java)
    }

    @SubscribeEvent
    fun onServerJoin(event: ClientConnectedToServerEvent) {
        event.manager.channel().pipeline().addBefore("packet_handler", "water_solver_handler", HypixelPacketHandler())
    }
}