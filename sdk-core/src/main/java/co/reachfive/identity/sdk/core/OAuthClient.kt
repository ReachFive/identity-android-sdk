package co.reachfive.identity.sdk.core

import android.app.Activity
import android.util.Log
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.AuthCodeRequest
import co.reachfive.identity.sdk.core.models.requests.RefreshRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success

internal interface ReachFiveOAuth {
    var defaultScope: Set<String>

    fun refreshAccessToken(
        authToken: AuthToken,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    )

    fun loginWithWeb(
        scope: Collection<String> = defaultScope,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
        activity: Activity,
    )
}

internal class ReachFiveOAuthClient(
    private val reachFiveApi: ReachFiveApi,
    private val sdkConfig: SdkConfig,
    private val webLauncher: RedirectionActivityLauncher,
    override var defaultScope: Set<String> = emptySet(),
) : ReachFiveOAuth {
    companion object {
        const val codeResponseType = "code"
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

    override fun loginWithWeb(
        scope: Collection<String>,
        state: String?,
        nonce: String?,
        origin: String?,
        activity: Activity,
    ) {
        webLauncher.loginWithWeb(activity, scope, state, nonce, origin)
    }
}