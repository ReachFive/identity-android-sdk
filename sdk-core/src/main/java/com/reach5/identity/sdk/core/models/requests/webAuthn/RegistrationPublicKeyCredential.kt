package com.reach5.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.utils.WebAuthn.encodeToBase64
import com.reach5.identity.sdk.core.utils.WebAuthn.publicKeyCredentialType
import kotlinx.android.parcel.Parcelize

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
    @SerializedName("raw_id")
    val rawId: String,
    val type: String,
    val response: R5AuthenticatorAttestationResponse
) : Parcelable

@Parcelize
data class R5AuthenticatorAttestationResponse(
    @SerializedName("attestation_object")
    val attestationObject: String,
    @SerializedName("client_data_json")
    val clientDataJSON: String
) : Parcelable