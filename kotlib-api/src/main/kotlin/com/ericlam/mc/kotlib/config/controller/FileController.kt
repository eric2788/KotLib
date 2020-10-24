package com.ericlam.mc.kotlib.config.controller

import com.ericlam.mc.kotlib.config.dto.ConfigFile

interface FileController {
    fun <T : ConfigFile> save(configFile: T)
    fun <T : ConfigFile> reload(configFile: T)
}