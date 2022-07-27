package co.reachfive.identity.sdk.webview

import android.app.Activity
import android.content.Intent
import co.reachfive.identity.sdk.core.Provider
import co.reachfive.identity.sdk.core.ProviderCreator
import co.reachfive.identity.sdk.core.RedirectionActivity
import co.reachfive.identity.sdk.core.RedirectionActivityLauncher
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.*
import co.reachfive.identity.sdk.core.models.requests.AuthCodeRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.webview.WebViewProvider.Companion.REQUEST_CODE

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

    companion object {
        const val REQUEST_CODE = 52559
    }
}

internal class ConfiguredWebViewProvider(
    private val providerConfig: ProviderConfig,
    private val sdkConfig: SdkConfig,
    private val reachFiveApi: ReachFiveApi
) : Provider {

    private val redirectionActivityLauncher = RedirectionActivityLauncher(sdkConfig, reachFiveApi)

    override val name: String = providerConfig.provider
    override val requestCode: Int = REQUEST_CODE

    override fun login(
        origin: String, scope: Collection<String>,
        state: String?,
        nonce: String?,
        activity: Activity
    ) {
        redirectionActivityLauncher.sloFlow(activity, this, scope, origin, state, nonce)
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
