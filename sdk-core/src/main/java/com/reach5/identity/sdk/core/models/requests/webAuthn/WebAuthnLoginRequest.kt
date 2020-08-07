package com.reach5.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

sealed class WebAuthnLoginRequest {
    @Parcelize
    data class EmailWebAuthnLoginRequest(
        val origin: String,
        val email: String,
        val scope: String? = null
    ) : WebAuthnLoginRequest(), Parcelable

    @Parcelize
    data class PhoneNumberWebAuthnLoginRequest(
        val origin: String,
        @SerializedName("phone_number")
        val phoneNumber: String,
        val scope: String? = null
    ) : WebAuthnLoginRequest(), Parcelable

    @Parcelize
    private data class EmailWithClientIdLoginRequest(
        @SerializedName("client_id")
        val clientId: String,
        val origin: String,
        val email: String,
        val scope: String? = null
    ) : WebAuthnLoginRequest(), Parcelable

    @Parcelize
    private data class PhoneNumberWithClientIdLoginRequest(
        @SerializedName("client_id")
        val clientId: String,
        val origin: String,
        @SerializedName("phone_number")
        val phoneNumber: String,
        val scope: String? = null
    ) : WebAuthnLoginRequest(), Parcelable

    companion object {
        fun <T: WebAuthnLoginRequest> enrichWithClientId(request: T, clientId: String): WebAuthnLoginRequest {
            return when (request) {
                is EmailWebAuthnLoginRequest ->
                    EmailWithClientIdLoginRequest(
                        clientId,
                        request.origin,
                        request.email,
                        request.scope
                    )
                is PhoneNumberWebAuthnLoginRequest ->
                    PhoneNumberWithClientIdLoginRequest(
                        clientId,
                        request.origin,
                        request.phoneNumber,
                        request.scope
                    )
                else -> request
            }
        }
    }
}
