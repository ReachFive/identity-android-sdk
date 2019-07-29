package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.lang.reflect.Type

sealed class UpdatePasswordRequest {
    @Parcelize
    data class FreshAccessTokenParams(
        val password: String
    ) : UpdatePasswordRequest(), Parcelable

    @Parcelize
    data class AccessTokenParams(
        @SerializedName("old_password")
        val oldPassword: String,
        val password: String
    ) : UpdatePasswordRequest(), Parcelable

    @Parcelize
    data class EmailParams(
        val email: String,
        val verificationCode: String,
        val password: String
    ) : UpdatePasswordRequest(), Parcelable

    @Parcelize
    data class EmailWithClientIdParams(
        val email: String,
        @SerializedName("verification_code")
        val verificationCode: String,
        val password: String,
        @SerializedName("client_id")
        val clientId: String
    ) : UpdatePasswordRequest(), Parcelable

    @Parcelize
    data class SmsParams(
        val phoneNumber: String,
        val verificationCode: String,
        val password: String
    ) : UpdatePasswordRequest(), Parcelable

    @Parcelize
    data class SmsWithClientIdParams(
        @SerializedName("phone_number")
        val phoneNumber: String,
        @SerializedName("verification_code")
        val verificationCode: String,
        val password: String,
        @SerializedName("client_id")
        val clientId: String
    ) : UpdatePasswordRequest(), Parcelable

    companion object {
        fun <T : UpdatePasswordRequest> enrichWithClientId(params: T, clientId: String): UpdatePasswordRequest {
            return when (params) {
                is EmailParams ->
                    EmailWithClientIdParams(
                        params.email,
                        params.verificationCode,
                        params.password,
                        clientId
                    )
                is SmsParams ->
                    SmsWithClientIdParams(
                        params.phoneNumber,
                        params.verificationCode,
                        params.password,
                        clientId
                    )
                else -> params
            }
        }
    }
}

internal class UpdatePasswordRequestSerializer : JsonSerializer<UpdatePasswordRequest> {
    override fun serialize(
        src: UpdatePasswordRequest?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return Gson().toJsonTree(src)
    }
}