package com.reach5.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import android.util.Base64
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import kotlinx.android.parcel.Parcelize

@Parcelize
data class R5PublicKeyCredentialDescriptor(
    val type: String,
    val id: String,
    val transports: List<String>? = null
): Parcelable {
    fun toPublicKeyCredentialDescriptor(): PublicKeyCredentialDescriptor {
        return PublicKeyCredentialDescriptor(
            type,
            Base64.decode(id.toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE),
            transports?.map { it -> Transport.valueOf(it) }
        )
    }
}