package com.ericlam.mc.kotlib.bukkit

import com.ericlam.mc.kotlib.*
import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.command.BukkitCommand
import com.ericlam.mc.kotlib.item.ItemMetaAdapter
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.TimeUnit

interface BukkitUtils {

    fun <T> runAsync(run: () -> T): AsyncInvoker<T>

    fun command(
            name: String,
            permission: String? = null,
            vararg aliases: String,
            executor: BukkitPluginCommand.(BukkitSender, Array<String>) -> Unit
    )

    fun registerCommand(cmd: BukkitCommand)

    fun command(
            name: String,
            permission: String? = null,
            vararg aliases: String,
            executor: BukkitSender.(Array<String>) -> Unit
    )

    fun <T : BukkitEvent> listen(cls: Class<T>,
                                 priority: BukkitEventPriority = BukkitEventPriority.NORMAL,
                                 ignoreCancelled: Boolean = false,
                                 callback: (T) -> Unit
    )

    fun createGUI(row: Int, title: String, fills: Map<IntRange, Clicker> = emptyMap(), items: () -> Map<Int, Clicker>): Inventory

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
    ): ItemStack

    fun schedule(
            async: Boolean = false,
            delay: Long = 0,
            period: Long = 0,
            unit: TimeUnit = TimeUnit.SECONDS,
            callback: BukkitTask.() -> Unit
    ): BukkitTask

    fun debug(msg: String?)

    fun getItemMetaAdapter(itemStack: ItemStack): ItemMetaAdapter

    val currentMCVersion: String

}

