package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class VerifyEmailRequest(
    @SerializedName("verification_code")
    val verificationCode: String
) : Parcelable