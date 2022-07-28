package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.AuthCodeRequest
import co.reachfive.identity.sdk.core.models.requests.RefreshRequest
import co.reachfive.identity.sdk.core.utils.*

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
}

class SessionUtilsClient(
    private val reachFiveApi: ReachFiveApi,
    private val sdkConfig: SdkConfig,
) : SessionUtils {
    companion object {
        const val REDIRECTION_REQUEST_CODE = 52558
        const val codeResponseType = "code"
        private const val CODE_VERIFIER_KEY = "code_verifier"

        fun isRedirectionRequestCode(code: Int): Boolean =
            code == REDIRECTION_REQUEST_CODE
    }

    override var defaultScope: Set<String> = emptySet()

    internal fun handleAuthorizationCompletion(
        intent: Intent,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val uri = intent.data ?: Uri.EMPTY
        val error = uri.let { ReachFiveError.fromRedirectionResult(it) }

        val authCode = uri.getQueryParameter("code")
        val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)

        if (error != null)
            failure(error)
        else {
            if (authCode == null) failure(ReachFiveError.Unexpected)
            else if (codeVerifier == null) failure(ReachFiveError.Unexpected)
            else {
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
        }
    }

    override fun loginWithWeb(
        scope: Collection<String>,
        state: String?,
        nonce: String?,
        origin: String?,
        activity: Activity,
    ) {
        val intent = prepareCustomTabIntent(scope, origin = origin, state = state, nonce = nonce)
        activity.startActivityForResult(intent, REDIRECTION_REQUEST_CODE)
    }

    internal fun webSocialLogin(
        activity: Activity,
        provider: Provider,
        scope: Collection<String>,
        origin: String?,
    ) {
        val intent = prepareCustomTabIntent(scope, origin = origin, provider = provider.name)
        activity.startActivityForResult(intent, REDIRECTION_REQUEST_CODE)
    }

    fun loginCallback(
        activity: Activity,
        scope: Collection<String>,
        tkn: String,
    ) {
        val intent = prepareCustomTabIntent(scope, tkn)
        activity.startActivityForResult(intent, REDIRECTION_REQUEST_CODE)
    }

    private fun prepareCustomTabIntent(
        scope: Collection<String>,
        tkn: String? = null,
        provider: String? = null,
        origin: String? = null,
        state: String? = null,
        nonce: String? = null,
    ): Intent {
        val redirectUri = sdkConfig.scheme
        val pkce = PkceAuthCodeFlow.generate(redirectUri)

        val maybeTkn = if (tkn != null) {
            mapOf("tkn" to tkn)
        } else emptyMap()
        val maybeProvider = if (provider != null) {
            mapOf("provider" to provider)
        } else emptyMap()
        val maybeOrigin = if (origin != null) {
            mapOf("origin" to origin)
        } else emptyMap()
        val maybeNonce = if (nonce != null) {
            mapOf("nonce" to nonce)
        } else emptyMap()
        val maybeState = if (state != null) {
            mapOf("state" to state)
        } else emptyMap()

        val request: Map<String, String> = mapOf(
            "client_id" to sdkConfig.clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to formatScope(scope),
            "code_challenge" to pkce.codeChallenge,
            "code_challenge_method" to pkce.codeChallengeMethod
        ) + SdkInfos.getQueries() + maybeTkn + maybeProvider + maybeOrigin + maybeNonce + maybeState

        val url = reachFiveApi.authorize(request).request().url.toString()

        val customTabsIntent = CustomTabsIntent.Builder().build().intent
        customTabsIntent.data = Uri.parse(url)
        customTabsIntent.putExtra(CODE_VERIFIER_KEY, pkce.codeVerifier)

        return customTabsIntent
    }

    internal fun logout(
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
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
        loginCallback(activity, scope, tkn)
    }
}