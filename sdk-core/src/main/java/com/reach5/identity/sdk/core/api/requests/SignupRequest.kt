package com.reach5.identity.sdk.core.api.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.models.ProfileSignupRequest
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SignupRequest(
    @SerializedName("client_id")
    val clientId: String,
    val data: ProfileSignupRequest,
    val scope: String,
    @SerializedName("accept_tos")
    val acceptTos: Boolean? = null
) : Parcelable
