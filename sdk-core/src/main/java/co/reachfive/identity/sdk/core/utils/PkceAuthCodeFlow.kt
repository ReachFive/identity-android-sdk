package co.reachfive.identity.sdk.core.utils

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import android.util.Base64
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.text.Charsets.UTF_8

@Parcelize
class PkceAuthCodeFlow(val codeVerifier: String, val redirectUri: String) : Parcelable {
    @IgnoredOnParcel
    val codeChallenge: String

    @IgnoredOnParcel
    val codeChallengeMethod: String

    init {
        this.codeChallenge = generateCodeChallenge(codeVerifier)
        this.codeChallengeMethod = "S256"
    }

    override fun toString(): String = ""

    companion object {
        private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

        fun generate(redirectUri: String): PkceAuthCodeFlow = PkceAuthCodeFlow(generateCodeVerifier(), redirectUri)

        private fun generateCodeVerifier(): String =
            SecureRandom()
                .let { secureRandom ->
                    ByteArray(32).also { secureRandom.nextBytes(it) }
                }
                .let { code ->
                    Base64.encodeToString(code, BASE64_FLAGS)
                }

        fun storeAuthCodeFlow(pkceAuthCodeFlow: PkceAuthCodeFlow, activity: Activity): Unit =
            activity
                .getSharedPreferences("pkce", Context.MODE_PRIVATE)
                .edit()
                .run {
                    putString("code_verifier", pkceAuthCodeFlow.codeVerifier)
                    putString("redirect_uri", pkceAuthCodeFlow.redirectUri)
                    apply()
                }

        fun readAuthCodeFlow(activity: Activity): PkceAuthCodeFlow? {
            val verifier =
                activity
                    .getSharedPreferences("pkce", Context.MODE_PRIVATE)
                    .getString("code_verifier", "")

            val redirectUri =
                activity
                    .getSharedPreferences("pkce", Context.MODE_PRIVATE)
                    .getString("redirect_uri", "")

            return if (verifier != null && redirectUri != null) {
                PkceAuthCodeFlow(verifier, redirectUri)
            } else null
        }
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
