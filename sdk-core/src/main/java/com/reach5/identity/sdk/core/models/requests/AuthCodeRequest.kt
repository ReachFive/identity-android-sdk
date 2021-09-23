package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AuthCodeRequest(
    @SerializedName("client_id")
    val clientId: String,
    val code: String,
    @SerializedName("redirect_uri")
    val redirectUri: String,
    @SerializedName("code_verifier")
    val codeVerifier: String,
    @SerializedName("grant_type")
    val grantType: String = "authorization_code"
) : Parcelable
