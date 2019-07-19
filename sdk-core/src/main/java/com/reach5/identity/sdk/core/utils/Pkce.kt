package com.reach5.identity.sdk.core.utils

import android.os.Parcelable
import android.util.Base64
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.text.Charsets.UTF_8

@Parcelize
class Pkce(val codeVerifier: String) : Parcelable {
    @IgnoredOnParcel
    val codeChallenge: String
    @IgnoredOnParcel
    val codeChallengeMethod: String

    init {
        this.codeChallenge = generateCodeChallenge(codeVerifier)
        this.codeChallengeMethod = "S256"
    }

    override fun toString(): String {
        return "code_verifier=$codeVerifier, code_challenge=$codeChallenge, code_challenge_method=$codeChallengeMethod"
    }

    companion object {
        fun generate(): Pkce {
            val codeVerifier= generateCodeVerifier()
            return Pkce(codeVerifier)
        }

        private fun generateCodeVerifier(): String {
            val secureRandom = SecureRandom()

            val code = ByteArray(32)
            secureRandom.nextBytes(code)

            return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(UTF_8)

        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()

        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}