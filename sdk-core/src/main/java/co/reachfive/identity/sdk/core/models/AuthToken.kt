package co.reachfive.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * The authentication token returned after a successful login.
 */
@Parcelize
data class AuthToken(
    /**
     * The ID token JWT that contains the profile's information.
     * Not returned if the `openid` scope was not requested.
     */
    val idToken: String? = null,
    /**
     * The access token JWT used to call the ReachFive API.
     */
    val accessToken: String? = null,
    /**
     * The refresh token JWT used to obtain new access tokens once they expire.
     */
    val refreshToken: String? = null,
    /**
     * The type of token. Always equal to `Bearer`.
     */
    val tokenType: String? = null,
    /**
     * The lifetime in seconds of the access token.
     * If `expiresIn` is less than or equal to `0`, the token is expired.
     */
    val expiresIn: Int? = null,
    /**
     * The step-up token used to continue an MFA step-up flow.
     */
    @SerializedName("token")
    val stepUpToken: String? = null,
    /**
     * Authentication Methods Reference. Indicates the method(s) used during authentication.
     */
    val amr: List<String>? = null,
    /**
     * The user's information contained in the ID token.
     * Not returned if the `openid` scope was not requested.
     */
    val user: OpenIdUser? = null
) : Parcelable {

    @IgnoredOnParcel
    val authHeader: String = "$tokenType $accessToken"
}
