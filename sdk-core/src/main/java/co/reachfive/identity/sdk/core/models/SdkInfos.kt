package co.reachfive.identity.sdk.core.models

import android.os.Build
import co.reachfive.identity.sdk.core.BuildConfig

object SdkInfos {
    val version = BuildConfig.r5_sdk_version
    private const val platform = "android"
    private val device: String = Build.DEVICE
    private val model: String = Build.MODEL
    private val product: String = Build.PRODUCT
    private val apiLevel: Int = Build.VERSION.SDK_INT

    private fun deviceInfo(): String {
        return "API Level $apiLevel, Device $device, Model $model, Product $product"
    }

    fun getQueries(sdkMethod: String? = null): Map<String, String> {
        val method = if (sdkMethod != null) mapOf("sdk_method" to sdkMethod) else emptyMap()
        return mapOf(
            "platform" to platform,
            "device" to deviceInfo(),
            "version" to version,
        ) + method
    }
}
