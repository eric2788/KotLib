package com.ericlam.mc.kotlib

import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.bukkit.BukkitUtils
import com.ericlam.mc.kotlib.bungee.BungeePlugin
import com.ericlam.mc.kotlib.bungee.BungeeUtils
import com.ericlam.mc.kotlib.config.ConfigFactory
import org.jetbrains.exposed.sql.Transaction

object KotLib : KotLibAPI {
    override fun <T> transaction(t: Transaction.() -> T): T {
        throw Exception("trying to run API.jar into server")
    }

    override fun getBukkitUtils(plugin: BukkitPlugin): BukkitUtils {
        throw Exception("trying to run API.jar into server")
    }

    override fun getBungeeUtils(plugin: BungeePlugin): BungeeUtils {
        throw Exception("trying to run API.jar into server")
    }

    override fun getConfigFactory(plugin: KotlinPlugin): ConfigFactory {
        throw Exception("trying to run API.jar into server")
    }

    override fun <T> asyncTransaction(plugin: KotlinPlugin, run: Transaction.() -> T): AsyncInvoker<T> {
        throw Exception("trying to run API.jar into server")
    }

    override val singlePoolEnabled: Boolean
        get() = throw Exception("trying to run API.jar into server")


}