package com.reach5.identity.sdk.core.models

import com.google.gson.annotations.SerializedName

data class RequestPasswordResetRequest (
    @SerializedName("client_id")
    val clientId: String,
    val email: String?,
    @SerializedName("redirect_url")
    val redirectUrl: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String?
)



