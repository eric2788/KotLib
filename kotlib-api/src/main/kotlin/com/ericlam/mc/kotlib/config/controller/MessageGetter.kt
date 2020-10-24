package com.ericlam.mc.kotlib.config.controller

import com.ericlam.mc.kotlib.config.Prefix
import com.ericlam.mc.kotlib.translateColorCode
import java.io.File

abstract class MessageGetter(protected val p: Prefix, protected val file: File) {

    abstract operator fun get(path: String): String

    abstract fun getPure(path: String): String

    abstract val prefix: String

    protected fun color(str: String?): String = str?.translateColorCode() ?: "null"

}