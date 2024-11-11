package com.ericlam.mc.kotlib.async

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import org.bukkit.Bukkit

class BukkitAsyncInvoker<T>(
        private val run: () -> T,
        plugin: BukkitPlugin
) : AsyncInvoker<T> {

    private val scheduler = Bukkit.getScheduler()
    private lateinit var syncRun: (T) -> Unit
    private lateinit var catchRun: (Throwable) -> Unit
    private lateinit var finallyRun: () -> Unit


    init {
        scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                val result = run()
                if (this::syncRun.isInitialized) {
                    scheduler.runTask(plugin, Runnable {
                        try {
                            syncRun(result)
                        } catch (e: Throwable) {
                            if (this::catchRun.isInitialized) {
                                catchRun(e)
                            } else {
                                throw Exception("Uncaught Error: ${e.message}", e)
                            }
                        } finally {
                            if (this::finallyRun.isInitialized) {
                                finallyRun()
                            }
                        }
                    })
                }
            } catch (e: Throwable) {
                if (this::catchRun.isInitialized) {
                    catchRun(e)
                } else {
                    throw Exception("Uncaught Error: ${e.message}", e)
                }
            } finally {
                if (this::finallyRun.isInitialized) {
                    finallyRun()
                }
            }
        })
    }

    override fun thenSync(run: (T) -> Unit): AsyncInvoker<T> {
        syncRun = run
        return this
    }

    override fun catch(catch: (Throwable) -> Unit): AsyncInvoker<T> {
        catchRun = catch
        return this
    }

    override fun finally(run: () -> Unit) {
        finallyRun = run
    }


}