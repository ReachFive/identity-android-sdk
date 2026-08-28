package co.reachfive.identity.sdk.core.models.requests

import android.app.Activity
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class LoginRequest(
    val email: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
    @SerializedName("custom_identifier")
    val customIdentifier: String?,
    val password: String,
    @SerializedName("client_id")
    val clientId: String,
    val scope: String,
    val origin: String?
) : Parcelable

/**
 * Configuration used to continue an MFA step-up when it is required during a login flow.
 */
@Parcelize
data class LoginMfaConf(
    /**
     * The Android activity used to continue the MFA step-up flow.
     */
    val activity: @RawValue Activity,
    /**
     * Optional redirect URI for the step-up continuation.
     */
    @Deprecated("redirectUri will be removed in a future release")
    val redirectUri: String? = null
) : Parcelable