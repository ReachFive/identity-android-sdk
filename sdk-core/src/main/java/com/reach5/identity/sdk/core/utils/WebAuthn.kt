package com.reach5.identity.sdk.core.utils

import android.util.Base64

object WebAuthn {
    const val publicKeyCredentialType = "public-key"

    fun encodeToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
    }
}