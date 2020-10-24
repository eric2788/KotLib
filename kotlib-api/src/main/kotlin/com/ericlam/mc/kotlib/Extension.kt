package com.ericlam.mc.kotlib

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/*
 Nullable
 */

fun <T> T.not(other: T) = takeUnless { it == other }
fun <T> T.notIn(container: Iterable<T>) = takeUnless { it in container }

/*
 String
 */

infix fun String.isNewerThan(v: String) = false.also {
    val s1 = split('.')
    val s2 = v.split('.')
    for (i in 0..s1.size.coerceAtLeast(s2.size)) {
        if (i !in s1.indices) return false
        if (i !in s2.indices) return true
        if (s1[i].toInt() > s2[i].toInt()) return true
        if (s1[i].toInt() < s2[i].toInt()) return false
    }
}

fun String.msgFormat(vararg o: Any?): String {
    return MessageFormat.format(this, *o)
}

val String.purified get() = toLowerCase().trim()

infix fun String.match(other: String) = purified == other.purified

/*
 Date
 */

val currentDate: String get() = SimpleDateFormat("MMM dd yyyy HH:mm:ss").format(Date())

fun String.toTimeWithUnit(): Pair<Long, TimeUnit> {
    val split = split(" ")
    val time = split[0].toLongOrNull()
            ?: error("Cannot convert ${split[0]} to integer")
    val unit = split[1].toTimeUnit()
            ?: error("Cannot convert ${split[1]} to a time unit")
    return Pair(time, unit)
}

fun String.toTimeUnit(default: TimeUnit? = null) =
        when (toLowerCase()) {
            "seconds", "second", "sec", "s" -> TimeUnit.SECONDS
            "minutes", "minute", "min", "m" -> TimeUnit.MINUTES
            "hours", "hour", "h" -> TimeUnit.HOURS
            "days", "day", "d" -> TimeUnit.DAYS
            else -> default
        }

/*
 Messages
 */

fun String.translateColorCode(): String = ChatColor.translateAlternateColorCodes('&', this)

fun textOf(string: String, builder: TextComponent.() -> Unit = {}) =
        TextComponent(*TextComponent.fromLegacyText(string.translateColorCode())).apply(builder)

class PluginException(msg: String) : Exception("&c$msg")

fun error(msg: String): Nothing = throw PluginException(msg)

fun colored(msg: String?, f: (String) -> Unit) {
    if (!msg.isNullOrBlank()) f(msg.translateColorCode())
}


/*
 Files
 */

operator fun File.get(path: String) = File(this, path)
operator fun File.contains(name: String) = name in list().orEmpty()

/*
 Exception
 */

inline fun <reified T : Throwable, reified U : Any> catch(
        err: (T) -> U,
        run: () -> U
): U = try {
    run()
} catch (ex: Throwable) {
    if (ex is T) err(ex) else throw ex
}

inline fun <reified T : Throwable> catch(
        err: (T) -> Unit = { it.printStackTrace() },
        run: () -> Unit
): Unit = catch<T, Unit>(err, run)

inline fun <reified T : Throwable, reified U : Any> catch(
        default: U,
        run: () -> U
): U = catch<T, U>({ default }, run)

/*
 Other
 */

fun <E> MutableList<E>.removeLast(): Boolean {
    val index = (this.size - 1).not(-1) ?: return false
    this.removeAt(index)
    return true
}

fun <E> Array<E>.joinString(prefix: CharSequence = "", postFix: CharSequence = "", sequence: CharSequence = " ", transform: ((E) -> CharSequence)? = null): String {
    return if (this.isEmpty()) {
        ""
    } else {
        this.joinToString(sequence, prefix, postFix, transform = transform)
    }
}

inline fun <reified T> classOf(): Class<T> = T::class.java

inline fun <reified T : Any> kClassOf(): KClass<T> = T::class

fun <T> Array<T>.copyFrom(from: Int): Array<T> {
    return this.copyOfRange(from, this.size)
}

fun <T> T.runIf(bool: Boolean, run: (T) -> Unit): T {
    if (bool) run.invoke(this)
    return this
}

fun <T> T.runIf(bool: () -> Boolean, run: (T) -> Unit): T {
    if (bool.invoke()) run.invoke(this)
    return this
}

infix fun Int.row(slot: Int): Int = (this - 1) * 9 - 1 + slot


/*
SQL
 */

fun <T : Table> T.insertOrUpdate(vararg onDuplicateUpdateKeys: Column<*>, body: T.(InsertStatement<Number>) -> Unit) =
        InsertOrUpdate<Number>(onDuplicateUpdateKeys, this).apply {
            body(this)
            execute(TransactionManager.current())
        }

class InsertOrUpdate<Key : Any>(
        private val onDuplicateUpdateKeys: Array<out Column<*>>,
        table: Table,
        isIgnore: Boolean = false
) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val onUpdateSQL = if (onDuplicateUpdateKeys.isNotEmpty()) {
            " ON DUPLICATE KEY UPDATE " + onDuplicateUpdateKeys.joinToString { "${transaction.identity(it)}=VALUES(${transaction.identity(it)})" }
        } else ""
        return super.prepareSQL(transaction) + onUpdateSQL
    }
}