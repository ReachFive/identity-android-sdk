package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.ABORT_RESULT_CODE
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_KEY
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_VERIFIER_KEY
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.NO_AUTH_ERROR_RESULT_CODE
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.AuthCodeRequest
import co.reachfive.identity.sdk.core.models.requests.RefreshRequest
import co.reachfive.identity.sdk.core.models.responses.ClientConfigResponse
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.SuccessWithNoContent

class ReachFive(
    override val activity: Activity,
    override val sdkConfig: SdkConfig,
    val providersCreators: List<ProviderCreator>
) : PasswordClient, PasswordlessClient, ProfileClient, SocialLoginClient {
    companion object {
        const val TAG = "Reach5"
        const val codeResponseType = "code"
    }

    override val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)
    private val redirectionActivityLauncher = RedirectionActivityLauncher(sdkConfig, reachFiveApi)

    override var defaultScope: Set<String> = emptySet()

    private val socialLoginManager: SocialLoginManager = SocialLoginManager(
        reachFiveApi,
        activity,
        defaultScope,
        providersCreators,
        sdkConfig
    )

    override fun getProvider(name: String): Provider? {
        return socialLoginManager.getProvider(name)
    }

    override fun loginWithProvider(
        name: String,
        scope: Collection<String>,
        origin: String,
        activity: Activity
    ) {
        return socialLoginManager.loginWithProvider(name, scope, origin, activity)
    }

    fun initialize(
        success: Success<List<Provider>> = {},
        failure: Failure<ReachFiveError> = {}
    ): ReachFive {
        reachFiveApi
            .clientConfig(mapOf("client_id" to sdkConfig.clientId))
            .enqueue(
                ReachFiveApiCallback<ClientConfigResponse>(
                    success = { clientConfig ->
                        defaultScope = clientConfig.scope.split(" ").toSet()
                        socialLoginManager.defaultScope = defaultScope
                        socialLoginManager.providersConfigs(success, failure)
                    },
                    failure = failure
                )
            )

        return this
    }

    fun logout(
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        socialLoginManager.providers.forEach { it.logout() }

        reachFiveApi
            .logout(SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
    }

    fun refreshAccessToken(
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

    fun exchangeCodeForToken(
        authorizationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val authCodeFlow = PkceAuthCodeFlow.readAuthCodeFlow(activity)
        return if (authCodeFlow != null) {
            val authCodeRequest = AuthCodeRequest(
                sdkConfig.clientId,
                authorizationCode,
                authCodeFlow.redirectUri,
                authCodeFlow.codeVerifier
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
            failure(ReachFiveError.from("No PKCE challenge found in memory."))
        }
    }

    fun loginCallback(
        tkn: String,
        scope: Collection<String>
    ) {
        redirectionActivityLauncher.loginCallback(activity, scope, tkn)
    }

    fun loginWithWeb(
        scope: Collection<String> = this.defaultScope,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
    ) {
        Log.d("SDK CORE", "ENTER LOGIN WITH WEB")

        redirectionActivityLauncher.loginWithWeb(activity, scope, state, nonce, origin)
    }

    fun onLoginCallbackResult(
        intent: Intent,
        resultCode: Int,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        when (resultCode) {
            RedirectionActivity.SUCCESS_RESULT_CODE -> {
                val code = intent.getStringExtra(CODE_KEY)!!
                val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)!!

                val authCodeRequest =
                    AuthCodeRequest(sdkConfig.clientId, code, sdkConfig.scheme, codeVerifier)

                reachFiveApi
                    .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                    .enqueue(
                        ReachFiveApiCallback(
                            success = { it.toAuthToken().fold(success, failure) },
                            failure = failure
                        )
                    )
            }
            NO_AUTH_ERROR_RESULT_CODE -> {
                failure(ReachFiveError("No authorization code found in activity result."))
            }
            ABORT_RESULT_CODE -> {
                Log.d(TAG, "The custom tab has been closed.")
                Unit
            }
            else -> {
                Log.e(TAG, "Unexpected event.")
                Unit
            }
        }
    }

    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val provider = socialLoginManager.providers.find { p -> p.requestCode == requestCode }
        if (provider != null) {
            provider.onActivityResult(requestCode, resultCode, data, success, failure)
        } else {
            failure(ReachFiveError.from("No provider found for this requestCode: $requestCode"))
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>
    ) {
        val provider = socialLoginManager.providers.find { p -> p.requestCode == requestCode }
        provider?.onRequestPermissionsResult(requestCode, permissions, grantResults, failure)
    }

    fun onStop() {
        socialLoginManager.providers.forEach { it.onStop() }
    }

    // TODO/cbu revise
    fun formatAuthorization(authToken: AuthToken): String {
        return "${authToken.tokenType} ${authToken.accessToken}"
    }
}
