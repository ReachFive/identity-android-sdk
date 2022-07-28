package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.RefreshRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.R5CustomTabsServiceConnection
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.SuccessWithNoContent

internal interface SessionUtils {
    var defaultScope: Set<String>

    // TODO move to ReachFive as used also for login callback + SLO
//    fun webLoginWarmup(context: Context)

    fun refreshAccessToken(
        authToken: AuthToken,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    )

    fun refreshOrLoginWithWeb(
        scope: Collection<String> = defaultScope,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
        activity: Activity,
    )

    fun logout(
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    )
}

internal class SessionUtilsClient(
    private val reachFiveApi: ReachFiveApi,
    private val sdkConfig: SdkConfig,
    private val webLauncher: RedirectionActivityLauncher,
    private val socialLoginAuth: SocialLoginAuthClient,
) : SessionUtils {
    private val customTabsServiceConnection: R5CustomTabsServiceConnection =
        R5CustomTabsServiceConnection()

    companion object {
        private const val pkgName = "com.android.chrome"
        const val codeResponseType = "code"
    }

    override fun webLoginWarmup(context: Context) {
        val ctSvcBind = CustomTabsClient.bindCustomTabsService(context, pkgName, customTabsServiceConnection)
        val warmup = customTabsServiceConnection.warmup()
        val success = ctSvcBind && warmup

        if (success)
            Log.d(TAG, "Successfully warmup browser for session refresh/login.")
        else
            Log.d(TAG, "Failed to warmup browser: service binding (${ctSvcBind}) ; warmup (${warmup})")
    }

    override var defaultScope: Set<String> = emptySet()

    override fun logout(
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        socialLoginAuth.logoutFromAll()
        reachFiveApi
            .logout(SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
    }

    override fun refreshAccessToken(
        authToken: AuthToken,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val refreshRequest = RefreshRequest(
            clientId = sdkConfig.clientId,
            refreshToken = authToken.refreshToken ?: "",
            redirectUri = sdkConfig.scheme
        )

        reachFiveApi
            .refreshAccessToken(refreshRequest, SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback(
                    success = { it.toAuthToken().fold(success, failure) },
                    failure = failure
                )
            )
    }

    internal fun loginCallback(
        tkn: String,
        scope: Collection<String>,
        activity: Activity
    ) {
        webLauncher.loginCallback(activity, scope, tkn)
    }

    override fun refreshOrLoginWithWeb(
        scope: Collection<String>,
        state: String?,
        nonce: String?,
        origin: String?,
        activity: Activity,
    ) {
        webLauncher.loginWithWeb(activity, scope, state, nonce, origin)
    }
}