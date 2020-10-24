package com.ericlam.mc.kotlib.bungee

import com.ericlam.mc.kotlib.*
import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.command.BungeeCommand
import java.util.concurrent.TimeUnit

interface BungeeUtils {

    fun <T> runAsync(run: () -> T): AsyncInvoker<T>

    fun command(
            name: String,
            permission: String? = null,
            vararg aliases: String,
            callback: BungeePluginCommand.(BungeeSender, Array<String>) -> Unit
    )

    fun registerCommand(cmd: BungeeCommand)

    fun command(
            name: String,
            permission: String? = null,
            vararg aliases: String,
            callback: BungeeSender.(Array<String>) -> Unit
    )

    fun <T : BungeeEvent> listen(
            cls: Class<T>,
            priority: Byte = BungeeEventPriority.NORMAL,
            callback: (T) -> Unit
    )

    fun schedule(
            delay: Long = 0,
            period: Long = 0,
            unit: TimeUnit = TimeUnit.SECONDS,
            callback: BungeeTask.() -> Unit
    ): BungeeTask


    fun debug(msg: String?)

    val currentProxyVersion: String
}