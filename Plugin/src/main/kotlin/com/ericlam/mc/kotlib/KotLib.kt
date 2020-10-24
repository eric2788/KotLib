package com.ericlam.mc.kotlib

import com.ericlam.mc.kotlib.async.AsyncInvoker
import com.ericlam.mc.kotlib.async.BukkitAsyncInvoker
import com.ericlam.mc.kotlib.async.BungeeAsyncInvoker
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.bukkit.BukkitUtils
import com.ericlam.mc.kotlib.bungee.BungeePlugin
import com.ericlam.mc.kotlib.bungee.BungeeUtils
import com.ericlam.mc.kotlib.command.CommandError
import com.ericlam.mc.kotlib.command.KCommand
import com.ericlam.mc.kotlib.config.ConfigBuilder
import com.ericlam.mc.kotlib.config.ConfigFactory
import com.ericlam.mc.kotlib.config.Prefix
import com.ericlam.mc.kotlib.config.Resource
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import com.ericlam.mc.kotlib.config.dto.MessageFile
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import java.util.logging.Logger


object KotLib : KotLibAPI {
    private lateinit var db: Database
    private val bMap: MutableMap<BukkitPlugin, BukkitUtils> = mutableMapOf()
    private val kMap: MutableMap<BungeePlugin, BungeeUtils> = mutableMapOf()

    internal lateinit var message: Message
    internal lateinit var config: Config

    override fun <T> transaction(t: Transaction.() -> T): T {
        if (!singlePoolEnabled) throw IllegalStateException("SQLPool Database is null, maybe single pool not enabled ?")
        return org.jetbrains.exposed.sql.transactions.transaction(db, t)
    }

    override fun getBukkitUtils(plugin: BukkitPlugin): BukkitUtils {
        return bMap[plugin] ?: BukkitTools(plugin).also { bMap[plugin] = it }
    }

    override fun getBungeeUtils(plugin: BungeePlugin): BungeeUtils {
        return kMap[plugin] ?: BungeeTools(plugin).also { kMap[plugin] = it }
    }

    override fun getConfigFactory(plugin: KotlinPlugin): ConfigFactory {
        return ConfigBuilder(plugin)
    }

    override fun <T> asyncTransaction(plugin: KotlinPlugin, run: Transaction.() -> T): AsyncInvoker<T> {
        if (!singlePoolEnabled) throw IllegalStateException("SQLPool Database is null, maybe single pool not enabled ?")
        return let {
            if (isBungee(plugin)) {
                BungeeAsyncInvoker({ transaction(run) }, plugin as BungeePlugin)
            } else {
                BukkitAsyncInvoker({ transaction(run) }, plugin as BukkitPlugin)
            }
        }
    }

    override val singlePoolEnabled: Boolean
        get() = this::db.isInitialized


    internal fun isBungee(plugin: KotlinPlugin): Boolean {
        return try {
            plugin is BungeePlugin
        } catch (e: NoClassDefFoundError) {
            false
        }
    }


    internal fun debug(name: String, logger: Logger, msg: String?) {
        if (config.debug_enabled && name in config.debug_plugins) {
            logger.info("[DEBUG] $msg")
        }
    }

    internal fun debug(msg: String?) {
        debug("KotLib", Logger.getLogger("KotLib"), msg)
    }

    internal fun initMySQL(sql: SQL) {
        with(sql) {
            val config = HikariConfig()
            if (!single_pool_enable) return
            config.jdbcUrl = "jdbc:$data_source://$host:$port/$database?useSSL=$use_ssl"
            config.username = username
            config.password = password
            config.driverClassName = driver
            config.poolName = pool.name
            config.maximumPoolSize = pool.max_size
            config.minimumIdle = pool.min_size
            config.connectionTimeout = pool.connection_timeout
            config.idleTimeout = pool.idle_timeout
            config.maxLifetime = pool.max_life_time
            config.addDataSourceProperty("cachePrepStmts", true)
            config.addDataSourceProperty("useServerPrepStmts", true)
            config.addDataSourceProperty("prepStmtCacheSize", 250)
            config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048)
            config.addDataSourceProperty("characterEncoding", "utf8")
            val dataSource = HikariDataSource(config)
            db = Database.connect(dataSource)
            org.jetbrains.exposed.sql.transactions.transaction(db) {
                addLogger(StdOutSqlLogger)
            }
        }
    }

    internal fun handleCommandError(cmdSender: KCommand.CommandSender, cmd: KCommand, args: Array<out String>) {
        catch<CommandError>({
            val path = when (it.mark) {
                CommandError.ErrorType.FEW_ARGS -> "no-args"
                CommandError.ErrorType.NO_PERMISSION -> "no-perm"
            }
            with(message) {
                cmdSender.sendMessage(this[path].msgFormat(it.message))
            }
        }) {
            cmd.invokeCommand(cmdSender, args)
        }
    }

    @Resource(locate = "sql.yml")
    data class SQL(val single_pool_enable: Boolean,
                   val data_source: String,
                   val driver: String,
                   val host: String,
                   val port: Int,
                   val database: String,
                   val username: String,
                   val password: String,
                   val use_ssl: Boolean,
                   val pool: Pool) : ConfigFile()

    data class Pool(val name: String,
                    val min_size: Int,
                    val max_size: Int,
                    val connection_timeout: Long,
                    val idle_timeout: Long,
                    val max_life_time: Long)

    @Resource(locate = "config.yml")
    data class Config(val yaml_warn_if_foreign_key_unknown: Boolean,
                      val debug_enabled: Boolean,
                      val debug_plugins: List<String>,
                      val foreign_key_unchangeable: Boolean,
                      val foreign_key_mode: ForeignKeyMode = ForeignKeyMode.NULLABLE) : ConfigFile() {

        enum class ForeignKeyMode {
            DELETE, NULLABLE
        }
    }

    @Prefix(path = "prefix")
    @Resource(locate = "lang.yml")
    class Message : MessageFile()
}