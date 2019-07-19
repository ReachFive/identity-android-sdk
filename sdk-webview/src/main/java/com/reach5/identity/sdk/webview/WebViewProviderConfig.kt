package com.reach5.identity.sdk.webview

import android.os.Parcel
import android.os.Parcelable
import com.reach5.identity.sdk.core.models.ProviderConfig
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.SdkInfos
import com.reach5.identity.sdk.core.utils.Pkce
import java.net.URLEncoder

internal data class WebViewProviderConfig(
    val providerConfig: ProviderConfig,
    val sdkConfig: SdkConfig,
    val origin: String
) : Parcelable {

    fun buildUrl(pkce: Pkce): String {
        val scope = (providerConfig.scope ?: setOf()).plus("openid")
        val params = mapOf(
            "client_id" to sdkConfig.clientId,
            "provider" to providerConfig.provider,
            "origin" to origin,
            "redirect_uri" to "reachfive://callback",
            "response_type" to "code",
            "scope" to scope.joinToString(" "),
            "code_challenge" to pkce.codeChallenge,
            "code_challenge_method" to pkce.codeChallengeMethod
        ) + SdkInfos.getQueries()
        val query = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "https://${sdkConfig.domain}/oauth/authorize?$query"
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(ProviderConfig::class.java.classLoader)!!,
        parcel.readParcelable(SdkConfig::class.java.classLoader)!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(providerConfig, flags)
        parcel.writeParcelable(sdkConfig, flags)
        parcel.writeString(origin)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WebViewProviderConfig> {
        override fun createFromParcel(parcel: Parcel): WebViewProviderConfig {
            return WebViewProviderConfig(parcel)
        }

        override fun newArray(size: Int): Array<WebViewProviderConfig?> {
            return arrayOfNulls(size)
        }
    }
}
