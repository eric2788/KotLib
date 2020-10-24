package com.ericlam.mc.kotlib.item

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta

class ItemMetaAdapterR113(private val itemStack: ItemStack) : ItemMetaAdapter {

    private val itemMeta: ItemMeta? = itemStack.itemMeta

    override fun setDurability(durability: Number) {
        itemMeta?.also {
            if (it is Damageable) {
                it.damage = durability.toInt()
            }
        }
        itemStack.itemMeta = itemMeta
    }

    override fun setModelData(data: Int) {
        // 1.13 not support
    }
}