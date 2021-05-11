package com.reach5.identity.sdk.core.utils

import android.app.Activity
import android.content.Context
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

    override fun toString(): String =
        "code_verifier=$codeVerifier, code_challenge=$codeChallenge, code_challenge_method=$codeChallengeMethod"

    companion object {
        private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

        fun generate(): Pkce = Pkce(generateCodeVerifier())

        private fun generateCodeVerifier(): String =
            SecureRandom()
                .let { secureRandom ->
                    ByteArray(32).also { secureRandom.nextBytes(it) }
                }
                .let { code ->
                    Base64.encodeToString(code, BASE64_FLAGS)
                }

        fun storeCodeVerifier(pkce: Pkce, activity: Activity): Unit =
            activity
                .getSharedPreferences("pkce", Context.MODE_PRIVATE)
                .edit()
                .run {
                    putString("code_verifier", pkce.codeVerifier)
                    apply()
                }

        fun readCodeVerifier(activity: Activity): String? =
            activity
                .getSharedPreferences("pkce", Context.MODE_PRIVATE)
                .getString("code_verifier", null)
    }

    private fun generateCodeChallenge(codeVerifier: String): String =
        codeVerifier
            .toByteArray(UTF_8)
            .let { bytes ->
                MessageDigest.getInstance("SHA-256")
                    .also { it.update(bytes, 0, bytes.size) }
                    .digest()
            }
            .let { digest ->
                Base64.encodeToString(digest, BASE64_FLAGS)
            }
}
