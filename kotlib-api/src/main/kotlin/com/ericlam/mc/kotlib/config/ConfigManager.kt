package com.ericlam.mc.kotlib.config

import com.ericlam.mc.kotlib.config.dao.Dao
import com.ericlam.mc.kotlib.config.dao.DataFile
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import kotlin.reflect.KClass

interface ConfigManager {

    fun <T : ConfigFile> getConfig(config: KClass<T>): T

    fun <T : DataFile, V : Dao<T, *>> getDao(dao: KClass<V>): V

}