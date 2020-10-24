package com.ericlam.mc.kotlib.config.dto

import com.ericlam.mc.kotlib.config.controller.MessageGetter

abstract class MessageFile : ConfigFile() {

    private lateinit var getter: MessageGetter

    val prefix: String
        get() = getter.prefix

    operator fun get(path: String) = getter[path]

    fun getPure(path: String) = getter.getPure(path)

}