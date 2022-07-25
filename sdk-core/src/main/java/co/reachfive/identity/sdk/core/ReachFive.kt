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
import co.reachfive.identity.sdk.core.models.responses.ClientConfigResponse
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.SuccessWithNoContent


class ReachFive private constructor(
    private val reachFiveApi: ReachFiveApi,
    private val passwordAuth: PasswordAuthClient,
    private val passwordlessAuth: PasswordlessAuthClient,
    private val profileManagement: ProfileManagementClient,
    private val socialLoginAuth: SocialLoginAuthClient,
    private val webauthnAuth: WebauthnAuthClient,
    private val oAuthClient: ReachFiveOAuthClient,
    override val sdkConfig: SdkConfig,
    override var defaultScope: Set<String> = emptySet(),
) :
    PasswordAuth by passwordAuth,
    PasswordlessAuth by passwordlessAuth,
    ProfileManagement by profileManagement,
    SocialLoginAuth by socialLoginAuth,
    WebauthnAuth by webauthnAuth,
    ReachFiveOAuth by oAuthClient {

    companion object {
        const val TAG = "Reach5"

        operator fun invoke(
            activity: Activity,
            sdkConfig: SdkConfig,
            providersCreators: List<ProviderCreator>,
        ): ReachFive {
            val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)
            val webLauncher = RedirectionActivityLauncher(sdkConfig, reachFiveApi)

            val passwordAuthClient = PasswordAuthClient(sdkConfig, reachFiveApi)
            val passwordlessAuthClient = PasswordlessAuthClient(activity, reachFiveApi, sdkConfig)
            val profileManagementClient = ProfileManagementClient(reachFiveApi)
            val socialLoginAuthClient =
                SocialLoginAuthClient(reachFiveApi, activity, sdkConfig, providersCreators)
            val oauthClient = ReachFiveOAuthClient(reachFiveApi, sdkConfig, webLauncher, activity)
            val webauthnAuthClient =
                WebauthnAuthClient(reachFiveApi, sdkConfig, activity, oauthClient)

            return ReachFive(
                reachFiveApi,
                passwordAuthClient,
                passwordlessAuthClient,
                profileManagementClient,
                socialLoginAuthClient,
                webauthnAuthClient,
                oauthClient,
                sdkConfig,
            )
        }
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
                        socialLoginAuth.providersConfigs(success, failure)
                    },
                    failure = failure
                )
            )

        return this
    }

    fun onStop() = socialLoginAuth.onStop()

    fun logout(
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
        loginSuccessHandler: Success<AuthToken>,
        webAuthnSuccessHandler: Success<Unit>, // TODO/cbu there is another way
        failure: Failure<ReachFiveError>
    ) {
        when (requestCode) {
            WebauthnAuth.LOGIN_REQUEST_CODE -> {
                if (data != null)
                    webauthnAuth.onLoginWithWebAuthnResult(resultCode, data, defaultScope, failure)
                else
                    failure(ReachFiveError.from(""))
            }

            WebauthnAuth.SIGNUP_REQUEST_CODE -> {
                if (data != null)
                    webauthnAuth.onSignupWithWebAuthnResult(resultCode, data, defaultScope, failure)
                else
                    failure(ReachFiveError.from(""))
            }

            WebauthnAuth.REGISTER_DEVICE_REQUEST_CODE -> {
                if (data != null)
                    webauthnAuth.onAddNewWebAuthnDeviceResult(data, webAuthnSuccessHandler, failure)
                else
                    failure(ReachFiveError.from(""))
            }

            RedirectionActivity.REDIRECTION_REQUEST_CODE -> {
                if (data != null)
                    this.onLoginCallbackResult(data, resultCode, loginSuccessHandler, failure)
                else
                    failure(ReachFiveError.from(""))
            }

            else -> socialLoginAuth.onActivityResult(
                requestCode,
                resultCode,
                data,
                loginSuccessHandler,
                failure
            )
        }
    }
}
