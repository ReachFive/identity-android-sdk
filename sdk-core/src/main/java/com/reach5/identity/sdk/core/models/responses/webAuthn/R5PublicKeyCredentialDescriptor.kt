package co.reachfive.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import co.reachfive.identity.sdk.core.utils.WebAuthn
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import kotlinx.parcelize.Parcelize

@Parcelize
data class R5PublicKeyCredentialDescriptor(
    val type: String,
    val id: String,
    val transports: List<String>? = null
) : Parcelable {
    fun toPublicKeyCredentialDescriptor(): PublicKeyCredentialDescriptor {
        return PublicKeyCredentialDescriptor(
            type,
            WebAuthn.decodeBase64(id),
            transports?.map { it -> Transport.valueOf(it) }
        )
    }
}