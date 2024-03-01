package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class MfaCredentialsStartPhoneRegisteringRequest(
    @SerializedName("phone_number")
    val phoneNumber: String
) : Parcelable

@Parcelize
data class MfaCredentialsStartEmailRegisteringRequest(
    @SerializedName("redirect_url")
    val redirectUrl: String?
): Parcelable

@Parcelize
data class MfaCredentialsVerifyPhoneRegisteringRequest(
    @SerializedName("verification_code")
    val verificationCode: String
) : Parcelable