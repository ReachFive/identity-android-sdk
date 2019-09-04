package com.reach5.identity.sdk.core.models.requests

import com.reach5.identity.sdk.core.models.PasswordlessAuthType

data class PasswordlessVerificationRequest(
    val clientId: String,
    val phoneNumber: String,
    val authType: PasswordlessAuthType = PasswordlessAuthType.SMS,
    val verificationCode: String,
    val codeVerifier: String,
    val responseType: String,
    val redirectUri: String
) {
    fun getQueries(): Map<String, String> {
        return mapOf(
            "client_id" to clientId,
            "phone_number" to phoneNumber,
            "verification_code" to verificationCode,
            "auth_type" to "sms",
            "code_verifier" to codeVerifier,
            "response_type" to responseType,
            "redirect_uri" to redirectUri
        )
    }
}