package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.models.PasswordlessAuthType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PasswordlessAuthorizationCodeRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("auth_type")
    val authType: PasswordlessAuthType = PasswordlessAuthType.SMS,
    @SerializedName("verification_code")
    val verificationCode: String,
    @SerializedName("code_verifier")
    val codeVerifier: String,
    @SerializedName("response_type")
    val responseType: String
) : Parcelable