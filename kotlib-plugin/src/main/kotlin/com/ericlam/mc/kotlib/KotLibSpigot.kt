package com.ericlam.mc.kotlib

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin

class KotLibSpigot : BukkitPlugin() {

    override fun enable() {
        val manager = KotLib.getConfigFactory(this)
                .register(KotLib.SQL::class)
                .register(KotLib.Message::class)
                .register(KotLib.Config::class)
                .dump()
        KotLib.initMySQL(manager.getConfig(kClassOf()))
        KotLib.message = manager.getConfig(kClassOf())
        KotLib.config = manager.getConfig(kClassOf())
    }
}