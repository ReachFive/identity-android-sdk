package com.reach5.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DeviceCredential(
    @SerializedName("friendly_name")
    val friendlyName: String,
    @SerializedName("id")
    val id: String
) : Parcelable