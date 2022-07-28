package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
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
            sdkConfig: SdkConfig,
            providersCreators: List<ProviderCreator>,
        ): ReachFive {
            val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)

            val passwordAuthClient = PasswordAuthClient(sdkConfig, reachFiveApi)
            val passwordlessAuthClient = PasswordlessAuthClient(reachFiveApi, sdkConfig)
            val profileManagementClient = ProfileManagementClient(reachFiveApi)
            val sessionUtils = SessionUtilsClient(reachFiveApi, sdkConfig)
            val socialLoginAuthClient =
                SocialLoginAuthClient(reachFiveApi, sdkConfig, sessionUtils, providersCreators)
            val webauthnAuthClient =
                WebauthnAuthClient(reachFiveApi, sdkConfig, sessionUtils)

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
        success: SuccessWithNoContent<Unit> = {},
        failure: Failure<ReachFiveError> = {}
    ): ReachFive {
        reachFiveApi
            .clientConfig(mapOf("client_id" to sdkConfig.clientId))
            .enqueue(
                ReachFiveApiCallback<ClientConfigResponse>(
                    success = { clientConfig ->
                        defaultScope = clientConfig.scope.split(" ").toSet()
                        success(Unit)
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
        sessionUtils.logout(successWithNoContent, failure)
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
        activity: Activity
    ) {
        when (requestCode) {
            SessionUtilsClient.REDIRECTION_REQUEST_CODE ->
                if (intent != null) sessionUtils.handleAuthorizationCompletion(
                    intent,
                    success,
                    failure
                )
                else failure(ReachFiveError.NoIntent)

            WebauthnAuth.LOGIN_REQUEST_CODE ->
                if (intent != null)
                    webauthnAuth.onLoginWithWebAuthnResult(
                        resultCode,
                        intent,
                        defaultScope,
                        failure,
                        activity
                    )
                else failure(ReachFiveError.NoIntent)

            WebauthnAuth.SIGNUP_REQUEST_CODE -> {
                if (intent != null)
                    webauthnAuth.onSignupWithWebAuthnResult(
                        resultCode,
                        intent,
                        defaultScope,
                        failure,
                        activity
                    )
                else failure(ReachFiveError.NoIntent)
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
                SessionUtilsClient.isRedirectionRequestCode(code)

    fun isReachFiveActionRequestCode(code: Int): Boolean =
        WebauthnAuth.isWebauthnActionRequestCode(code)
}
