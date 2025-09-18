package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class MfaCredentialsStartPhoneRegisteringRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    val action: String?,
    @SerializedName("trust_device")
    val trustDevice: Boolean
) : Parcelable

@Parcelize
data class MfaCredentialsStartEmailRegisteringRequest(
    @SerializedName("redirect_url")
    val redirectUrl: String?,
    val action: String?,
    @SerializedName("trust_device")
    val trustDevice: Boolean
) : Parcelable

@Parcelize
data class MfaCredentialsVerifyPhoneRegisteringRequest(
    @SerializedName("verification_code")
    val verificationCode: String,
    @SerializedName("trust_device")
    val trustDevice: Boolean
) : Parcelable

@Parcelize
data class MfaRemovePhoneNumberRequest(
    @SerializedName("phone_number")
    val phoneNumber: String
) : Parcelable