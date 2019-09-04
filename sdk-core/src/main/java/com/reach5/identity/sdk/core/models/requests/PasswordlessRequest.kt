package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.models.PasswordlessAuthType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PasswordlessRequest(
    @SerializedName("client_id")
    val clientId: String,
    val email: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
    @SerializedName("auth_type")
    val authType: PasswordlessAuthType,
    @SerializedName("code_challenge")
    val codeChallenge: String,
    @SerializedName("code_challenge_method")
    val codeChallengeMethod: String,
    @SerializedName("response_type")
    val responseType: String,
    @SerializedName("redirect_uri")
    val redirectUri: String
) : Parcelable