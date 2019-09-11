package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.models.PasswordlessAuthType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PasswordlessCodeVerificationRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("verification_code")
    val verificationCode: String,
    @SerializedName("auth_type")
    val authType: PasswordlessAuthType
) : Parcelable