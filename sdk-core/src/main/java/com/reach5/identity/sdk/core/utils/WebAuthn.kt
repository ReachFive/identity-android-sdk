package com.reach5.identity.sdk.core.utils

import android.util.Base64

object WebAuthn {
    const val publicKeyCredentialType = "public-key"

    private const val base64Flags = Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE

    fun encodeToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, base64Flags)
    }

    fun decodeBase64(value: String): ByteArray {
        return Base64.decode(value.toByteArray(), base64Flags)
    }
}