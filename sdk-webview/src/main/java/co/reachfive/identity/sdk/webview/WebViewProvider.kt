package co.reachfive.identity.sdk.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import co.reachfive.identity.sdk.core.*
import co.reachfive.identity.sdk.core.models.*
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.webview.WebViewProvider.Companion.REQUEST_CODE

class WebViewProvider : ProviderCreator {
    override val name: String = "webview"

    override fun create(
        providerConfig: ProviderConfig,
        socialLoginClient: SocialLoginAuthClient,
        context: Context
    ): Provider {
        return ConfiguredWebViewProvider(providerConfig, socialLoginClient)
    }

    companion object {
        const val REQUEST_CODE = 52559
    }
}

internal class ConfiguredWebViewProvider(
    private val providerConfig: ProviderConfig,
    private val socialLoginClient: SocialLoginAuthClient
) : Provider {
    override val name: String = providerConfig.provider
    override val requestCode: Int = REQUEST_CODE

    override fun login(origin: String, scope: Collection<String>, activity: Activity) {
        socialLoginClient.webProviderAuth(activity, this, scope, origin)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {}

    override fun toString(): String =
        providerConfig.provider

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>,
        activity: Activity,
    ) {
        // Do nothing
    }
}
