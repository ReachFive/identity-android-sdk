package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PasswordlessVerificationRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("c")
    val verificationCode: String
): Parcelable
