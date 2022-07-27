package co.reachfive.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import co.reachfive.identity.sdk.core.utils.WebAuthn
import com.google.android.gms.fido.fido2.api.common.*
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class RegistrationOptions(
    @SerializedName("friendly_name")
    val friendlyName: String,
    val options: CredentialCreationOptions
) : Parcelable {
    fun toFido2Model(): PublicKeyCredentialCreationOptions {
        val publicKey = options.publicKey

        return PublicKeyCredentialCreationOptions.Builder()
            .setRp(
                PublicKeyCredentialRpEntity(
                    publicKey.rp.id,
                    publicKey.rp.name,
                    null
                )
            )
            .setUser(
                PublicKeyCredentialUserEntity(
                    publicKey.user.id.toByteArray(),
                    publicKey.user.name,
                    null,
                    publicKey.user.displayName
                )
            )
            .setChallenge(WebAuthn.decodeBase64(publicKey.challenge))
            .setParameters(publicKey.pubKeyCredParams.map {
                PublicKeyCredentialParameters(it.type, it.alg)
            })
            .setTimeoutSeconds(publicKey.timeout?.toDouble()?.div(1000))
            .setExcludeList(publicKey.excludeCredentials?.map { it.toPublicKeyCredentialDescriptor() })
            .setAuthenticatorSelection(
                AuthenticatorSelectionCriteria.Builder()
                    .setAttachment(publicKey.authenticatorSelection?.authenticatorAttachment?.let {
                        Attachment.valueOf(
                            it
                        )
                    })
                    .build()
            )
            .setAttestationConveyancePreference(AttestationConveyancePreference.fromString(publicKey.attestation))
            .build()
    }
}

@Parcelize
data class CredentialCreationOptions(
    @SerializedName("public_key")
    val publicKey: R5PublicKeyCredentialCreationOptions
) : Parcelable

@Parcelize
data class R5PublicKeyCredentialCreationOptions(
    val rp: R5PublicKeyCredentialRpEntity,
    val user: R5PublicKeyCredentialUserEntity,
    val challenge: String,
    @SerializedName("pub_key_cred_params")
    val pubKeyCredParams: List<R5PublicKeyCredentialParameter>,
    val timeout: Int? = null,
    @SerializedName("exclude_credentials")
    val excludeCredentials: List<R5PublicKeyCredentialDescriptor>? = null,
    @SerializedName("authenticator_selection")
    val authenticatorSelection: R5AuthenticatorSelectionCriteria? = null,
    val attestation: String
) : Parcelable

@Parcelize
data class R5PublicKeyCredentialRpEntity(
    val id: String,
    val name: String
) : Parcelable

@Parcelize
data class R5PublicKeyCredentialUserEntity(
    val id: String,
    @SerializedName("display_name")
    val displayName: String,
    val name: String
) : Parcelable

@Parcelize
data class R5PublicKeyCredentialParameter(
    val alg: Int,
    val type: String
) : Parcelable

@Parcelize
data class R5AuthenticatorSelectionCriteria(
    @SerializedName("authenticator_attachement")
    val authenticatorAttachment: String,
    @SerializedName("require_resident_key")
    val requireResidentKey: Boolean,
    @SerializedName("resident_key")
    val residentKey: String,
    @SerializedName("user_verification")
    val userVerification: String
) : Parcelable






