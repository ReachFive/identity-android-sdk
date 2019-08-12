package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.utils.Jwt
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthToken(
    // If the `openid` scope is not provided, the `idToken` is not returned
    val idToken: String? = null,
    val accessToken: String,
    val tokenType: String?,
    val expiresIn: Int?,
    // The `user` field is optional because if the `openid` scope is not provided, the `user` is not retrieved
    val user: OpenIdUser? = null
) : Parcelable

@Parcelize
data class AuthTokenResponse(
    @SerializedName("id_token")
    val idToken: String? = null,

    @SerializedName("access_token")
    val accessToken: String? = null,

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
                        tokenType = tokenType,
                        expiresIn = expiresIn,
                        user = it
                    )
                }
            } else {
                Result.of {
                    AuthToken(
                        accessToken = accessToken,
                        tokenType = tokenType,
                        expiresIn = expiresIn
                    )
                }
            }
        } else {
            Result.error(ReachFiveError.from("No access_token returned"))
        }
    }

    private fun getUser(): Result<OpenIdUser, ReachFiveError> {
        return Result.of {
            if (idToken != null) {
                Jwt.decode(idToken, OpenIdUser::class.java)
            } else {
                throw ReachFiveError.from("Invalid id_token")
            }
        }
    }
}
