package dev.desco.watersolver.core

import gg.essential.loader.stage0.EssentialSetupTweaker
import net.hypixel.modapi.tweaker.HypixelModAPITweaker
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.LaunchClassLoader

class WaterTweaker: EssentialSetupTweaker() {

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader?) {
        super.injectIntoClassLoader(classLoader)

        val tweakClasses = Launch.blackboard["TweakClasses"] as MutableList<String>?
        tweakClasses?.add(HypixelModAPITweaker::class.java.name)
    }
}