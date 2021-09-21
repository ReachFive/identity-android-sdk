package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.google.gson.annotations.SerializedName
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.OpenIdUser
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.utils.Jwt
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TokenEndpointResponse(
    @SerializedName("id_token")
    val idToken: String? = null,

    @SerializedName("access_token")
    val accessToken: String? = null,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("token_type")
    val tokenType: String? = null,

    @SerializedName("expires_in")
    val expiresIn: Int? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("error_description")
    val errorDescription: String? = null
) : Parcelable {
    fun toAuthToken(): Result<AuthToken, ReachFiveError> {
        return if (accessToken != null) {
            if (idToken != null) {
                getUser().map {
                    AuthToken(
                        idToken = idToken,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        tokenType = tokenType,
                        expiresIn = expiresIn,
                        user = it
                    )
                }
            } else {
                Result.of {
                    AuthToken(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        tokenType = tokenType,
                        expiresIn = expiresIn
                    )
                }
            }
        } else {
            Result.error(
                ReachFiveError.from(
                    "No access_token returned"
                )
            )
        }
    }

    private fun getUser(): Result<OpenIdUser, ReachFiveError> {
        return Result.of {
            if (idToken != null) {
                Jwt.decode(idToken, OpenIdUser::class.java)
            } else {
                throw ReachFiveError.from(
                    "Invalid id_token"
                )
            }
        }
    }
}
