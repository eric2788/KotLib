package com.ericlam.mc.kotlib.item

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta

class ItemMetaAdapterR114(private val itemStack: ItemStack) : ItemMetaAdapter {

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
        itemMeta?.also {
            it.setCustomModelData(data)
        }
        itemStack.itemMeta = itemMeta
    }
}