package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RefreshRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("redirect_uri")
    val redirectUri: String,
    @SerializedName("grant_type")
    val grantType: String = "refresh_token"
) : Parcelable
