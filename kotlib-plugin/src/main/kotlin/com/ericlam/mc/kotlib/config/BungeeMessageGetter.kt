package com.ericlam.mc.kotlib.config

import com.ericlam.mc.kotlib.BungeeConfiguration
import com.ericlam.mc.kotlib.BungeeConfigurationProvider
import com.ericlam.mc.kotlib.BungeeYaml
import com.ericlam.mc.kotlib.config.controller.MessageGetter
import java.io.File

class BungeeMessageGetter(p: Prefix, file: File) : MessageGetter(p, file) {

    private val config: BungeeConfiguration by lazy { BungeeConfigurationProvider.getProvider(BungeeYaml::class.java)!!.load(file) }

    override fun get(path: String): String = prefix + getPure(path)

    override fun getPure(path: String): String = color(config.getString(path))

    override val prefix: String
        get() = getPure(p.path)

}