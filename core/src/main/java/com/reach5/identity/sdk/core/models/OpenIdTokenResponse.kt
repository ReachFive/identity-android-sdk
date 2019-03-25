package com.reach5.identity.sdk.core.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.utils.Jwt

data class OpenIdTokenResponse(
    @SerializedName("id_token")
    val idToken: String? = null,

    @SerializedName("access_token")
    val accessToken: String? = null,

    // TODO enum ?
    @SerializedName("code")
    val code: String? = null,

    @SerializedName("token_type")
    val tokenType: String? = null,

    @SerializedName("expires_in")
    val expiresIn: Int? = null,

    // TODO create new type for this error
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

    fun getUser(): User  {
        return if (idToken != null) {
            Jwt.decode(idToken, User::class.java)
        } else {
            throw ReachFiveError.from("Invalid idToken")
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
