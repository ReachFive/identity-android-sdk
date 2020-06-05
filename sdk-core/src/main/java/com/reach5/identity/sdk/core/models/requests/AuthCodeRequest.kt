package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.ReachFive
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthCodeRequest(
    @SerializedName("client_id")
    val clientId: String,
    val code: String,
    @SerializedName("redirect_uri")
    val redirectUri: String,
    @SerializedName("code_verifier")
    val codeVerifier: String
) : Parcelable {
    @SerializedName("grant_type")
    val grantType: String = "authorization_code"
}
