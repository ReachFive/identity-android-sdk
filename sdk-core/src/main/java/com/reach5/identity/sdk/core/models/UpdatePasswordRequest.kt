package com.reach5.identity.sdk.core.models

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

sealed class UpdatePasswordRequest {
    data class FreshAccessTokenParams (
        val password: String
    ) : UpdatePasswordRequest()

    data class AccessTokenParams (
        @SerializedName("old_password")
        val oldPassword: String,
        val password: String
    ) : UpdatePasswordRequest()

    data class EmailParams (
        val email: String,
        val verificationCode: String,
        val password: String
    ) : UpdatePasswordRequest()

    data class EmailWithClientIdParams (
        val email: String,
        @SerializedName("verification_code")
        val verificationCode: String,
        val password: String,
        @SerializedName("client_id")
        val clientId: String
    ) : UpdatePasswordRequest()

    data class SmsParams (
        val phoneNumber: String,
        val verificationCode: String,
        val password: String
    ) : UpdatePasswordRequest()

    data class SmsWithClientIdParams (
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
                is EmailParams ->
                    EmailWithClientIdParams(params.email, params.verificationCode, params.password, clientId)
                is SmsParams ->
                    SmsWithClientIdParams(params.phoneNumber, params.verificationCode, params.password, clientId)
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
