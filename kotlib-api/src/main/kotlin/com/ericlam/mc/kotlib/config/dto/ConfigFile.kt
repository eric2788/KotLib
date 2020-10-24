package com.ericlam.mc.kotlib.config.dto

import com.ericlam.mc.kotlib.config.controller.FileController

abstract class ConfigFile {

    private lateinit var controller: FileController

    fun save() = controller.save(this)

    fun reload() = controller.reload(this)

}