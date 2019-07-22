package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthCodeRequest(
    @SerializedName("client_id")
    val clientId: String,
    val code: String,
    @SerializedName("grant_type")
    val grantType: String,
    @SerializedName("redirect_uri")
    val redirectUri: String,
    @SerializedName("code_verifier")
    val codeVerifier: String
) : Parcelable