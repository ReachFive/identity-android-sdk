package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PasswordlessVerificationResponse(
    @SerializedName("code")
    val authCode: String
) : Parcelable
