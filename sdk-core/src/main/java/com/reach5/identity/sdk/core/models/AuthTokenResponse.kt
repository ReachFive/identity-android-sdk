package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.utils.Jwt
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthToken(
    val idToken: String,
    val accessToken: String,
    val code: String?,
    val tokenType: String?,
    val expiresIn: Int?,
    // The `user` field is optional because if the `openid` scope is not provided, the `idToken` is not returned
    val user: User?
) : Parcelable

@Parcelize
data class AuthTokenResponse(
    @SerializedName("id_token")
    val idToken: String? = null,

    @SerializedName("access_token")
    val accessToken: String? = null,

    @SerializedName("code")
    val code: String? = null,

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
        if (idToken != null) {
            return if (accessToken != null) {
                getUser().map {
                    AuthToken(
                        idToken = idToken,
                        accessToken = accessToken,
                        code = code,
                        tokenType = tokenType,
                        expiresIn = expiresIn,
                        user = it
                    )
                }
            } else {
                Result.error(ReachFiveError.from("No access_token returned"))
            }
        } else {
            return Result.error(ReachFiveError.from("No id_token returned, verify that you have the `openid` scope configured in your API Client Settings."))
        }
    }

    private fun getUser(): Result<User, ReachFiveError> {
        return Result.of {
            if (idToken != null) {
                Jwt.decode(idToken, User::class.java)
            } else {
                throw ReachFiveError.from("Invalid id_token")
            }
        }
    }
}
