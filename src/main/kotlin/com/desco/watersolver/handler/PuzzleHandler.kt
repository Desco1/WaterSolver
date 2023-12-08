package com.desco.watersolver.handler

import com.desco.watersolver.WaterSolverMod
import com.desco.watersolver.utils.LocUtil
import com.desco.watersolver.utils.Utils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.*
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object PuzzleHandler {

    private var waterSolutions: JsonObject

    init {
        val isr = PuzzleHandler::class.java.getResourceAsStream("/watertimes.json")
            ?.let { InputStreamReader(it, StandardCharsets.UTF_8) }
        waterSolutions = JsonParser().parse(isr).asJsonObject
    }

    private var chestPos: BlockPos? = null
    private var roomFacing: EnumFacing? = null
    private var prevInWaterRoom = false
    private var inWaterRoom = false
    private var variant = -1
    private var extendedSlots = ""
    private var ticks = 0
    private var solutions = mutableMapOf<LeverBlock, DoubleArray>()
    private var openedWater = -1L
    private var job: Job? = null

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) return
        if (LocUtil.location != "dungeon") return
        val player = Minecraft.getMinecraft().thePlayer ?: return
        val world = Minecraft.getMinecraft().theWorld ?: return
        if (ticks % 20 == 0) {
            if (variant == -1 && (job == null || job?.isCancelled == true || job?.isCompleted == true)) {
                job = WaterSolverMod.launch {
                    prevInWaterRoom = inWaterRoom
                    inWaterRoom = false

                    val nearPiston = BlockPos.getAllInBox(
                        BlockPos(player.posX.toInt() - 13, 54, player.posZ.toInt() - 13),
                        BlockPos(player.posX.toInt() + 13, 54, player.posZ.toInt() + 13)
                    ).any { world.getBlockState(it).block == Blocks.sticky_piston }

                    if (!nearPiston) {
                        solutions.clear()
                        variant = -1
                        return@launch
                    }

                    val xRange = player.posX.toInt() - 25..player.posX.toInt() + 25
                    val zRange = player.posZ.toInt() - 25..player.posZ.toInt() + 25
                    world.loadedTileEntityList.find {
                        it is TileEntityChest && it.pos.y == 56 && it.numPlayersUsing == 0 &&
                                it.pos.x in xRange && it.pos.z in zRange &&
                                world.getBlockState(it.pos.down()).block == Blocks.stone &&
                                world.getBlockState(it.pos.up(2)).block == Blocks.stained_glass
                    }?.let {
                        for (horizontal in EnumFacing.HORIZONTALS) {
                            val opposite = world.getBlockState(it.pos.offset(horizontal.opposite, 3).down(2)).block
                            val facing = world.getBlockState(it.pos.offset(horizontal, 2)).block
                            if (opposite == Blocks.sticky_piston && facing == Blocks.stone) {
                                chestPos = it.pos
                                roomFacing = horizontal
                                break
                            }
                        }
                    }

                    if (chestPos == null) return@launch

                    val piston = BlockPos.getAllInBox(
                        BlockPos(player.posX.toInt() - 25, 82, player.posZ.toInt() - 25),
                        BlockPos(player.posX.toInt() + 25, 82, player.posZ.toInt() + 25)
                    ).find { world.getBlockState(it).block == Blocks.piston_head } ?: return@launch

                    inWaterRoom = true
                    if (prevInWaterRoom) return@launch

                    val blockList = BlockPos.getAllInBox(
                        BlockPos(piston.x + 1, 78, piston.z + 1),
                        BlockPos(piston.x - 1, 77, piston.z - 1)
                    )

                    var foundGold = false
                    var foundClay = false
                    var foundEmerald = false
                    var foundQuartz = false
                    var foundDiamond = false
                    for (blockPos in blockList) {
                        when (world.getBlockState(blockPos).block) {
                            Blocks.gold_block -> foundGold = true
                            Blocks.hardened_clay -> foundClay = true
                            Blocks.emerald_block -> foundEmerald = true
                            Blocks.quartz_block -> foundQuartz = true
                            Blocks.diamond_block -> foundDiamond = true
                        }
                    }

                    variant = when {
                        foundGold && foundClay -> 0
                        foundEmerald && foundQuartz -> 1
                        foundQuartz && foundDiamond -> 2
                        foundGold && foundQuartz -> 3
                        else -> -1
                    }

                    for (value in WoolColor.entries) {
                        if (value.isExtended) {
                            extendedSlots += value.ordinal.toString()
                        }
                    }

                    if (extendedSlots.length != 3) {
                        println("Didn't find the solution! Retrying")
                        println("Slots: $extendedSlots")
                        println("Water: $inWaterRoom ($prevInWaterRoom)")
                        println("Chest: $chestPos")
                        println("Rotation: $roomFacing")
                        extendedSlots = ""
                        inWaterRoom = false
                        prevInWaterRoom = false
                        variant = -1
                        return@launch
                    }

                    player.addChatMessage(
                        ChatComponentText(
                            EnumChatFormatting.AQUA.toString() + "[WS] " + EnumChatFormatting.RESET.toString() + "Variant: $variant:$extendedSlots:${roomFacing?.name}"
                        )
                    )

                    solutions.clear()
                    val solutionObj = waterSolutions[variant.toString()].asJsonObject[extendedSlots].asJsonObject
                    for ((block, times) in solutionObj.entrySet()) {
                        val lever = when (block) {
                            "minecraft:quartz_block" -> LeverBlock.QUARTZ
                            "minecraft:gold_block" -> LeverBlock.GOLD
                            "minecraft:coal_block" -> LeverBlock.COAL
                            "minecraft:diamond_block" -> LeverBlock.DIAMOND
                            "minecraft:emerald_block" -> LeverBlock.EMERALD
                            "minecraft:hardened_clay" -> LeverBlock.CLAY
                            "minecraft:water" -> LeverBlock.WATER
                            else -> LeverBlock.NONE
                        }
                        solutions[lever] = times.asJsonArray.map { it.asDouble }.toDoubleArray()
                    }
                }
            }
            ticks = 0
        }
        ticks++
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        val solution = solutions
            .map { (block, times) -> block to times.drop(block.i) }
            .filter { (_, times) -> times.isNotEmpty() }

        var allPreDone = true
        val orderedSolutions = mutableListOf<Pair<Double, LeverBlock>>()
        for ((leverBlock, times) in solution) {
            if (leverBlock != LeverBlock.WATER && openedWater == -1L && times[0] == 0.0) {
                allPreDone = false
            }
            orderedSolutions.addAll(times.mapNotNull { if (it == 0.0) null else it to leverBlock })
        }
        orderedSolutions.sortBy { it.first }

        for ((block, times) in solution) {
            if (openedWater != -1L) {
                val orderText = times.filter { it != 0.0 }
                    .joinToString(", ", prefix = EnumChatFormatting.RESET.toString()) {
                        val num = orderedSolutions.indexOfFirst { (time, _) -> time == it } + 1
                        if (num == 1) {
                            EnumChatFormatting.GREEN.toString() + EnumChatFormatting.BOLD.toString() + num
                        } else {
                            EnumChatFormatting.YELLOW.toString() + num
                        }
                    }

                Utils.drawLabel(
                    Vec3(block.leverPos).addVector(0.5, if (block == LeverBlock.WATER) 2.0 else 0.0, 0.5),
                    orderText,
                    event.partialTicks
                )
            }

            times.forEachIndexed { i, it ->
                val time = if (openedWater == -1L) {
                    it
                } else {
                    it - (System.currentTimeMillis() - openedWater) / 1000.0
                }

                val displayText = if (time <= 0.0) {
                    if (block == LeverBlock.WATER && !allPreDone) {
                        EnumChatFormatting.RED.toString() + EnumChatFormatting.BOLD.toString() + "CLICK ME!"
                    } else {
                        EnumChatFormatting.GREEN.toString() + EnumChatFormatting.BOLD.toString() + "CLICK ME!"
                    }
                } else {
                    EnumChatFormatting.YELLOW.toString() + "%.2f".format(time) + "s"
                }

                Utils.drawLabel(
                    Vec3(block.leverPos).addVector(0.5, (i - block.i) * 0.5 + 1.5, 0.5),
                    displayText,
                    event.partialTicks
                )
            }
        }
    }

    @SubscribeEvent
    fun onBlockInteract(event: PlayerInteractEvent) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return
        if (solutions.isEmpty()) return
        for (value in LeverBlock.entries) {
            if (value.leverPos == event.pos) {
                value.i++
                if (value == LeverBlock.WATER) {
                    if (openedWater == -1L) {
                        openedWater = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun reset(event: WorldEvent.Load) {
        chestPos = null
        roomFacing = null
        prevInWaterRoom = false
        inWaterRoom = false
        variant = -1
        extendedSlots = ""
        ticks = 0
        solutions.clear()
        openedWater = -1L
        LeverBlock.entries.forEach { it.i = 0 }
    }

    enum class WoolColor(var dyeColor: EnumDyeColor) {
        PURPLE(EnumDyeColor.PURPLE),
        ORANGE(EnumDyeColor.ORANGE),
        BLUE(EnumDyeColor.BLUE),
        GREEN(EnumDyeColor.GREEN),
        RED(EnumDyeColor.RED);

        val isExtended: Boolean
            get() = if (chestPos == null || roomFacing == null) false else Minecraft.getMinecraft().theWorld.getBlockState(
                chestPos!!.offset(roomFacing!!.opposite, 3 + ordinal)).block === Blocks.wool
    }

    enum class LeverBlock(var i: Int = 0) {
        QUARTZ,
        GOLD,
        COAL,
        DIAMOND,
        EMERALD,
        CLAY,
        WATER,
        NONE;

        val leverPos: BlockPos?
            get() {
                if (chestPos == null || roomFacing == null) return null
                return if (this == WATER) {
                    chestPos!!.offset(roomFacing!!.opposite, 17).up(4)
                } else {
                    val shiftBy = ordinal % 3 * 5
                    val leverSide = if (ordinal < 3) roomFacing!!.rotateY() else roomFacing!!.rotateYCCW()
                    chestPos!!.up(5).offset(leverSide.opposite, 6).offset(roomFacing!!.opposite, 2 + shiftBy)
                        .offset(leverSide)
                }
            }
    }
}