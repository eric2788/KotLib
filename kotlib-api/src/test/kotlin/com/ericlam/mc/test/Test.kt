package com.ericlam.mc.test

import com.ericlam.mc.kotlib.bungee.BungeePlugin
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


fun main() {
    println(1 in 1..6)
    println("abc")

    object : BungeePlugin() {
        override fun enable() {
            runAsync {
                println("hello world")
            }.thenSync {
                println("finished")
            }.catch { e ->
                e.printStackTrace()
            }.finally {
                println("done")
            }
        }

    }
}

interface Data

class DataImpl : Data

class DataController(d: DaoTest<DataImpl, String>) : DaoTest<DataImpl, String> by d

interface DaoTest<T : Data, V> {
    fun test()
}

class CustomDao<T : Data, V : Comparable<*>>(t: Class<T>, v: Class<V>) : DaoTest<T, V> {
    override fun test() {
        println("TEST SUCCESS")
    }

}

class Manager {

    fun <T : Data, V : DaoTest<T, *>> getConfig(dao: KClass<V>): V {
        val d = CustomDao(DataImpl::class.java, String::class.java)
        println(dao.qualifiedName)
        println(dao.constructors)
        return dao.primaryConstructor!!.call(d)
    }
}