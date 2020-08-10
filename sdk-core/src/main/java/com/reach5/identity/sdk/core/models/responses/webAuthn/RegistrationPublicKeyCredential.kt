package com.reach5.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import android.util.Base64
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

object WebAuthnRegistration {
    private const val publicKeyCredentialType = "public-key"

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

    private fun encodeToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
    }
}

@Parcelize
data class RegistrationPublicKeyCredential(
    val id: String,
    @SerializedName("raw_id")
    val rawId: String,
    val type: String,
    val response: R5AuthenticatorAttestationResponse
): Parcelable

@Parcelize
data class R5AuthenticatorAttestationResponse(
    @SerializedName("attestation_object")
    val attestationObject: String,
    @SerializedName("client_data_json")
    val clientDataJSON: String
): Parcelable