package com.ericlam.mc.kotlib

import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.async.BukkitAsyncInvoker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.bukkit.BukkitUtils
import com.ericlam.mc.kotlib.command.BukkitCommand
import com.ericlam.mc.kotlib.command.KCommand
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.TimeUnit

class BukkitTools(private val plugin: BukkitPlugin) : BukkitUtils {

    override fun <T> runAsync(run: () -> T): AsyncInvoker<T> {
        return BukkitAsyncInvoker(run, plugin)
    }

    override fun command(
        name: String,
        permission: String?,
        vararg aliases: String,
        executor: BukkitPluginCommand.(BukkitSender, Array<String>) -> Unit
    ) {
        plugin.getCommand(name)!!.also {
            it.aliases = aliases.toList()
            it.setExecutor { sender, _, _, args ->
                it.executor(sender, args)
                true
            }
            it.permission = permission ?: return@also
        }
    }

    override fun command(
        name: String,
        permission: String?,
        vararg aliases: String,
        executor: BukkitSender.(Array<String>) -> Unit
    ) {
        command(name, permission, *aliases) { sender, args -> sender.executor(args) }
    }

    private class BukkitCommandSender(override val sender: BukkitSender) : BukkitCommand.SenderWrapper {
        override fun sendMessage(str: String) {
            sender.sendMessage(str)
        }

        override fun hasPermission(permission: String): Boolean {
            return sender.hasPermission(permission)
        }
    }

    override fun registerCommand(cmd: BukkitCommand) {
        val pcmd = plugin.getCommand(cmd.name)
            ?: let { plugin.logger.warning("${cmd.name} is not exist in plugin.yml"); return }
        pcmd.description = cmd.description
        pcmd.setExecutor { commandSender, _, _, strings ->
            val sender: KCommand.CommandSender = BukkitCommandSender(commandSender)
            KotLib.handleCommandError(sender, cmd, strings)
            true
        }
        pcmd.permission = cmd.permission ?: return
    }

    private val listener: BukkitListener = object : BukkitListener {}

    override fun <T : BukkitEvent> listen(
        cls: Class<T>,
        priority: BukkitEventPriority,
        ignoreCancelled: Boolean,
        callback: (T) -> Unit
    ) {
        plugin.server.pluginManager.registerEvent(
            cls, listener,
            priority, { _, it -> if (cls.isInstance(it)) callback(cls.cast(it)) },
            plugin, ignoreCancelled
        )
    }

    override fun createGUI(
        row: Int,
        title: String,
        fills: Map<IntRange, Clicker>,
        items: () -> Map<Int, Clicker>
    ): Inventory {
        val inv = Bukkit.createInventory(null, row.takeIf { it in 1..6 }?.let { it * 9 }
            ?: 54, title.translateColorCode())
        val map = items.invoke()
        for (fill in fills) {
            fill.key.forEach { inv.setItem(it, fill.value.stack) }
        }
        for ((slot, clicker) in map) {
            inv.setItem(slot, clicker.stack)
        }
        plugin.listen<InventoryClickEvent> {
            if (inv != it.clickedInventory) return@listen
            it.isCancelled = true
            val player = it.whoClicked as Player
            val slot = it.slot
            val stack = it.currentItem ?: return@listen
            map[slot]?.click?.invoke(it, player, stack) ?: kotlin.run {
                fills.filterKeys { range -> slot in range }.entries.singleOrNull()?.value?.click?.invoke(
                    it,
                    player,
                    stack
                )
            }
        }
        return inv
    }

    override fun itemStack(
        material: Material,
        amount: Int, display: String?,
        lore: List<String>, unbreakable: Boolean,
        durability: Int, enchant: Map<Enchantment, Int>,
        customModelData: Int, vararg itemFlags: ItemFlag,
        consumeEvent: (PlayerItemConsumeEvent.(ItemStack) -> Unit)?,
        clickEvent: (PlayerInteractEvent.(ItemStack) -> Unit)?
    ): ItemStack {
        val stack = ItemStack(material, amount)
        stack.addEnchantments(enchant)

        stack.itemMeta?.also { meta ->
            display?.translateColorCode()?.let { meta.setDisplayName(it) }
            meta.lore = lore.map { it.translateColorCode() }
            meta.isUnbreakable = unbreakable
            (meta as? Damageable)?.also { it.damage = durability }
            itemFlags.not(emptyArray())?.also { meta.addItemFlags(*itemFlags) }
            customModelData.not(-1)?.also { meta.setCustomModelData(it) }
            stack.itemMeta = meta
        }

        consumeEvent?.let {
            plugin.listen<PlayerItemConsumeEvent> { e ->
                if (e.item != stack) return@listen
                it.invoke(e, e.item)
            }
        }
        clickEvent?.let {
            plugin.listen<PlayerInteractEvent> { e ->
                if (!e.hasItem() || e.item != stack) return@listen
                it.invoke(e, e.item!!)
            }
        }
        return stack
    }

    override fun schedule(
        async: Boolean,
        delay: Long,
        period: Long,
        unit: TimeUnit,
        callback: BukkitTask.() -> Unit
    ): BukkitTask {
        lateinit var task: BukkitTask
        fun f() = task.callback()
        with(plugin.server.scheduler) {
            task = run {
                val sdelay = unit.toSeconds(delay) * 20
                val speriod = unit.toSeconds(period) * 20
                when {
                    period > 0 -> {
                        when {
                            async -> runTaskTimerAsynchronously(plugin, ::f, sdelay, speriod)
                            else -> runTaskTimer(plugin, ::f, sdelay, speriod)
                        }
                    }

                    delay > 0 -> {
                        when {
                            async -> runTaskLaterAsynchronously(plugin, ::f, sdelay)
                            else -> runTaskLater(plugin, ::f, sdelay)
                        }
                    }

                    async -> runTaskAsynchronously(plugin, ::f)
                    else -> runTask(plugin, ::f)
                }
            }
        }
        return task
    }

    override fun debug(msg: String?) {
        KotLib.debug(plugin.name, plugin.kLogger, msg)
    }

    override val currentMCVersion: String = Bukkit.getBukkitVersion().substring(0..5)


}
