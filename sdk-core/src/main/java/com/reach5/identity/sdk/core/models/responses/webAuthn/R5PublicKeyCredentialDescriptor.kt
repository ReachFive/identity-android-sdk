package com.reach5.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
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
        return PublicKeyCredentialDescriptor(type, id.toByteArray(), transports?.map { it -> Transport.valueOf(it) })
    }
}