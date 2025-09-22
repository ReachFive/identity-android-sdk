package co.reachfive.identity.sdk.core

import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.*
import co.reachfive.identity.sdk.core.models.responses.AuthenticationToken
import co.reachfive.identity.sdk.core.models.responses.SignupResponse
import co.reachfive.identity.sdk.core.models.responses.TokenEndpointResponse
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.formatScope
import com.github.kittinunf.result.map
import com.github.kittinunf.result.success

internal class PasswordAuthClient(
    private val sdkConfig: SdkConfig,
    private val reachFiveApi: ReachFiveApi,
    private val sessionUtils: SessionUtilsClient,
) : PasswordAuth {
    override var defaultScope: Set<String> = emptySet()

    override fun signup(
        profile: ProfileSignupRequest,
        scope: Collection<String>,
        redirectUrl: String?,
        origin: String?,
        success: Success<SignupResponse>,
        failure: Failure<ReachFiveError>
    ) {
        val signupRequest = SignupRequest(
            clientId = sdkConfig.clientId,
            data = profile,
            redirectUrl = redirectUrl,
            scope = formatScope(scope),
            origin = origin
        )
        reachFiveApi
            .signup(signupRequest, SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback.withContent<TokenEndpointResponse>(
                    success = {
                        if(it.accessToken != null ) {
                            it.toAuthToken().map { el -> SignupResponse.AchievedLogin(el) }.fold(success, failure)
                        } else {
                            SignupResponse.AwaitingIdentifierVerification.success().fold(success, failure)
                        }
                    },
                    failure = failure
                )
            )
    }

    /**
     * @param username You can use email or phone number
     */
    override fun loginWithPassword(
        email: String?,
        phoneNumber: String?,
        customIdentifier: String?,
        password: String,
        scope: Collection<String>,
        origin: String?,
        mfaConf: LoginMfaConf?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val loginRequest = LoginRequest(
            clientId = sdkConfig.clientId,
            email = email,
            phoneNumber = phoneNumber,
            customIdentifier = customIdentifier,
            password = password,
            origin = origin,
            scope = formatScope(scope)
        )
        reachFiveApi
            .loginWithPassword(loginRequest, SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback.withContent<AuthenticationToken>(
                    success = {
                        if(it.mfaRequired == true) {
                            if(mfaConf == null) {
                                failure(ReachFiveError.from("An activity is required to achieve a login with password."))
                            } else {
                                PkceAuthCodeFlow.generate(sdkConfig.scheme).let { pkce ->
                                    PkceAuthCodeFlow.storeAuthCodeFlow(pkce, mfaConf.activity)
                                    reachFiveApi
                                        .getMfaStepUpToken(
                                            mapOf(),
                                            StartStepUpRequest(
                                                clientId = sdkConfig.clientId,
                                                redirectUri = sdkConfig.scheme,
                                                codeChallenge = pkce.codeChallenge,
                                                codeChallengeMethod = pkce.codeChallengeMethod,
                                                scope = formatScope(scope),
                                                tkn = it.tkn
                                            )
                                        ).enqueue(ReachFiveApiCallback.withContent<AuthToken>(success = success, failure = failure))
                                }
                            }
                        } else {
                            sessionUtils.loginCallback(it.tkn, scope, success, failure, origin)
                        }
                    },
                    failure = failure
                )
            )
    }

    override fun updatePassword(
        updatePasswordRequest: UpdatePasswordRequest,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        val headers = UpdatePasswordRequest.getAccessToken(updatePasswordRequest)
            ?.let { mapOf("Authorization" to it.authHeader) }
            ?: emptyMap()

        reachFiveApi
            .updatePassword(
                headers,
                UpdatePasswordRequest.enrichWithClientId(updatePasswordRequest, sdkConfig.clientId),
                SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun requestPasswordReset(
        email: String?,
        redirectUrl: String?,
        phoneNumber: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        origin: String?
    ) {
        reachFiveApi
            .requestPasswordReset(
                RequestPasswordResetRequest(
                    sdkConfig.clientId,
                    email,
                    redirectUrl,
                    phoneNumber,
                    origin
                ),
                SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun requestAccountRecovery(
        email: String?,
        phoneNumber: String?,
        redirectUrl: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .requestAccountRecovery(
                AccountRecoveryRequest(
                    sdkConfig.clientId,
                    email,
                    phoneNumber,
                    redirectUrl,
                ),
                SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }
}

internal interface PasswordAuth {
    var defaultScope: Set<String>

    fun signup(
        profile: ProfileSignupRequest,
        scope: Collection<String> = defaultScope,
        redirectUrl: String? = null,
        origin: String? = null,
        success: Success<SignupResponse>,
        failure: Failure<ReachFiveError>
    )

    fun loginWithPassword(
        email: String? = null,
        phoneNumber: String? = null,
        customIdentifier: String? = null,
        password: String,
        scope: Collection<String> = defaultScope,
        origin: String? = null,
        mfaConf: LoginMfaConf? = null,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    )

    fun updatePassword(
        updatePasswordRequest: UpdatePasswordRequest,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    )

    fun requestPasswordReset(
        email: String? = null,
        redirectUrl: String? = null,
        phoneNumber: String? = null,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        origin: String? = null
    )

    fun requestAccountRecovery(
        email: String? = null,
        phoneNumber: String? = null,
        redirectUrl: String? = null,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    )
}