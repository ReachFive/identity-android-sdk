package co.reachfive.identity.sdk.core

import android.app.Activity
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.requests.MfaCredentialsStartEmailRegisteringRequest
import co.reachfive.identity.sdk.core.models.requests.MfaCredentialsStartPhoneRegisteringRequest
import co.reachfive.identity.sdk.core.models.requests.MfaCredentialsVerifyPhoneRegisteringRequest
import co.reachfive.identity.sdk.core.models.requests.VerifyEmailRequest
import co.reachfive.identity.sdk.core.models.responses.ListMfaCredentials
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success

internal class MfaCredentialsClient(
    private val reachFiveApi: ReachFiveApi,
): MfaCredentials {
    override fun startMfaPhoneNumberRegistration(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.startMfaPhoneNumberRegistration(authToken.authHeader, MfaCredentialsStartPhoneRegisteringRequest(phoneNumber))
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun verifyMfaPhoneNumberRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.verifyMfaPhoneNumberRegistration(authToken.authHeader, MfaCredentialsVerifyPhoneRegisteringRequest(verificationCode))
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun startMfaEmailRegistration(
        authToken: AuthToken,
        redirectUri: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.startMfaEmailRegistration(authToken.authHeader, MfaCredentialsStartEmailRegisteringRequest(redirectUri))
            .enqueue(ReachFiveApiCallback.noContent(success, failure))
    }

    override fun verifyMfaEmailRegistration(
        authToken: AuthToken,
        verificationCode: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.verifyMfaEmailRegistration(authToken.authHeader, VerifyEmailRequest(verificationCode))
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