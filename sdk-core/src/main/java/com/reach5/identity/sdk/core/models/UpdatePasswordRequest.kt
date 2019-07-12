package com.reach5.identity.sdk.core.models

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

sealed class UpdatePasswordRequest {
    data class FreshAccessTokenUpdatePasswordParams (
        val password: String
    ) : UpdatePasswordRequest()

    data class AccessTokenUpdatePasswordParams (
        @SerializedName("old_password")
        val oldPassword: String,
        val password: String
    ) : UpdatePasswordRequest()

    data class EmailUpdatePasswordParams (
        val email: String,
        val verificationCode: String,
        val password: String
    ) : UpdatePasswordRequest()

    data class EmailUpdatePasswordParamsWithClientId (
        val email: String,
        @SerializedName("verification_code")
        val verificationCode: String,
        val password: String,
        @SerializedName("client_id")
        val clientId: String
    ) : UpdatePasswordRequest()

    data class SmsUpdatePasswordParams (
        val phoneNumber: String,
        val verificationCode: String,
        val password: String
    ) : UpdatePasswordRequest()

    data class SmsUpdatePasswordParamsWithClientId (
        @SerializedName("phone_number")
        val phoneNumber: String,
        @SerializedName("verification_code")
        val verificationCode: String,
        val password: String,
        @SerializedName("client_id")
        val clientId: String
    ) : UpdatePasswordRequest()

    companion object {
        fun<T : UpdatePasswordRequest> enrichWithClientId(params: T, clientId: String): UpdatePasswordRequest  {
            return when(params) {
                is EmailUpdatePasswordParams ->
                    EmailUpdatePasswordParamsWithClientId(params.email, params.verificationCode, params.password, clientId)
                is SmsUpdatePasswordParams ->
                    SmsUpdatePasswordParamsWithClientId(params.phoneNumber, params.verificationCode, params.password, clientId)
                else -> params
            }
        }
    }
}

internal class UpdatePasswordRequestSerializer: JsonSerializer<UpdatePasswordRequest> {
    override fun serialize(src: UpdatePasswordRequest?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return Gson().toJsonTree(src)
    }
}
