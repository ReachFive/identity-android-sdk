package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PasswordlessStartRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
    @SerializedName("code_challenge")
    val codeChallenge: String,
    @SerializedName("code_challenge_method")
    val codeChallengeMethod: String,
    @SerializedName("response_type")
    val responseType: String,
    @SerializedName("redirect_uri")
    val redirectUri: String
) : Parcelable
