package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SignupRequest(
    @SerializedName("client_id")
    val clientId: String,
    val data: ProfileSignupRequest,
    val scope: String,
    @SerializedName("redirect_url")
    val redirectUrl: String? = null,
    val origin: String?
) : Parcelable