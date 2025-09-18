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
import co.reachfive.identity.sdk.core.models.requests.MfaRemovePhoneNumberRequest
import co.reachfive.identity.sdk.core.models.requests.StartMfaPasswordlessRequest
import co.reachfive.identity.sdk.core.models.requests.StartStepUpAuthTokenFlow
import co.reachfive.identity.sdk.core.models.requests.StartStepUpFlow
import co.reachfive.identity.sdk.core.models.requests.StartStepUpLoginFlow
import co.reachfive.identity.sdk.core.models.requests.StartStepUpRequest
import co.reachfive.identity.sdk.core.models.requests.VerifyMfaEmailRequest
import co.reachfive.identity.sdk.core.models.requests.VerifyMfaPasswordlessRequest
import co.reachfive.identity.sdk.core.models.responses.ListMfaCredentials
import co.reachfive.identity.sdk.core.models.responses.ListMfaTrustedDevices
import co.reachfive.identity.sdk.core.models.responses.StartMfaPasswordlessResponse
import co.reachfive.identity.sdk.core.models.responses.VerifyMfaPassordlessResponse
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.formatScope

internal class MfaClient(
    private val sdkConfig: SdkConfig,
    private val reachFiveApi: ReachFiveApi,
    private val sessionUtils: SessionUtilsClient,
) : MfaStepUp, MfaCredentials, MfaTrustedDevices {
    override var defaultScope: Set<String> = emptySet()

    override fun startMfaPhoneNumberRegistration(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        action: String?,
        trustDevice: Boolean
    ) {
        reachFiveApi.startMfaPhoneNumberRegistration(
            authToken.authHeader,
            MfaCredentialsStartPhoneRegisteringRequest(phoneNumber, action, trustDevice)
        )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun verifyMfaPhoneNumberRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        trustDevice: Boolean
    ) {
        reachFiveApi.verifyMfaPhoneNumberRegistration(
            authToken.authHeader,
            MfaCredentialsVerifyPhoneRegisteringRequest(verificationCode, trustDevice)
        )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun startMfaEmailRegistration(
        authToken: AuthToken,
        redirectUri: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        action: String?,
        trustDevice: Boolean
    ) {
        reachFiveApi.startMfaEmailRegistration(
            authToken.authHeader,
            MfaCredentialsStartEmailRegisteringRequest(redirectUri, action, trustDevice)
        )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun verifyMfaEmailRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        trustDevice: Boolean
    ) {
        reachFiveApi.verifyMfaEmailRegistration(
            authToken.authHeader,
            VerifyMfaEmailRequest(verificationCode, trustDevice)
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

    override fun removeMfaEmail(
        authToken: AuthToken,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .deleteMfaEmail(authToken.authHeader)
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun removeMfaPhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .deleteMfaPhoneNumber(authToken.authHeader, MfaRemovePhoneNumberRequest(phoneNumber))
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun startStepUp(
        startStepUpFlow: StartStepUpFlow,
        authType: CredentialMfaType,
        redirectUri: String,
        scope: Collection<String>,
        success: Success<StartMfaPasswordlessResponse>,
        failure: Failure<ReachFiveError>,
        origin: String?
    ) {
        when (startStepUpFlow) {
            is StartStepUpLoginFlow -> achieveStartStepUp(
                authType,
                redirectUri,
                startStepUpFlow.stepUpToken,
                origin,
                success,
                failure
            )

            is StartStepUpAuthTokenFlow -> PkceAuthCodeFlow.generate(redirectUri).let { pkce ->
                PkceAuthCodeFlow.storeAuthCodeFlow(pkce, startStepUpFlow.activity)
                reachFiveApi
                    .getMfaStepUpToken(
                        mapOf("Authorization" to startStepUpFlow.authToken.authHeader),
                        StartStepUpRequest
                            (
                            clientId = sdkConfig.clientId,
                            redirectUri = redirectUri,
                            codeChallenge = pkce.codeChallenge,
                            codeChallengeMethod = pkce.codeChallengeMethod,
                            scope = formatScope(scope),
                            action = startStepUpFlow.action
                        )
                    )
                    .enqueue(
                        ReachFiveApiCallback.withContent<AuthToken>(
                            success = { stepUpResponse ->
                                achieveStartStepUp(
                                    authType,
                                    redirectUri,
                                    stepUpResponse.stepUpToken,
                                    origin,
                                    success,
                                    failure
                                )

                            },
                            failure = failure
                        )
                    )
            }
        }
    }

    private fun achieveStartStepUp(
        authType: CredentialMfaType,
        redirectUri: String,
        stepUpToken: String?,
        origin: String?,
        success: Success<StartMfaPasswordlessResponse>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .startMfaPasswordless(
                StartMfaPasswordlessRequest(
                    authType = authType,
                    redirectUri = redirectUri,
                    clientId = sdkConfig.clientId,
                    stepUp = stepUpToken!!,
                    origin = origin
                )
            ).enqueue(
                ReachFiveApiCallback.withContent<StartMfaPasswordlessResponse>(
                    success = success,
                    failure = failure
                )
            )
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

    override fun listMfaTrustedDevices(
        authToken: AuthToken,
        success: Success<ListMfaTrustedDevices>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .listMfaTrustedDevices(authToken.authHeader)
            .enqueue(
                ReachFiveApiCallback.withContent<ListMfaTrustedDevices>(
                    success = success,
                    failure = failure
                )
            )
    }

    override fun removeMfaTrustedDevice(
        authToken: AuthToken,
        trustedDeviceId: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .deleteMfaTrustedDevice(
                authToken.authHeader,
                trustedDeviceId,
            ).enqueue(
                ReachFiveApiCallback.noContent(
                    success = success,
                    failure = failure
                )
            )
    }

}

internal interface MfaTrustedDevices {
    fun listMfaTrustedDevices(
        authToken: AuthToken,
        success: Success<ListMfaTrustedDevices>,
        failure: Failure<ReachFiveError>
    )

    fun removeMfaTrustedDevice(
        authToken: AuthToken,
        trustedDeviceId: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    )
}

internal interface MfaCredentials {

    fun startMfaPhoneNumberRegistration(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        action: String? = null,
        trustDevice: Boolean = false,
    )

    fun verifyMfaPhoneNumberRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        trustDevice: Boolean = false,
    )

    fun startMfaEmailRegistration(
        authToken: AuthToken,
        redirectUri: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        action: String? = null,
        trustDevice: Boolean = false,
        )

    fun verifyMfaEmailRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        trustDevice: Boolean = false,
        )

    fun listMfaCredentials(
        authToken: AuthToken,
        success: Success<ListMfaCredentials>,
        failure: Failure<ReachFiveError>
    )

    fun removeMfaPhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    )

    fun removeMfaEmail(
        authToken: AuthToken,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    )
}

internal interface MfaStepUp {
    var defaultScope: Set<String>

    fun startStepUp(
        startStepUpFlow: StartStepUpFlow,
        authType: CredentialMfaType,
        redirectUri: String,
        scope: Collection<String> = defaultScope,
        success: Success<StartMfaPasswordlessResponse>,
        failure: Failure<ReachFiveError>,
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