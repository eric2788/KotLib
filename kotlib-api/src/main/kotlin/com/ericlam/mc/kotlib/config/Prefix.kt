package com.ericlam.mc.kotlib.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Prefix(val path: String)