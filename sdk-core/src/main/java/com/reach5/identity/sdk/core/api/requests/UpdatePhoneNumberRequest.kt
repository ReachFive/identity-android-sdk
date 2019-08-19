package com.reach5.identity.sdk.core.api.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UpdatePhoneNumberRequest(
    @SerializedName("phone_number")
    val phoneNumber: String
) : Parcelable
