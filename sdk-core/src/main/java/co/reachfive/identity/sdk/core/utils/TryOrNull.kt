package co.reachfive.identity.sdk.core.utils

internal object TryOrNull {
    fun <T> tryOrNull(callback: () -> T): T? {
        return try {
            callback()
        } catch (e: Exception) {
            null
        }
    }
}