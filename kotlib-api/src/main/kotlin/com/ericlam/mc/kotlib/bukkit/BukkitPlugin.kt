package com.ericlam.mc.kotlib.bukkit

import com.ericlam.mc.kotlib.*
import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.command.BukkitCommand
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

abstract class BukkitPlugin : JavaPlugin(), KotlinPlugin {

    override val kDataFolder: File
        get() = dataFolder
    override val kLogger: Logger
        get() = logger

    val utils: BukkitUtils
        get() = KotLib.getBukkitUtils(this)

    final override fun onEnable() {
        instance = this
        this.enable()
    }

    private val stopRunnerQueue: ConcurrentLinkedDeque<() -> Unit> = ConcurrentLinkedDeque()

    final override fun onStop(run: () -> Unit) {
        stopRunnerQueue.add(run)
    }

    final override fun onDisable() {
        stopRunnerQueue.forEach { it() }
        this.disable()
    }

    companion object {
        private lateinit var instance: BukkitPlugin
        val plugin: BukkitPlugin
            get() = instance
    }

    override fun saveResource(resource: String, file: File) {
        if (file.exists()) return
        file.parentFile.mkdirs()

        file.createNewFile()
        getResource(resource)!!.copyTo(file.outputStream())
    }

    override fun execute(cmd: String) {
        server.dispatchCommand(server.consoleSender, cmd)
    }

    override fun notifyUnPaid() {
        this.notifyUnPaid(
                unpaidText = "${ChatColor.YELLOW}{0} v{1} - 未付費版本，付費後刪除此訊息。",
                hoverText = "${ChatColor.LIGHT_PURPLE}點擊查看作者 Github",
                website = "https://github.com/eric2788"
        )
    }

    fun notifyUnPaid(unpaidText: String, hoverText: String, website: String) {
        listen<PlayerJoinEvent> {
            val msg = ComponentBuilder(unpaidText.msgFormat(description.name, description.version))
                    .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Array(1) { textOf(hoverText) }))
                    .event(ClickEvent(ClickEvent.Action.OPEN_URL, website))
                    .create()
            it.player.spigot().sendMessage(*msg)
        }
    }

    override fun cancelTasks() = server.scheduler.cancelTasks(this)

    override fun cancelTask(int: Int) = server.scheduler.cancelTask(int)

    override fun <T> runAsync(run: () -> T): AsyncInvoker<T> {
        return utils.runAsync(run)
    }

    fun command(
            name: String, permission: String? = null, vararg aliases: String,
            executor: BukkitPluginCommand.(BukkitSender, Array<String>) -> Unit
    ) = getCommand(name)!!.also {
        it.aliases = aliases.toList()
        it.setExecutor { sender, _, _, args ->
            it.executor(sender, args)
            true
        }
        it.permission = permission ?: return@also
    }


    fun createGUI(rows: Int, title: String, fills: Map<IntRange, Clicker> = emptyMap(), items: () -> Map<Int, Clicker>): Inventory {
        return utils.createGUI(rows, title, fills, items)
    }

    fun registerCmd(cmd: BukkitCommand) = utils.registerCommand(cmd)


    fun itemStack(
            material: Material,
            amount: Int = 1,
            display: String? = null,
            lore: List<String> = emptyList(),
            unbreakable: Boolean = false,
            durability: Int = 0,
            enchant: Map<Enchantment, Int> = emptyMap(),
            customModelData: Int = -1,
            vararg itemFlags: ItemFlag = emptyArray(),
            consumeEvent: (PlayerItemConsumeEvent.(ItemStack) -> Unit)? = null,
            clickEvent: (PlayerInteractEvent.(ItemStack) -> Unit)? = null
    ): ItemStack = utils.itemStack(material, amount, display, lore, unbreakable, durability, enchant, customModelData, *itemFlags, consumeEvent = consumeEvent, clickEvent = clickEvent)

    fun schedule(
            async: Boolean = false,
            delay: Long = 0,
            period: Long = 0,
            unit: TimeUnit = TimeUnit.SECONDS,
            callback: BukkitTask.() -> Unit
    ): BukkitTask = utils.schedule(async, delay, period, unit, callback)

    fun command(
            name: String, permission: String? = null, vararg aliases: String,
            executor: BukkitSender.(Array<String>) -> Unit
    ) = command(name, permission, *aliases) { sender, args -> sender.executor(args) }

    inline fun <reified T : BukkitEvent> listen(
            priority: BukkitEventPriority = BukkitEventPriority.NORMAL,
            ignoreCancelled: Boolean = false,
            noinline callback: (T) -> Unit
    ) = utils.listen(T::class.java, priority, ignoreCancelled, callback)


    final override fun debug(msg: String?) {
        utils.debug(msg)
    }
}