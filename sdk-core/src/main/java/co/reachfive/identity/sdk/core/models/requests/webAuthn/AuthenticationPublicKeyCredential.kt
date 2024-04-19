package co.reachfive.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import co.reachfive.identity.sdk.core.utils.WebAuthn.encodeToBase64
import co.reachfive.identity.sdk.core.utils.WebAuthn.publicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

object WebAuthnAuthentication {
    fun createAuthenticationPublicKeyCredential(authenticatorAssertionResponse: AuthenticatorAssertionResponse): AuthenticationPublicKeyCredential {
        return AuthenticationPublicKeyCredential(
            id = encodeToBase64(authenticatorAssertionResponse.keyHandle),
            rawId = encodeToBase64(authenticatorAssertionResponse.keyHandle),
            type = publicKeyCredentialType,
            response = R5AuthenticatorAssertionResponse(
                signature = encodeToBase64(authenticatorAssertionResponse.signature),
                userHandle = authenticatorAssertionResponse.userHandle?.let { encodeToBase64(it) },
                clientDataJSON = encodeToBase64(authenticatorAssertionResponse.clientDataJSON),
                authenticatorData = encodeToBase64(authenticatorAssertionResponse.authenticatorData)
            )
        )
    }
}

@Parcelize
data class AuthenticationPublicKeyCredential(
    val id: String,
    val rawId: String,
    val type: String,
    val response: R5AuthenticatorAssertionResponse
) : Parcelable

@Parcelize
data class R5AuthenticatorAssertionResponse(
    val authenticatorData: String,
    val clientDataJSON: String,
    val signature: String,
    val userHandle: String? = null
) : Parcelable
