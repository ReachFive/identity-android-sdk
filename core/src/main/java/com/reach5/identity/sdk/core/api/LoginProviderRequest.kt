package com.reach5.identity.sdk.core.api

import com.google.gson.annotations.SerializedName

data class LoginProviderRequest(
    val provider: String,
    val code: String,
    val origin: String,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("response_type")
    val responseType: String = "token",
    val scope: String
)
