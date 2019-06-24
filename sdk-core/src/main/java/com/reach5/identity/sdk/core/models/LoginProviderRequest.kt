package com.reach5.identity.sdk.core.models

import com.google.gson.annotations.SerializedName

data class LoginProviderRequest(
    val provider: String,
    @SerializedName("provider_token")
    val providerToken: String? = null,
    val code: String? = null,
    val origin: String? = null,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("response_type")
    val responseType: String = "token",
    val scope: String
)
