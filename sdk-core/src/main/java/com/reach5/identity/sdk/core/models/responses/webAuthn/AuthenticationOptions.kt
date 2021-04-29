package com.reach5.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.utils.WebAuthn
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthenticationOptions(
    @SerializedName("public_key")
    val publicKey: R5PublicKeyCredentialRequestOptions
) : Parcelable {
    fun toFido2Model(): PublicKeyCredentialRequestOptions {
        return PublicKeyCredentialRequestOptions.Builder()
            .setChallenge(WebAuthn.decodeBase64(publicKey.challenge))
            .setTimeoutSeconds(publicKey.timeout?.toDouble()?.div(1000))
            .setRpId(publicKey.rpId)
            .setAllowList(publicKey.allowCredentials.map { it.toPublicKeyCredentialDescriptor() })
            .build()
    }
}

@Parcelize
data class R5PublicKeyCredentialRequestOptions(
    val challenge: String,
    val timeout: Int? = null,
    @SerializedName("rp_id")
    val rpId: String,
    @SerializedName("allow_credentials")
    val allowCredentials: List<R5PublicKeyCredentialDescriptor>,
    @SerializedName("user_verification")
    val userVerification: String
) : Parcelable