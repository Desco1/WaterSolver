package com.desco.watersolver

import com.desco.watersolver.handler.PuzzleHandler
import com.desco.watersolver.utils.LocUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
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
    const val VERSION = "1.0"

    override val coroutineContext: CoroutineContext = Executors.newFixedThreadPool(10).asCoroutineDispatcher() + SupervisorJob()

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent?) {
        MinecraftForge.EVENT_BUS.register(LocUtil)
        MinecraftForge.EVENT_BUS.register(PuzzleHandler)
    }
}