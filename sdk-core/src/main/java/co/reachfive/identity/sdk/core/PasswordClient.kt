package co.reachfive.identity.sdk.core

import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.*
import co.reachfive.identity.sdk.core.models.responses.AuthenticationToken
import co.reachfive.identity.sdk.core.models.responses.TokenEndpointResponse
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.formatScope

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
        success: Success<AuthToken>,
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
                    success = { it.toAuthToken().fold(success, failure) },
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
                        sessionUtils.loginCallback(it.tkn, scope, success, failure, origin)
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
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .requestPasswordReset(
                RequestPasswordResetRequest(
                    sdkConfig.clientId,
                    email,
                    redirectUrl,
                    phoneNumber
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
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    )

    fun loginWithPassword(
        email: String? = null,
        phoneNumber: String? = null,
        customIdentifier: String? = null,
        password: String,
        scope: Collection<String> = defaultScope,
        origin: String? = null,
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
        failure: Failure<ReachFiveError>
    )

    fun requestAccountRecovery(
        email: String? = null,
        phoneNumber: String? = null,
        redirectUrl: String? = null,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    )
}