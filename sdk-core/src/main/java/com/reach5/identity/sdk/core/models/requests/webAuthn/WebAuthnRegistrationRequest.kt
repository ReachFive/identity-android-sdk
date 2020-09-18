package com.reach5.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.models.Profile
import com.reach5.identity.sdk.core.models.requests.ProfileSignupRequest
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WebAuthnRegistrationRequest(
    val origin: String,
    @SerializedName("friendly_name")
    val friendlyName: String,
    val profile: ProfileSignupRequest? = null
): Parcelable