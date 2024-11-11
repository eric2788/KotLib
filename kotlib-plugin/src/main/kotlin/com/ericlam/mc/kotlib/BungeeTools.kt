package com.ericlam.mc.kotlib

import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.async.BungeeAsyncInvoker
import com.ericlam.mc.kotlib.bungee.BungeePlugin
import com.ericlam.mc.kotlib.bungee.BungeeUtils
import com.ericlam.mc.kotlib.command.BungeeCommand
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.event.EventBus
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

class BungeeTools(private val plugin: BungeePlugin) : BungeeUtils {

    override fun <T> runAsync(run: () -> T): AsyncInvoker<T> {
        return BungeeAsyncInvoker(run, plugin)
    }

    override fun command(name: String, permission: String?, vararg aliases: String, callback: BungeePluginCommand.(BungeeSender, Array<String>) -> Unit) {
        object : BungeePluginCommand(name, permission, *aliases) {
            override fun execute(sender: BungeeSender, args: Array<String>) = callback(sender, args)
        }.also {
            plugin.proxy.pluginManager.registerCommand(plugin, it)
        }
    }

    override fun command(name: String, permission: String?, vararg aliases: String, callback: BungeeSender.(Array<String>) -> Unit) {
        command(name, permission, *aliases) { sender, args -> sender.callback(args) }
    }

    private class BungeeCommandSender(override val sender: BungeeSender) : BungeeCommand.SenderWrapper {
        override fun sendMessage(str: String) {
            sender.sendMessage(textOf(str))
        }

        override fun hasPermission(permission: String): Boolean {
            return sender.hasPermission(permission)
        }
    }

    override fun registerCommand(cmd: BungeeCommand) {
        object : BungeePluginCommand(cmd.name, cmd.permission) {
            override fun execute(sender: CommandSender, args: Array<out String>) {
                val cmdSender = BungeeCommandSender(sender)
                KotLib.handleCommandError(cmdSender, cmd, args)
            }
        }.also { plugin.proxy.pluginManager.registerCommand(plugin, it) }
    }

    override fun <T : BungeeEvent> listen(cls: Class<T>, priority: Byte, callback: (T) -> Unit) {
        val pluginManager = plugin.proxy.pluginManager
        val eventBus = pluginManager::class.java.getDeclaredField("eventBus").run {
            isAccessible = true; get(pluginManager) as EventBus
        }

        @Suppress("UNCHECKED_CAST")
        val byListenerAndPriority = eventBus::class.java.getDeclaredField("byListenerAndPriority").run {
            isAccessible = true; get(eventBus) as? HashMap<Class<*>, Map<Byte, Map<Any, Array<Method>>>>
                ?: throw IllegalStateException("Casting null into HashMap")
        }

        val priorities = byListenerAndPriority[cls] as? HashMap<Byte, Map<Any, Array<Method>>>
                ?: HashMap<Byte, Map<Any, Array<Method>>>().also { byListenerAndPriority[cls] = it }

        val handlers = priorities[priority] as? HashMap<Any, Array<Method>>
                ?: HashMap<Any, Array<Method>>().also { priorities[priority] = it }

        val listener = object : BungeeListener {
            fun onEvent(it: T) = callback(it)
        }

        handlers[listener] = arrayOf(listener::class.java.getMethod("onEvent", BungeeEvent::class.java))
        eventBus::class.java.getDeclaredMethod("bakeHandlers", Class::class.java).apply {
            isAccessible = true; invoke(eventBus, cls)
        }

        plugin.proxy.pluginManager.registerListener(plugin, listener)
    }

    override fun schedule(delay: Long, period: Long, unit: TimeUnit, callback: BungeeTask.() -> Unit): BungeeTask {
        with(plugin) {
            lateinit var task: BungeeTask
            fun f() = task.callback()
            task = proxy.scheduler.run {
                when {
                    period > 0 -> schedule(plugin, ::f, delay, period, unit)
                    delay > 0 -> schedule(plugin, ::f, delay, unit)
                    else -> schedule(plugin, ::f, 0, unit)
                }
            }
            return task
        }
    }

    override fun debug(msg: String?) {
        KotLib.debug(plugin.description.name, plugin.kLogger, msg)
    }

    override val currentProxyVersion: String = ProxyServer.getInstance().version
}