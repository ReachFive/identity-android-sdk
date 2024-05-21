package co.reachfive.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class WebAuthnResetRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("public_key_credential")
    val publicKeyCredential: RegistrationPublicKeyCredential,
    @SerializedName("verification_code")
    val verificationCode: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
) : Parcelable
