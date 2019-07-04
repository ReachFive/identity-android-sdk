package com.reach5.identity.sdk.core.models

import com.google.gson.annotations.SerializedName

data class VerifyPhoneNumberRequest (
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("verification_code")
    val verificationCode: String
)