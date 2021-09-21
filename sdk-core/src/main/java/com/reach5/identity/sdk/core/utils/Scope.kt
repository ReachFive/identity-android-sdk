package co.reachfive.identity.sdk.core.utils

fun formatScope(scope: Collection<String>): String {
    return scope.toSet().joinToString(" ")
}