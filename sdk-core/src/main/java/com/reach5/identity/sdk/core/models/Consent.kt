package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Consent(
    val granted: Boolean,
    @SerializedName("consent_type")
    val consentType: Boolean?,
    val date: String
) : Parcelable