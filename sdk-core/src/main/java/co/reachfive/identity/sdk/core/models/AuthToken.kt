package co.reachfive.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class AuthToken(
    // If the `openid` scope is not provided, the `idToken` is not returned
    val idToken: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: Int? = null,
    @SerializedName("token")
    val stepUpToken: String? = null,
    val amr: List<String>? = null,
    // The `user` field is optional because if the `openid` scope is not provided, the `user` is not retrieved
    val user: OpenIdUser? = null
) : Parcelable {

    @IgnoredOnParcel
    val authHeader: String = "$tokenType $accessToken"
}
