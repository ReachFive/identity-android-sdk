package com.reach5.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import android.util.Base64
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.*
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RegistrationOptions(
    @SerializedName("friendly_name")
    val friendlyName: String,
    @SerializedName("options")
    val options: CredentialCreationOptions
): Parcelable {
    fun toFido2Model(): PublicKeyCredentialCreationOptions {
        val publicKey = options.publicKey

        return PublicKeyCredentialCreationOptions.Builder()
            .setRp(PublicKeyCredentialRpEntity(
                publicKey.rp.id,
                publicKey.rp.name,
                null
            ))
            .setUser(PublicKeyCredentialUserEntity(
                publicKey.user.id.toByteArray(),
                publicKey.user.name,
                null,
                publicKey.user.displayName
            ))
            .setChallenge(Base64.decode(publicKey.challenge, Base64.DEFAULT))
            .setParameters(publicKey.pubKeyCredParams.map {
                PublicKeyCredentialParameters(it.type, it.alg)
            })
            .setTimeoutSeconds(publicKey.timeout?.toDouble()?.div(1000))
            .setExcludeList(publicKey.excludeCredentials?.map {
                PublicKeyCredentialDescriptor(it.type, it.id.toByteArray(), it.transports?.map { it -> Transport.valueOf(it) })
            })
            .setAuthenticatorSelection(
                AuthenticatorSelectionCriteria.Builder()
                    .setAttachment(publicKey.authenticatorSelection?.authenticatorAttachment?.let { Attachment.valueOf(it) })
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
): Parcelable

@Parcelize
data class R5PublicKeyCredentialCreationOptions(
    @SerializedName("rp")
    val rp: R5PublicKeyCredentialRpEntity,
    @SerializedName("user")
    val user: R5PublicKeyCredentialUserEntity,
    @SerializedName("challenge")
    val challenge: String,
    @SerializedName("pub_key_cred_params")
    val pubKeyCredParams: List<R5PublicKeyCredentialParameter>,
    @SerializedName("timeout")
    val timeout: Int? = null,
    @SerializedName("exclude_credentials")
    val excludeCredentials: List<R5PublicKeyCredentialDescriptor>? = null,
    @SerializedName("authenticator_selection")
    val authenticatorSelection: R5AuthenticatorSelectionCriteria? = null,
    @SerializedName("attestation")
    val attestation: String
): Parcelable

@Parcelize
data class R5PublicKeyCredentialRpEntity(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String
): Parcelable

@Parcelize
data class R5PublicKeyCredentialUserEntity(
    @SerializedName("id")
    val id: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("name")
    val name: String
): Parcelable

@Parcelize
data class R5PublicKeyCredentialParameter(
    @SerializedName("alg")
    val alg: Int,
    @SerializedName("type")
    val type: String
): Parcelable

@Parcelize
data class R5PublicKeyCredentialDescriptor(
    @SerializedName("type")
    val type: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("transports")
    val transports: List<String>? = null
): Parcelable

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
): Parcelable






