package com.ericlam.mc.kotlib.bungee

import com.ericlam.mc.kotlib.*
import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.command.BungeeCommand
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Plugin
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

abstract class BungeePlugin : Plugin(), KotlinPlugin {

    override val kDataFolder: File
        get() = dataFolder
    override val kLogger: Logger
        get() = logger
    val utils: BungeeUtils
        get() = KotLib.getBungeeUtils(this)

    final override fun onEnable() {
        instance = this
        this.enable()
    }

    companion object {
        private lateinit var instance: BungeePlugin
        val plugin: BungeePlugin
            get() = instance
    }

    private val stopRunnerQueue: ConcurrentLinkedDeque<() -> Unit> = ConcurrentLinkedDeque()

    final override fun onStop(run: () -> Unit) {
        stopRunnerQueue.add(run)
    }

    final override fun onDisable() {
        stopRunnerQueue.forEach { it() }
        this.disable()
    }

    override fun <T> runAsync(run: () -> T): AsyncInvoker<T> {
        return utils.runAsync(run)
    }

    override fun saveResource(resource: String, file: File) {
        if (file.exists()) return
        file.parentFile.mkdirs()
        file.createNewFile()
        getResourceAsStream(resource).copyTo(file.outputStream())
    }

    override fun execute(cmd: String) {
        proxy.pluginManager.dispatchCommand(proxy.console, cmd)
    }

    override fun notifyUnPaid() {
        this.notifyUnPaid(
                unpaidText = "${ChatColor.YELLOW}{0} v{1} - 未付費版本，付費後刪除此訊息。",
                hoverText = "${ChatColor.LIGHT_PURPLE}點擊查看作者 Github",
                website = "https://github.com/eric2788"
        )
    }

    fun notifyUnPaid(unpaidText: String, hoverText: String, website: String) {
        listen<PostLoginEvent> {
            val msg = ComponentBuilder(unpaidText.msgFormat(description.name, description.version))
                    .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Array(1) { textOf(hoverText) }))
                    .event(ClickEvent(ClickEvent.Action.OPEN_URL, website))
                    .create()
            it.player.sendMessage(*msg)
        }
    }

    fun registerCmd(cmd: BungeeCommand) = utils.registerCommand(cmd)

    fun command(
            name: String,
            permission: String? = null,
            vararg aliases: String,
            callback: BungeePluginCommand.(BungeeSender, Array<String>) -> Unit
    ) = utils.command(name, permission, *aliases, callback = callback)

    fun command(
            name: String,
            permission: String? = null,
            vararg aliases: String,
            callback: BungeeSender.(Array<String>) -> Unit
    ) = utils.command(name, permission, *aliases, callback = callback)

    inline fun <reified T : BungeeEvent> listen(
            priority: Byte = BungeeEventPriority.NORMAL,
            noinline callback: (T) -> Unit
    ) = utils.listen(T::class.java, priority, callback)

    override fun cancelTasks() = proxy.scheduler.cancel(this)
    override fun cancelTask(int: Int) = proxy.scheduler.cancel(int)


    fun schedule(
            delay: Long = 0,
            period: Long = 0,
            unit: TimeUnit = TimeUnit.SECONDS,
            callback: BungeeTask.() -> Unit
    ): BungeeTask = utils.schedule(delay, period, unit, callback)

    final override fun debug(msg: String?) {
        utils.debug(msg)
    }
}