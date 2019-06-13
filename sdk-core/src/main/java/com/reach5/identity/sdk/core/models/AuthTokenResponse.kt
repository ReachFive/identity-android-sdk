package com.reach5.identity.sdk.core.models

import android.os.Parcel
import android.os.Parcelable
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.utils.Jwt

data class AuthToken(
    val idToken: String,
    val accessToken: String,
    val code: String?,
    val tokenType: String?,
    val expiresIn: Int?,
    val user: User
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readParcelable(User::class.java.classLoader)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(idToken)
        parcel.writeString(accessToken)
        parcel.writeString(code)
        parcel.writeString(tokenType)
        parcel.writeValue(expiresIn)
        parcel.writeParcelable(user, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AuthToken> {
        override fun createFromParcel(parcel: Parcel): AuthToken {
            return AuthToken(parcel)
        }

        override fun newArray(size: Int): Array<AuthToken?> {
            return arrayOfNulls(size)
        }
    }
}

data class OpenIdTokenResponse(
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
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString()
    )

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
            return Result.error(ReachFiveError.from("No id_token returned, verify if you have the open_id scope configured into your API Client Settings"))
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

    companion object {

        @JvmStatic
        fun fromQueryString(params: Map<String, String>): OpenIdTokenResponse {
            return OpenIdTokenResponse(
                idToken = params["id_token"],
                accessToken = params["access_token"],
                expiresIn = params["expires_in"]?.toIntOrNull(),
                tokenType = params["token_type"],
                error = params["error"],
                errorDescription = params["error_description"],
                code = null
            )
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<OpenIdTokenResponse> {
            override fun createFromParcel(parcel: Parcel): OpenIdTokenResponse {
                return OpenIdTokenResponse(parcel)
            }

            override fun newArray(size: Int): Array<OpenIdTokenResponse?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(idToken)
        parcel.writeString(accessToken)
        parcel.writeString(code)
        parcel.writeString(tokenType)
        parcel.writeValue(expiresIn)
        parcel.writeString(error)
        parcel.writeString(errorDescription)
    }

    override fun describeContents(): Int {
        return 0
    }
}
