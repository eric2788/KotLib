package com.ericlam.mc.kotlib.config.dao

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class DataResource(val folder: String)