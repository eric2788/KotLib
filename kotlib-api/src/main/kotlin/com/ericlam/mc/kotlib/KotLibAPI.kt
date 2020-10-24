package com.ericlam.mc.kotlib

import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.bukkit.BukkitUtils
import com.ericlam.mc.kotlib.bungee.BungeePlugin
import com.ericlam.mc.kotlib.bungee.BungeeUtils
import com.ericlam.mc.kotlib.config.ConfigFactory
import org.jetbrains.exposed.sql.Transaction

interface KotLibAPI {

    fun <T> transaction(t: Transaction.() -> T): T

    fun getBukkitUtils(plugin: BukkitPlugin): BukkitUtils

    fun getBungeeUtils(plugin: BungeePlugin): BungeeUtils

    fun getConfigFactory(plugin: KotlinPlugin): ConfigFactory

    fun <T> asyncTransaction(plugin: KotlinPlugin, run: Transaction.() -> T): AsyncInvoker<T>

    val singlePoolEnabled: Boolean

}