package com.ericlam.mc.kotlib

import com.ericlam.mc.kotlib.async.AsyncInvoker
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.logging.Logger

interface KotlinPlugin {

    val kDataFolder: File
    val kLogger: Logger

    fun <T> runAsync(run: () -> T): AsyncInvoker<T>

    fun enable()

    fun disable() {}

    fun saveResource(resource: String, file: File)

    fun execute(cmd: String)

    fun info(msg: String?) = colored(msg, kLogger::info)

    fun info(msg: Throwable) = info(msg.message)

    fun warning(msg: String?) = colored(msg, kLogger::warning)

    fun warning(msg: Throwable) = warning(msg.message)

    fun severe(msg: String?) = colored(msg, kLogger::severe)

    fun severe(msg: Throwable) = severe(msg.message)

    fun error(ex: Exception) {
        severe(ex.message ?: "&cAn internal error occured, check the logs");
        logToFile(ex)
    }

    fun debug(msg: String?)

    fun onStop(run: () -> Unit)

    fun cancelTasks(): Any?

    fun notifyUnPaid()

    fun cancelTask(int: Int)

    fun logToFile(ex: Exception) = logToFile { ex.printStackTrace(this) }

    fun logToFile(msg: String) = logToFile { println(msg) }

    private fun logToFile(action: PrintWriter.() -> Unit) =
            PrintWriter(FileWriter(logFile, true), true)
                    .apply { print(currentDate); action() }.close()

    private val logFile get() = kDataFolder["log.txt"].apply { if (!exists()) createNewFile() }
}