package com.ericlam.mc.kotlib.config.dao

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class ForeignKey(val link: KClass<out DataFile>)