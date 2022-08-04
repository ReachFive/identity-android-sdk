package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_VERIFIER_KEY
import co.reachfive.identity.sdk.core.api.LoginCallbackHandler
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
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success

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

    @Deprecated("Contact ReachFive support if you are using this method.")
    fun exchangeCodeForToken(
        authorizationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )
}

class SessionUtilsClient(
    private val reachFiveApi: ReachFiveApi,
    private val sdkConfig: SdkConfig,
    private val webLauncher: RedirectionActivityLauncher,
) : SessionUtils {
    companion object {
        const val codeResponseType = "code"
    }

    private val loginCallbackHandler = LoginCallbackHandler.create(sdkConfig)

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
            if (authCode == null) failure(ReachFiveError.WebFlowCanceled)
            else if (codeVerifier == null) failure(ReachFiveError.NoPkce)
            else exchangeAuthorizationCode(
                authCode,
                sdkConfig.scheme,
                codeVerifier,
                success,
                failure,
                sdkMethod = "handleAuthorizationCompletion"
            )
        }
    }

    @Deprecated("Contact ReachFive support if you are using this method.")
    override fun exchangeCodeForToken(
        authorizationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        val authCodeFlow = PkceAuthCodeFlow.readAuthCodeFlow(activity)

        if (authCodeFlow != null) {
            exchangeAuthorizationCode(
                authCode = authorizationCode,
                redirectUri = authCodeFlow.redirectUri,
                codeVerifier = authCodeFlow.codeVerifier,
                success,
                failure,
                sdkMethod = "exchangeCodeForToken"
            )
        } else {
            failure(ReachFiveError.NoPkce)
        }
    }

    private fun exchangeAuthorizationCode(
        authCode: String,
        redirectUri: String,
        codeVerifier: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        sdkMethod: String,
    ) {
        val authCodeRequest = AuthCodeRequest(
            clientId = sdkConfig.clientId,
            code = authCode,
            redirectUri = redirectUri,
            codeVerifier = codeVerifier
        )

        val metadata = SdkInfos.getQueries(sdkMethod)

        reachFiveApi
            .authenticateWithCode(authCodeRequest, metadata)
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
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
    ) {
        val redirectUri = sdkConfig.scheme
        val pkce = PkceAuthCodeFlow.generate(redirectUri)

        loginCallbackHandler.getAuthorizationCode(
            tkn = tkn,
            pkce = pkce,
            clientId = sdkConfig.clientId,
            redirectUri = redirectUri,
            scope = scope,
            success = { authCode ->
                exchangeAuthorizationCode(authCode, pkce.codeVerifier, success, failure)
            },
            failure = failure
        )
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