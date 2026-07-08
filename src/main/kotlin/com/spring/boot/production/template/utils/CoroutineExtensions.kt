package com.spring.boot.production.template.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext

private val context = Dispatchers.IO + MDCContext()

suspend fun <A, B> Iterable<A>.mapAsync(
    transform: suspend (A) -> B): List<B> {
    return coroutineScope {
        map { item -> async(context) { transform(item) } }.awaitAll()
    }
}

fun <T> CoroutineScope.asyncIO(block: suspend CoroutineScope.() -> T): Deferred<T> {
    return async(context = context, block = block)
}

suspend fun <T> withContextIO(block: suspend CoroutineScope.() -> T): T {
    return withContext(context = context, block = block)
}