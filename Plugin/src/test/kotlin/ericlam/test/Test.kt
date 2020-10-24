package ericlam.test

import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation

fun main() {
    val key = Test::class.declaredMemberProperties.find { f -> f.hasAnnotation<Key>() }
    println("key in test class: ${key?.name}")
}


data class Test(@Key val test: String)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Key