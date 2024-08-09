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

@Parcelize
data class LoginMfaConf(
    val activity: @RawValue Activity,
    val redirectUri: String
) : Parcelable