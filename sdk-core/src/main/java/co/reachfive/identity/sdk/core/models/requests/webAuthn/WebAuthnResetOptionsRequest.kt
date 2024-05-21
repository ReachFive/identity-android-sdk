package co.reachfive.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class WebAuthnResetOptionsRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("origin")
    val origin: String,
    @SerializedName("friendly_name")
    val friendlyName: String,
    @SerializedName("verification_code")
    val verificationCode: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
) : Parcelable
