package com.ericlam.mc.kotlib.async

interface AsyncInvoker<T> {

    fun thenSync(run: (T) -> Unit): AsyncInvoker<T>

    fun catch(catch: (Throwable) -> Unit): AsyncInvoker<T>

    fun finally(run: () -> Unit)

}