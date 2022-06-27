package co.reachfive.identity.sdk.webview

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.Provider
import co.reachfive.identity.sdk.core.ProviderCreator
import co.reachfive.identity.sdk.core.RedirectionActivity
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.*
import co.reachfive.identity.sdk.core.models.requests.AuthCodeRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success

class WebViewProvider : ProviderCreator {
    override val name: String = "webview"

    override fun create(
        providerConfig: ProviderConfig,
        sdkConfig: SdkConfig,
        reachFiveApi: ReachFiveApi,
        activity: Activity
    ): Provider {
        return ConfiguredWebViewProvider(providerConfig, sdkConfig, reachFiveApi)
    }
}

class ConfiguredWebViewProvider(
    private val providerConfig: ProviderConfig,
    private val sdkConfig: SdkConfig,
    private val reachFiveApi: ReachFiveApi
) : Provider {

    override val name: String = providerConfig.provider
    override val requestCode: Int = PROVIDER_REDIRECTION_REQUEST_CODE

    companion object {
        const val AuthCode = "AuthCode"
        const val PROVIDER_REDIRECTION_REQUEST_CODE = 52559
    }

    override fun login(origin: String, scope: Collection<String>, activity: Activity) {
        val intent = Intent(activity, RedirectionActivity::class.java)

        val pkce = PkceAuthCodeFlow.generate(sdkConfig.scheme)
        val webviewProvider =
            WebViewProviderConfig(
                providerConfig = providerConfig,
                sdkConfig = sdkConfig,
                origin = origin,
                scope = scope.joinToString(" ")
            )

        val url = webviewProvider.buildUrl(pkce)
        intent.putExtra(RedirectionActivity.URL_KEY, url)
        intent.putExtra(RedirectionActivity.CODE_VERIFIER_KEY, pkce.codeVerifier)
        Log.d("WebViewProvider", "URL: ${url}")

        activity.startActivityForResult(intent, PROVIDER_REDIRECTION_REQUEST_CODE)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        if (data == null) {
            failure(ReachFiveError.from("WebViewProvider: Data"))
        } else {
            val authCode = data.getStringExtra(RedirectionActivity.CODE_KEY)
            val codeVerifier = data.getStringExtra(RedirectionActivity.CODE_VERIFIER_KEY)

            return if (authCode != null && codeVerifier != null) {
                val authCodeRequest = AuthCodeRequest(
                    clientId = sdkConfig.clientId,
                    code = authCode,
                    redirectUri = sdkConfig.scheme,
                    codeVerifier = codeVerifier
                )
                reachFiveApi
                    .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                    .enqueue(
                        ReachFiveApiCallback(
                            success = { it.toAuthToken().fold(success, failure) },
                            failure = failure
                        )
                    )
            } else {
                failure(ReachFiveError.from("No authorization code or PKCE verifier code found in activity result"))
            }
        }
    }

    override fun toString(): String {
        return providerConfig.provider
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>
    ) {
        // Do nothing
    }
}
