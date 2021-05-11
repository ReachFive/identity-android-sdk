package com.reach5.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WebauthnSignupCredential(
    @SerializedName("webauthn_id")
    val webauthnId: String,
    @SerializedName("public_key_credential")
    val publicKeyCredential: RegistrationPublicKeyCredential
) : Parcelable