package com.ericlam.mc.kotlib.async

import com.ericlam.mc.kotlib.bungee.BungeePlugin
import net.md_5.bungee.api.ProxyServer

class BungeeAsyncInvoker<T>(
        private val run: () -> T,
        plugin: BungeePlugin
) : AsyncInvoker<T> {

    private val scheduler = ProxyServer.getInstance().scheduler
    private lateinit var syncRun: (T) -> Unit
    private lateinit var catchRun: (Throwable) -> Unit
    private lateinit var finallyRun: () -> Unit

    init {
        scheduler.runAsync(plugin, Runnable {
            try {
                val result = run()
                if (this::syncRun.isInitialized) {
                    syncRun(result)
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