package com.reach5.identity.sdk.core.models

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    @SerializedName("client_id")
    val clientId: String,
    val data: Profile,
    val scope: String,
    @SerializedName("accept_tos")
    val acceptTos: Boolean? = null
)

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

data class LoginRequest(
    val username: String,
    val password: String,
    @SerializedName("grant_type")
    val grantType: String,
    @SerializedName("client_id")
    val clientId: String,
    val scope: String
)

data class UpdateEmailRequest (
    val email: String,
    @SerializedName("redirect_url")
    val redirectUrl: String? = null
)

data class RequestPasswordResetRequest (
    @SerializedName("client_id")
    val clientId: String,
    val email: String?,
    @SerializedName("redirect_url")
    val redirectUrl: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String?
)

data class VerifyPhoneNumberRequest (
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("verification_code")
    val verificationCode: String
)