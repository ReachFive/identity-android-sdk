package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.formatScope

class RedirectionActivityLauncher(
    val sdkConfig: SdkConfig,
    val api: ReachFiveApi,
) {

    fun webLogout(activity: Activity) {
        val intent = Intent(activity, RedirectionActivity::class.java)

        val query = mapOf(
            "redirect_to" to sdkConfig.scheme,
        ) + SdkInfos.getQueries()

        val url = api.logout(query).request().url.toString()
        intent.putExtra(RedirectionActivity.URL_KEY, url)

        activity.startActivityForResult(intent, RedirectionActivity.RC_WEBLOGOUT)
    }

    /**
     * Orchestrated login. The client must configure a Login URL and enable orchestration tokens
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

    /**
     * Internal login callback to exchange authentication token (`tkn`)
     */
    fun loginCallback(
        activity: Activity,
        scope: Collection<String>,
        tkn: String,
    ) {
        val intent = prepareIntent(activity, scope, tkn)
        activity.startActivityForResult(intent, RedirectionActivity.RC_LOGINCALLBACK)
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