package com.reach5.identity.sdk.web

import android.os.Parcelable
import com.reach5.identity.sdk.core.models.ProviderConfig
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.SdkInfos
import com.reach5.identity.sdk.core.utils.Pkce
import kotlinx.android.parcel.Parcelize
import java.net.URLEncoder

@Parcelize
internal data class CustomTabProviderConfig(
    val providerConfig: ProviderConfig,
    val sdkConfig: SdkConfig,
    val origin: String
) : Parcelable {
    fun buildUrl(pkce: Pkce): String {
        val scope = (providerConfig.scope)
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
}