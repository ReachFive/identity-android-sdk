package com.reach5.identity.sdk.core.utils

fun formatScope(scope: Collection<String>): String {
    return scope.toSet().joinToString(" ")
}