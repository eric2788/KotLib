package com.ericlam.mc.kotlib.command

import com.ericlam.mc.kotlib.BungeeSender

open class BungeeCommand(
        name: String,
        description: String,
        permission: String? = null,
        placeholders: Array<String> = emptyArray(),
        optionalPlaceholders: Array<String> = emptyArray(),
        _parent: KCommand? = null,
        child: Array<KCommand> = arrayOf(),
        execute: ((BungeeSender, Array<out String>) -> Unit)? = null
) : KCommand(
        name,
        description,
        permission,
        placeholders,
        optionalPlaceholders,
        _parent,
        child,
        execute = execute?.let {
            { sender: CommandSender, args: Array<out String> ->
                it.invoke((sender as SenderWrapper).sender, args)
            }
        }
) {
    interface SenderWrapper : CommandSender {
        val sender: BungeeSender
    }
}