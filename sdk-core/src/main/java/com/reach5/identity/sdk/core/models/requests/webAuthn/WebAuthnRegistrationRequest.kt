package com.reach5.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WebAuthnRegistrationRequest(
    val origin: String,
    @SerializedName("friendly_name")
    val friendlyName: String
): Parcelable