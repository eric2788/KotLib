package com.ericlam.mc.kotlib.config

import com.ericlam.mc.kotlib.config.dao.Dao
import com.ericlam.mc.kotlib.config.dao.DataFile
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import kotlin.reflect.KClass

interface ConfigFactory {

    fun <T : ConfigFile> register(config: KClass<T>): ConfigFactory

    fun <T : DataFile, V : Dao<T, *>> registerDao(config: KClass<T>, dao: KClass<V>): ConfigFactory

    fun dump(): ConfigManager
}