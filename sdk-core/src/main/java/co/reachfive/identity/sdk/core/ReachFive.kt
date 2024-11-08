package co.reachfive.identity.sdk.core

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.requests.RevokeRequest
import co.reachfive.identity.sdk.core.models.responses.ClientConfigResponse
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success

class ReachFive private constructor(
    private val reachFiveApi: ReachFiveApi,
    private val passwordAuth: PasswordAuthClient,
    private val mfa: MfaClient,
    private val passwordlessAuth: PasswordlessAuthClient,
    private val profileManagement: ProfileManagementClient,
    private val socialLoginAuth: SocialLoginAuthClient,
    private val webauthnAuth: WebauthnAuthClient,
    private val credentialManagerAuth: CredentialManagerAuthClient,
    private val sessionUtils: SessionUtilsClient,
    override val sdkConfig: SdkConfig,
    override var defaultScope: Set<String> = emptySet(),
) :
    PasswordAuth by passwordAuth,
    MfaStepUp by mfa,
    MfaCredentials by mfa,
    MfaTrustedDevices by mfa,
    PasswordlessAuth by passwordlessAuth,
    ProfileManagement by profileManagement,
    SocialLoginAuth by socialLoginAuth,
    WebauthnAuth by webauthnAuth,
    CredentialManagerAuth by credentialManagerAuth,
    SessionUtils by sessionUtils {

    companion object {
        const val TAG = "Reach5"

        operator fun invoke(
            sdkConfig: SdkConfig,
            providersCreators: List<ProviderCreator>,
        ): ReachFive {
            val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)
            val webLauncher = RedirectionActivityLauncher(sdkConfig, reachFiveApi)
            val sessionUtils =
                SessionUtilsClient(reachFiveApi, sdkConfig, webLauncher)

            val passwordAuthClient = PasswordAuthClient(sdkConfig, reachFiveApi, sessionUtils)
            val passwordlessAuthClient = PasswordlessAuthClient(reachFiveApi, sdkConfig)
            val profileManagementClient = ProfileManagementClient(reachFiveApi)
            val socialLoginAuthClient =
                SocialLoginAuthClient(reachFiveApi, providersCreators, sessionUtils)
            val webauthnAuthClient = WebauthnAuthClient(reachFiveApi, sdkConfig, sessionUtils)
            val credentialManagerAuthClient =
                CredentialManagerAuthClient(reachFiveApi, sdkConfig, passwordAuthClient, sessionUtils)
            val mfaClient = MfaClient(sdkConfig, reachFiveApi, sessionUtils)

            return ReachFive(
                reachFiveApi,
                passwordAuthClient,
                mfaClient,
                passwordlessAuthClient,
                profileManagementClient,
                socialLoginAuthClient,
                webauthnAuthClient,
                credentialManagerAuthClient,
                sessionUtils,
                sdkConfig
            )
        }
    }

    fun initialize(
        success: Success<Unit> = {},
        failure: Failure<ReachFiveError> = {}
    ): ReachFive {
        reachFiveApi
            .clientConfig(mapOf("client_id" to sdkConfig.clientId))
            .enqueue(
                ReachFiveApiCallback.withContent<ClientConfigResponse>(
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
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        tokens: AuthToken? = null,
        ssoCustomTab: Activity? = null
    ) {
        val tokenToRevoke = tokens?.accessToken?.let { Pair(it, "access_token") }
            ?: tokens?.refreshToken?.let { Pair(it, "refresh_token") }
        val revokeCall = tokenToRevoke?.let { (token, hint) ->
            {
                Log.d(TAG, "logout revoke $hint")
                reachFiveApi.revokeTokens(RevokeRequest(sdkConfig.clientId, token, hint))
            }
        }

        val logoutCall = {
            Log.d(TAG, "logout WebView")
            reachFiveApi.logout(emptyMap())
        }

        val customTabCallback = {
            if (ssoCustomTab != null) {
                Log.d(TAG, "logout CustomTab")
                sessionUtils.logoutWithWeb(ssoCustomTab)
            }
            success(Unit)
        }

        revokeCall?.let {
            it().enqueue(ReachFiveApiCallback.noContent({
                logoutCall()
                    .enqueue(ReachFiveApiCallback.noContent({ customTabCallback() }, failure))
            }, failure))
        } ?: logoutCall()
            .enqueue(ReachFiveApiCallback.noContent({ customTabCallback() }, failure))

    }

    fun onAddNewWebAuthnDeviceResult(
        requestCode: Int,
        intent: Intent?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    ) {
        if (requestCode == WebauthnAuth.RC_REGISTER_DEVICE) {
            if (intent != null)
                webauthnAuth.onAddNewWebAuthnDeviceResult(intent, success, failure)
            else
                failure(ReachFiveError.NullIntent)
        } else logNotReachFiveRequestCode(requestCode)
    }

    fun onLoginActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity,
        origin: String?
    ) {
        when (requestCode) {
            WebauthnAuth.RC_LOGIN ->
                if (intent != null)
                    webauthnAuth.onLoginWithWebAuthnResult(
                        resultCode,
                        intent,
                        origin,
                        defaultScope,
                        success,
                        failure,
                    )
                else failure(ReachFiveError.NullIntent)

            WebauthnAuth.RC_SIGNUP -> {
                if (intent != null)
                    webauthnAuth.onSignupWithWebAuthnResult(
                        resultCode,
                        intent,
                        defaultScope,
                        origin,
                        success,
                        failure,
                        activity
                    )
                else failure(ReachFiveError.NullIntent)
            }

            else ->
                if (RedirectionActivity.isLoginRequestCode(requestCode)) {
                    if (resultCode == RESULT_CANCELED)
                        failure(ReachFiveError.WebFlowCanceled)
                    else if (intent != null)
                        sessionUtils.handleAuthorizationCompletion(intent, success, failure)
                    else
                        failure(ReachFiveError.NullIntent)
                } else if (socialLoginAuth.isSocialLoginRequestCode(requestCode)) {
                    socialLoginAuth.onActivityResult(
                        requestCode,
                        resultCode,
                        intent,
                        success,
                        failure
                    )
                } else logNotReachFiveRequestCode(requestCode)

        }
    }

    fun onLogoutResult(
        requestCode: Int,
        intent: Intent?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    ) {
        if (requestCode == RedirectionActivity.RC_WEBLOGOUT)
            if (intent != null)
                success(Unit)
            else
                failure(ReachFiveError.NullIntent)
        else logNotReachFiveRequestCode(requestCode)
    }

    fun resolveResultHandler(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ): ActivityResultHandler? {
        return if (isReachFiveLoginRequestCode(requestCode))
            LoginResultHandler(this, requestCode, resultCode, intent)
        else if (isReachFiveLogoutRequestCode(requestCode))
            LogoutResultHandler(this, requestCode, intent)
        else if (WebauthnAuth.isWebauthnActionRequestCode(requestCode)) {
            if (WebauthnAuth.RC_REGISTER_DEVICE == requestCode)
                WebAuthnDeviceAddResult(this, requestCode, intent)
            else null
        } else null
    }

    fun isReachFiveLoginRequestCode(code: Int): Boolean =
        socialLoginAuth.isSocialLoginRequestCode(code) ||
                WebauthnAuth.isWebauthnLoginRequestCode(code) ||
                RedirectionActivity.isLoginRequestCode(code)

    fun isReachFiveActionRequestCode(code: Int): Boolean =
        WebauthnAuth.isWebauthnActionRequestCode(code)

    fun isReachFiveLogoutRequestCode(code: Int): Boolean =
        RedirectionActivity.isLogoutRequestCode(code)

    private fun logNotReachFiveRequestCode(code: Int) {
        Log.d(TAG, "Request code $code does not match any ReachFive actions.")
    }
}
