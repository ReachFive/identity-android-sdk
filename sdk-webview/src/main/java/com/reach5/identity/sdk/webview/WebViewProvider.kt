package com.reach5.identity.sdk.webview

import android.app.Activity
import android.content.Intent
import com.reach5.identity.sdk.core.Provider
import com.reach5.identity.sdk.core.ProviderCreator
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.api.ReachFiveApiCallback
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.models.requests.AuthCodeRequest
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Pkce
import com.reach5.identity.sdk.core.utils.Success

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
    override val requestCode: Int = RequestCode

    companion object {
        const val BUNDLE_ID = "BUNDLE_REACH_FIVE"
        const val AuthCode = "AuthCode"
        const val PKCE = "PKCE"
        const val RequestCode = 52558
        const val RESULT_INTENT_ERROR = "RESULT_INTENT_ERROR"
    }

    override fun login(origin: String, activity: Activity) {
        val intent = Intent(activity, ReachFiveLoginActivity::class.java)
        intent.putExtra(
            BUNDLE_ID, WebViewProviderConfig(
                providerConfig = providerConfig,
                sdkConfig = sdkConfig,
                origin = origin
            )
        )
        intent.putExtra(PKCE, Pkce.generate())
        activity.startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val authCode = data?.getStringExtra(AuthCode)
        val pkce = data?.getParcelableExtra<Pkce>(PKCE)
        return if (authCode != null && pkce != null) {
            val authCodeRequest = AuthCodeRequest(
                sdkConfig.clientId,
                authCode,
                sdkConfig.scheme,
                pkce.codeVerifier
            )
            reachFiveApi
                .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                .enqueue(ReachFiveApiCallback(success = { it.toAuthToken().fold(success, failure) }, failure = failure))
        } else {
            failure(ReachFiveError.from("No authorization code or PKCE verifier code found in activity result"))
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
