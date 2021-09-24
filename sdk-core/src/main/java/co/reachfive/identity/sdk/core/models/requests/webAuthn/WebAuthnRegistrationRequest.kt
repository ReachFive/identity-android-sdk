package co.reachfive.identity.sdk.core.models.requests.webAuthn

import android.os.Parcelable
import co.reachfive.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class WebAuthnRegistrationRequest(
    val origin: String,
    @SerializedName("friendly_name")
    val friendlyName: String,
    val profile: ProfileWebAuthnSignupRequest? = null,
    @SerializedName("client_id")
    val clientId: String? = null
) : Parcelable