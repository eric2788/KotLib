package com.ericlam.mc.kotlib

import com.ericlam.mc.kotlib.bungee.BungeePlugin

class KotLibBungee : BungeePlugin() {

    override fun enable() {
        val manager = KotLib.getConfigFactory(this)
                .register(kClassOf<KotLib.SQL>())
                .register(kClassOf<KotLib.Message>())
                .register(kClassOf<KotLib.Config>())
                .dump()
        KotLib.initMySQL(manager.getConfig(kClassOf()))
        KotLib.message = manager.getConfig(kClassOf())
        KotLib.config = manager.getConfig(kClassOf())
    }
}