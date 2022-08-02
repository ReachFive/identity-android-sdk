package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_VERIFIER_KEY
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.AuthCodeRequest
import co.reachfive.identity.sdk.core.models.requests.LoginProviderRequest
import co.reachfive.identity.sdk.core.models.requests.RefreshRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.SuccessWithNoContent

internal interface SessionUtils {
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

    fun logout(
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    )
}

class SessionUtilsClient(
    private val reachFiveApi: ReachFiveApi,
    private val sdkConfig: SdkConfig,
    private val webLauncher: RedirectionActivityLauncher,
    private val socialLoginAuth: SocialLoginAuthClient,
) : SessionUtils {
    companion object {
        const val codeResponseType = "code"
    }

    override var defaultScope: Set<String> = emptySet()

    fun loginWithProvider(
        activity: Activity,
        provider: Provider,
        scope: Collection<String>,
        origin: String,
    ) = webLauncher.loginWithProvider(activity, provider, scope, origin)

    fun handleAuthorizationCompletion(
        intent: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val uri = intent?.data ?: Uri.EMPTY
        val error = uri.let { ReachFiveError.fromRedirectionResult(it) }

        val authCode = uri.getQueryParameter("code")
        val codeVerifier = intent?.getStringExtra(CODE_VERIFIER_KEY)

        if (error != null)
            failure(error)
        else {
            if (authCode == null) failure(ReachFiveError.UserCanceled)
            else if (codeVerifier == null) failure(ReachFiveError.from("Could not retrieve code verifier!"))
            else exchangeAuthorizationCode(authCode, codeVerifier, success, failure)
        }
    }

    private fun exchangeAuthorizationCode(
        authCode: String,
        codeVerifier: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
    ) {
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
    }

    fun loginWithProvider(
        provider: String,
        authCode: String? = null,
        providerAccessToken: String? = null,
        origin: String? = null,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val loginProviderRequest = LoginProviderRequest(
            provider = provider,
            clientId = sdkConfig.clientId,
            code = authCode,
            providerToken = providerAccessToken,
            origin = origin,
            scope = scope.joinToString(" ")
        )
        reachFiveApi
            .loginWithProvider(loginProviderRequest, SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback(
                    success = { it.toAuthToken().fold(success, failure) },
                    failure = failure
                )
            )
    }

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