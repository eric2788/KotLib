package com.ericlam.mc.kotlib.config

import com.ericlam.mc.kotlib.config.controller.MessageGetter
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class BukkitMessageGetter(p: Prefix, file: File) : MessageGetter(p, file) {

    private val config: FileConfiguration by lazy { YamlConfiguration.loadConfiguration(file) }

    override fun get(path: String): String = prefix + getPure(path)

    override fun getPure(path: String): String = color(config.getString(path))

    override val prefix: String
        get() = getPure(p.path)

}