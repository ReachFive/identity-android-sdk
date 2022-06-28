package co.reachfive.identity.sdk.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import co.reachfive.identity.sdk.core.Provider
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.formatScope

class ReachFiveLoginActivityLauncher(
    val sdkConfig: SdkConfig,
    val api: ReachFiveApi,
) {

    fun startProviderFlow(
        activity: Activity,
        provider: Provider,
        scope: Collection<String>,
        origin: String,
    ) {
        val intent = prepareIntent(activity, scope, provider = provider.name, origin = origin)
        activity.startActivityForResult(intent, provider.requestCode)
    }

    private fun prepareIntent(
        activity: Activity,
        scope: Collection<String>,
        provider: String,
        origin: String,
    ): Intent {
        val intent = Intent(activity, ReachFiveLoginActivity::class.java)

        val redirectUri = sdkConfig.scheme
        val pkce = PkceAuthCodeFlow.generate(redirectUri)

        val request: Map<String, String> = mapOf(
            "client_id" to sdkConfig.clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to formatScope(scope),
            "code_challenge" to pkce.codeChallenge,
            "code_challenge_method" to pkce.codeChallengeMethod,
            "provider" to provider,
            "origin" to origin
        ) + SdkInfos.getQueries()

        val url = api.authorize(request).request().url.toString()
        intent.putExtra(ReachFiveLoginActivity.URL_KEY, url)
        intent.putExtra(ReachFiveLoginActivity.CODE_VERIFIER_KEY, pkce.codeVerifier)

        return intent
    }
}

class ReachFiveLoginActivity : Activity() {
    companion object {
        const val CODE_KEY = "CODE"
        const val CODE_VERIFIER_KEY = "CODE_VERIFIER"
        const val URL_KEY = "URL"

        const val CHROME_CUSTOM_TAB_REQUEST_CODE = 100
    }

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
        val data = newIntent.data

        if (newIntent.action != Intent.ACTION_VIEW || data == null) {
            loginFailure("Unexpected error")
        } else {
            val authCode = data.getQueryParameter("code")
            loginSuccess(authCode)
        }
    }

    override fun onBackPressed() {
        loginFailure("User aborted login!")
    }

    private fun loginFailure(message: String) {
        val intent = Intent()
        intent.putExtra(ConfiguredWebViewProvider.RESULT_INTENT_ERROR, message)
        setResult(ConfiguredWebViewProvider.PROVIDER_REDIRECTION_REQUEST_CODE, intent)
        finish()
    }

    fun loginSuccess(authCode: String?) {
        intent.putExtra(ConfiguredWebViewProvider.AuthCode, authCode)
        setResult(ConfiguredWebViewProvider.PROVIDER_REDIRECTION_REQUEST_CODE, intent)
        finish()
    }
}