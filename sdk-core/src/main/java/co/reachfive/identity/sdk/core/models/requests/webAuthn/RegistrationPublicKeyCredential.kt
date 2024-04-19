package co.reachfive.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import co.reachfive.identity.sdk.core.utils.WebAuthn.encodeToBase64
import co.reachfive.identity.sdk.core.utils.WebAuthn.publicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import kotlinx.parcelize.Parcelize

object WebAuthnRegistration {
    fun createRegistrationPublicKeyCredential(authenticatorAttestationResponse: AuthenticatorAttestationResponse): RegistrationPublicKeyCredential {
        return RegistrationPublicKeyCredential(
            id = encodeToBase64(authenticatorAttestationResponse.keyHandle),
            rawId = encodeToBase64(authenticatorAttestationResponse.keyHandle),
            type = publicKeyCredentialType,
            response = R5AuthenticatorAttestationResponse(
                attestationObject = encodeToBase64(authenticatorAttestationResponse.attestationObject),
                clientDataJSON = encodeToBase64(authenticatorAttestationResponse.clientDataJSON)
            )
        )
    }
}

@Parcelize
data class RegistrationPublicKeyCredential(
    val id: String,
    val rawId: String,
    val type: String,
    val response: R5AuthenticatorAttestationResponse
) : Parcelable

@Parcelize
data class R5AuthenticatorAttestationResponse(
    val attestationObject: String,
    val clientDataJSON: String
) : Parcelable
