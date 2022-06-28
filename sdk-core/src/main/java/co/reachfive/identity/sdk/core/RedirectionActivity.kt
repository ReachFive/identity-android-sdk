package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.formatScope

class RedirectionActivityLauncher(
    val sdkConfig: SdkConfig,
    val api: ReachFiveApi,
) {

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
        tkn: String,
    ): Intent {
        val intent = Intent(activity, RedirectionActivity::class.java)

        val redirectUri = sdkConfig.scheme
        val pkce = PkceAuthCodeFlow.generate(redirectUri)

        val request: Map<String, String> = mapOf(
            "client_id" to sdkConfig.clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to formatScope(scope),
            "code_challenge" to pkce.codeChallenge,
            "code_challenge_method" to pkce.codeChallengeMethod,
            "tkn" to tkn
        ) + SdkInfos.getQueries()

        val url = api.authorize(request).request().url.toString()
        intent.putExtra(RedirectionActivity.URL_KEY, url)
        intent.putExtra(RedirectionActivity.CODE_VERIFIER_KEY, pkce.codeVerifier)

        return intent
    }
}

class RedirectionActivity: Activity() {
    companion object {
        const val CODE_KEY = "CODE"
        const val CODE_VERIFIER_KEY = "CODE_VERIFIER"
        const val URL_KEY = "URL"

        const val REDIRECTION_REQUEST_CODE = 52558
        const val CHROME_CUSTOM_TAB_REQUEST_CODE = 100

        const val SUCCESS_RESULT_CODE = 0
        const val UNEXPECTED_ERROR_RESULT_CODE = -1
        const val ABORT_RESULT_CODE = 1
        const val NO_AUTH_ERROR_RESULT_CODE = 2
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

        val newResultCode = if (newIntent.action != Intent.ACTION_VIEW || data == null) {
            UNEXPECTED_ERROR_RESULT_CODE
        } else {
            val authCode = data.getQueryParameter("code")

            if (authCode == null) NO_AUTH_ERROR_RESULT_CODE
            else {
                intent.putExtra(CODE_KEY, authCode)
                SUCCESS_RESULT_CODE
            }
        }

        setResult(newResultCode, intent)
        finish()
    }
}