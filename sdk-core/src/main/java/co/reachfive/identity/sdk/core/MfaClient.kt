package co.reachfive.identity.sdk.core

import android.app.Activity
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.CredentialMfaType
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.requests.MfaCredentialsStartEmailRegisteringRequest
import co.reachfive.identity.sdk.core.models.requests.MfaCredentialsStartPhoneRegisteringRequest
import co.reachfive.identity.sdk.core.models.requests.MfaCredentialsVerifyPhoneRegisteringRequest
import co.reachfive.identity.sdk.core.models.requests.StartMfaPasswordlessRequest
import co.reachfive.identity.sdk.core.models.requests.StartStepUpRequest
import co.reachfive.identity.sdk.core.models.requests.VerifyEmailRequest
import co.reachfive.identity.sdk.core.models.requests.VerifyMfaPasswordlessRequest
import co.reachfive.identity.sdk.core.models.responses.ListMfaCredentials
import co.reachfive.identity.sdk.core.models.responses.StartMfaPasswordlessResponse
import co.reachfive.identity.sdk.core.models.responses.StepUpResponse
import co.reachfive.identity.sdk.core.models.responses.VerifyMfaPassordlessResponse
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.formatScope

internal class MfaClient(
    private val sdkConfig: SdkConfig,
    private val reachFiveApi: ReachFiveApi,
    private val sessionUtils: SessionUtilsClient,
) : MfaStepUp, MfaCredentials {
    override var defaultScope: Set<String> = emptySet()

    override fun startMfaPhoneNumberRegistration(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.startMfaPhoneNumberRegistration(
            authToken.authHeader,
            MfaCredentialsStartPhoneRegisteringRequest(phoneNumber)
        )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun verifyMfaPhoneNumberRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.verifyMfaPhoneNumberRegistration(
            authToken.authHeader,
            MfaCredentialsVerifyPhoneRegisteringRequest(verificationCode)
        )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun startMfaEmailRegistration(
        authToken: AuthToken,
        redirectUri: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.startMfaEmailRegistration(
            authToken.authHeader,
            MfaCredentialsStartEmailRegisteringRequest(redirectUri)
        )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun verifyMfaEmailRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.verifyMfaEmailRegistration(
            authToken.authHeader,
            VerifyEmailRequest(verificationCode)
        )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun listMfaCredentials(
        authToken: AuthToken,
        success: Success<ListMfaCredentials>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .listMfaCredentials(authToken.authHeader)
            .enqueue(ReachFiveApiCallback.withContent<ListMfaCredentials>(success, failure))
    }

    override fun startStepUp(
        authToken: AuthToken,
        authType: CredentialMfaType,
        redirectUri: String,
        scope: Collection<String>,
        success: Success<StartMfaPasswordlessResponse>,
        failure: Failure<ReachFiveError>,
        activity: Activity,
        origin: String?
    ) {
        PkceAuthCodeFlow.generate(redirectUri).let { pkce ->
            PkceAuthCodeFlow.storeAuthCodeFlow(pkce, activity)
            reachFiveApi
                .getMfaStepUpToken(
                    authToken.authHeader,
                    StartStepUpRequest
                        (
                        clientId = sdkConfig.clientId,
                        redirectUri = redirectUri,
                        codeChallenge = pkce.codeChallenge,
                        codeChallengeMethod = pkce.codeChallengeMethod,
                        scope = formatScope(scope)
                    )
                )
                .enqueue(
                    ReachFiveApiCallback.withContent<StepUpResponse>(
                        success = { stepUpResponse ->
                            reachFiveApi
                                .startMfaPasswordless(
                                    StartMfaPasswordlessRequest(
                                        authType = authType,
                                        redirectUri = redirectUri,
                                        clientId = sdkConfig.clientId,
                                        stepUp = stepUpResponse.token,
                                        origin = origin,
                                    )
                                )
                                .enqueue(
                                    ReachFiveApiCallback.withContent<StartMfaPasswordlessResponse>(
                                        success = success,
                                        failure = failure
                                    )
                                )

                        },
                        failure = failure
                    )
                )
        }
    }

    override fun endStepUp(
        challengeId: String,
        verificationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity,
        trustDevice: Boolean?
    ) {
        reachFiveApi
            .verifyMfaPasswordless(
                VerifyMfaPasswordlessRequest(
                    challengeId,
                    verificationCode,
                    trustDevice
                )
            )
            .enqueue(
                ReachFiveApiCallback.withContent<VerifyMfaPassordlessResponse>(
                    success = {
                        sessionUtils.exchangeCodeForToken(
                            it.authCode,
                            success = success,
                            failure = failure,
                            activity = activity
                        )

                    },
                    failure = failure
                )
            )
    }

}

internal interface MfaCredentials {

    fun startMfaPhoneNumberRegistration(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    )

    fun verifyMfaPhoneNumberRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    )

    fun startMfaEmailRegistration(
        authToken: AuthToken,
        redirectUri: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    )

    fun verifyMfaEmailRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    )

    fun listMfaCredentials(
        authToken: AuthToken,
        success: Success<ListMfaCredentials>,
        failure: Failure<ReachFiveError>
    )
}

internal interface MfaStepUp {
    var defaultScope: Set<String>

    fun startStepUp(
        authToken: AuthToken,
        authType: CredentialMfaType,
        redirectUri: String,
        scope: Collection<String> = defaultScope,
        success: Success<StartMfaPasswordlessResponse>,
        failure: Failure<ReachFiveError>,
        activity: Activity,
        origin: String? = null
    )

    fun endStepUp(
        challengeId: String,
        verificationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity,
        trustDevice: Boolean? = null
    )
}