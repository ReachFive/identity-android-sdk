package com.reach5.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.utils.formatScope
import kotlinx.android.parcel.Parcelize
import java.lang.reflect.Type

sealed class WebAuthnLoginRequest {
    @Parcelize
    data class EmailWebAuthnLoginRequest(
        val origin: String,
        val email: String,
        val scope: Set<String>? = null
    ) : WebAuthnLoginRequest(), Parcelable

    @Parcelize
    data class PhoneNumberWebAuthnLoginRequest(
        val origin: String,
        @SerializedName("phone_number")
        val phoneNumber: String,
        val scope: Set<String>? = null
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
        fun <T : WebAuthnLoginRequest> enrichWithClientId(
            request: T,
            clientId: String
        ): WebAuthnLoginRequest {
            return when (request) {
                is EmailWebAuthnLoginRequest ->
                    EmailWithClientIdLoginRequest(
                        clientId,
                        request.origin,
                        request.email,
                        formatScope(request.scope as Collection<String>)
                    )
                is PhoneNumberWebAuthnLoginRequest ->
                    PhoneNumberWithClientIdLoginRequest(
                        clientId,
                        request.origin,
                        request.phoneNumber,
                        formatScope(request.scope as Collection<String>)
                    )
                else -> request
            }
        }
    }
}

internal class WebAuthnLoginRequestSerializer : JsonSerializer<WebAuthnLoginRequest> {
    override fun serialize(
        src: WebAuthnLoginRequest?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return Gson().toJsonTree(src)
    }
}
