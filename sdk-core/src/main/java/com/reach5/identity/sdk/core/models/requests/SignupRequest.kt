package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.models.Profile
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SignupRequest(
    @SerializedName("client_id")
    val clientId: String,
    val data: Profile,
    val scope: String,
    @SerializedName("accept_tos")
    val acceptTos: Boolean? = null
) : Parcelable