package com.reach5.identity.sdk.core.models

import com.google.gson.annotations.SerializedName

enum class PasswordlessAuthType{
    @SerializedName("sms")
    SMS,
    @SerializedName("magic_link")
    MAGIC_LINK
}
