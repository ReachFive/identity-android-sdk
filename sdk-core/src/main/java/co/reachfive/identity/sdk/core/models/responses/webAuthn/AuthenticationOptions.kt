package co.reachfive.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import co.reachfive.identity.sdk.core.utils.WebAuthn
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

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
    val rpId: String,
    val allowCredentials: List<R5PublicKeyCredentialDescriptor>,
    val userVerification: String
) : Parcelable
