package com.reach5.identity.sdk.core.models

import android.os.Build

object SdkInfos {
    const val version = "4.0.0"
    const val platform = "android"
    val device: String = Build.DEVICE
    val model: String = Build.MODEL
    val product: String = Build.PRODUCT
    val apiLevel: Int = Build.VERSION.SDK_INT

    private fun deviceInfo(): String {
        return "API Level $apiLevel, Device $device, Model $model, Product $product"
    }

    fun getQueries(): Map<String, String> {
        return mapOf(
            "platform" to platform,
            "device" to deviceInfo(),
            "version" to version
        )
    }
}
