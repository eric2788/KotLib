package com.ericlam.mc.kotlib.config.dao

import kotlin.reflect.KClass

interface Dao<T : DataFile, V : Comparable<V>> {

    fun findAll(): List<T>

    fun findById(id: V): T?

    fun findByForeignId(foreignData: KClass<out DataFile>, id: Any): List<T>

    fun find(filter: T.() -> Boolean): List<T>

    fun update(id: V, update: T.() -> Unit): T?

    fun delete(id: V): Boolean

    fun deleteSome(filter: T.() -> Boolean): List<V>

    fun save(data: () -> T): V?

}