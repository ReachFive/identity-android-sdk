package com.reach5.identity.sdk.core.utils

import android.content.Intent

@FunctionalInterface
interface Callback<T> {
    fun call(v: T)
}

typealias Success<T> = (data: T) -> Unit
typealias SuccessWithNoContent<Unit> = Success<Unit>
typealias Redirect = (intent: Intent) -> Unit
typealias Failure<E> = (error: E) -> Unit
