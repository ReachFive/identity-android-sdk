package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_KEY
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_VERIFIER_KEY
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

class ReachFive private constructor(
    private val reachFiveApi: ReachFiveApi,
    private val passwordAuth: PasswordAuthClient,
    private val passwordlessAuth: PasswordlessAuthClient,
    private val profileManagement: ProfileManagementClient,
    private val socialLoginAuth: SocialLoginAuthClient,
    private val webauthnAuth: WebauthnAuthClient,
    private val sessionUtils: SessionUtilsClient,
    override val sdkConfig: SdkConfig,
    override var defaultScope: Set<String> = emptySet(),
) :
    PasswordAuth by passwordAuth,
    PasswordlessAuth by passwordlessAuth,
    ProfileManagement by profileManagement,
    SocialLoginAuth by socialLoginAuth,
    WebauthnAuth by webauthnAuth,
    SessionUtils by sessionUtils {

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
            val sessionUtils = SessionUtilsClient(reachFiveApi, sdkConfig, webLauncher, socialLoginAuthClient, activity)
            val webauthnAuthClient =
                WebauthnAuthClient(reachFiveApi, sdkConfig, activity, sessionUtils)

            return ReachFive(
                reachFiveApi,
                passwordAuthClient,
                passwordlessAuthClient,
                profileManagementClient,
                socialLoginAuthClient,
                webauthnAuthClient,
                sessionUtils,
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

    private fun onLoginCallbackResult(
        intent: Intent,
        resultCode: Int,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        when (resultCode) {
            LoginResult.SUCCESS.code -> {
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

            LoginResult.NO_AUTHORIZATION_CODE.code -> {
                failure(ReachFiveError("No authorization code found in activity result."))
            }

            LoginResult.UNEXPECTED_ERROR.code ->
                failure(ReachFiveError("Unexpected error during login callback."))
        }
    }


    fun onWebauthnDeviceAddResult(
        requestCode: Int,
        intent: Intent?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    ) {
        if (requestCode == WebauthnAuth.REGISTER_DEVICE_REQUEST_CODE) {
            if (intent != null)
                webauthnAuth.onAddNewWebAuthnDeviceResult(intent, success, failure)
            else
                failure(ReachFiveError.NoIntent)
        }
    }

    fun onLoginActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
    ) {
        when (requestCode) {
            RedirectionActivity.REDIRECTION_REQUEST_CODE -> {
                if (intent != null)
                    this.onLoginCallbackResult(intent, resultCode, success, failure)
                else
                    failure(ReachFiveError.NoIntent)
            }

            WebauthnAuth.LOGIN_REQUEST_CODE -> {
                if (intent != null)
                    webauthnAuth.onLoginWithWebAuthnResult(
                        resultCode,
                        intent,
                        defaultScope,
                        failure,
                    )
                else
                    failure(ReachFiveError.NoIntent)
            }

            WebauthnAuth.SIGNUP_REQUEST_CODE -> {
                if (intent != null)
                    webauthnAuth.onSignupWithWebAuthnResult(
                        resultCode,
                        intent,
                        defaultScope,
                        failure,
                    )
                else
                    failure(ReachFiveError.NoIntent)
            }

            else ->
                if (socialLoginAuth.isSocialLoginRequestCode(requestCode)) {
                    socialLoginAuth.onActivityResult(
                        requestCode,
                        resultCode,
                        intent,
                        success,
                        failure
                    )
                } else Log.d(
                    TAG,
                    "Request code ${requestCode} does not match any ReachFive actions."
                )

        }
    }

    fun resolveResultHandler(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ): ActivityResultHandler? {
        if (isReachFiveLoginRequestCode(requestCode))
            return LoginResultHandler(this, requestCode, resultCode, intent)
        else if (WebauthnAuth.isWebauthnActionRequestCode(requestCode))
            return WebauthnActionHandler(this, requestCode, intent)
        else return null
    }

    fun isReachFiveLoginRequestCode(code: Int): Boolean =
        socialLoginAuth.isSocialLoginRequestCode(code) ||
                WebauthnAuth.isWebauthnLoginRequestCode(code) ||
                RedirectionActivity.isRedirectionActivityRequestCode(code)

    fun isReachFiveActionRequestCode(code: Int): Boolean =
        WebauthnAuth.isWebauthnActionRequestCode(code)
}
