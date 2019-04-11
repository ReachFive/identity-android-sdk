package com.reach5.identity.sdk.core.utils

@FunctionalInterface
interface Callback<T> {
    fun call(v: T)
}

typealias Success<T> = (data: T) -> Unit
typealias Failure<E> = (error: E) -> Unit
