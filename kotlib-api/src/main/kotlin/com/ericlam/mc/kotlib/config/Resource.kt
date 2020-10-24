package com.ericlam.mc.kotlib.config

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Resource(val locate: String, val copyTo: String = "")