package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
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
) : Parcelable