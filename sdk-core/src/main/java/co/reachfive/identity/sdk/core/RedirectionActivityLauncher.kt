package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.formatScope


class RedirectionActivityLauncher(
    val sdkConfig: SdkConfig,
    val api: ReachFiveApi,
) {

    fun logoutWithWeb(activity: Activity) {
        val intent = Intent(activity, RedirectionActivity::class.java)
        val redirectUri = sdkConfig.scheme
        val request: Map<String, String> =
            mapOf("redirect_to" to redirectUri) + SdkInfos.getQueries()

        val url = api.logout(request).request().url.toString()

        intent.putExtra(RedirectionActivity.URL_KEY, url)
        intent.putExtra(RedirectionActivity.SCHEME, sdkConfig.scheme)
        activity.startActivityForResult(intent, RedirectionActivity.RC_WEBLOGOUT)

    }

    /**
     * Orchestrated login with a Custom Tab. The client must configure a Login URL and enable orchestration tokens
     * in order to delegate auth to a Web identity client.
     */
    fun loginWithWeb(
        activity: Activity,
        scope: Collection<String>,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
    ) {
        val intent = prepareIntent(activity, scope, origin = origin, state = state, nonce = nonce)
        activity.startActivityForResult(intent, RedirectionActivity.RC_WEBLOGIN)
    }

    /**
     * Orchestrated login with a native WebView. The client must configure a Login URL and enable orchestration tokens
     * in order to delegate auth to a Web identity client.
     */
    fun loginWithWebView(
        activity: Activity,
        scope: Collection<String>,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
    ) {
        val intent = prepareIntent(activity, scope, origin = origin, state = state, nonce = nonce)
        intent.putExtra(RedirectionActivity.USE_NATIVE_WEBVIEW, true)
        activity.startActivityForResult(intent, RedirectionActivity.RC_WEBLOGIN)
    }

    /**
     * Custom Tab provider login
     */
    fun loginWithProvider(
        activity: Activity,
        provider: Provider,
        scope: Collection<String>,
        origin: String,
    ) {
        val intent = prepareIntent(activity, scope, origin = origin, provider = provider.name)
        activity.startActivityForResult(intent, provider.requestCode)
    }

    private fun prepareIntent(
        activity: Activity,
        scope: Collection<String>,
        provider: String? = null,
        origin: String? = null,
        state: String? = null,
        nonce: String? = null,
    ): Intent {
        val intent = Intent(activity, RedirectionActivity::class.java)

        val redirectUri = sdkConfig.scheme
        val pkce = PkceAuthCodeFlow.generate(redirectUri)

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
        ) + SdkInfos.getQueries() + maybeProvider + maybeOrigin + maybeNonce + maybeState

        val url = api.authorize(request).request().url.toString()
        intent.putExtra(RedirectionActivity.URL_KEY, url)
        intent.putExtra(RedirectionActivity.CODE_VERIFIER_KEY, pkce.codeVerifier)
        intent.putExtra(RedirectionActivity.SCHEME, sdkConfig.scheme)
        sdkConfig.originWebAuthn?.let {
            intent.putExtra(RedirectionActivity.ORIGIN_WEBAUTHN, it)
        }

        return intent
    }
}
