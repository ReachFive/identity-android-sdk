package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LoginRequest(
    val username: String,
    val password: String,
    @SerializedName("grant_type")
    val grantType: String,
    @SerializedName("client_id")
    val clientId: String,
    val scope: String
) : Parcelable