package com.spring.boot.production.template.utils

import com.spring.boot.production.template.utils.Utils.context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

suspend fun <A, B> Iterable<A>.mapAsync(transform: suspend (A) -> B): List<B> {
    return coroutineScope {
        map { item -> async(context()) { transform(item) } }.awaitAll()
    }
}

fun <A, B> Iterable<A>.mapAsyncDeferred(scope: CoroutineScope, transform: suspend (A) -> B): List<Deferred<B>> {
    return this.map { item ->
        scope.async(context()) { transform(item) }
    }
}

fun <T> CoroutineScope.asyncIO(block: suspend CoroutineScope.() -> T): Deferred<T> {
    return async(context = context(), block = block)
}

suspend fun <T> withContextIO(block: suspend CoroutineScope.() -> T): T {
    return withContext(context = context(), block = block)
}