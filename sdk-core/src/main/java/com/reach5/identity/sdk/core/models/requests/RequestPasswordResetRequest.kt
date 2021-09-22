package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RequestPasswordResetRequest(
    @SerializedName("client_id")
    val clientId: String,
    val email: String?,
    @SerializedName("redirect_url")
    val redirectUrl: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String?
) : Parcelable