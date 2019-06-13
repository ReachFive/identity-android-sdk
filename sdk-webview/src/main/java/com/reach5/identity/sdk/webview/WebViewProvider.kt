package com.reach5.identity.sdk.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.Provider
import com.reach5.identity.sdk.core.ProviderCreator
import com.reach5.identity.sdk.core.utils.Success
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.models.OpenIdTokenResponse
import com.reach5.identity.sdk.core.models.ProviderConfig
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig

class WebViewProvider : ProviderCreator {
    override val name: String = "webview"

    override fun create(
        providerConfig: ProviderConfig,
        sdkConfig: SdkConfig,
        reachFiveApi: ReachFiveApi,
        context: Context
    ): Provider {
        return ConfiguredWebViewProvider(providerConfig, sdkConfig)
    }
}

class ConfiguredWebViewProvider(
    private val providerConfig: ProviderConfig,
    private val sdkConfig: SdkConfig
) : Provider {

    override val name: String = providerConfig.provider
    override val requestCode: Int = RequestCode

    companion object {
        const val BUNDLE_ID = "BUNDLE_REACH_FIVE"
        const val OpenIdTokenResponse = "OpenIdTokenResponse"
        const val RequestCode = 52558689
        const val RESULT_INTENT_ERROR = "RESULT_INTENT_ERROR"
    }

    override fun login(origin: String, activity: Activity) {
        val intent = Intent(activity, ReachFiveLoginActivity::class.java)
        intent.putExtra(BUNDLE_ID, WebViewProviderConfig(
            providerConfig = providerConfig,
            sdkConfig = sdkConfig,
            origin = origin
        ))
        activity.startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<OpenIdTokenResponse>,
        failure: Failure<ReachFiveError>
    ) {
        val openIdTokenResponse = data?.getParcelableExtra<OpenIdTokenResponse>(OpenIdTokenResponse)
        return if (openIdTokenResponse != null) {
            success(openIdTokenResponse)
        } else {
            failure(ReachFiveError.from("No token into activity result"))
        }
    }

    override fun toString(): String {
        return providerConfig.provider
    }
}
