package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.browser.customtabs.CustomTabsIntent
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.formatScope

class RedirectionActivityLauncher(
    val sdkConfig: SdkConfig,
    val api: ReachFiveApi,
) {

    fun loginWithWeb(
        activity: Activity,
        scope: Collection<String>,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
    ) {
        val intent = prepareIntent(activity, scope, origin = origin, state = state, nonce = nonce)
        activity.startActivityForResult(intent, RedirectionActivity.REDIRECTION_REQUEST_CODE)
    }

    fun sloFlow(
        activity: Activity,
        provider: Provider,
        scope: Collection<String>,
        origin: String,
    ) {
        val intent = prepareIntent(activity, scope, origin = origin, provider = provider.name)
        activity.startActivityForResult(intent, provider.requestCode)
    }

    fun loginCallback(
        activity: Activity,
        scope: Collection<String>,
        tkn: String,
    ) {
        val intent = prepareIntent(activity, scope, tkn)
        activity.startActivityForResult(intent, RedirectionActivity.REDIRECTION_REQUEST_CODE)
    }

    private fun prepareIntent(
        activity: Activity,
        scope: Collection<String>,
        tkn: String? = null,
        provider: String? = null,
        origin: String? = null,
        state: String? = null,
        nonce: String? = null,
    ): Intent {
        val intent = Intent(activity, RedirectionActivity::class.java)

        val redirectUri = sdkConfig.scheme
        val pkce = PkceAuthCodeFlow.generate(redirectUri)

        val maybeTkn = if (tkn != null) {
            mapOf("tkn" to tkn)
        } else emptyMap()
        val maybeProvider = if (provider != null) {
            mapOf("provider" to provider)
        } else emptyMap()
        val maybeOrigin = if (origin != null) {
            mapOf("origin" to origin)
        } else emptyMap()
        val maybeNonce = if (nonce != null) {
            mapOf("nonce" to nonce)
        } else emptyMap()
        val maybeState = if (state != null) {
            mapOf("state" to state)
        } else emptyMap()

        val request: Map<String, String> = mapOf(
            "client_id" to sdkConfig.clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to formatScope(scope),
            "code_challenge" to pkce.codeChallenge,
            "code_challenge_method" to pkce.codeChallengeMethod
        ) + SdkInfos.getQueries() + maybeTkn + maybeProvider + maybeOrigin + maybeNonce + maybeState

        val url = api.authorize(request).request().url.toString()
        intent.putExtra(RedirectionActivity.URL_KEY, url)
        intent.putExtra(RedirectionActivity.CODE_VERIFIER_KEY, pkce.codeVerifier)

        return intent
    }
}

class RedirectionActivity : Activity() {
    companion object {
        const val CODE_KEY = "CODE"
        const val CODE_VERIFIER_KEY = "CODE_VERIFIER"
        const val URL_KEY = "URL"

        const val REDIRECTION_REQUEST_CODE = 52558
        const val CHROME_CUSTOM_TAB_REQUEST_CODE = 100

        fun isRedirectionActivityRequestCode(code: Int): Boolean =
            code == REDIRECTION_REQUEST_CODE || code == CHROME_CUSTOM_TAB_REQUEST_CODE

        enum class Result(val code: Int) {
            Success(0),
            ReachFiveError(1),
            UnexpectedError(2);
        }
    }

    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlString = intent.getStringExtra(URL_KEY)
        val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)

        val customTabsIntent = CustomTabsIntent.Builder().build().intent
        customTabsIntent.data = Uri.parse(urlString)
        customTabsIntent.putExtra(CODE_VERIFIER_KEY, codeVerifier)

        startActivityForResult(customTabsIntent, CHROME_CUSTOM_TAB_REQUEST_CODE)
    }

    override fun onNewIntent(newIntent: Intent) {
        val uri = newIntent.data

        val authCode = uri?.getQueryParameter("code")
        val error = uri?.let { ReachFiveError.fromRedirectionResult(it) }

        val result =
            if (authCode != null) {
                intent.putExtra(CODE_KEY, authCode)
                Result.Success
            } else if (error != null) {
                intent.putExtra(ReachFiveError.INTENT_EXTRA_KEY, error as Parcelable)
                Result.ReachFiveError
            } else {
                Result.UnexpectedError
            }

        setResult(result.code, intent)
        finish()
    }

    override fun onResume() {
        super.onResume()

        if (!started) {
            started = true
        } else {
            finish()
        }
    }
}