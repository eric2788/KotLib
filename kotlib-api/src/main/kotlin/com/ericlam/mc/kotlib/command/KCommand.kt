package com.ericlam.mc.kotlib.command

import com.ericlam.mc.kotlib.copyFrom
import org.bukkit.ChatColor

abstract class KCommand(
        val name: String,
        val description: String,
        val permission: String?,
        private val placeholders: Array<String>,
        private val optionalPlaceholders: Array<String>,
        private var parent: KCommand?,
        private val child: Array<KCommand>,
        private val execute: ((CommandSender, Array<out String>) -> Unit)?
) {

    init {
        child.forEach { it.parent = this }
    }

    interface CommandSender {

        fun sendMessage(str: String)

        fun hasPermission(permission: String): Boolean

    }


    open fun invokeCommand(sender: CommandSender, args: Array<out String>) {

        val hasPerm = permission?.let { sender.hasPermission(it) } ?: true

        if (!hasPerm) {
            throw CommandError(CommandError.ErrorType.NO_PERMISSION, permission!!)
        }

        if (args.isNotEmpty()) {
            for (sub in child) {
                if (sub.name.equals(args[0], true)) {
                    val passArgs = args.copyFrom(1)
                    return sub.invokeCommand(sender, passArgs)
                }
            }
        }

        if (args.size < placeholders.size) {
            val help = getHelpLine(this)
            throw CommandError(CommandError.ErrorType.FEW_ARGS, placeholders.copyFrom(args.size).joinToString(" ")).also { sender.sendMessage(help) }
        }

        return execute?.invoke(sender, args) ?: let {
            val helps = child.map { getHelpLine(it) }
            return helps.forEach { sender.sendMessage(it) }
        }
    }

    private fun getHelpLine(it: KCommand) = "${ChatColor.GRAY}${getCommandString(it)}${it.placeholders.joinToString(" ", prefix = it.placeholders.prefix) { "<$it>" }}${it.optionalPlaceholders.joinToString(" ", prefix = it.optionalPlaceholders.prefix) { "[$it]" }} ${ChatColor.YELLOW}- ${ChatColor.RED}${it.description}"

    private fun getCommandString(cmd: KCommand, originString: String = cmd.name): String {
        return cmd.parent?.let { getCommandString(it, "${it.name} $originString") } ?: "/$originString"
    }

    private val <T> Array<T>.prefix: String
        get() = if (this.isEmpty()) "" else " "
}