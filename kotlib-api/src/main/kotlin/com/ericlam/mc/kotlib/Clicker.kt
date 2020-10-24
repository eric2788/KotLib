package com.ericlam.mc.kotlib

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

data class Clicker(val stack: ItemStack, val click: (InventoryClickEvent.(Player, ItemStack) -> Unit)? = null)